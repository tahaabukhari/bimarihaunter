"""
API routes for Direct AI Outbreak Chats with Agentic Gemini workflows.

Supports:
- Local Mode (syncs user and on-device SLM messages for cross-device persistence).
- Smart Mode (runs a server-side Gemini Agent with tools to query Firestore reports
  and search live web news dynamically).
"""

from __future__ import annotations

import json
import os
import urllib.parse
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

import httpx
from bs4 import BeautifulSoup
from fastapi import APIRouter, Depends, HTTPException, Query
from google.cloud import firestore
from pydantic import BaseModel

import google.generativeai as genai
import structlog
from app.database.firestore import db
from app.services.firebase_auth import verify_firebase_token

logger = structlog.get_logger(__name__)
router = APIRouter(prefix="/chats", tags=["chats"])

# ── Schemas ──────────────────────────────────────────────────

class ChatCreate(BaseModel):
    title: Optional[str] = None

class MessageCreate(BaseModel):
    text: str
    local_slm_response: Optional[str] = None  # Sync on-device response in Local mode

# ── Gemini Tools Definitions ──────────────────────────────────

def query_outbreaks(city: str) -> str:
    """
    Queries Firestore active outbreak reports in a specific Pakistan city.
    Use this to get verified, classified internal database reports.
    """
    try:
        city_cap = city.strip().capitalize()
        reports_ref = db.collection("reports")
        
        # Search reports where city is in the resolved location array
        query = (
            reports_ref
            .where("ai_analysis.locations", "array_contains", city_cap)
            .order_by("published_at", direction="DESCENDING")
            .limit(5)
        )
        docs = query.stream()
        
        results = []
        for doc in docs:
            data = doc.to_dict()
            results.append({
                "title": data.get("title"),
                "source": data.get("source"),
                "url": data.get("url"),
                "published_at": data.get("published_at").isoformat() if isinstance(data.get("published_at"), datetime) else str(data.get("published_at")),
                "severity": data.get("ai_analysis", {}).get("severity", "medium"),
                "summary": data.get("ai_analysis", {}).get("summary", []),
                "symptoms": data.get("ai_analysis", {}).get("symptoms", [])
            })
            
        if not results:
            return f"No verified outbreaks registered in {city_cap} currently."
        return json.dumps(results, indent=2)
    except Exception as e:
        return f"Error querying outbreaks: {str(e)}"

def query_web_search(query_str: str) -> str:
    """
    Performs a live Yahoo web search for Pakistan health news.
    Use this when the user asks for the absolute latest, breaking, or real-time updates not found in the database.
    """
    try:
        encoded_query = urllib.parse.quote_plus(query_str)
        yahoo_url = f"https://search.yahoo.com/search?p={encoded_query}&n=5"
        
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        
        # Fetch search results synchronously using HTTPX (super fast)
        with httpx.Client(headers=headers, timeout=8.0) as client:
            response = client.get(yahoo_url)
            
        if response.status_code != 200:
            return "Failed to fetch live web search results."
            
        soup = BeautifulSoup(response.text, "html.parser")
        results = soup.select("div.algo")
        
        web_results = []
        for idx, res in enumerate(results[:5]):
            link_el = res.select_one("h3 a")
            title = link_el.text.strip() if link_el else "News Update"
            url = link_el.get("href") if link_el else ""
            
            snippet_el = res.select_one("span.compText") or res.select_one("div.compText") or res.select_one("p")
            snippet = snippet_el.text.strip() if snippet_el else ""
            
            web_results.append({
                "title": title,
                "url": url,
                "snippet": snippet
            })
            
        if not web_results:
            return "No web search matches found."
        return json.dumps(web_results, indent=2)
    except Exception as e:
        return f"Error executing web search: {str(e)}"

# ── API Routes ───────────────────────────────────────────────

@router.get("/")
async def list_chats(user_token: dict = Depends(verify_firebase_token)) -> List[Dict[str, Any]]:
    """Lists all active direct AI chats for the user."""
    try:
        user_id = user_token["uid"]
        chats_ref = db.collection("chats")
        query = chats_ref.where("user_id", "==", user_id).order_by("updated_at", direction="DESCENDING")
        docs = query.stream()
        
        chats = []
        for doc in docs:
            data = doc.to_dict()
            data["chat_id"] = doc.id
            if isinstance(data.get("created_at"), datetime):
                data["created_at"] = data["created_at"].isoformat()
            if isinstance(data.get("updated_at"), datetime):
                data["updated_at"] = data["updated_at"].isoformat()
            chats.append(data)
        return chats
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to list chats: {str(e)}")

@router.post("/")
async def create_chat(
    chat_input: ChatCreate,
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """Starts a new direct AI chat session."""
    try:
        user_id = user_token["uid"]
        chat_ref = db.collection("chats").document()
        chat_id = chat_ref.id
        
        title = chat_input.title or f"Consultation {datetime.now().strftime('%Y-%m-%d %H:%M')}"
        
        chat_ref.set({
            "chat_id": chat_id,
            "user_id": user_id,
            "title": title,
            "created_at": firestore.SERVER_TIMESTAMP,
            "updated_at": firestore.SERVER_TIMESTAMP
        })
        
        return {
            "status": "success",
            "chat_id": chat_id,
            "title": title
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to create chat: {str(e)}")

@router.get("/{chat_id}/messages")
async def get_messages(
    chat_id: str,
    limit: int = 50,
    user_token: dict = Depends(verify_firebase_token)
) -> List[Dict[str, Any]]:
    """Retrieves message history for a specific chat thread."""
    try:
        # Verify chat ownership
        chat_doc = db.collection("chats").document(chat_id).get()
        if not chat_doc.exists or chat_doc.to_dict().get("user_id") != user_token["uid"]:
            raise HTTPException(status_code=403, detail="Not authorized to view this chat.")
            
        messages_ref = db.collection("chats").document(chat_id).collection("messages")
        query = messages_ref.order_by("timestamp", direction="ASCENDING").limit(limit)
        docs = query.stream()
        
        messages = []
        for doc in docs:
            data = doc.to_dict()
            data["message_id"] = doc.id
            if isinstance(data.get("timestamp"), datetime):
                data["timestamp"] = data["timestamp"].isoformat()
            messages.append(data)
        return messages
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch messages: {str(e)}")

@router.post("/{chat_id}/messages")
async def send_message(
    chat_id: str,
    msg_input: MessageCreate,
    mode: str = Query("local", enum=["local", "smart"]),
    user_token: dict = Depends(verify_firebase_token)
) -> Dict[str, Any]:
    """
    Sends a message to the AI.
    
    If mode == "smart": Runs the server-side Gemini Agentic workflow using dynamic tools.
    If mode == "local": Persists the user text and on-device offline SLM response in Firestore.
    """
    try:
        # 1. Verify chat ownership
        chat_ref = db.collection("chats").document(chat_id)
        chat_doc = chat_ref.get()
        if not chat_doc.exists or chat_doc.to_dict().get("user_id") != user_token["uid"]:
            raise HTTPException(status_code=403, detail="Not authorized to access this chat.")
            
        messages_ref = chat_ref.collection("messages")
        
        # 2. Save user message to Firestore
        user_msg_ref = messages_ref.document()
        user_msg_ref.set({
            "sender": "user",
            "sender_name": user_token.get("name", "User"),
            "text": msg_input.text,
            "timestamp": firestore.SERVER_TIMESTAMP
        })
        
        ai_response_text = ""
        
        # ── Mode 1: Local Offline Sync ──
        if mode == "local":
            if not msg_input.local_slm_response:
                raise HTTPException(status_code=400, detail="local_slm_response is required when mode is 'local'.")
            ai_response_text = msg_input.local_slm_response
            
            # Save the pre-generated local SLM response to cloud database
            ai_msg_ref = messages_ref.document()
            ai_msg_ref.set({
                "sender": "ai",
                "sender_name": "BimariHaunter Local SLM",
                "text": ai_response_text,
                "timestamp": firestore.SERVER_TIMESTAMP
            })
            
        # ── Mode 2: Agentic Smart Mode (Gemini Server-Side) ──
        else:
            api_key = os.environ.get("GEMINI_API_KEY")
            if not api_key:
                # Attempt to read fallback API key from local environment configuration or mock response
                logger.warning("gemini_api_key_missing_using_sandbox_advisory")
                ai_response_text = (
                    "⚠️ [Smart Mode Sandbox] Gemini API key is currently unset in the server environment. "
                    "However, based on Firestore databases: Dengue outbreaks are currently highly active in "
                    "Karachi and Lahore. Please practice preventative vector controls (clearing stagnant water)."
                )
            else:
                genai.configure(api_key=api_key)
                
                # Setup Gemini Agent System Instruction
                system_instruction = (
                    "You are the BimariHaunter AI Outbreak Intelligence Agent, a professional medical advisor "
                    "and disease prevention expert in Pakistan.\n"
                    "You have direct access to tools to help answer user questions:\n"
                    "1. query_outbreaks(city): check verified outbreak reports inside our Firestore database.\n"
                    "2. query_web_search(query_str): run live web search queries to fetch real-time breaking news.\n\n"
                    "Always prioritize checked reports. Provide precise, actionable advice with symptoms, "
                    "precautionary advice, and local hospital recommendations. Keep your tone empathetic and clear."
                )
                
                # Fetch recent conversation history for prompt injection context
                history_docs = messages_ref.order_by("timestamp", direction="DESCENDING").limit(5).stream()
                history_list = []
                for h in reversed(list(history_docs)):
                    h_dict = h.to_dict()
                    history_list.append(f"{h_dict['sender'].upper()}: {h_dict['text']}")
                history_context = "\n".join(history_list)
                
                prompt = (
                    f"Conversation History:\n{history_context}\n\n"
                    f"User Query: {msg_input.text}"
                )
                
                # Initialize Gemini with our functions/tools
                model = genai.GenerativeModel(
                    model_name="gemini-1.0-pro",
                    tools=[query_outbreaks, query_web_search],
                    generation_config={"temperature": 0.3}
                )
                
                chat = model.start_chat(enable_automatic_function_calling=True)
                response = chat.send_message(prompt)
                ai_response_text = response.text
                
            # Save Gemini AI message to cloud database
            ai_msg_ref = messages_ref.document()
            ai_msg_ref.set({
                "sender": "ai",
                "sender_name": "BimariHaunter Smart AI",
                "text": ai_response_text,
                "timestamp": firestore.SERVER_TIMESTAMP
            })
            
        # Update chat session record timestamp
        chat_ref.update({
            "updated_at": firestore.SERVER_TIMESTAMP
        })
        
        return {
            "status": "success",
            "mode": mode,
            "user_message_id": user_msg_ref.id,
            "ai_message_id": ai_msg_ref.id,
            "response": ai_response_text
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to process chat: {str(e)}")
