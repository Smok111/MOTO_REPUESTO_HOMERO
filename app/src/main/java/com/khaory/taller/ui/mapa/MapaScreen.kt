package com.khaory.taller.ui.mapa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.khaory.taller.ui.theme.OrangePrimary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapaScreen() {
    val context = LocalContext.current

    // 1. TUS COORDENADAS EXACTAS
    val lat = -7.123301
    val lon = -78.452873
    val tallerLocation = GeoPoint(lat, lon)

    // Inicializar osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Usar Scaffold para que el mapa no tape el título de arriba
    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                Column(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text("UBICACIÓN", color = OrangePrimary, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Encuéntranos", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Visítanos en nuestro taller",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }
    ) { paddingValues ->

        Box(Modifier.fillMaxSize().padding(paddingValues)) {

            // 2. MAPA CON BORDES REDONDEADOS
            Box(
                Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.3f),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    )
            ) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(16.0)
                            controller.setCenter(tallerLocation)

                            val marker = Marker(this)
                            marker.position = tallerLocation
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = "MotoRepuestos Khaory"
                            marker.snippet = "Ubicación exacta del taller"
                            overlays.add(marker)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. TARJETA FLOTANTE INFERIOR CON EL BOTÓN "IR"
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = Color.Black.copy(alpha = 0.4f)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícono de ubicación
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = OrangePrimary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text("MotoRepuestos Khaory", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Cajamarca, Perú", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("Lun-Sáb: 8:00 - 18:00", fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.Medium)
                    }

                    // 4. BOTÓN "IR" QUE ABRE EL MAPA DEL MÓVIL
                    OutlinedButton(
                        onClick = {
                            // Este enlace genérico abre la app de mapas que tenga instalada el usuario
                            val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(MotoRepuestos+Khaory)")
                            val intent = Intent(Intent.ACTION_VIEW, uri)

                            try {
                                context.startActivity(intent)
                                Toast.makeText(context, "Abriendo ubicación exacta...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo abrir el mapa", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Directions, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ir", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
