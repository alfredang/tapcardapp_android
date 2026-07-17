package com.tertiaryinfotech.tapcard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.Contact
import com.tertiaryinfotech.tapcard.model.Lead
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlueDeep
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.util.launchSafely
import com.tertiaryinfotech.tapcard.vm.CardViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(vm: CardViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Contact?>(null) }
    var query by remember { mutableStateOf("") }
    val shareMyCard = { vm.cards.firstOrNull()?.let { vm.openShare(it) } ?: Unit }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (!vm.isAuthenticated) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                SignedOutState(
                    icon = Icons.Filled.Groups,
                    title = "Keep your connections",
                    message = "Sign in to capture leads from your card and save the people you meet.",
                    onSignIn = vm::openAuth,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenTitle("Contacts", "Leads and people you've saved", Modifier.weight(1f))
                    IconButton(onClick = shareMyCard) {
                        Icon(Icons.Filled.Share, contentDescription = "Share my card", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add contact", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            item { SearchField(query, onChange = { query = it }) }

            val q = query.trim()
            val leads = if (q.isBlank()) vm.leads else vm.leads.filter { leadMatches(it, q) }
            val contacts = if (q.isBlank()) vm.contacts else vm.contacts.filter { contactMatches(it, q) }

            if (vm.isPeopleLoading && vm.leads.isEmpty() && vm.contacts.isEmpty()) {
                item { LoadingRow() }
            }

            if (leads.isNotEmpty()) {
                item { SectionHeader("NEW LEADS", leads.size) }
                items(leads, key = { it.id }) { lead ->
                    LeadRow(lead, onSave = { vm.saveLeadAsContact(lead) }, onDismiss = { vm.dismissLead(lead) })
                }
            }

            if (contacts.isNotEmpty()) {
                item { SectionHeader("SAVED CONTACTS", contacts.size) }
                items(contacts, key = { it.id }) { contact ->
                    ContactRow(contact, onClick = { detail = contact })
                }
            }

            // Empty states: no search matches vs. genuinely no contacts.
            if (!vm.isPeopleLoading && leads.isEmpty() && contacts.isEmpty()) {
                if (q.isNotBlank()) {
                    item { NoSearchResults(q) }
                } else {
                    item { EmptyPeople(onShare = shareMyCard) }
                }
            }
        }
    }

    if (showAdd) {
        AddContactDialog(
            onDismiss = { showAdd = false },
            onSave = { contact ->
                showAdd = false
                vm.addContact(contact)
            },
        )
    }

    detail?.let { contact ->
        ContactDetailDialog(
            contact = contact,
            onDismiss = { detail = null },
            onDelete = {
                detail = null
                vm.deleteContact(contact)
            },
        )
    }
}

private fun leadMatches(l: Lead, q: String) =
    listOf(l.displayName, l.company, l.email, l.phone).any { it.contains(q, ignoreCase = true) }

private fun contactMatches(c: Contact, q: String) =
    listOf(c.displayName, c.subtitle, c.email, c.phone).any { it.contains(q, ignoreCase = true) }

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Search contacts", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NoSearchResults(query: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("No matches for “$query”", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SectionHeader(text: String, count: Int) {
    Text(
        "$text · $count",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun LoadingRow() {
    Row(
        Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalArrangement = Arrangement.Center,
    ) { CircularProgressIndicator(color = BrandBlue) }
}

@Composable
private fun LeadRow(lead: Lead, onSave: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(lead.displayName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        val sub = listOf(lead.company, lead.email, lead.phone).filter { it.isNotBlank() }.joinToString(" · ")
        if (sub.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (lead.message.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("“${lead.message}”", fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep)))
                    .tappable(onSave)
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .tappable(onDismiss)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
}

// A small, pleasant palette; each contact gets a stable colour from its name so
// the list reads like a real directory rather than a wall of identical avatars.
private val AvatarColors = listOf(
    0xFF2563EB, 0xFF7C3AED, 0xFFDB2777, 0xFFEA580C,
    0xFF059669, 0xFF0891B2, 0xFFD97706, 0xFF4F46E5,
).map(::Color)

private fun avatarColor(seed: String): Color =
    AvatarColors[abs(seed.hashCode()) % AvatarColors.size]

@Composable
private fun ContactRow(contact: Contact, onClick: () -> Unit) {
    val context = LocalContext.current
    val color = avatarColor(contact.displayName)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.06f), ambientColor = Color.Black.copy(alpha = 0.04f))
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .tappable(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.78f)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                contact.displayName.take(1).uppercase(),
                color = Color.White,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                contact.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val secondary = contact.subtitle.ifBlank { contact.email.ifBlank { contact.phone } }
            if (secondary.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(secondary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // One-tap reach-out actions, when we have the detail.
        if (contact.phone.isNotBlank()) {
            QuickAction(Icons.Filled.Call, "Call ${contact.displayName}") {
                context.launchSafely(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone.filter { it.isDigit() || it == '+' }}")),
                    "No phone app available",
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        if (contact.email.isNotBlank()) {
            QuickAction(Icons.Filled.Email, "Email ${contact.displayName}") {
                context.launchSafely(
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}")),
                    "No email app available",
                )
            }
        }
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(BrandBlue.copy(alpha = 0.10f))
            .tappable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = BrandBlue, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EmptyPeople(onShare: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 56.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("No contacts yet", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            "When you share your card and they share their details back, it will appear here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        PrimaryButton("Share my card", icon = Icons.Filled.Share, onClick = onShare)
    }
}

@Composable
private fun ContactDetailDialog(contact: Contact, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val color = avatarColor(contact.displayName)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // ---- Header: colored avatar + name + role ----
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.78f)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        contact.displayName.take(1).uppercase(),
                        color = Color.White,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        contact.displayName,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (contact.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(contact.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ---- Detail rows ----
            val rows = buildList {
                if (contact.phone.isNotBlank()) add(Triple(Icons.Filled.Call, "Mobile", contact.phone))
                if (contact.whatsapp.isNotBlank()) add(Triple(Icons.AutoMirrored.Filled.Chat, "WhatsApp", contact.whatsapp))
                if (contact.email.isNotBlank()) add(Triple(Icons.Filled.Email, "Email", contact.email))
                if (contact.address.isNotBlank()) add(Triple(Icons.Filled.LocationOn, "Address", contact.address))
            }
            if (rows.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    rows.forEach { (icon, label, value) -> DetailRowChip(icon, label, value) }
                }
            }

            if (contact.notes.isNotBlank()) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text(
                        "NOTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(contact.notes, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // ---- Primary actions ----
            if (contact.phone.isNotBlank() || contact.email.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (contact.phone.isNotBlank()) {
                        ActionPill("Call", Icons.Filled.Call, Modifier.weight(1f), filled = true) {
                            context.launchSafely(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone.filter { it.isDigit() || it == '+' }}")),
                                "No phone app available",
                            )
                        }
                    }
                    if (contact.email.isNotBlank()) {
                        ActionPill("Email", Icons.Filled.Email, Modifier.weight(1f), filled = contact.phone.isBlank()) {
                            context.launchSafely(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}")),
                                "No email app available",
                            )
                        }
                    }
                }
            }

            // ---- Footer ----
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Close", fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DetailRowChip(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ActionPill(text: String, icon: ImageVector, modifier: Modifier, filled: Boolean, onClick: () -> Unit) {
    Row(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (filled) Modifier.background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep)))
                else Modifier.border(1.5.dp, BrandBlue.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            )
            .tappable(onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) Color.White else BrandBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (filled) Color.White else BrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun AddContactDialog(onDismiss: () -> Unit, onSave: (Contact) -> Unit) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Add contact",
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconField("Name", name, Icons.Filled.Person, KeyboardType.Text) { name = it }
                IconField("Company", company, Icons.Filled.Apartment, KeyboardType.Text) { company = it }
                IconField("Position", position, Icons.Filled.Badge, KeyboardType.Text) { position = it }
                IconField("Email", email, Icons.Filled.Email, KeyboardType.Email) { email = it }
                IconField("Phone", phone, Icons.Filled.Phone, KeyboardType.Phone) { phone = it }
                IconField("Notes", notes, Icons.Filled.Description, KeyboardType.Text, singleLine = false) { notes = it }
            }

            Spacer(Modifier.height(18.dp))
            PrimaryButton("Save contact", icon = Icons.Filled.PersonAdd) {
                onSave(
                    Contact(
                        name = name, company = company, position = position,
                        email = email, phone = phone, notes = notes,
                    ),
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun IconField(
    label: String,
    value: String,
    icon: ImageVector,
    keyboardType: KeyboardType,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}
