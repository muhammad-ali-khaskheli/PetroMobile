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
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
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
    SectionTitle("Black-oil PVT", "Standing correlations · field units")
    var api by remember { mutableStateOf("35") }
    var gas by remember { mutableStateOf("0.75") }
    var temp by remember { mutableStateOf("180") }
    var rsInput by remember { mutableStateOf("600") }
    var table by remember { mutableStateOf<List<PvtSample>>(emptyList()) }
    var pb by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextInput("Oil API gravity (°API)", api) { api = it }
        TextInput("Gas specific gravity (air = 1)", gas) { gas = it }
        TextInput("Reservoir temperature (°F)", temp) { temp = it }
        TextInput("Solution GOR at bubble point (scf/STB)", rsInput) { rsInput = it }
        Button(onClick = {
            val a = api.toDoubleOrNull(); val g = gas.toDoubleOrNull()
            val t = temp.toDoubleOrNull(); val rs = rsInput.toDoubleOrNull()
            try {
                if (a == null || g == null || t == null || rs == null) error = "Enter valid numbers in every field."
                else {
                    val calculatedPb = PetroleumModels.standingBubblePointPsi(a, g, t, rs)
                    if (calculatedPb <= 0.0) throw IllegalArgumentException("Calculated bubble-point pressure is not positive. Check inputs and correlation range.")
                    val samples = PetroleumModels.standingPvtTable(calculatedPb, a, g, t, 41)
                    pb = calculatedPb
                    table = samples
                    error = null
                }
            } catch (e: IllegalArgumentException) { error = e.message ?: "Invalid input." }
        }, modifier = Modifier.fillMaxWidth()) { Text("Calculate black-oil PVT") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } }
    pb?.let { bubble ->
        Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PVT results", color = navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Standing bubble point: %.1f psi".format(bubble), color = blue, fontWeight = FontWeight.Bold)
            Text("Solution GOR and oil FVF vs pressure", color = Color.Gray, fontSize = 11.sp)
            PvtGraph(table)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pressure (psi)", fontSize = 10.sp, color = Color.Gray)
                Text("Rs (scf/STB) / Bo (rb/STB)", fontSize = 10.sp, color = Color.Gray)
            }
            table.filterIndexed { index, _ -> index % 5 == 0 || index == table.lastIndex }.forEach { sample ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("%.0f".format(sample.pressurePsi), fontSize = 12.sp)
                    Text("%.1f / %.4f".format(sample.solutionGasOilRatioScfStb, sample.oilFormationVolumeFactorRbStb), fontSize = 12.sp)
                }
            }
            Text("Values are correlation estimates, not lab-matched PVT data.", color = Color.Gray, fontSize = 10.sp)
        } }
    }
    InfoCard("Correlation and limitations", "Standing bubble-point pressure, solution GOR and saturated-oil FVF correlations. No undersaturated-oil compressibility, viscosity, gas Z-factor, Bg, density, EOS or laboratory matching yet. Correlations have limited empirical ranges; validate against fluid data.")
}
@Composable
private fun IprScreen(prText: String, onPr: (String) -> Unit, pbText: String, onPb: (String) -> Unit, qText: String, onQ: (String) -> Unit, points: List<IprPoint>, onPoints: (List<IprPoint>) -> Unit, error: String?, onError: (String?) -> Unit) {
    SectionTitle("IPR / Inflow performance", "Composite linear-above-Pb and Vogel-below-Pb inflow")
    Text("Pressures in psi · rate in STB/day", color = Color.Gray, fontSize = 12.sp)
    TextInput("Reservoir pressure Pr (psi)", prText, onPr)
    TextInput("Bubble-point pressure Pb (psi)", pbText, onPb)
    TextInput("Productivity index J (STB/day/psi)", qText, onQ)
    Button(onClick = {
        val pr = prText.toDoubleOrNull()
        val pb = pbText.toDoubleOrNull()
        val j = qText.toDoubleOrNull()
        try {
            if (pr == null || pb == null || j == null) throw IllegalArgumentException("Enter valid numeric values.")
            val samples = (0..40).map { i ->
                val pwf = pr * i / 40.0
                IprPoint(PetroleumModels.compositeIpRRate(j, pr, pb, pwf).toFloat(), pwf.toFloat())
            }.reversed()
            onPoints(samples)
            onError(null)
        } catch (e: IllegalArgumentException) {
            onError(e.message ?: "Invalid input.")
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("Calculate composite IPR") }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (points.isNotEmpty()) {
        Card(shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Composite IPR result", color = navy, fontWeight = FontWeight.Bold)
                Text("AOF: %.1f STB/day".format(points.maxOfOrNull { it.rate } ?: 0f), color = blue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IprGraph(points)
                points.filterIndexed { i, _ -> i % 5 == 0 }.forEach {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("%.0f psi".format(it.pressure))
                        Text("%.1f STB/day".format(it.rate), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
    InfoCard("Model limitation", "Composite IPR assumes a supplied constant productivity index and a Vogel saturated-flow branch. It requires calibration against well-test data and does not model completion damage or multiphase inflow.")
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
private fun PvtGraph(samples: List<PvtSample>) {
    val maxRs = (samples.maxOfOrNull { it.solutionGasOilRatioScfStb } ?: 1.0).coerceAtLeast(1.0)
    val maxP = (samples.maxOfOrNull { it.pressurePsi } ?: 1.0).coerceAtLeast(1.0)
    Canvas(Modifier.fillMaxWidth().height(270.dp).padding(8.dp)) {
        val left = 58f; val top = 18f; val right = size.width - 16f; val bottom = size.height - 48f
        val w = right - left; val h = bottom - top
        val paint = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 11f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        for (i in 0..4) {
            val fraction = i / 4f
            val x = left + fraction * w; val y = bottom - fraction * h
            drawLine(Color(0xFFE0E5EA), Offset(x, top), Offset(x, bottom), 1f)
            drawLine(Color(0xFFE0E5EA), Offset(left, y), Offset(right, y), 1f)
            drawContext.canvas.nativeCanvas.drawText("%.0f".format(maxP * fraction), x, bottom + 17f, paint)
            paint.textAlign = Paint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText("%.0f".format(maxRs * fraction), left - 8f, y + 4f, paint)
            paint.textAlign = Paint.Align.CENTER
        }
        drawLine(Color.DarkGray, Offset(left, top), Offset(left, bottom), 2f)
        drawLine(Color.DarkGray, Offset(left, bottom), Offset(right, bottom), 2f)
        val curve = samples.map { Offset(left + (it.pressurePsi / maxP * w).toFloat(), bottom - (it.solutionGasOilRatioScfStb / maxRs * h).toFloat()) }
        curve.zipWithNext().forEach { (p1,p2) -> drawLine(blue,p1,p2,4f) }
        paint.textSize = 12f
        drawContext.canvas.nativeCanvas.drawText("Pressure (psi)", left + w / 2f, size.height - 5f, paint)
        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(-90f, 13f, top + h / 2f)
        drawContext.canvas.nativeCanvas.drawText("Solution GOR (scf/STB)", 13f, top + h / 2f, paint)
        drawContext.canvas.nativeCanvas.restore()
    }
}

@Composable
private fun IprGraph(points: List<IprPoint>) {
    val maxQ = max(1f, points.maxOfOrNull { it.rate } ?: 1f)
    val maxP = max(1f, points.maxOfOrNull { it.pressure } ?: 1f)
    Canvas(Modifier.fillMaxWidth().height(270.dp).padding(8.dp)) {
        val left = 58f; val top = 18f; val right = size.width - 16f; val bottom = size.height - 48f
        val w = right - left; val h = bottom - top
        val paint = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 11f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        for (i in 0..4) {
            val fraction = i / 4f
            val x = left + fraction * w; val y = bottom - fraction * h
            drawLine(Color(0xFFE0E5EA), Offset(x, top), Offset(x, bottom), 1f)
            drawLine(Color(0xFFE0E5EA), Offset(left, y), Offset(right, y), 1f)
            drawContext.canvas.nativeCanvas.drawText("%.0f".format(maxQ * fraction), x, bottom + 17f, paint)
            paint.textAlign = Paint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText("%.0f".format(maxP * fraction), left - 8f, y + 4f, paint)
            paint.textAlign = Paint.Align.CENTER
        }
        drawLine(Color.DarkGray, Offset(left, top), Offset(left, bottom), 2f)
        drawLine(Color.DarkGray, Offset(left, bottom), Offset(right, bottom), 2f)
        val curve = points.map { Offset(left + it.rate / maxQ * w, bottom - it.pressure / maxP * h) }
        curve.zipWithNext().forEach { (p1,p2) -> drawLine(blue,p1,p2,4f) }
        paint.textSize = 12f
        drawContext.canvas.nativeCanvas.drawText("Oil rate (STB/day)", left + w / 2f, size.height - 5f, paint)
        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(-90f, 13f, top + h / 2f)
        drawContext.canvas.nativeCanvas.drawText("Flowing pressure (psi)", 13f, top + h / 2f, paint)
        drawContext.canvas.nativeCanvas.restore()
    }
}
