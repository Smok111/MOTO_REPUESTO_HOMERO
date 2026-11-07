package com.khaory.taller.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class UserRole { CLIENT, ADMIN }

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val email: String = "",
    val role: UserRole = UserRole.CLIENT,
    val message: String? = null,
    val messageIsError: Boolean = true
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var uiState by mutableStateOf(AuthUiState(isLoading = auth.currentUser != null))
        private set

    init {
        auth.currentUser?.let { user ->
            viewModelScope.launch {
                runCatching { roleFor(user.uid, user.email.orEmpty()) }
                    .onSuccess { role ->
                        uiState = AuthUiState(isAuthenticated = true, email = user.email.orEmpty(), role = role)
                    }
                    .onFailure { uiState = AuthUiState(message = errorMessage(it)) }
            }
        }
    }

    fun login(email: String, password: String, requestedRole: UserRole) {
        if (uiState.isLoading) return
        uiState = AuthUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
                    ?: error("No se pudo iniciar sesión.")
                val role = roleFor(user.uid, user.email.orEmpty())
                if (role != requestedRole) {
                    auth.signOut()
                    uiState = AuthUiState(message = "Selecciona el tipo de cuenta correcto para ingresar.")
                } else {
                    uiState = AuthUiState(isAuthenticated = true, email = user.email.orEmpty(), role = role)
                }
            } catch (error: Exception) {
                uiState = AuthUiState(message = errorMessage(error))
            }
        }
    }

    fun register(email: String, password: String) {
        if (uiState.isLoading) return
        uiState = AuthUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
                    ?: error("No se pudo crear la cuenta.")
                db.collection("users").document(user.uid)
                    .set(mapOf("email" to user.email.orEmpty(), "role" to "client"))
                    .await()
                uiState = AuthUiState(isAuthenticated = true, email = user.email.orEmpty())
            } catch (error: Exception) {
                auth.signOut()
                uiState = AuthUiState(message = errorMessage(error))
            }
        }
    }

    fun resetPassword(email: String) {
        if (uiState.isLoading) return
        uiState = uiState.copy(isLoading = true, message = null)
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                uiState = uiState.copy(isLoading = false,
                    message = "Si existe una cuenta para $email, recibirás un enlace de recuperación.",
                    messageIsError = false)
            } catch (error: Exception) {
                uiState = uiState.copy(isLoading = false, message = errorMessage(error), messageIsError = true)
            }
        }
    }

    fun logout() {
        auth.signOut()
        uiState = AuthUiState()
    }

    fun clearMessage() {
        if (uiState.message != null) uiState = uiState.copy(message = null)
    }

    private suspend fun roleFor(uid: String, email: String): UserRole {
        val ref = db.collection("users").document(uid)
        val profile = ref.get().await()
        if (!profile.exists()) ref.set(mapOf("email" to email, "role" to "client")).await()
        val role = profile.getString("role")
        return if (role == "admin") UserRole.ADMIN else UserRole.CLIENT
    }

    private fun errorMessage(error: Throwable): String = when {
        error.message?.contains("network", ignoreCase = true) == true -> "No hay conexión. Inténtalo otra vez."
        error.message?.contains("email", ignoreCase = true) == true -> "Revisa el correo electrónico."
        error.message?.contains("password", ignoreCase = true) == true -> "Revisa la contraseña."
        else -> error.localizedMessage ?: "No se pudo completar la operación."
    }
}
