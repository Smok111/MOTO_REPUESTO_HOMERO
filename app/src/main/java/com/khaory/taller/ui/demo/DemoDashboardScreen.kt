package com.khaory.taller.ui.demo

import android.Manifest
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.Geocoder
import android.location.Address
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khaory.taller.R
import com.khaory.taller.ui.login.UserRole
import com.khaory.taller.ui.mapa.MapaScreen
import com.khaory.taller.ui.citas.CitasScreen
import com.khaory.taller.data.location.addressForLocation
import com.khaory.taller.data.location.cachedStreetAddress
import com.khaory.taller.data.location.isRecentLocation
import com.khaory.taller.data.location.rememberStreetAddress
import com.khaory.taller.ui.theme.BrandIdentity
import com.khaory.taller.ui.theme.OrangePrimary
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import coil.compose.AsyncImage

private enum class ClientTab(val title: String, val icon: ImageVector) {
    CATALOG("Repuestos", Icons.Default.Storefront),
    CART("Carrito", Icons.Default.ShoppingCart),
    ORDERS("Pedidos", Icons.Default.ReceiptLong),
    APPOINTMENTS("Citas", Icons.Default.EventAvailable),
    MAP("Ubicación", Icons.Default.Map),
    ACCOUNT("Cuenta", Icons.Default.Person)
}

private enum class AdminTab(val title: String, val icon: ImageVector) {
    HOME("Resumen", Icons.Default.Dashboard),
    INVENTORY("Inventario", Icons.Default.Inventory2),
    ORDERS("Pedidos", Icons.Default.ReceiptLong),
    APPOINTMENTS("Citas", Icons.Default.EventAvailable),
    MAP("Ubicación", Icons.Default.Map),
    ACCOUNT("Cuenta", Icons.Default.Person)
}

@Composable
fun DemoDashboardScreen(
    email: String,
    role: UserRole,
    onLogout: () -> Unit,
    store: DemoStoreViewModel = viewModel()
) {
    var clientTab by remember { mutableStateOf(ClientTab.CATALOG) }
    var adminTab by remember { mutableStateOf(AdminTab.HOME) }
    var notice by remember { mutableStateOf<String?>(null) }
    val isAdmin = role == UserRole.ADMIN
    LaunchedEffect(email, role) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { store.connect(it, role) }
    }
    LaunchedEffect(store.message) {
        store.message?.let { notice = it; store.clearMessage() }
    }
    LaunchedEffect(notice) {
        if (notice != null) { delay(2800); notice = null }
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    BrandIdentity(compact = true)
                    Spacer(Modifier.weight(1f))
                    Surface(color = OrangePrimary.copy(alpha = 0.13f), shape = RoundedCornerShape(30.dp)) {
                        Text("KHAORY", color = OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                if (isAdmin) AdminTab.entries.forEach { tab ->
                    NavigationBarItem(selected = adminTab == tab, onClick = { adminTab = tab; notice = null },
                        modifier = Modifier.testTag("admin_${tab.name.lowercase()}_tab"),
                        alwaysShowLabel = false, icon = { Icon(tab.icon, tab.title) }, label = { Text(tab.title) })
                } else ClientTab.entries.forEach { tab ->
                    val badge = tab == ClientTab.CART && store.cart.values.sum() > 0
                    NavigationBarItem(selected = clientTab == tab, onClick = { clientTab = tab; notice = null },
                        icon = {
                            if (badge) BadgedBox(badge = { Badge { Text(store.cart.values.sum().toString()) } }) {
                                Icon(tab.icon, tab.title)
                            } else Icon(tab.icon, tab.title)
                        }, alwaysShowLabel = false, label = { Text(tab.title) })
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            AnimatedVisibility(notice != null) {
                Surface(color = OrangePrimary.copy(alpha = 0.18f), modifier = Modifier.fillMaxWidth()) {
                    Text(notice.orEmpty(), color = OrangePrimary, modifier = Modifier.padding(14.dp))
                }
            }
            Box(Modifier.fillMaxSize()) {
                if (isAdmin) AnimatedContent(adminTab,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "admin-tab") { activeTab ->
                    when (activeTab) {
                        AdminTab.HOME -> AdminOverview(store, onInventory = { adminTab = AdminTab.INVENTORY },
                            onOrders = { adminTab = AdminTab.ORDERS })
                        AdminTab.INVENTORY -> InventoryScreen(store) { notice = it }
                        AdminTab.ORDERS -> OrdersScreen(store.orders, admin = true, onAdvance = store::advanceOrder)
                        AdminTab.APPOINTMENTS -> CitasScreen(email, admin = true)
                        AdminTab.MAP -> MapaScreen()
                        AdminTab.ACCOUNT -> AccountScreen(email, role, onLogout)
                    }
                } else AnimatedContent(clientTab,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "client-tab") { activeTab ->
                    when (activeTab) {
                        ClientTab.CATALOG -> CatalogScreen(store, onAdded = { notice = it }, onOpenCart = { clientTab = ClientTab.CART })
                        ClientTab.CART -> CartScreen(store, email, onMessage = { notice = it }, onOrdered = {
                            notice = "Pedido creado. Puedes seguirlo aquí."; clientTab = ClientTab.ORDERS
                        })
                        ClientTab.ORDERS -> OrdersScreen(store.orders, admin = false)
                        ClientTab.APPOINTMENTS -> CitasScreen(email, admin = false)
                        ClientTab.MAP -> MapaScreen()
                        ClientTab.ACCOUNT -> AccountScreen(email, role, onLogout)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
private fun ProductIcon(category: String) {
    val icon = when (category) {
        "Lubricantes" -> Icons.Default.OilBarrel
        "Frenos" -> Icons.Default.TireRepair
        "Eléctrico" -> Icons.Default.BatteryChargingFull
        "Accesorios" -> Icons.Default.SportsMotorsports
        else -> Icons.Default.Settings
    }
    Surface(shape = RoundedCornerShape(16.dp), color = OrangePrimary.copy(alpha = 0.14f)) {
        Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = OrangePrimary, modifier = Modifier.size(31.dp))
        }
    }
}

@Composable
private fun ProductVisual(product: DemoProduct, modifier: Modifier = Modifier) {
    val photo = remember(product.photoBytes) {
        product.photoBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }
    val imageRes = when (product.id) {
        1 -> R.drawable.product_oil
        2 -> R.drawable.product_filter
        3 -> R.drawable.product_brakes
        4 -> R.drawable.product_spark
        5 -> R.drawable.product_chain
        6 -> R.drawable.product_battery
        7 -> R.drawable.product_drivekit
        8 -> R.drawable.product_helmet
        else -> null
    }
    Box(modifier.clip(RoundedCornerShape(15.dp)).background(Color(0xFFE8E6E2)),
        contentAlignment = Alignment.Center) {
        when {
            photo != null -> Image(
                bitmap = photo, contentDescription = product.name,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            imageRes != null -> Image(painterResource(imageRes), product.name,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else -> ProductIcon(product.category)
        }
    }
}

@Composable
private fun CatalogScreen(store: DemoStoreViewModel, onAdded: (String) -> Unit, onOpenCart: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todos") }
    val categories = listOf("Todos") + store.products.map { it.category }.distinct()
    val filtered = store.products.filter {
        (category == "Todos" || it.category == category) &&
                (search.isBlank() || it.name.contains(search, ignoreCase = true) || it.category.contains(search, ignoreCase = true))
    }
    LazyColumn(modifier = Modifier.testTag("catalog_list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("CATÁLOGO KHAORY", color = OrangePrimary, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(7.dp))
            SectionTitle("Encuentra tu repuesto", "Lo que tu moto necesita, en un solo lugar")
            Box(Modifier.fillMaxWidth().height(166.dp).clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF9E4513), Color(0xFF3A251C), Color(0xFF20252A))))) {
                Icon(Icons.Default.Speed, null, tint = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.align(Alignment.CenterEnd).size(148.dp).padding(end = 8.dp))
                Column(Modifier.align(Alignment.CenterStart).padding(20.dp)) {
                    Text("LISTOS PARA RODAR", color = Color(0xFFFFC294),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("Piezas que mueven\ntu próxima ruta", color = Color.White,
                        fontSize = 22.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { category = "Transmisión" }, contentPadding = PaddingValues(0.dp)) {
                        Text("Explorar transmisión  →", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(search, { search = it }, label = { Text("Buscar repuesto") },
                leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { item ->
                    item {
                        FilterChip(selected = category == item, onClick = { category = item },
                            label = { Text(item) }, shape = RoundedCornerShape(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Repuestos disponibles", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        if (filtered.isEmpty()) item { EmptyState("No encontramos repuestos", "Prueba con otro nombre o categoría.") }
        items(filtered.chunked(2), key = { it.first().id }) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { product ->
                    CatalogProductCard(product, store, Modifier.weight(1f)) {
                        onAdded("${product.name} agregado al carrito")
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (store.cart.isNotEmpty()) item {
            OutlinedButton(onClick = onOpenCart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ShoppingCart, null); Spacer(Modifier.width(8.dp))
                Text("Ver carrito (${store.cart.values.sum()})")
            }
        }
    }
}

@Composable
private fun CatalogProductCard(product: DemoProduct, store: DemoStoreViewModel,
                               modifier: Modifier = Modifier, onAdded: () -> Unit) {
    Surface(modifier = modifier.animateContentSize(), shape = RoundedCornerShape(19.dp),
        color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(10.dp)) {
            ProductVisual(product, Modifier.fillMaxWidth().height(135.dp))
            Spacer(Modifier.height(10.dp))
            Text(product.category.uppercase(), color = OrangePrimary, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Text(product.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                minLines = 2, lineHeight = 18.sp, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text("S/ ${product.price}", fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text(if (product.stock > 0) "${product.stock} disponibles" else "Agotado",
                color = if (product.stock > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Button(enabled = product.stock > (store.cart[product.id] ?: 0),
                onClick = { if (store.addToCart(product.id)) onAdded() },
                modifier = Modifier.fillMaxWidth().height(38.dp),
                contentPadding = PaddingValues(horizontal = 5.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(11.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Text("Agregar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartScreen(store: DemoStoreViewModel, email: String, onMessage: (String) -> Unit, onOrdered: () -> Unit) {
    var address by remember { mutableStateOf("") }
    var deliveryLocation by remember { mutableStateOf<DeliveryLocation?>(null) }
    var addressConfirmed by remember { mutableStateOf(false) }
    var locationAccuracy by remember { mutableStateOf<Float?>(null) }
    var locating by remember { mutableStateOf(false) }
    var locationStatus by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locate: () -> Unit = {
        if (!locating) {
            locating = true
            locationStatus = null
            scope.launch {
                val token = CancellationTokenSource()
                try {
                    val precise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val priority = if (precise) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    val client = LocationServices.getFusedLocationProviderClient(context)
                    val current = try {
                        withTimeoutOrNull(20_000) { client.getCurrentLocation(priority, token.token).await() }
                    } catch (_: Exception) { null }
                    val recent = if (current == null) {
                        val cached = try { client.lastLocation.await() } catch (_: Exception) { null }
                        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        cached?.takeIf(::isRecentLocation)
                            ?: (if (precise) manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null)
                                ?.takeIf(::isRecentLocation)
                            ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                ?.takeIf(::isRecentLocation)
                    } else null
                    val fix = current ?: recent
                    if (fix == null) {
                        locationStatus = "No encontramos una ubicación reciente. Activa la ubicación del teléfono e inténtalo de nuevo."
                    } else {
                        deliveryLocation = DeliveryLocation(fix.latitude, fix.longitude)
                        locationAccuracy = if (fix.hasAccuracy()) fix.accuracy else null
                        addressConfirmed = false
                        val firstAddress = try {
                            withTimeoutOrNull(10_000) { addressForLocation(context, fix) }
                        } catch (_: Exception) { null }
                        val liveAddress = firstAddress ?: try {
                            delay(700)
                            withTimeoutOrNull(10_000) { addressForLocation(context, fix) }
                        } catch (_: Exception) { null }
                        if (liveAddress != null) rememberStreetAddress(context, fix, liveAddress)
                        val cachedAddress = if (liveAddress == null) cachedStreetAddress(context, fix) else null
                        val suggestedAddress = liveAddress ?: cachedAddress
                        val canSuggestAddress = address.isBlank() || address.startsWith("Ubicación GPS:")
                        if (canSuggestAddress) {
                            address = suggestedAddress ?: "Ubicación GPS: %.5f, %.5f".format(Locale.US, fix.latitude, fix.longitude)
                        }
                        locationStatus = if (!canSuggestAddress)
                            "Punto GPS agregado. Comprueba que coincida con la dirección que escribiste."
                        else if (suggestedAddress == null)
                            "No encontramos el nombre de la calle para este punto. Escribe calle, número y referencia antes de confirmar."
                        else if (cachedAddress != null)
                            "Dirección encontrada antes cerca de este punto. Revisa calle, número y referencia antes de confirmar."
                        else "Dirección sugerida. Revisa calle, número y referencia antes de confirmar."
                    }
                } catch (error: SecurityException) {
                    locationStatus = "Concede el permiso de ubicación para usar el GPS."
                } catch (error: Exception) {
                    locationStatus = "No se pudo obtener la ubicación: ${error.localizedMessage ?: "inténtalo otra vez"}."
                } finally {
                    token.cancel()
                    locating = false
                }
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) locate()
        else locationStatus = "Permiso denegado. Puedes escribir la dirección de entrega manualmente."
    }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Tu carrito", "Ajusta cantidades antes de confirmar") }
        if (store.cart.isEmpty()) item { EmptyState("Tu carrito está vacío", "Agrega productos desde Repuestos.") }
        items(store.cart.keys.toList(), key = { it }) { id ->
            val product = store.products.find { it.id == id }
            if (product != null) DemoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductVisual(product, Modifier.size(68.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.Bold)
                        Text("S/ ${product.price} c/u", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { store.removeFromCart(id) }) { Icon(Icons.Default.RemoveCircleOutline, "Quitar uno") }
                    Text("${store.cart[id] ?: 0}", fontWeight = FontWeight.Bold)
                    IconButton(enabled = (store.cart[id] ?: 0) < product.stock, onClick = { store.addToCart(id) }) {
                        Icon(Icons.Default.AddCircleOutline, "Agregar uno")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("S/ ${product.price * (store.cart[id] ?: 0)}", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (store.cart.isNotEmpty()) item {
            DemoCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total estimado", fontWeight = FontWeight.Bold)
                    Text("S/ ${store.cartTotal()}", color = OrangePrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                store.exchangeRate?.let { tc ->
                    Text("≈ US$ ${"%.2f".format(store.cartTotal() / tc)} · T.C. S/ ${"%.3f".format(tc)} por US$",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(address, { address = it; addressConfirmed = false }, label = { Text("Dirección exacta de entrega") },
                    minLines = 2, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    val hasLocationPermission = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                    if (hasLocationPermission) locate()
                    else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }, enabled = !locating && !store.isCheckingOut, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.MyLocation, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (locating) "Buscando ubicación..." else if (deliveryLocation == null) "Usar mi ubicación actual" else "Actualizar ubicación")
                }
                deliveryLocation?.let { location ->
                    Text("Punto GPS: %.5f, %.5f".format(location.latitude, location.longitude),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    locationAccuracy?.let { accuracy ->
                        Text("Precisión estimada: ±${accuracy.toInt()} m", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        deliveryLocation = null
                        locationAccuracy = null
                        addressConfirmed = false
                        if (address.startsWith("Ubicación GPS:")) address = ""
                        locationStatus = null
                    }) { Text("Quitar ubicación") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = addressConfirmed, onCheckedChange = { addressConfirmed = it })
                        Text("Confirmo que la dirección y el punto corresponden al lugar de entrega.", fontSize = 12.sp)
                    }
                }
                locationStatus?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                Text("El GPS sugiere una dirección; añade el número de casa y una referencia si faltan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    when {
                        address.isBlank() || address.startsWith("Ubicación GPS:") -> onMessage("Escribe una dirección con calle o referencia de entrega.")
                        deliveryLocation != null && !addressConfirmed -> onMessage("Confirma que la dirección y el punto GPS son correctos.")
                        else -> confirm = true
                    }
                }, enabled = !store.isCheckingOut, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) {
                    Text(if (store.isCheckingOut) "Creando pedido..." else "Crear pedido de prueba")
                }
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text("Confirmar pedido de prueba") },
        text = { Text("Se registrará un pedido de prueba por S/ ${store.cartTotal()}. No se realizará ningún cobro.") },
        confirmButton = { TextButton(onClick = {
            confirm = false
            store.checkout(email, address, deliveryLocation) { error ->
                if (error == null) { address = ""; deliveryLocation = null; addressConfirmed = false; onOrdered() } else onMessage(error)
            }
        }) { Text("Crear pedido") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Volver") } })
}

@Composable
private fun OrdersScreen(orders: List<DemoOrder>, admin: Boolean, onAdvance: (Int) -> Unit = {}) {
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle(if (admin) "Gestionar pedidos" else "Mis pedidos",
            if (admin) "Actualiza el avance de cada pedido" else "Consulta el estado de tus pedidos") }
        if (orders.isEmpty()) item { EmptyState("Todavía no hay pedidos", "Crea uno desde el carrito para verlo aquí.") }
        items(orders, key = { it.id }) { order ->
            DemoCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Pedido #${order.id}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    StatusPill(order.status)
                }
                if (admin) Text(order.email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                order.lines.forEach { line ->
                    Text("${line.quantity} × ${line.name}", fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text("Entrega: ${order.address}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                order.deliveryLocation?.let { location ->
                    TextButton(onClick = {
                        val coordinates = "${location.latitude},${location.longitude}"
                        val url = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(coordinates)}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }) {
                        Icon(Icons.Default.Place, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Ver ubicación en el mapa")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Total S/ ${order.total}", color = OrangePrimary, fontWeight = FontWeight.Bold)
                if (admin && order.status != "Entregado") {
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onAdvance(order.id) }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) {
                        Text(when (order.status) {
                            "Pendiente" -> "Marcar en preparación"
                            "En preparación" -> "Marcar en camino"
                            else -> "Marcar entregado"
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = when (status) {
        "Entregado" -> Color(0xFF5DD68A)
        "Pendiente" -> Color(0xFFFFC05A)
        else -> OrangePrimary
    }
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.17f)) {
        Text(status, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
private fun AdminOverview(store: DemoStoreViewModel, onInventory: () -> Unit, onOrders: () -> Unit) {
    val pending = store.orders.count { it.status != "Entregado" }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("PANEL DE CONTROL", color = OrangePrimary, fontWeight = FontWeight.Bold,
                fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            SectionTitle("Hola, administrador", "Tu tienda de un vistazo")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFB4511B), Color(0xFF6A351C), Color(0xFF25272B))))
                .padding(22.dp)) {
                Icon(Icons.Default.TrendingUp, null, tint = Color(0xFFFFC096), modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(14.dp))
                Text("Tu negocio en movimiento", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Gestiona inventario y pedidos desde aquí", color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$pending", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(9.dp))
                    Text("pedidos por atender", color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Productos", "${store.products.size}", Icons.Default.Inventory2, Modifier.weight(1f))
                MetricCard("Unidades", "${store.products.sumOf { it.stock }}", Icons.Default.Warehouse, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Stock bajo", "${store.products.count { it.stock <= 5 }}", Icons.Default.WarningAmber, Modifier.weight(1f))
                MetricCard("Pedidos", "${store.orders.size}", Icons.Default.ShoppingBag, Modifier.weight(1f))
            }
        }
        item {
            Text("Accesos rápidos", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            DemoCard(onClick = onInventory) { RowLink(Icons.Default.Inventory2, "Gestionar inventario") }
            Spacer(Modifier.height(10.dp))
            DemoCard(onClick = onOrders) { RowLink(Icons.Default.ReceiptLong, "Revisar pedidos") }
        }
        item {
            Text("Actividad reciente", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))
            Text(if (store.orders.isEmpty()) "Cuando un cliente cree un pedido, aparecerá aquí."
            else "Último pedido: #${store.orders.first().id} · ${store.orders.first().status}",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("Servicios conectados", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))
            Text("API REST: ${store.apiStatus}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = OrangePrimary)
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RowLink(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = OrangePrimary)
        Spacer(Modifier.width(14.dp))
        Text(text, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InventoryScreen(store: DemoStoreViewModel, onMessage: (String) -> Unit) {
    var editing by remember { mutableStateOf<DemoProduct?>(null) }
    var addProduct by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<DemoProduct?>(null) }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionTitle("Inventario", "Ajusta existencias o agrega un repuesto")
            Button(onClick = { addProduct = true }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Nuevo repuesto")
            }
        }
        items(store.products, key = { it.id }) { product ->
            DemoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductVisual(product, Modifier.size(62.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.Bold)
                        Text("${product.category} · S/ ${product.price}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End) {
                    Text("Stock: ${product.stock}", modifier = Modifier.weight(1f),
                        color = if (product.stock <= 5) Color(0xFFFFC05A) else MaterialTheme.colorScheme.onSurface)
                    IconButton(enabled = product.stock > 0, onClick = { store.changeStock(product.id, -1) }) {
                        Icon(Icons.Default.RemoveCircleOutline, "Reducir stock")
                    }
                    IconButton(onClick = { store.changeStock(product.id, 1) }) {
                        Icon(Icons.Default.AddCircleOutline, "Aumentar stock")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { editing = product }) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp))
                        Text("Editar")
                    }
                    TextButton(onClick = { deleteTarget = product }) {
                        Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp))
                        Text("Retirar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (addProduct || editing != null) ProductEditorDialog(editing, onDismiss = {
        addProduct = false; editing = null
    }) { name, category, description, price, stock, selectedPhoto ->
        val prior = editing
        val error = when {
            name.isBlank() || category.isBlank() -> "Completa nombre y categoría."
            price <= 0 || stock < 0 -> "Revisa el precio y el stock."
            prior == null && selectedPhoto == null -> "Selecciona una foto del repuesto."
            else -> {
                val photoPath = if (selectedPhoto == null) prior?.photoPath else store.importPhoto(selectedPhoto)
                if (selectedPhoto != null && photoPath == null) "No se pudo guardar la foto."
                else if (prior == null) store.addProduct(name, category, description, price, stock, photoPath)
                else store.updateProduct(prior.id, name, category, description, price, stock, photoPath)
            }
        }
        if (error == null) {
            addProduct = false; editing = null
            onMessage("Guardando repuesto...")
        }
        error
    }
    deleteTarget?.let { product ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Retirar repuesto") },
            text = { Text("¿Retirar ${product.name} del catálogo? Los pedidos anteriores conservarán su detalle.") },
            confirmButton = { TextButton(onClick = {
                store.deleteProduct(product.id); deleteTarget = null
            }) { Text("Retirar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun ProductEditorDialog(product: DemoProduct?, onDismiss: () -> Unit,
                                onSave: (String, String, String, Int, Int, Uri?) -> String?) {
    var name by remember(product?.id) { mutableStateOf(product?.name.orEmpty()) }
    var category by remember(product?.id) { mutableStateOf(product?.category.orEmpty()) }
    var description by remember(product?.id) { mutableStateOf(product?.description.orEmpty()) }
    var price by remember(product?.id) { mutableStateOf(product?.price?.toString().orEmpty()) }
    var stock by remember(product?.id) { mutableStateOf(product?.stock?.toString().orEmpty()) }
    var selectedPhoto by remember(product?.id) { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPhoto = uri
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Nuevo repuesto" else "Editar repuesto") },
        text = {
            Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedPhoto != null) AsyncImage(model = selectedPhoto, contentDescription = "Foto del repuesto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(12.dp)))
                else if (product != null) ProductVisual(product, Modifier.fillMaxWidth().height(90.dp))
                OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AddPhotoAlternate, null); Spacer(Modifier.width(8.dp))
                    Text(if (product == null && selectedPhoto == null) "Elegir foto" else "Cambiar foto")
                }
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Descripción") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Precio S/") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(stock, { stock = it.filter(Char::isDigit) }, label = { Text("Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        modifier = Modifier.weight(1f))
                }
                if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = {
            error = onSave(name, category, description, price.toIntOrNull() ?: 0,
                stock.toIntOrNull() ?: -1, selectedPhoto)
        }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun AccountScreen(email: String, role: UserRole, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        SectionTitle("Mi cuenta", "Tu sesión de Firebase")
        DemoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, null, tint = OrangePrimary, modifier = Modifier.size(52.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(email, fontWeight = FontWeight.Bold)
                    Text(if (role == UserRole.ADMIN) "Administrador" else "Cliente",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Los pedidos y el inventario se sincronizan con Firebase.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Cerrar sesión")
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.SearchOff, null, tint = OrangePrimary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun DemoCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
