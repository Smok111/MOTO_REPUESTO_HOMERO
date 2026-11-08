package com.khaory.taller.data.remote

import com.google.gson.annotations.SerializedName

/** Respuesta JSON del servicio de tipo de cambio (open.er-api.com). */
data class TipoCambioDto(
    @SerializedName("result") val resultado: String?,
    @SerializedName("base_code") val moneda: String?,
    @SerializedName("time_last_update_utc") val actualizado: String?,
    @SerializedName("rates") val tasas: Map<String, Double>?
)

/** Recurso JSON "post" del servicio REST de pruebas (JSONPlaceholder), usado para el CRUD. */
data class PublicacionDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("userId") val usuarioId: Int = 1,
    @SerializedName("title") val titulo: String,
    @SerializedName("body") val cuerpo: String
)
