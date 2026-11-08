package com.khaory.taller.ui.citas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.khaory.taller.data.firebase.Cita
import com.khaory.taller.data.firebase.slotKey
import com.khaory.taller.ui.theme.OrangePrimary
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

private fun millisToDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * Cliente: calendario + horarios cada 2 horas + sus citas.
 * Administrador: agenda con todas las citas de los clientes (puede cancelarlas).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitasScreen(email: String, admin: Boolean, vm: CitasViewModel = viewModel()) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(uid, admin) { uid?.let { vm.connect(it, admin) } }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(vm.message) { vm.message?.let { notice = it; vm.clearMessage() } }
    LaunchedEffect(notice) { if (notice != null) { delay(3000); notice = null } }

    val today = remember { LocalDate.now() }
    val pickerState = rememberDatePickerState(selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val day = millisToDate(utcTimeMillis)
            return !day.isBefore(today) && day.dayOfWeek != DayOfWeek.SUNDAY // el taller no abre domingos
        }
    })
    val selectedDate = pickerState.selectedDateMillis?.let(::millisToDate)
    var selectedHour by remember(selectedDate) { mutableStateOf<Int?>(null) }
    var service by remember { mutableStateOf("") }
    var toCancel by remember { mutableStateOf<Cita?>(null) }

    Column(Modifier.fillMaxSize()) {
        if (notice != null) Surface(color = OrangePrimary.copy(alpha = 0.18f), modifier = Modifier.fillMaxWidth()) {
            Text(notice.orEmpty(), color = OrangePrimary, modifier = Modifier.padding(14.dp))
        }
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text(if (admin) "Agenda del taller" else "Reservar cita", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(if (admin) "Todas las citas reservadas por los clientes"
                else "Elige una fecha y un horario de 2 horas (lunes a sábado)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!admin) {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                        DatePicker(state = pickerState, showModeToggle = false, title = null, headline = null,
                            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface,
                                selectedDayContainerColor = OrangePrimary, todayDateBorderColor = OrangePrimary,
                                todayContentColor = OrangePrimary))
                    }
                }
                item {
                    Text("Horarios disponibles", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    if (selectedDate == null) {
                        Text("Selecciona una fecha para ver los horarios.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val nowHour = LocalDateTime.now().hour
                        SLOT_HOURS.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                row.forEach { hour ->
                                    val time = "%02d:00".format(hour)
                                    val taken = slotKey(selectedDate.toString(), time) in vm.takenSlots
                                    val past = selectedDate == today && hour <= nowHour
                                    val selected = selectedHour == hour
                                    Button(
                                        onClick = { selectedHour = hour }, enabled = !taken && !past,
                                        modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$time–%02d:00".format(hour + 2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            if (taken) Text("Ocupado", fontSize = 10.sp)
                                        }
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                item {
                    OutlinedTextField(service, { service = it }, label = { Text("Servicio o motivo (ej. cambio de aceite)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), minLines = 2)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        enabled = !vm.isSaving && selectedDate != null && selectedHour != null,
                        onClick = {
                            val date = selectedDate ?: return@Button
                            val hour = selectedHour ?: return@Button
                            vm.reserve(email, date, hour, service) { error ->
                                notice = error ?: "Cita reservada: $date a las %02d:00.".format(hour)
                                if (error == null) { selectedHour = null; service = "" }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) { Text(if (vm.isSaving) "Reservando…" else "Reservar cita") }
                }
            }
            item {
                Text(if (admin) "Citas reservadas" else "Mis citas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            if (vm.appointments.isEmpty()) item {
                Text(if (admin) "Todavía no hay citas reservadas." else "Aún no tienes citas reservadas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(vm.appointments, key = { it.id }) { cita ->
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${cita.date} · ${cita.time}–%02d:00".format(cita.time.take(2).toInt() + 2), fontWeight = FontWeight.Bold)
                        if (admin) Text(cita.email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(cita.service, fontSize = 14.sp)
                        TextButton(onClick = { toCancel = cita }) { Text("Cancelar cita") }
                    }
                }
            }
        }
    }
    toCancel?.let { cita ->
        AlertDialog(onDismissRequest = { toCancel = null }, title = { Text("Cancelar cita") },
            text = { Text("¿Cancelar la cita del ${cita.date} a las ${cita.time}? El horario quedará libre.") },
            confirmButton = { TextButton(onClick = { vm.cancel(cita); toCancel = null }) { Text("Sí, cancelar") } },
            dismissButton = { TextButton(onClick = { toCancel = null }) { Text("Volver") } })
    }
}
