package com.khaory.taller.ui.demo

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.GeoPoint
import com.khaory.taller.ui.login.UserRole
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

data class DemoProduct(
    val id: Int,
    val name: String,
    val category: String,
    val description: String,
    val price: Int,
    val stock: Int,
    val photoPath: String? = null,
    val photoBytes: ByteArray? = null
)

data class DemoOrderLine(val productId: Int, val name: String, val quantity: Int, val price: Int)

data class DeliveryLocation(val latitude: Double, val longitude: Double)

data class DemoOrder(
    val id: Int,
    val userId: String,
    val email: String,
    val address: String,
    val deliveryLocation: DeliveryLocation?,
    val lines: List<DemoOrderLine>,
    val total: Int,
    val status: String = "Pendiente",
    val createdAt: Long = 0L
)

/** Catálogo y pedidos compartidos en Firestore. El carrito solo vive en este dispositivo. */
class DemoStoreViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private var productsListener: ListenerRegistration? = null
    private var ordersListener: ListenerRegistration? = null
    private var connectedUid: String? = null
    private var connectedRole: UserRole? = null

    val products = mutableStateListOf<DemoProduct>()
    val orders = mutableStateListOf<DemoOrder>()
    val cart = mutableStateMapOf<Int, Int>()
    var message by mutableStateOf<String?>(null)
        private set
    var isCheckingOut by mutableStateOf(false)
        private set

    fun connect(uid: String, role: UserRole) {
        if (connectedUid == uid && connectedRole == role) return
        productsListener?.remove()
        ordersListener?.remove()
        connectedUid = uid
        connectedRole = role
        products.clear()
        orders.clear()
        cart.clear()

        productsListener = db.collection("products").addSnapshotListener { snapshot, error ->
            if (error != null) { message = "No se pudo cargar el catálogo: ${error.localizedMessage}"; return@addSnapshotListener }
            products.clear()
            products.addAll(snapshot?.documents.orEmpty().mapNotNull { doc ->
                val id = doc.id.toIntOrNull() ?: return@mapNotNull null
                DemoProduct(id, doc.getString("name").orEmpty(), doc.getString("category").orEmpty(),
                    doc.getString("description").orEmpty(), (doc.getLong("price") ?: 0).toInt(),
                    (doc.getLong("stock") ?: 0).toInt(),
                    photoBytes = doc.getBlob("photoBytes")?.toBytes())
            }.sortedBy { it.name })
        }

        val query = if (role == UserRole.ADMIN) db.collection("orders")
            else db.collection("orders").whereEqualTo("userId", uid)
        ordersListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) { message = "No se pudieron cargar los pedidos: ${error.localizedMessage}"; return@addSnapshotListener }
            orders.clear()
            orders.addAll(snapshot?.documents.orEmpty().mapNotNull { doc ->
                val id = doc.id.toIntOrNull() ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                val lines = (doc.get("lines") as? List<Map<String, Any?>>).orEmpty().map { line ->
                    DemoOrderLine((line["productId"] as? Number)?.toInt() ?: 0,
                        line["name"] as? String ?: "", (line["quantity"] as? Number)?.toInt() ?: 0,
                        (line["price"] as? Number)?.toInt() ?: 0)
                }
                DemoOrder(id, doc.getString("userId").orEmpty(), doc.getString("email").orEmpty(),
                    doc.getString("address").orEmpty(), doc.getGeoPoint("deliveryLocation")?.let {
                        DeliveryLocation(it.latitude, it.longitude)
                    }, lines, (doc.getLong("total") ?: 0).toInt(),
                    doc.getString("status") ?: "Pendiente", doc.getLong("createdAt") ?: 0L)
            }.sortedByDescending { it.createdAt })
        }
    }

    fun clearMessage() { message = null }

    fun addToCart(productId: Int): Boolean {
        val product = products.find { it.id == productId } ?: return false
        val current = cart[productId] ?: 0
        if (current >= product.stock) return false
        cart[productId] = current + 1
        return true
    }

    fun removeFromCart(productId: Int) {
        val quantity = cart[productId] ?: return
        if (quantity <= 1) cart.remove(productId) else cart[productId] = quantity - 1
    }

    fun cartTotal(): Int = cart.entries.sumOf { (id, quantity) ->
        (products.find { it.id == id }?.price ?: 0) * quantity
    }

    fun checkout(email: String, address: String, deliveryLocation: DeliveryLocation?, onResult: (String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { onResult("Inicia sesión para crear un pedido."); return }
        if (cart.isEmpty()) { onResult("Agrega repuestos al carrito."); return }
        if (address.isBlank()) { onResult("Ingresa una dirección o referencia de entrega."); return }
        if (isCheckingOut) return
        val selected = cart.toMap()
        val orderId = freshId()
        isCheckingOut = true
        viewModelScope.launch {
            try {
                db.runTransaction { transaction ->
                    // Todas las lecturas deben ocurrir antes de cualquier escritura.
                    val current = selected.map { (id, quantity) ->
                        val ref = db.collection("products").document(id.toString())
                        Triple(ref, transaction.get(ref), quantity)
                    }
                    val lines = current.map { (_, doc, quantity) ->
                        val stock = (doc.getLong("stock") ?: 0).toInt()
                        require(doc.exists() && quantity > 0 && stock >= quantity) { "El stock cambió. Revisa el carrito." }
                        DemoOrderLine(doc.id.toInt(), doc.getString("name").orEmpty(), quantity,
                            (doc.getLong("price") ?: 0).toInt())
                    }
                    current.forEach { (ref, doc, quantity) ->
                        transaction.update(ref, "stock", (doc.getLong("stock") ?: 0) - quantity)
                    }
                    val orderData = mutableMapOf<String, Any>(
                        "userId" to uid, "email" to email, "address" to address.trim(),
                        "lines" to lines.map { mapOf("productId" to it.productId, "name" to it.name,
                            "quantity" to it.quantity, "price" to it.price) },
                        "total" to lines.sumOf { it.price * it.quantity },
                        "status" to "Pendiente", "createdAt" to System.currentTimeMillis()
                    )
                    deliveryLocation?.let { orderData["deliveryLocation"] = GeoPoint(it.latitude, it.longitude) }
                    transaction.set(db.collection("orders").document(orderId.toString()), orderData)
                }.await()
                cart.clear()
                onResult(null)
            } catch (error: Exception) {
                onResult(error.localizedMessage ?: "No se pudo crear el pedido.")
            } finally {
                isCheckingOut = false
            }
        }
    }

    fun changeStock(productId: Int, delta: Int) {
        viewModelScope.launch {
            try {
                val ref = db.collection("products").document(productId.toString())
                db.runTransaction { transaction ->
                    val stock = (transaction.get(ref).getLong("stock") ?: 0).toInt()
                    transaction.update(ref, "stock", (stock + delta).coerceAtLeast(0))
                }.await()
            } catch (error: Exception) { message = "No se pudo cambiar el stock: ${error.localizedMessage}" }
        }
    }

    fun addProduct(name: String, category: String, description: String, price: Int, stock: Int,
        photoPath: String?): String? {
        if (name.isBlank() || category.isBlank()) return "Completa nombre y categoría."
        if (price <= 0 || stock < 0) return "Revisa el precio y el stock."
        if (photoPath == null) return "Selecciona una foto del repuesto."
        val id = freshId()
        viewModelScope.launch {
            try {
                val photo = compressPhoto(photoPath)
                db.collection("products").document(id.toString()).set(productData(name, category,
                    description, price, stock, photo)).await()
                message = "Repuesto agregado al inventario."
            } catch (error: Exception) { message = "No se pudo agregar el repuesto: ${error.localizedMessage}" }
        }
        return null
    }

    fun updateProduct(id: Int, name: String, category: String, description: String,
        price: Int, stock: Int, photoPath: String?): String? {
        if (name.isBlank() || category.isBlank()) return "Completa nombre y categoría."
        if (price <= 0 || stock < 0) return "Revisa el precio y el stock."
        viewModelScope.launch {
            try {
                val photo = if (photoPath != null && File(photoPath).exists()) compressPhoto(photoPath)
                    else products.find { it.id == id }?.photoBytes
                db.collection("products").document(id.toString()).update(productData(name, category,
                    description, price, stock, photo)).await()
                message = "Repuesto actualizado."
            } catch (error: Exception) { message = "No se pudo actualizar el repuesto: ${error.localizedMessage}" }
        }
        return null
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            try {
                db.collection("products").document(id.toString()).delete().await()
                cart.remove(id)
                message = "Repuesto retirado del catálogo."
            } catch (error: Exception) { message = "No se pudo retirar el repuesto: ${error.localizedMessage}" }
        }
    }

    fun importPhoto(uri: Uri): String? = runCatching {
        val mime = getApplication<Application>().contentResolver.getType(uri)
        val extension = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val file = File(getApplication<Application>().cacheDir, "product_${System.nanoTime()}.$extension")
        val input = getApplication<Application>().contentResolver.openInputStream(uri)
            ?: error("No se pudo abrir la foto")
        input.use { source -> file.outputStream().use { destination -> source.copyTo(destination) } }
        file.absolutePath
    }.getOrNull()

    fun advanceOrder(orderId: Int) {
        viewModelScope.launch {
            try {
                val ref = db.collection("orders").document(orderId.toString())
                db.runTransaction { transaction ->
                    val status = transaction.get(ref).getString("status")
                    val next = when (status) {
                        "Pendiente" -> "En preparación"
                        "En preparación" -> "En camino"
                        "En camino" -> "Entregado"
                        else -> error("Este pedido ya no se puede avanzar.")
                    }
                    transaction.update(ref, "status", next)
                }.await()
                message = "Estado del pedido actualizado."
            } catch (error: Exception) { message = "No se pudo actualizar el pedido: ${error.localizedMessage}" }
        }
    }

    private suspend fun compressPhoto(path: String): ByteArray = withContext(Dispatchers.IO) {
        val file = File(path)
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "La foto no es válida." }
            var sample = 1
            while (bounds.outWidth / sample > 960 || bounds.outHeight / sample > 960) sample *= 2
            val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: error("No se pudo abrir la foto.")
            val scale = (720f / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
            val resized = if (scale < 1f) Bitmap.createScaledBitmap(bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1), true) else bitmap
            var quality = 80
            var bytes: ByteArray
            do {
                val output = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
                bytes = output.toByteArray()
                quality -= 10
            } while (bytes.size > 250_000 && quality >= 30)
            require(bytes.size <= 300_000) { "La foto es demasiado grande. Elige una más sencilla." }
            bytes
        } finally {
            file.delete()
        }
    }

    private fun productData(name: String, category: String, description: String, price: Int,
        stock: Int, photoBytes: ByteArray?) = mapOf(
        "name" to name.trim(), "category" to category.trim(), "description" to description.trim(),
        "price" to price, "stock" to stock, "photoBytes" to photoBytes?.let(Blob::fromBytes)
    )

    private fun freshId() = UUID.randomUUID().hashCode() and Int.MAX_VALUE

    override fun onCleared() {
        productsListener?.remove()
        ordersListener?.remove()
        super.onCleared()
    }
}
