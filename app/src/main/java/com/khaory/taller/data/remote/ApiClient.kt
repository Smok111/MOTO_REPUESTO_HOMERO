package com.khaory.taller.data.remote

import com.khaory.taller.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Configuración única de Retrofit/OkHttp: HTTPS, tiempos de espera y conversión JSON con Gson. */
object ApiClient {
    private const val URL_TIPO_CAMBIO = "https://open.er-api.com/"
    private const val URL_PUBLICACIONES = "https://jsonplaceholder.typicode.com/"

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .addInterceptor { cadena ->
                cadena.proceed(
                    cadena.request().newBuilder()
                        .header("User-Agent", "KhaoryTaller/1.0 (Android)")
                        .header("Accept", "application/json")
                        .build()
                )
            }
            .apply {
                // El registro de peticiones solo se activa en compilaciones de depuración
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()
    }

    private fun retrofit(urlBase: String): Retrofit = Retrofit.Builder()
        .baseUrl(urlBase)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val tipoCambio: TipoCambioApi by lazy { retrofit(URL_TIPO_CAMBIO).create(TipoCambioApi::class.java) }
    val publicaciones: PublicacionesApi by lazy { retrofit(URL_PUBLICACIONES).create(PublicacionesApi::class.java) }
}
