package com.khaory.taller.ui.citas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.khaory.taller.data.firebase.AppointmentsRepository
import com.khaory.taller.data.firebase.Cita
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Horarios de atención: bloques de 2 horas (08:00 a 20:00). */
val SLOT_HOURS = listOf(8, 10, 12, 14, 16, 18)

class CitasViewModel : ViewModel() {
    private val repo = AppointmentsRepository()
    private var slotsListener: ListenerRegistration? = null
    private var appointmentsListener: ListenerRegistration? = null
    private var connectedKey: String? = null

    val appointments = mutableStateListOf<Cita>()
    var takenSlots by mutableStateOf<Set<String>>(emptySet())
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var isSaving by mutableStateOf(false)
        private set

    fun connect(uid: String, admin: Boolean) {
        val key = "$uid-$admin"
        if (connectedKey == key) return
        slotsListener?.remove(); appointmentsListener?.remove()
        connectedKey = key
        appointments.clear()
        slotsListener = repo.listenTakenSlots(LocalDate.now().toString(),
            onChange = { takenSlots = it }, onError = { message = it })
        appointmentsListener = repo.listenAppointments(uid, admin,
            onChange = { appointments.clear(); appointments.addAll(it) }, onError = { message = it })
    }

    fun reserve(email: String, date: LocalDate, hour: Int, service: String, onResult: (String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { onResult("Inicia sesión para reservar."); return }
        if (service.isBlank()) { onResult("Indica el servicio que necesitas."); return }
        if (isSaving) return
        isSaving = true
        viewModelScope.launch {
            try {
                repo.reserve(uid, email, date.toString(), "%02d:00".format(hour), service)
                onResult(null)
            } catch (error: Exception) {
                onResult(error.localizedMessage ?: "No se pudo reservar la cita.")
            } finally { isSaving = false }
        }
    }

    fun cancel(cita: Cita) {
        viewModelScope.launch {
            try { repo.cancel(cita.id); message = "Cita cancelada. El horario quedó libre." }
            catch (error: Exception) { message = "No se pudo cancelar: ${error.localizedMessage}" }
        }
    }

    fun clearMessage() { message = null }

    override fun onCleared() {
        slotsListener?.remove(); appointmentsListener?.remove()
        super.onCleared()
    }
}
