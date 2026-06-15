package com.tertiaryinfotech.tapcard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.tapcard.vm.CardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(vm: CardViewModel) {
    val card = vm.draft
    val isNew = card.createdAtEpoch == 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Review details" else "Edit card") },
                navigationIcon = {
                    IconButton(onClick = { if (isNew) vm.goHome() else vm.openCard(card) }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Field("Name", card.name, Icons.Filled.Person) { vm.draft = card.copy(name = it) }
            Field("Job title", card.title, Icons.Filled.Badge) { vm.draft = card.copy(title = it) }
            Field("Company", card.company, Icons.Filled.Apartment) { vm.draft = card.copy(company = it) }
            Field("Phone", card.phone, Icons.Filled.Phone, KeyboardType.Phone) { vm.draft = card.copy(phone = it) }
            Field("Email", card.email, Icons.Filled.Email, KeyboardType.Email) { vm.draft = card.copy(email = it) }
            Field("Website", card.website, Icons.Filled.Language, KeyboardType.Uri) { vm.draft = card.copy(website = it) }
            Field("Address", card.address, Icons.Filled.LocationOn) { vm.draft = card.copy(address = it) }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.saveDraft(System.currentTimeMillis()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isNew) "Create card" else "Save changes")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = label != "Address",
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}
