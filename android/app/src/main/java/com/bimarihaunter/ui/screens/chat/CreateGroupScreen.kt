package com.bimarihaunter.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.theme.*

data class Contact(val name: String, val initials: String)

private val mockContacts = listOf(
    Contact("Ahmad Khan", "AK"),
    Contact("Sana Fatima", "SF"),
    Contact("Dr. Fatima Zahra", "FZ"),
    Contact("Imran Ali", "IA"),
    Contact("Zainab Malik", "ZM"),
    Contact("Usman Sheikh", "US"),
    Contact("Hira Nawaz", "HN"),
    Contact("Bilal Ahmed", "BA"),
)

@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit = {},
    onCreate: () -> Unit = {}
) {
    var groupName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<Contact>() }

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
        BimarihaunterTopAppBar(
            title = "New Group",
            showBackArrow = true,
            onBackClick = onNavigateBack,
            actions = {
                TextButton(onClick = onCreate, enabled = groupName.isNotBlank()) {
                    Text("Create", color = if (groupName.isNotBlank()) LimeGreen else MediumGrey,
                        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))

            // Avatar placeholder
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(CharcoalGrey)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, "Photo", tint = MediumGrey, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.height(20.dp))

            // Group name
            OutlinedTextField(
                value = groupName, onValueChange = { groupName = it },
                placeholder = { Text("Group Name", color = MediumGrey) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = CharcoalGrey, focusedContainerColor = CharcoalGrey,
                    unfocusedBorderColor = CharcoalGrey, focusedBorderColor = LimeGreen,
                    cursorColor = LimeGreen, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
                ), singleLine = true
            )

            Spacer(Modifier.height(24.dp))

            Text("Add Members", color = OffWhite, fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(Modifier.height(12.dp))

            // Search
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search contacts...", color = MediumGrey) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MediumGrey) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = CharcoalGrey, focusedContainerColor = CharcoalGrey,
                    unfocusedBorderColor = CharcoalGrey, focusedBorderColor = LimeGreen,
                    cursorColor = LimeGreen, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
                ), singleLine = true
            )

            // Selected members row
            if (selectedMembers.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedMembers) { member ->
                        Box(contentAlignment = Alignment.TopEnd) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(40.dp).clip(CircleShape).background(CharcoalGrey),
                                    contentAlignment = Alignment.Center) {
                                    Text(member.initials, color = OffWhite, fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold)
                                }
                                Text(member.name.split(" ").first(), color = MediumGrey,
                                    fontSize = 10.sp, fontFamily = InterFamily)
                            }
                            Box(
                                Modifier.size(16.dp).clip(CircleShape).background(LimeGreen)
                                    .clickable { selectedMembers.remove(member) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, "Remove", tint = MidnightBlack,
                                    modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // Contacts list
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val filtered = mockContacts.filter {
                searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
            }
            items(filtered) { contact ->
                val isSelected = contact in selectedMembers
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (isSelected) selectedMembers.remove(contact)
                            else selectedMembers.add(contact)
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(CharcoalGrey),
                        contentAlignment = Alignment.Center) {
                        Text(contact.initials, color = OffWhite, fontWeight = FontWeight.Bold,
                            fontSize = 15.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(contact.name, color = OffWhite, fontFamily = InterFamily, fontSize = 15.sp,
                        modifier = Modifier.weight(1f))
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, "Selected", tint = LimeGreen,
                            modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}
