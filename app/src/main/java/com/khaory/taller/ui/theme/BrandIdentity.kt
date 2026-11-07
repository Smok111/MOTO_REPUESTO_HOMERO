package com.khaory.taller.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khaory.taller.R

@Composable
fun BrandIdentity(compact: Boolean = false) {
    val size = if (compact) 42.dp else 70.dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.brand_mark),
            contentDescription = "Logo Khaory",
            modifier = Modifier.size(size).clip(RoundedCornerShape(if (compact) 13.dp else 20.dp))
                .background(DarkBackground)
        )
        Spacer(Modifier.width(if (compact) 10.dp else 14.dp))
        Column {
            Text("KHAORY", fontWeight = FontWeight.Black,
                fontSize = if (compact) 18.sp else 29.sp, letterSpacing = if (compact) 1.4.sp else 2.5.sp)
            Text("MOTO REPUESTOS", color = OrangePrimary,
                fontSize = if (compact) 9.sp else 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}
