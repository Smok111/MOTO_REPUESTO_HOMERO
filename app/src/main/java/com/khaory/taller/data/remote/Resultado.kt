package com.khaory.taller.data.remote

import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Clasificación de los errores de red para mostrar un mensaje claro al usuario. */
enum class TipoError { SIN_CONEXION, TIEMPO_AGOTADO, CLIENTE, SERVIDOR, JSON_INVALIDO, DESCONOCIDO }

/** Resultado de una llamada REST: éxito con datos y código HTTP, o error ya traducido a español. */
sealed class Resultado<out T> {
    data class Exito<out T>(val datos: T, val codigoHttp: Int) : Resultado<T>()
    data class Error(
        val mensaje: String,
        val tipo: TipoError,
        val codigoHttp: Int? = null
    ) : Resultado<Nothing>()
}

/** Ejecuta una llamada Retrofit que devuelve JSON y captura los errores de red, HTTP y de formato. */
suspend fun <T> llamadaSegura(llamada: suspend () -> Response<T>): Resultado<T> = try {
    val respuesta = llamada()
    if (respuesta.isSuccessful) {
        val cuerpo = respuesta.body()
        if (cuerpo != null) Resultado.Exito(cuerpo, respuesta.code())
        else Resultado.Error("El servicio respondió sin contenido.", TipoError.JSON_INVALIDO, respuesta.code())
    } else {
        errorHttp(respuesta.code())
    }
} catch (e: CancellationException) {
    throw e // nunca se debe tragar la cancelación de una corrutina
} catch (e: Exception) {
    errorDeExcepcion(e)
}

/** Igual que [llamadaSegura], para operaciones sin cuerpo en la respuesta (por ejemplo DELETE). */
suspend fun llamadaSinCuerpo(llamada: suspend () -> Response<Unit>): Resultado<Unit> = try {
    val respuesta = llamada()
    if (respuesta.isSuccessful) Resultado.Exito(Unit, respuesta.code()) else errorHttp(respuesta.code())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    errorDeExcepcion(e)
}

private fun errorHttp(codigo: Int): Resultado.Error = when (codigo) {
    404 -> Resultado.Error("Recurso no encontrado (HTTP 404).", TipoError.CLIENTE, codigo)
    in 400..499 -> Resultado.Error("El servicio rechazó la solicitud (HTTP $codigo).", TipoError.CLIENTE, codigo)
    in 500..599 -> Resultado.Error("El servicio no está disponible por ahora (HTTP $codigo).", TipoError.SERVIDOR, codigo)
    else -> Resultado.Error("Respuesta inesperada del servicio (HTTP $codigo).", TipoError.DESCONOCIDO, codigo)
}

private fun errorDeExcepcion(e: Exception): Resultado.Error = when (e) {
    is SocketTimeoutException ->
        Resultado.Error("El servicio tardó demasiado en responder.", TipoError.TIEMPO_AGOTADO)
    is UnknownHostException ->
        Resultado.Error("Sin conexión a internet. Revisa tu red e inténtalo de nuevo.", TipoError.SIN_CONEXION)
    is JsonParseException ->
        Resultado.Error("La respuesta del servicio no tiene el formato esperado.", TipoError.JSON_INVALIDO)
    is IOException ->
        Resultado.Error("No se pudo comunicar con el servicio. Revisa tu conexión.", TipoError.SIN_CONEXION)
    else ->
        Resultado.Error("Ocurrió un error inesperado.", TipoError.DESCONOCIDO)
}
