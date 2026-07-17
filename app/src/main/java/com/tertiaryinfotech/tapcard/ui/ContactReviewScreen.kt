package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.model.Contact
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.vm.CardViewModel

/**
 * Entry point for the contact-review screen. Scanned data is shown read-only
 * (their info, as-is); manual entry gets an editable form. Both save to Contacts.
 */
@Composable
fun ContactReviewScreen(vm: CardViewModel) {
    if (vm.contactManualEntry) ManualContactForm(vm) else ScannedContactReview(vm)
}

/**
 * Read-only review of a scanned person before saving them to Contacts. It's their
 * info, captured as-is — so nothing is editable; you just confirm and save.
 */
@Composable
private fun ScannedContactReview(vm: CardViewModel) {
    val card = vm.draft

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .tappable(vm::startScan),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(4.dp))
                ScreenTitle("New contact", "Save this person to your contacts")
            }

            if (card.isBlank) {
                Text(
                    "No details were captured from that scan. Go back and try again.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                // Top — the person's business card, rendered like the real thing.
                CardFace(card)

                // Read-only detail rows — only the fields that were actually captured.
                val rows = buildList {
                    if (card.phone.isNotBlank()) add(Triple(Icons.Filled.Phone, "Mobile", card.phone))
                    if (card.officePhone.isNotBlank()) add(Triple(Icons.Filled.Call, "Office", card.officePhone))
                    if (card.whatsapp.isNotBlank()) add(Triple(Icons.AutoMirrored.Filled.Chat, "WhatsApp", card.whatsapp))
                    if (card.email.isNotBlank()) add(Triple(Icons.Filled.Email, "Email", card.email))
                    if (card.website.isNotBlank()) add(Triple(Icons.Filled.Language, "Website", card.website))
                    if (card.address.isNotBlank()) add(Triple(Icons.Filled.LocationOn, "Address", card.address))
                    card.socialLinks.forEach { (label, url) -> add(Triple(Icons.Filled.Link, label, url)) }
                }

                if (rows.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    SectionLabel("CONTACT DETAILS")
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.md))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(Radius.md)),
                    ) {
                        rows.forEachIndexed { i, (icon, label, value) ->
                            DetailRow(icon, label, value)
                            if (i < rows.lastIndex) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 62.dp)
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = "Save contact",
                loading = vm.isSaving,
                enabled = !card.isBlank,
            ) { vm.saveScannedContact(card.toContact()) }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Editable form for entering a contact by hand (from the scanner's "Enter
 * manually"). Its own screen — not a dialog over Contacts. Saves to Contacts.
 */
@Composable
private fun ManualContactForm(vm: CardViewModel) {
    var contact by remember { mutableStateOf(Contact()) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .tappable(vm::startScan),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(4.dp))
                ScreenTitle("New contact", "Enter their details to save them")
            }

            EditContactSection {
                EditField("Name", contact.name, Icons.Filled.Person) { contact = contact.copy(name = it) }
                EditField("Job title", contact.position, Icons.Filled.Badge) { contact = contact.copy(position = it) }
                EditField("Company", contact.company, Icons.Filled.Apartment) { contact = contact.copy(company = it) }
                EditField("Mobile", contact.phone, Icons.Filled.Phone, KeyboardType.Phone) { contact = contact.copy(phone = it) }
                EditField("WhatsApp", contact.whatsapp, Icons.AutoMirrored.Filled.Chat, KeyboardType.Phone) { contact = contact.copy(whatsapp = it) }
                EditField("Email", contact.email, Icons.Filled.Email, KeyboardType.Email) { contact = contact.copy(email = it) }
                EditField("Address", contact.address, Icons.Filled.LocationOn, singleLine = false) { contact = contact.copy(address = it) }
                EditField("Notes", contact.notes, Icons.Filled.Description, singleLine = false) { contact = contact.copy(notes = it) }
            }

            Spacer(Modifier.height(8.dp))
            PrimaryButton("Save contact", loading = vm.isSaving) { vm.saveScannedContact(contact) }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun EditContactSection(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    SectionLabel("CONTACT DETAILS")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(Radius.md))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun EditField(
    label: String,
    value: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
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
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}
