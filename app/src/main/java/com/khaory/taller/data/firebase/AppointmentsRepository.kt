package com.khaory.taller.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/** Cita reservada. [id] = "yyyy-MM-dd_HH", por ejemplo "2026-11-10_08". */
data class Cita(
    val id: String, val userId: String, val email: String,
    val date: String, val time: String, val service: String
)

fun slotKey(date: String, time: String) = "${date}_${time.take(2)}"

/**
 * Citas en Firestore. Dos colecciones con el mismo id:
 *  - slots/{id}         → solo marca el horario como ocupado (visible para todos, sin datos personales)
 *  - appointments/{id}  → detalle de la cita (solo su dueño y el administrador)
 * Al reservar se crean ambos en una transacción: si el horario ya existe, falla y nadie más puede tomarlo.
 */
class AppointmentsRepository {
    private val db = FirebaseFirestore.getInstance()

    fun listenTakenSlots(fromDate: String, onChange: (Set<String>) -> Unit,
                         onError: (String) -> Unit): ListenerRegistration =
        db.collection("slots").whereGreaterThanOrEqualTo("date", fromDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { onError(error.localizedMessage ?: "No se pudo cargar la disponibilidad."); return@addSnapshotListener }
                onChange(snapshot?.documents.orEmpty().map { it.id }.toSet())
            }

    fun listenAppointments(uid: String, admin: Boolean, onChange: (List<Cita>) -> Unit,
                           onError: (String) -> Unit): ListenerRegistration {
        val query = if (admin) db.collection("appointments")
        else db.collection("appointments").whereEqualTo("userId", uid)
        return query.addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error.localizedMessage ?: "No se pudieron cargar las citas."); return@addSnapshotListener }
            onChange(snapshot?.documents.orEmpty().map { doc ->
                Cita(doc.id, doc.getString("userId").orEmpty(), doc.getString("email").orEmpty(),
                    doc.getString("date").orEmpty(), doc.getString("time").orEmpty(), doc.getString("service").orEmpty())
            }.sortedBy { it.id })
        }
    }

    suspend fun reserve(uid: String, email: String, date: String, time: String, service: String) {
        val key = slotKey(date, time)
        val slotRef = db.collection("slots").document(key)
        val appointmentRef = db.collection("appointments").document(key)
        db.runTransaction { tx ->
            require(!tx.get(slotRef).exists()) { "Ese horario ya fue reservado. Elige otro." }
            tx.set(slotRef, mapOf("userId" to uid, "date" to date, "time" to time))
            tx.set(appointmentRef, mapOf("userId" to uid, "email" to email, "date" to date, "time" to time,
                "service" to service.trim(), "createdAt" to System.currentTimeMillis()))
            Unit
        }.await()
    }

    suspend fun cancel(key: String) {
        val slotRef = db.collection("slots").document(key)
        val appointmentRef = db.collection("appointments").document(key)
        db.runTransaction { tx ->
            tx.delete(slotRef)
            tx.delete(appointmentRef)
            Unit
        }.await()
    }
}
