package com.petromobile.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
private val navy = Color(0xFF10243A)
private val blue = Color(0xFF2878B9)
private val page = Color(0xFFF3F6FA)
private val modules = listOf("Overview", "PVT", "IPR", "VLP", "Nodal", "Gas Lift", "ESP", "Reports")
private fun vogelRate(qMax: Float, pwf: Float, pr: Float): Float {
    if (pr <= 0f || pwf >= pr) return 0f
    val x = (pwf / pr).coerceIn(0f, 1f)
    return qMax * (1f - 0.2f * x - 0.8f * x * x)
}

@Composable
fun PetroMobileApp() {
    var selected by remember { mutableStateOf("Overview") }
    var well by remember { mutableStateOf("Well-01") }
    var field by remember { mutableStateOf("Field-01") }
    var reservoir by remember { mutableStateOf("Reservoir-01") }
    var prText by remember { mutableStateOf("4000") }
    var pbText by remember { mutableStateOf("2000") }
    var qText by remember { mutableStateOf("1000") }
    var points by remember { mutableStateOf<List<IprPoint>>(emptyList()) }
    var iprError by remember { mutableStateOf<String?>(null) }
    MaterialTheme(colorScheme = lightColorScheme(primary = blue, background = page)) {
        Column(Modifier.fillMaxSize().background(page)) {
            Column(Modifier.fillMaxWidth().background(navy).padding(18.dp)) {
                Text("PETROMOBILE", color = Color(0xFF9EC7E8), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Well Performance Studio", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Independent petroleum engineering platform · v0.2", color = Color(0xFFD0DCE8), fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderValue("FIELD", field, Modifier.weight(1f))
                    HeaderValue("RESERVOIR", reservoir, Modifier.weight(1f))
                    HeaderValue("WELL", well, Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth().background(Color.White).horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                modules.forEach { module -> FilterChip(selected = selected == module, onClick = { selected = module }, label = { Text(module) }) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (selected) {
                    "Overview" -> OverviewScreen(well, { well = it }, field, { field = it }, reservoir, { reservoir = it }, { selected = it })
                    "PVT" -> PvtScreen()
                    "IPR" -> IprScreen(prText, { prText = it }, pbText, { pbText = it }, qText, { qText = it }, points, { points = it }, iprError, { iprError = it })
                    "VLP" -> PlaceholderModule("VLP / Outflow Performance", "Tubing and wellbore pressure-loss workspace.", listOf("Tubing size / ID", "Measured depth (ft)", "Wellhead pressure (psi)", "Wellhead temperature (°F)", "Oil rate (STB/day)", "Water cut (%)", "Gas-oil ratio (scf/STB)"), "Multiphase VLP correlations and pressure traverse are planned; this screen does not calculate VLP.")
                    "Nodal" -> PlaceholderModule("Nodal Analysis", "Combine inflow and outflow performance to identify the system operating point.", listOf("Reservoir pressure (psi)", "Water cut (%)", "Gas-oil ratio (scf/STB)", "Tubing head pressure (psi)", "Flowline pressure (psi)"), "IPR–VLP intersection solver is planned; no operating point is calculated in this release.")
                    "Gas Lift" -> PlaceholderModule("Gas Lift Design", "Gas-lift design and optimization workspace.", listOf("Injection gas rate (Mscf/day)", "Injection pressure (psi)", "Gas specific gravity", "Valve depth (ft)", "Target liquid rate (STB/day)"), "Gas-lift valve design and optimization calculations are not implemented yet.")
                    "ESP" -> PlaceholderModule("ESP Design", "Electric submersible pump design workspace.", listOf("Liquid rate (STB/day)", "Water cut (%)", "Pump setting depth (ft)", "Required head (ft)", "Fluid specific gravity"), "Pump selection, head, stages and power calculations are not implemented yet.")
                    "Reports" -> PlaceholderModule("Engineering Reports", "Project study summary and output center.", listOf("Report title", "Engineer", "Field / asset", "Well name"), "PDF and spreadsheet report export is planned for a future release.")
                }
                Text("Independent prototype; not affiliated with or equivalent to IPM PROSPER. Verify methods and results independently. Not for operational decisions.", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun HeaderValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color(0xFF1C3852), RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text(label, color = Color(0xFF9EC7E8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 11.sp, maxLines = 1)
    }
}
@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Text(title, color = navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
}
@Composable
private fun OverviewScreen(well: String, onWell: (String) -> Unit, field: String, onField: (String) -> Unit, reservoir: String, onReservoir: (String) -> Unit, navigate: (String) -> Unit) {
    SectionTitle("Project overview", "Set up the study and open an engineering workspace.")
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PROJECT & WELL SETUP", color = blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            TextInput("Field name", field, onField)
            TextInput("Reservoir name", reservoir, onReservoir)
            TextInput("Well name", well, onWell)
        }
    }
    Text("ENGINEERING WORKSPACES", color = navy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    val descriptions = listOf("PVT" to "Fluid properties and correlations", "IPR" to "Reservoir inflow performance", "VLP" to "Tubing / outflow performance", "Nodal" to "System operating-point analysis", "Gas Lift" to "Gas-lift design workspace", "ESP" to "Electric submersible pump workspace", "Reports" to "Study summaries and outputs")
    descriptions.forEach { item ->
        Card(onClick = { navigate(item.first) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.first, color = navy, fontWeight = FontWeight.Bold)
                    Text(item.second, color = Color.Gray, fontSize = 11.sp)
                }
                Text("OPEN  ›", color = blue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
@Composable
private fun PvtScreen() {
    SectionTitle("PVT analysis", "Fluid characterization workspace")
    var model by remember { mutableStateOf("Black Oil") }
    var api by remember { mutableStateOf("35") }
    var gasGravity by remember { mutableStateOf("0.75") }
    var gor by remember { mutableStateOf("600") }
    var temperature by remember { mutableStateOf("180") }
    var saved by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Fluid model", color = navy, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = model == "Black Oil", onClick = { model = "Black Oil" }, label = { Text("Black Oil") })
                FilterChip(selected = model == "Compositional", onClick = { model = "Compositional" }, label = { Text("Compositional") })
            }
            TextInput("Oil API gravity (°API)", api) { api = it }
            TextInput("Gas specific gravity", gasGravity) { gasGravity = it }
            TextInput("Solution GOR (scf/STB)", gor) { gor = it }
            TextInput("Reservoir temperature (°F)", temperature) { temperature = it }
            Button(onClick = { saved = true }, modifier = Modifier.fillMaxWidth()) { Text("Save PVT inputs") }
            if (saved) Text("Inputs accepted for this session. Property calculations are not implemented.", color = blue, fontSize = 12.sp)
        }
    }
    InfoCard("Planned PVT outputs", "Bubble-point pressure, solution GOR, oil formation volume factor, oil viscosity, gas Z-factor and water properties.")
}
@Composable
private fun IprScreen(prText: String, onPr: (String) -> Unit, pbText: String, onPb: (String) -> Unit, qText: String, onQ: (String) -> Unit, points: List<IprPoint>, onPoints: (List<IprPoint>) -> Unit, error: String?, onError: (String?) -> Unit) {
    SectionTitle("IPR / Inflow performance", "Simplified Vogel inflow performance curve")
    Text("Pressures in psi · rate in STB/day", color = Color.Gray, fontSize = 12.sp)
    TextInput("Reservoir pressure Pr (psi)", prText, onPr)
    TextInput("Bubble-point pressure Pb (psi)", pbText, onPb)
    TextInput("Maximum rate qmax (STB/day)", qText, onQ)
    Button(onClick = {
        val pr = prText.toFloatOrNull()
        val pb = pbText.toFloatOrNull()
        val q = qText.toFloatOrNull()
        onError(when {
            pr == null || pb == null || q == null -> "Enter valid numeric values."
            pr <= 0f || pb < 0f || q <= 0f -> "Pr and qmax must be positive; Pb cannot be negative."
            pb > pr -> "Pb cannot exceed Pr."
            else -> null
        })
        if (pr != null && pb != null && q != null && pr > 0f && pb >= 0f && pb <= pr && q > 0f) {
            onPoints((0..30).map { i -> val p = pr * i / 30f; IprPoint(vogelRate(q, p, pr), p) }.reversed())
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("Calculate IPR curve") }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (points.isNotEmpty()) {
        Card(shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vogel IPR result", color = navy, fontWeight = FontWeight.Bold)
                Text("AOF: $qText STB/day", color = blue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IprGraph(points)
                points.filterIndexed { i, _ -> i % 5 == 0 }.forEach {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${"%.0f".format(it.pressure)} psi")
                        Text("${"%.1f".format(it.rate)} STB/day", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
    InfoCard("Model limitation", "Bubble-point pressure is validated but does not select a composite IPR model in this release.")
}
@Composable
private fun PlaceholderModule(title: String, description: String, inputs: List<String>, limitation: String) {
    SectionTitle(title, description)
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("STUDY INPUTS", color = blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            inputs.forEach { label ->
                var value by remember(label) { mutableStateOf("") }
                TextInput(label, value) { value = it }
            }
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Calculation engine — planned") }
        }
    }
    InfoCard("Development status", limitation)
}
@Composable
private fun InfoCard(title: String, message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE5EDF5)), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = navy, fontWeight = FontWeight.Bold)
            Text(message, color = Color(0xFF46586A), fontSize = 12.sp)
        }
    }
}
@Composable
private fun TextInput(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}
@Composable
private fun IprGraph(points: List<IprPoint>) {
    val maxQ = max(1f, points.maxOfOrNull { it.rate } ?: 1f)
    val maxP = max(1f, points.maxOfOrNull { it.pressure } ?: 1f)
    Canvas(Modifier.fillMaxWidth().height(210.dp).padding(8.dp)) {
        val l = 12f
        val t = 12f
        val w = size.width - 24f
        val h = size.height - 24f
        drawLine(Color.LightGray, Offset(l, t), Offset(l, t + h), 2f)
        drawLine(Color.LightGray, Offset(l, t + h), Offset(l + w, t + h), 2f)
        val coords = points.map { Offset(l + it.rate / maxQ * w, t + h - it.pressure / maxP * h) }
        coords.zipWithNext().forEach { (a, b) -> drawLine(blue, a, b, 4f) }
    }
}
