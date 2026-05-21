package com.bimarihaunter.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.ui.components.BimarihaunterButton
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.components.StatBox
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// ─────────────────────────── QR Code Helper ──────────────────────────────────

private fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                // Dark modules = LimeGreen (#A7FF83), light modules = MidnightBlack (#121212)
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF121212.toInt() else 0xFFA7FF83.toInt())
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

// ─────────────────────────── QR Dialog ───────────────────────────────────────

@Composable
private fun QrCodeDialog(uid: String, displayName: String, onDismiss: () -> Unit) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uid) {
        qrBitmap = withContext(Dispatchers.Default) { generateQrBitmap(uid) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalGrey)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "My QR Code",
                    color = OffWhite,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Let friends scan this to add you",
                    color = MediumGrey,
                    fontFamily = InterFamily,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(20.dp))

                // QR Code image
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(LimeGreen)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code for $displayName",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = MidnightBlack, modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Name label
                Text(
                    displayName,
                    color = OffWhite,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                // Truncated UID label
                Text(
                    uid.take(20) + "…",
                    color = MediumGrey,
                    fontFamily = InterFamily,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LimeGreen)
                ) {
                    Text("Done", color = MidnightBlack, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─────────────────────────── Profile Screen ──────────────────────────────────

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()

    // Dynamic display values from Firebase Auth + Firestore profile
    val displayName = userProfile?.name?.ifBlank { null }
        ?: currentUser?.displayName?.ifBlank { null }
        ?: "Bimari Haunter"
    val subtitleText = userProfile?.email?.ifBlank { null }
        ?: currentUser?.email?.ifBlank { null }
        ?: currentUser?.phoneNumber?.ifBlank { null }
        ?: "@anonymous"
    val initials = displayName.split(" ").take(2)
        .mapNotNull { it.firstOrNull() }.joinToString("").uppercase()

    // Dynamic stats from Firestore
    var reportsCount by remember { mutableStateOf<Int?>(null) }
    var savedCount by remember { mutableStateOf<Int?>(null) }
    var groupsCount by remember { mutableStateOf<Int?>(null) }

    // Dynamic saved articles from Firestore
    var savedArticles by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    // Dynamic groups from Firestore
    var userGroups by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }

    var showQrDialog by remember { mutableStateOf(false) }

    val uid = currentUser?.uid

    LaunchedEffect(uid) {
        if (uid == null) {
            reportsCount = 0
            savedCount = 0
            groupsCount = 0
            return@LaunchedEffect
        }
        val db = FirebaseFirestore.getInstance()

        // Count reports (global count — user-submitted reports not yet tracked per-user)
        try {
            val reportsSnap = db.collection("reports").limit(1000).get().await()
            reportsCount = reportsSnap.size()
        } catch (e: Exception) {
            reportsCount = 0
        }

        // Count saved articles
        try {
            val savedSnap = db.collection("users").document(uid)
                .collection("saved").get().await()
            savedCount = savedSnap.size()

            // Load actual saved article titles/sources
            savedArticles = savedSnap.documents.take(6).map { doc ->
                val title = doc.getString("title") ?: "Saved Article"
                val source = doc.getString("source") ?: "BimariHaunter"
                Pair(title, source)
            }
        } catch (e: Exception) {
            savedCount = 0
        }

        // Count and load groups
        try {
            val groupsSnap = db.collection("groups")
                .whereArrayContains("members", uid)
                .get().await()
            groupsCount = groupsSnap.size()

            userGroups = groupsSnap.documents.take(5).map { doc ->
                val name = doc.getString("name") ?: "Group"
                val memberCount = (doc.get("members") as? List<*>)?.size ?: 0
                val ini = name.split(" ").take(2)
                    .mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
                    .ifEmpty { "G" }
                Triple(name, ini, "$memberCount member${if (memberCount != 1) "s" else ""}")
            }
        } catch (e: Exception) {
            groupsCount = 0
        }
    }

    // QR code dialog
    if (showQrDialog && uid != null) {
        QrCodeDialog(uid = uid, displayName = displayName, onDismiss = { showQrDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .verticalScroll(rememberScrollState())
    ) {
        BimarihaunterTopAppBar(
            title = "Profile",
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, "Settings", tint = OffWhite)
                }
            }
        )


        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Avatar — initials from real display name
            Box(
                Modifier.size(80.dp).clip(CircleShape).background(CharcoalGrey),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initials.ifEmpty { "U" },
                    color = LimeGreen,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                displayName,
                color = OffWhite,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(subtitleText, color = MediumGrey, fontFamily = InterFamily, fontSize = 14.sp)

            Spacer(Modifier.height(12.dp))

            // ── UID chip row ──────────────────────────────────────────────────
            // Shows a truncated UID with a copy button and a QR code button.
            // Tapping the chip itself also copies the UID to clipboard.
            if (uid != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CharcoalGrey)
                        .border(1.dp, LimeGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("UID", uid))
                            Toast.makeText(context, "UID copied!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UID: ${uid.take(16)}…",
                        color = LimeGreen,
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy UID",
                        tint = LimeGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    // Divider
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(LimeGreen.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Show QR Code",
                        tint = LimeGreen,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { showQrDialog = true }
                    )
                }
            }
            // ─────────────────────────────────────────────────────────────────

            Spacer(Modifier.height(16.dp))

            BimarihaunterButton("Edit Profile", onClick = {})

            Spacer(Modifier.height(20.dp))

            // Dynamic stats row — shows loading skeleton until data arrives
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(
                    value = reportsCount?.toString() ?: "—",
                    label = "Reports",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    value = savedCount?.toString() ?: "—",
                    label = "Saved",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    value = groupsCount?.toString() ?: "—",
                    label = "Groups",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Saved Articles header
            Text(
                "Saved Articles",
                color = OffWhite,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(12.dp))
        }

        // Saved Articles — dynamic from Firestore, empty state if none
        if (savedArticles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CharcoalGrey)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (savedCount == null) "Loading saved articles..." else "No saved articles yet.",
                    color = MediumGrey,
                    fontFamily = InterFamily,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedArticles) { (title, source) ->
                    Column(
                        Modifier
                            .width(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CharcoalGrey)
                            .padding(14.dp)
                    ) {
                        Text(
                            title,
                            color = OffWhite,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(source, color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))

            Text(
                "My Groups",
                color = OffWhite,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(12.dp))

            if (userGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CharcoalGrey)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (groupsCount == null) "Loading groups..." else "You haven't joined any groups yet.",
                        color = MediumGrey,
                        fontFamily = InterFamily,
                        fontSize = 14.sp
                    )
                }
            } else {
                userGroups.forEach { (name, ini, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CharcoalGrey)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(MidnightBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                ini,
                                color = LimeGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                name,
                                color = OffWhite,
                                fontSize = 14.sp,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(count, color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
