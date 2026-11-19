package com.petromobile.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PetroMobileApp() }
    }
}
private data class IprPoint(val rate: Float, val pressure: Float)
private fun vogelRate(qMax: Float, pwf: Float, pr: Float): Float {
    if (pr <= 0f || pwf >= pr) return 0f
    val x = (pwf / pr).coerceIn(0f, 1f)
    return qMax * (1f - 0.2f * x - 0.8f * x * x)
}
@Composable
fun PetroMobileApp() {
    var well by remember { mutableStateOf("Well-01") }
    var prText by remember { mutableStateOf("3000") }
    var pbText by remember { mutableStateOf("2000") }
    var qText by remember { mutableStateOf("1000") }
    var points by remember { mutableStateOf<List<IprPoint>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val navy = Color(0xFF10243A)
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF2878B9))) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF3F6FA)).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("PETROMOBILE", color = navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Well Engineering", color = navy, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Independent prototype 0.1", color = Color.Gray, fontSize = 12.sp)
            Card(colors = CardDefaults.cardColors(containerColor = navy), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("PROJECT", color = Color(0xFF9EC7E8), fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = well, onValueChange = { well = it }, label = { Text("Well name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
            Text("Vogel IPR calculator", color = navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Pressures in psi; rate in STB/day", color = Color.Gray, fontSize = 12.sp)
            NumericField("Reservoir pressure Pr (psi)", prText) { prText = it }
            NumericField("Bubble-point pressure Pb (psi)", pbText) { pbText = it }
            NumericField("Maximum rate qmax (STB/day)", qText) { qText = it }
            Button(onClick = {
                val pr = prText.toFloatOrNull()
                val pb = pbText.toFloatOrNull()
                val q = qText.toFloatOrNull()
                error = when {
                    pr == null || pb == null || q == null -> "Enter valid numbers."
                    pr <= 0f || pb < 0f || q <= 0f -> "Pr and qmax must be positive; Pb cannot be negative."
                    pb > pr -> "Pb cannot exceed Pr."
                    else -> null
                }
                if (error == null) points = (0..20).map { i -> val p = pr!! * i / 20f; IprPoint(vogelRate(q!!, p, pr), p) }.reversed()
            }, modifier = Modifier.fillMaxWidth()) { Text("Calculate IPR curve") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (points.isNotEmpty()) {
                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("IPR result: $well", color = navy, fontWeight = FontWeight.Bold)
                        Text("AOF (Vogel model): $qText STB/day", color = Color(0xFF2878B9), fontWeight = FontWeight.Bold)
                        IprGraph(points)
                    }
                }
                Text("Sample points", color = navy, fontWeight = FontWeight.Bold)
                points.filterIndexed { i, _ -> i % 4 == 0 }.forEach {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${"%.0f".format(it.pressure)} psi")
                        Text("${"%.1f".format(it.rate)} STB/day", fontWeight = FontWeight.Medium)
                    }
                }
            }
            Text("Educational prototype only. Pb is validated but does not yet select a composite IPR model. Not a validated PROSPER replacement; do not use for operational decisions.", color = Color.Gray, fontSize = 11.sp)
        }
    }
}
@Composable
private fun NumericField(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}
@Composable
private fun IprGraph(points: List<IprPoint>) {
    val maxQ = max(1f, points.maxOfOrNull { it.rate } ?: 1f)
    val maxP = max(1f, points.maxOfOrNull { it.pressure } ?: 1f)
    Canvas(Modifier.fillMaxWidth().height(200.dp).padding(8.dp)) {
        val l = 12f; val t = 12f; val w = size.width - 24f; val h = size.height - 24f
        drawLine(Color.LightGray, Offset(l, t), Offset(l, t + h), 2f)
        drawLine(Color.LightGray, Offset(l, t + h), Offset(l + w, t + h), 2f)
        val coords = points.map { Offset(l + it.rate / maxQ * w, t + h - it.pressure / maxP * h) }
        coords.zipWithNext().forEach { (a, b) -> drawLine(Color(0xFF2878B9), a, b, 4f) }
    }
}
