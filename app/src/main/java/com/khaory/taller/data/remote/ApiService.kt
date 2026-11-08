package com.khaory.taller.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** GET: tipo de cambio USD → PEN (sin API key). */
interface TipoCambioApi {
    @GET("v6/latest/USD")
    suspend fun obtenerTipoCambio(): Response<TipoCambioDto>
}

/** CRUD completo sobre JSON: Create (POST), Read (GET), Update (PUT) y Delete (DELETE). */
interface PublicacionesApi {
    @GET("posts")
    suspend fun listar(@Query("_limit") limite: Int = 3): Response<List<PublicacionDto>>

    @GET("posts/{id}")
    suspend fun obtener(@Path("id") id: Int): Response<PublicacionDto>

    @POST("posts")
    suspend fun crear(@Body publicacion: PublicacionDto): Response<PublicacionDto>

    @PUT("posts/{id}")
    suspend fun actualizar(@Path("id") id: Int, @Body publicacion: PublicacionDto): Response<PublicacionDto>

    @DELETE("posts/{id}")
    suspend fun eliminar(@Path("id") id: Int): Response<Unit>
}
