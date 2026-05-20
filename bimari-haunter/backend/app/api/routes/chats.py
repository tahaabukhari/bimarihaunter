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
from app.config import settings

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
            .limit(50)
        )
        docs = list(query.stream())
        
        # Sort in memory to avoid composite index requirements
        def get_pub_date(d):
            dt = d.to_dict().get("published_at")
            if isinstance(dt, datetime):
                return dt
            return datetime.min.replace(tzinfo=timezone.utc)
            
        docs.sort(key=get_pub_date, reverse=True)
        docs = docs[:5]
        
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
        # 1. Verify chat ownership / Auto-create if not exists
        chat_ref = db.collection("chats").document(chat_id)
        chat_doc = chat_ref.get()
        if not chat_doc.exists:
            logger.info("Chat does not exist, creating dynamically", chat_id=chat_id, user_id=user_token["uid"])
            chat_ref.set({
                "chat_id": chat_id,
                "user_id": user_token["uid"],
                "title": f"Consultation {datetime.now().strftime('%Y-%m-%d %H:%M')}",
                "created_at": firestore.SERVER_TIMESTAMP,
                "updated_at": firestore.SERVER_TIMESTAMP
            })
        elif chat_doc.to_dict().get("user_id") != user_token["uid"]:
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
            
        # ── Mode 2: Agentic Smart Mode (Vertex AI with AI Studio fallback) ──
        else:
            ai_response_text = ""
            vertex_success = False
            
            # 1. Try Vertex AI first (GCP Production Architecture)
            try:
                import vertexai
                from vertexai.generative_models import GenerativeModel as VertexGenerativeModel
                from vertexai.generative_models import Tool as VertexTool
                from vertexai.generative_models import FunctionDeclaration as VertexFunctionDeclaration
                
                # Dynamic Credential Integration
                base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
                key_path = None
                for root_dir in [base_dir, os.path.dirname(base_dir)]:
                    for file in os.listdir(root_dir):
                        if "firebase-adminsdk" in file and file.endswith(".json"):
                            key_path = os.path.join(root_dir, file)
                            break
                    if key_path:
                        break
                
                if key_path:
                    logger.info("Initializing Vertex AI with Service Account Key", path=key_path)
                    from google.auth import load_credentials_from_file
                    credentials, project_id = load_credentials_from_file(key_path)
                    vertexai.init(project=project_id, location="us-central1", credentials=credentials)
                else:
                    logger.info("Initializing Vertex AI with Cloud Default Credentials")
                    project_id = os.environ.get("GCP_PROJECT", "bimarihaunter-backend")
                    vertexai.init(project=project_id, location="us-central1")
                
                # Define Vertex AI function declarations matching local tools
                query_outbreaks_decl = VertexFunctionDeclaration(
                    name="query_outbreaks",
                    description="Queries Firestore active outbreak reports in a specific Pakistan city. Use this to get verified database reports.",
                    parameters={
                        "type": "OBJECT",
                        "properties": {
                            "city": {"type": "STRING", "description": "The name of the city, e.g., Karachi, Lahore"}
                        },
                        "required": ["city"]
                    }
                )
                
                query_web_search_decl = VertexFunctionDeclaration(
                    name="query_web_search",
                    description="Performs a live Yahoo web search for Pakistan health news. Use when breaking or real-time news is needed.",
                    parameters={
                        "type": "OBJECT",
                        "properties": {
                            "query_str": {"type": "STRING", "description": "Specific search query text"}
                        },
                        "required": ["query_str"]
                    }
                )
                
                vertex_tools = VertexTool(
                    function_declarations=[query_outbreaks_decl, query_web_search_decl]
                )
                
                system_instruction = (
                    "You are the BimariHaunter AI Outbreak Intelligence Agent, a professional medical advisor "
                    "and disease prevention expert in Pakistan. Always prioritize checked reports. Provide precise, "
                    "actionable advice with symptoms, precautionary advice, and local hospital recommendations. "
                    "Keep your tone empathetic and clear."
                )
                
                # Fetch recent conversation history
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
                
                # Use standard gemini-1.5-flash for Vertex AI
                model = VertexGenerativeModel(
                    model_name="gemini-1.5-flash",
                    tools=[vertex_tools],
                    system_instruction=system_instruction,
                    generation_config={"temperature": 0.3}
                )
                
                chat = model.start_chat()
                response = chat.send_message(prompt)
                
                # Vertex AI Chat Tool Calling Execution Loop
                curr_response = response
                while curr_response.candidates[0].content.parts and len(curr_response.candidates[0].content.parts) > 0 and curr_response.candidates[0].content.parts[0].function_calls:
                    function_calls = curr_response.candidates[0].content.parts[0].function_calls
                    
                    from vertexai.generative_models import Part
                    response_parts = []
                    
                    for function_call in function_calls:
                        name = function_call.name
                        args = function_call.args
                        
                        logger.info("Executing Vertex tool call", tool=name, args=args)
                        
                        if name == "query_outbreaks":
                            city = args.get("city", "")
                            result = query_outbreaks(city)
                        elif name == "query_web_search":
                            query_str = args.get("query_str", "")
                            result = query_web_search(query_str)
                        else:
                            result = f"Error: Tool {name} not found."
                            
                        response_parts.append(Part.from_function_response(
                            name=name,
                            response={"result": result}
                        ))
                    
                    curr_response = chat.send_message(response_parts)
                
                ai_response_text = curr_response.text
                vertex_success = True
                logger.info("Vertex AI Chat successfully processed", response_len=len(ai_response_text))
                
            except Exception as vertex_err:
                logger.warning("Vertex AI processing failed; falling back to Google AI Studio", error=str(vertex_err))
                vertex_success = False
            
            # 2. Fallback to Google AI Studio (Hobbyist / API Key mode)
            if not vertex_success:
                api_key = settings.gemini_api_key or os.environ.get("GEMINI_API_KEY", "")
                if not api_key:
                    logger.warning("gemini_api_key_missing_using_sandbox_advisory")
                    ai_response_text = (
                        "⚠️ Gemini API key is not configured. Please contact support."
                    )
                else:
                    genai.configure(api_key=api_key)
                    
                    system_instruction = (
                        "You are the BimariHaunter AI Outbreak Intelligence Agent, a professional medical advisor "
                        "and disease prevention expert in Pakistan.\n"
                        "You have direct access to tools to help answer user questions:\n"
                        "1. query_outbreaks(city): check verified outbreak reports inside our Firestore database.\n"
                        "2. query_web_search(query_str): run live web search queries to fetch real-time breaking news.\n\n"
                        "Always prioritize checked reports. Provide precise, actionable advice with symptoms, "
                        "precautionary advice, and local hospital recommendations. Keep your tone empathetic and clear."
                    )
                    
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
                    
                    try:
                        # Prioritize gemini-1.5-flash which is much faster and supports AQ keys
                        logger.info("Initializing Google AI Studio with gemini-1.5-flash")
                        model = genai.GenerativeModel(
                            model_name="gemini-1.5-flash",
                            tools=[query_outbreaks, query_web_search],
                            generation_config={"temperature": 0.3},
                            system_instruction=system_instruction
                        )
                        chat = model.start_chat(enable_automatic_function_calling=True)
                        response = chat.send_message(prompt)
                        ai_response_text = response.text
                    except Exception as flash_err:
                        logger.warning("gemini-1.5-flash failed, falling back to gemini-1.0-pro", error=str(flash_err))
                        model = genai.GenerativeModel(
                            model_name="gemini-1.0-pro",
                            tools=[query_outbreaks, query_web_search],
                            generation_config={"temperature": 0.3}
                        )
                        chat = model.start_chat(enable_automatic_function_calling=True)
                        response = chat.send_message(prompt)
                        ai_response_text = response.text
                
            # Save AI message to cloud database
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
