package com.khaory.taller.data.remote

/** Punto único de acceso a los servicios REST; la interfaz no conoce Retrofit. */
object RestRepository {
    private val publicaciones get() = ApiClient.publicaciones

    /** READ: tipo de cambio PEN por cada US$ 1. */
    suspend fun tipoCambioPen(): Resultado<Double> =
        when (val r = llamadaSegura { ApiClient.tipoCambio.obtenerTipoCambio() }) {
            is Resultado.Exito -> {
                val tasa = r.datos.tasas?.get("PEN")
                if (r.datos.resultado == "success" && tasa != null && tasa > 0.0) Resultado.Exito(tasa, r.codigoHttp)
                else Resultado.Error("El servicio no devolvió el tipo de cambio.", TipoError.JSON_INVALIDO, r.codigoHttp)
            }
            is Resultado.Error -> r
        }

    /** READ: consulta de disponibilidad del servicio (lista corta de recursos). */
    suspend fun consultarServicio(): Resultado<List<PublicacionDto>> =
        llamadaSegura { publicaciones.listar(3) }

    /** CREATE: registra en el servicio REST un repuesto nuevo del inventario. */
    suspend fun crearProducto(nombre: String, categoria: String, precio: Int, stock: Int): Resultado<PublicacionDto> =
        llamadaSegura {
            publicaciones.crear(PublicacionDto(titulo = nombre, cuerpo = detalle(categoria, precio, stock)))
        }

    /** UPDATE: actualiza el repuesto en el servicio REST. */
    suspend fun actualizarProducto(id: Int, nombre: String, categoria: String, precio: Int, stock: Int): Resultado<PublicacionDto> {
        val remoto = idRemoto(id)
        return llamadaSegura {
            publicaciones.actualizar(remoto, PublicacionDto(id = remoto, titulo = nombre, cuerpo = detalle(categoria, precio, stock)))
        }
    }

    /** DELETE: retira el repuesto en el servicio REST. */
    suspend fun retirarProducto(id: Int): Resultado<Unit> =
        llamadaSinCuerpo { publicaciones.eliminar(idRemoto(id)) }

    private fun detalle(categoria: String, precio: Int, stock: Int) =
        "categoria=$categoria; precio=S/ $precio; stock=$stock"

    // El servicio de pruebas solo admite ids del 1 al 100 para editar o eliminar
    private fun idRemoto(id: Int): Int = (id - 1).mod(100) + 1
}
