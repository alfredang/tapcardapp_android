package com.tertiaryinfotech.tapcard.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlueDeep
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.vm.AuthStep
import com.tertiaryinfotech.tapcard.vm.CardViewModel

private const val OtpLength = 6

/**
 * Auth. "Get started" (from the welcome page) shows the sign-up form (email +
 * password). "Log in" offers two ways to sign in: email + password, or a
 * one-time email code — switchable via a tab, mirroring the website.
 */
@Composable
fun AuthScreen(vm: CardViewModel) {
    BackHandler { vm.backToWelcome() }

    Box(Modifier.fillMaxSize()) {
        AppBackground()
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 20.dp, bottom = 28.dp),
        ) {
            IconButton(
                onClick = vm::backToWelcome,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Logo mark
            Box(
                Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.height(22.dp))

            if (vm.authStartInLogin) LoginForm(vm) else SignUpForm(vm)
        }
    }
}

@Composable
private fun SignUpForm(vm: CardViewModel) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    Heading("Create your account", "Sign up with your email and a password.")
    Spacer(Modifier.height(26.dp))

    AuthField(name, { name = it }, "Full name", KeyboardType.Text)
    Spacer(Modifier.height(12.dp))
    AuthField(email, { email = it }, "Email", KeyboardType.Email)
    Spacer(Modifier.height(12.dp))
    PasswordField(password, { password = it }, showPassword) { showPassword = !showPassword }

    AuthError(vm)

    Spacer(Modifier.height(22.dp))
    PrimaryButton(text = "Create account", loading = vm.isAuthBusy) {
        keyboard?.hide()
        vm.signUpWithPassword(name, email, password)
    }

    Spacer(Modifier.height(16.dp))
    SwitchRow("Already have an account? ", "Log in", vm::beginLogin)
}

@Composable
private fun LoginForm(vm: CardViewModel) {
    var useCode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Heading(
        "Welcome back",
        if (useCode) "We'll email you a one-time code — no password needed."
        else "Log in with your email and password.",
    )
    Spacer(Modifier.height(20.dp))

    AuthModeTabs(useCode) { wantCode ->
        if (wantCode != useCode) {
            useCode = wantCode
            code = ""
            vm.authError = null
            vm.backToEmailStep() // ensure the code flow starts at the email step
        }
    }
    Spacer(Modifier.height(20.dp))

    if (!useCode) {
        // ── Email + password ──
        AuthField(email, { email = it }, "Email", KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        PasswordField(password, { password = it }, showPassword) { showPassword = !showPassword }
        AuthError(vm)
        Spacer(Modifier.height(22.dp))
        PrimaryButton(text = "Log in", loading = vm.isAuthBusy) {
            keyboard?.hide()
            vm.logInWithPassword(email, password)
        }
    } else if (vm.authStep == AuthStep.EMAIL) {
        // ── Email code · step 1: enter email ──
        AuthField(email, { email = it }, "Email", KeyboardType.Email)
        AuthError(vm)
        Spacer(Modifier.height(22.dp))
        PrimaryButton(text = "Send code", loading = vm.isAuthBusy) {
            keyboard?.hide()
            vm.requestOtp(email)
        }
    } else {
        // ── Email code · step 2: enter the 6-digit code ──
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        Text(
            "We sent a 6-digit code to $email.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OtpBoxes(
            value = code,
            onChange = {
                code = it
                if (it.length == OtpLength) {
                    keyboard?.hide()
                    vm.verifyOtp(it)
                }
            },
            modifier = Modifier.focusRequester(focus),
        )
        AuthError(vm)
        Spacer(Modifier.height(22.dp))
        PrimaryButton(text = "Verify & log in", loading = vm.isAuthBusy, enabled = code.length == OtpLength) {
            keyboard?.hide()
            vm.verifyOtp(code)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = vm::resendOtp, enabled = !vm.isAuthBusy) {
                Text("Didn't get it? Resend code", color = BrandBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    SwitchRow("New to Tapcard? ", "Sign up", vm::beginSignUp)
}

@Composable
private fun AuthModeTabs(useCode: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TabButton("Password", selected = !useCode, modifier = Modifier.weight(1f)) { onSelect(false) }
        TabButton("Email OTP", selected = useCode, modifier = Modifier.weight(1f)) { onSelect(true) }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .tappable(onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Heading(title: String, subtitle: String) {
    Text(
        title,
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        subtitle,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    show: Boolean,
    onToggleShow: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Password") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleShow) {
                Icon(
                    if (show) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (show) "Hide password" else "Show password",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Six single-digit boxes backed by a hidden text field. */
@Composable
private fun OtpBoxes(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(OtpLength)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = modifier,
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(OtpLength) { i ->
                    val char = value.getOrNull(i)?.toString() ?: ""
                    val active = i == value.length
                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = if (active || char.isNotEmpty()) BrandBlue
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            char,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun AuthError(vm: CardViewModel) {
    vm.authError?.let { err ->
        Spacer(Modifier.height(12.dp))
        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
    }
}

@Composable
private fun SwitchRow(prefix: String, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(prefix, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            action,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = BrandBlue,
            modifier = Modifier.tappable(onClick),
        )
    }
}
