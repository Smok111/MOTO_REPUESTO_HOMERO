package com.khaory.taller.ui.login

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khaory.taller.ui.theme.OrangePrimary
import com.khaory.taller.ui.theme.BrandIdentity
import com.khaory.taller.ui.demo.DemoDashboardScreen

@Composable
fun LoginScreen(authViewModel: AuthViewModel = viewModel()) {
    val state = authViewModel.uiState
    if (state.isAuthenticated) DemoDashboardScreen(state.email, state.role, authViewModel::logout)
    else AuthForm(state, authViewModel)
}

@Composable
private fun AuthForm(state: AuthUiState, authViewModel: AuthViewModel) {
    var selectedRole by remember { mutableStateOf(UserRole.CLIENT) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var registering by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val submit: () -> Unit = {
        keyboardController?.hide()
        localError = validate(email, password, confirmation, registering)
        if (localError == null) {
            if (registering) authViewModel.register(email, password)
            else authViewModel.login(email, password, selectedRole)
        }
    }

    LaunchedEffect(email, password, confirmation, selectedRole, registering) {
        localError = null
        authViewModel.clearMessage()
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(42.dp))
        BrandIdentity()
        Spacer(Modifier.height(10.dp))
        Text("Repuestos para seguir en ruta", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(26.dp))
        Text(if (registering) "Crea tu cuenta" else "Bienvenido de nuevo", fontSize = 27.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Text(if (registering) "Regístrate como cliente para comenzar" else "Selecciona cómo deseas ingresar",
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(22.dp))

        if (!registering) {
            Row(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant), verticalAlignment = Alignment.CenterVertically
            ) {
                RoleTab("Soy cliente", selectedRole == UserRole.CLIENT, Modifier.weight(1f)) { selectedRole = UserRole.CLIENT }
                RoleTab("Administrador", selectedRole == UserRole.ADMIN, Modifier.weight(1f)) { selectedRole = UserRole.ADMIN }
            }
            Spacer(Modifier.height(22.dp))
        }

        AuthTextField(email, { email = it.trim() }, "Correo electrónico", { Icon(Icons.Default.Email, null) },
            KeyboardType.Email, imeAction = ImeAction.Next)
        Spacer(Modifier.height(14.dp))
        AuthTextField(password, { password = it }, "Contraseña", { Icon(Icons.Default.Lock, null) }, KeyboardType.Password,
            if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            imeAction = if (registering) ImeAction.Next else ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() })) {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar contraseña")
            }
        }
        AnimatedVisibility(registering) {
            Column {
                Spacer(Modifier.height(14.dp))
                AuthTextField(confirmation, { confirmation = it }, "Confirmar contraseña", { Icon(Icons.Default.Lock, null) },
                    KeyboardType.Password, PasswordVisualTransformation(),
                    imeAction = ImeAction.Done, keyboardActions = KeyboardActions(onDone = { submit() }))
            }
        }
        if (!registering) {
            TextButton(onClick = { showReset = true }, modifier = Modifier.align(Alignment.End)) {
                Text("¿Olvidaste tu contraseña?", color = OrangePrimary)
            }
        } else Spacer(Modifier.height(12.dp))

        val error = localError ?: state.message
        AnimatedVisibility(error != null) {
            Text(error.orEmpty(), color = if (state.messageIsError || localError != null) MaterialTheme.colorScheme.error else Color(0xFF69D18B),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        }
        Button(
            onClick = submit,
            enabled = !state.isLoading, modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary), shape = RoundedCornerShape(14.dp)
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            else Text(if (registering) "Crear cuenta" else "Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = { registering = !registering; selectedRole = UserRole.CLIENT; password = ""; confirmation = "" },
            enabled = !state.isLoading, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
        ) { Text(if (registering) "Ya tengo una cuenta" else "Crear cuenta nueva") }
        Spacer(Modifier.height(28.dp))
    }

    if (showReset) ResetPasswordDialog(email, state.isLoading, { showReset = false }) {
        authViewModel.resetPassword(it); showReset = false
    }
}

@Composable
private fun AuthTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    leadingIcon: @Composable (() -> Unit), keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions, visualTransformation = visualTransformation,
        modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary,
            focusedLabelColor = OrangePrimary), shape = RoundedCornerShape(14.dp))
}

@Composable
private fun RoleTab(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(if (selected) OrangePrimary else Color.Transparent)
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ResetPasswordDialog(initialEmail: String, loading: Boolean, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var resetEmail by remember { mutableStateOf(initialEmail) }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Recuperar contraseña") }, text = {
        Column {
            Text("Te enviaremos un enlace para crear una contraseña nueva."); Spacer(Modifier.height(14.dp))
            AuthTextField(resetEmail, { resetEmail = it.trim(); invalid = false }, "Correo electrónico",
                { Icon(Icons.Default.Email, null) }, KeyboardType.Email)
            if (invalid) Text("Ingresa un correo válido", color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        TextButton(enabled = !loading, onClick = {
            invalid = !Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches(); if (!invalid) onSend(resetEmail)
        }) { Text("Enviar") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

private fun validate(email: String, password: String, confirmation: String, register: Boolean): String? = when {
    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Ingresa un correo electrónico válido."
    password.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
    register && password != confirmation -> "Las contraseñas no coinciden."
    else -> null
}
