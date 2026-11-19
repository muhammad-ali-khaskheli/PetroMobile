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
                    "VLP" -> VlpScreen()
                    "Nodal" -> NodalScreen()
                    "Gas Lift" -> GasLiftScreen()
                    "ESP" -> EspScreen()
                    "Reports" -> ReportsScreen(field, reservoir, well, prText, pbText, qText)
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
    SectionTitle("PVT analysis", "Standing solution gas-oil ratio correlation")
    var api by remember { mutableStateOf("35") }
    var gas by remember { mutableStateOf("0.75") }
    var temp by remember { mutableStateOf("180") }
    var pressure by remember { mutableStateOf("4000") }
    var curve by remember { mutableStateOf<List<IprPoint>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextInput("Oil API gravity (degree)", api) { api = it }
        TextInput("Gas specific gravity (air = 1)", gas) { gas = it }
        TextInput("Reservoir temperature (F)", temp) { temp = it }
        TextInput("Maximum pressure (psi)", pressure) { pressure = it }
        Button(onClick = {
            val a = api.toFloatOrNull(); val g = gas.toFloatOrNull(); val t = temp.toFloatOrNull(); val p = pressure.toFloatOrNull()
            error = when {
                a == null || g == null || t == null || p == null -> "Enter valid numbers in all fields."
                a <= 0f || g <= 0f || p <= 0f -> "API, gas gravity and pressure must be positive."
                else -> null
            }
            if (error == null) curve = (0..40).map { i ->
                val pr = p!! * i / 40f
                val term = (pr / 18.2f + 1.4f) * Math.pow(10.0, (0.0125f * a!! - 0.00091f * t!!).toDouble()).toFloat()
                IprPoint(pr, g!! * Math.pow(term.toDouble(), 1.2048).toFloat())
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Calculate PVT curve") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } }
    if (curve.isNotEmpty()) Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Solution GOR vs pressure", color = navy, fontWeight = FontWeight.Bold)
        Text("Standing correlation · Rs (scf/STB)", color = Color.Gray, fontSize = 11.sp)
        IprGraph(curve)
        val last = curve.last()
        Text("At " + "%.0f".format(last.pressure) + " psi: Rs = " + "%.1f".format(last.rate) + " scf/STB", color = blue, fontWeight = FontWeight.Bold)
    } }
    InfoCard("Model limitation", "Standing empirical solution-GOR correlation only. This is not a complete PVT package: oil FVF, viscosity, gas Z-factor, EOS, lab matching and calibration are not included.")
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
private fun VlpScreen() {
    SectionTitle("VLP / pressure traverse", "Static single-phase hydrostatic estimate")
    var depth by remember { mutableStateOf("8000") }; var sg by remember { mutableStateOf("0.85") }; var whp by remember { mutableStateOf("200") }
    var result by remember { mutableStateOf<Float?>(null) }; var error by remember { mutableStateOf<String?>(null) }
    Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextInput("Vertical depth (ft)", depth) { depth = it }; TextInput("Average fluid specific gravity", sg) { sg = it }; TextInput("Wellhead pressure (psi)", whp) { whp = it }
        Button(onClick = { val d=depth.toFloatOrNull(); val s=sg.toFloatOrNull(); val w=whp.toFloatOrNull(); error=if(d==null||s==null||w==null) "Enter valid numbers." else if(d<=0||s<=0||w<0) "Depth and SG must be positive; WHP cannot be negative." else null; if(error==null) result=w!!+0.433f*s!!*d!! }, modifier=Modifier.fillMaxWidth()) { Text("Calculate bottomhole pressure") }
    } }
    result?.let { InfoCard("Estimated hydrostatic BHP", "%.1f".format(it)+" psi\nWHP + 0.433 × fluid SG × TVD") }
    error?.let { Text(it, color=MaterialTheme.colorScheme.error) }
    InfoCard("Limitation", "Static single-phase hydrostatic estimate only. No multiphase flow, friction, acceleration, temperature profile or VLP correlation is implemented.")
}
@Composable
private fun NodalScreen() {
    SectionTitle("Nodal analysis", "Vogel inflow at a specified flowing bottomhole pressure")
    var pr by remember { mutableStateOf("4000") }; var qmax by remember { mutableStateOf("1000") }; var pwf by remember { mutableStateOf("2000") }
    var result by remember { mutableStateOf<Float?>(null) }; var error by remember { mutableStateOf<String?>(null) }
    Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextInput("Reservoir pressure Pr (psi)",pr){pr=it}; TextInput("AOF / qmax (STB/day)",qmax){qmax=it}; TextInput("Required flowing BHP (psi)",pwf){pwf=it}
        Button(onClick={val p=pr.toFloatOrNull();val q=qmax.toFloatOrNull();val b=pwf.toFloatOrNull();error=if(p==null||q==null||b==null) "Enter valid numbers." else if(p<=0||q<=0||b<0||b>p) "Require Pr and qmax > 0 and 0 ≤ Pwf ≤ Pr." else null;if(error==null){val x=b!!/p!!;result=q!!*(1f-0.2f*x-0.8f*x*x)}},modifier=Modifier.fillMaxWidth()){Text("Calculate inflow rate")}
    } }
    result?.let { InfoCard("Vogel inflow at specified BHP","%.2f".format(it)+" STB/day\nRate calculated using the Vogel inflow equation.") }
    error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    InfoCard("Limitation","This is not a full nodal operating-point solver. A validated VLP curve and IPR–VLP intersection are still required.")
}
@Composable
private fun GasLiftScreen() {
    SectionTitle("Gas lift calculator","Injection gas to produced-liquid ratio")
    var gas by remember { mutableStateOf("500") }; var liquid by remember { mutableStateOf("500") }
    var result by remember { mutableStateOf<Float?>(null) }; var error by remember { mutableStateOf<String?>(null) }
    Card { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        TextInput("Injected gas (Mscf/day)",gas){gas=it};TextInput("Produced liquid (STB/day)",liquid){liquid=it}
        Button(onClick={val g=gas.toFloatOrNull();val l=liquid.toFloatOrNull();error=if(g==null||l==null) "Enter valid numbers." else if(g<0||l<=0) "Gas must be non-negative and liquid rate positive." else null;if(error==null)result=g!!*1000f/l!!},modifier=Modifier.fillMaxWidth()){Text("Calculate injection GLR")}
    }}
    result?.let{InfoCard("Injection gas/liquid ratio","%.1f".format(it)+" scf/STB\nInjected gas rate × 1000 / liquid rate")}
    error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    InfoCard("Limitation","Ratio calculator only. Valve design, unloading, injection pressure traverse and gas-lift optimization are not implemented.")
}
@Composable
private fun EspScreen() {
    SectionTitle("ESP hydraulic calculator","Approximate required horsepower")
    var rate by remember { mutableStateOf("1000") };var head by remember { mutableStateOf("5000") };var sg by remember { mutableStateOf("1.0") };var eff by remember { mutableStateOf("65") }
    var result by remember { mutableStateOf<Float?>(null) };var error by remember { mutableStateOf<String?>(null)}
    Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        TextInput("Liquid rate (STB/day)",rate){rate=it};TextInput("Total dynamic head (ft)",head){head=it};TextInput("Liquid specific gravity",sg){sg=it};TextInput("Pump efficiency (%)",eff){eff=it}
        Button(onClick={val q=rate.toFloatOrNull();val h=head.toFloatOrNull();val s=sg.toFloatOrNull();val e=eff.toFloatOrNull();error=if(q==null||h==null||s==null||e==null) "Enter valid numbers." else if(q<=0||h<=0||s<=0||e<=0||e>100) "Inputs must be positive and efficiency ≤ 100%." else null;if(error==null){val gpm=q!!*42f/1440f;result=gpm*h!!*s!!/(3960f*(e!!/100f))}},modifier=Modifier.fillMaxWidth()){Text("Calculate approximate horsepower")}
    }}
    result?.let{InfoCard("Approximate pump horsepower","%.2f".format(it)+" hp\nQ (gpm) × head (ft) × SG / [3960 × efficiency]")}
    error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    InfoCard("Limitation","Hydraulic horsepower estimate only. No pump-series selection, stage count, motor sizing, cable losses, gas handling or operating envelope.")
}
@Composable
private fun ReportsScreen(field:String,reservoir:String,well:String,pr:String,pb:String,q:String) {
    val report="PETROMOBILE STUDY SUMMARY\nField: "+field+"\nReservoir: "+reservoir+"\nWell: "+well+"\nReservoir pressure: "+pr+" psi\nBubble point: "+pb+" psi\nVogel AOF input: "+q+" STB/day\n\nEducational prototype. Independently validate all results; not for operational decisions."
    val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
    SectionTitle("Study report","Review and copy current project summary")
    Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(report,color=navy,fontSize=13.sp)
        Button(onClick={clipboard.setText(androidx.compose.ui.text.AnnotatedString(report))},modifier=Modifier.fillMaxWidth()){Text("Copy report text")}
    }}
    InfoCard("Export status","PDF and spreadsheet exports are not included yet. Copyable text summary is available.")
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
