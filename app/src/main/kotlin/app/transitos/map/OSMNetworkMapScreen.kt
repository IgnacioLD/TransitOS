package app.transitos.map

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay

private data class StationInfo(
    val name: String,
    val lines: List<String>,
)

private data class LineData(
    val name: String,
    val color: Int,
    val path: List<GeoPoint>,
    val stations: List<Pair<String, Int>>,
    val tram: Boolean = false,
)

private val metroLines = listOf(
    LineData("L1", AndroidColor.rgb(227, 6, 19), path = listOf(
        GeoPoint(39.5900,-0.4760), GeoPoint(39.5860,-0.4740), GeoPoint(39.5800,-0.4700),
        GeoPoint(39.5700,-0.4650), GeoPoint(39.5600,-0.4620), GeoPoint(39.5530,-0.4590),
        GeoPoint(39.5450,-0.4560), GeoPoint(39.5360,-0.4530), GeoPoint(39.5280,-0.4500),
        GeoPoint(39.5200,-0.4470), GeoPoint(39.5120,-0.4460), GeoPoint(39.5050,-0.4450),
        GeoPoint(39.5032,-0.4447), GeoPoint(39.4980,-0.4430), GeoPoint(39.4930,-0.4390),
        GeoPoint(39.4880,-0.4300), GeoPoint(39.4840,-0.4200), GeoPoint(39.4800,-0.4100),
        GeoPoint(39.4770,-0.4030), GeoPoint(39.4761,-0.3981), GeoPoint(39.4740,-0.3940),
        GeoPoint(39.4720,-0.3894), GeoPoint(39.4680,-0.3840), GeoPoint(39.4664,-0.3813),
        GeoPoint(39.4645,-0.3804), GeoPoint(39.4630,-0.3795), GeoPoint(39.4600,-0.3795),
        GeoPoint(39.4580,-0.3805), GeoPoint(39.4545,-0.3825), GeoPoint(39.4500,-0.3845),
        GeoPoint(39.4480,-0.3870), GeoPoint(39.4440,-0.3900), GeoPoint(39.4420,-0.3930),
        GeoPoint(39.4370,-0.3970), GeoPoint(39.4330,-0.4000), GeoPoint(39.4300,-0.4070),
        GeoPoint(39.4275,-0.4165), GeoPoint(39.4240,-0.4200), GeoPoint(39.4220,-0.4230),
        GeoPoint(39.4190,-0.4280), GeoPoint(39.4160,-0.4330), GeoPoint(39.4130,-0.4400),
        GeoPoint(39.4100,-0.4480), GeoPoint(39.4070,-0.4560), GeoPoint(39.4040,-0.4630),
        GeoPoint(39.3980,-0.4670), GeoPoint(39.3900,-0.4700), GeoPoint(39.3800,-0.4740),
        GeoPoint(39.3750,-0.4780), GeoPoint(39.3680,-0.4850), GeoPoint(39.3610,-0.4940),
        GeoPoint(39.3530,-0.4980), GeoPoint(39.3450,-0.5020), GeoPoint(39.3380,-0.5050),
        GeoPoint(39.3305,-0.5080), GeoPoint(39.3200,-0.5095), GeoPoint(39.3100,-0.5110),
        GeoPoint(39.3000,-0.5125), GeoPoint(39.2900,-0.5140), GeoPoint(39.2800,-0.5140),
        GeoPoint(39.2700,-0.5140), GeoPoint(39.2600,-0.5145), GeoPoint(39.2500,-0.5150),
        GeoPoint(39.2400,-0.5150), GeoPoint(39.2300,-0.5150), GeoPoint(39.2200,-0.5150),
        GeoPoint(39.2100,-0.5150), GeoPoint(39.2000,-0.5150), GeoPoint(39.1900,-0.5150),
        GeoPoint(39.1800,-0.5150), GeoPoint(39.1700,-0.5150), GeoPoint(39.1600,-0.5150),
        GeoPoint(39.1500,-0.5150), GeoPoint(39.1400,-0.5150), GeoPoint(39.1300,-0.5150),
        GeoPoint(39.1200,-0.5150), GeoPoint(39.1100,-0.5150), GeoPoint(39.0950,-0.5152),
        GeoPoint(39.0785,-0.5153),
    ), stations = listOf(
        "Bétera" to 0, "Horta Vella" to 2, "Masies" to 4, "Seminari-CEU" to 6,
        "Moncada-Alfara" to 8, "Massarrojos" to 10, "Rocafort" to 12,
        "Godella" to 14, "Burjassot-Godella" to 16, "Burjassot" to 18,
        "Sant Pau" to 20, "Empalme" to 22, "Beniferri" to 24,
        "Campanar" to 26, "Túria" to 28, "Àngel Guimerà" to 30,
        "Plaça d'Espanya" to 32, "Jesús" to 34, "Patraix" to 36,
        "Safranar" to 38, "Sant Isidre" to 40, "València Sud" to 42,
        "Paiporta" to 44, "Picanya" to 46, "Torrent" to 48,
        "Picassent" to 50, "Espioca" to 52, "Font Almaguer" to 54,
        "Alginet" to 56, "Ausiàs March" to 58, "Carlet" to 60,
        "Benimodo" to 62, "L'Alcúdia" to 64, "Montortal" to 66,
        "Massalavés" to 68, "Alberic" to 70, "Castelló" to 72,
    )),
    LineData("L2", AndroidColor.rgb(0, 92, 171), path = listOf(
        GeoPoint(39.6240,-0.5950), GeoPoint(39.6150,-0.5900), GeoPoint(39.6050,-0.5870),
        GeoPoint(39.5940,-0.5840), GeoPoint(39.5850,-0.5790), GeoPoint(39.5800,-0.5700),
        GeoPoint(39.5720,-0.5620), GeoPoint(39.5650,-0.5550), GeoPoint(39.5570,-0.5480),
        GeoPoint(39.5500,-0.5400), GeoPoint(39.5420,-0.5330), GeoPoint(39.5350,-0.5250),
        GeoPoint(39.5280,-0.5130), GeoPoint(39.5200,-0.5000), GeoPoint(39.5150,-0.4800),
        GeoPoint(39.5100,-0.4600), GeoPoint(39.5070,-0.4520), GeoPoint(39.5032,-0.4447),
        GeoPoint(39.4980,-0.4430), GeoPoint(39.4930,-0.4390), GeoPoint(39.4880,-0.4300),
        GeoPoint(39.4840,-0.4200), GeoPoint(39.4800,-0.4100), GeoPoint(39.4770,-0.4030),
        GeoPoint(39.4761,-0.3981), GeoPoint(39.4740,-0.3940), GeoPoint(39.4720,-0.3894),
        GeoPoint(39.4680,-0.3840), GeoPoint(39.4664,-0.3813), GeoPoint(39.4645,-0.3804),
        GeoPoint(39.4630,-0.3795), GeoPoint(39.4600,-0.3795), GeoPoint(39.4580,-0.3805),
        GeoPoint(39.4545,-0.3825), GeoPoint(39.4500,-0.3845), GeoPoint(39.4480,-0.3870),
        GeoPoint(39.4440,-0.3900), GeoPoint(39.4420,-0.3930), GeoPoint(39.4370,-0.3970),
        GeoPoint(39.4330,-0.4000), GeoPoint(39.4300,-0.4070), GeoPoint(39.4275,-0.4165),
        GeoPoint(39.4240,-0.4200), GeoPoint(39.4220,-0.4230), GeoPoint(39.4190,-0.4280),
        GeoPoint(39.4160,-0.4330), GeoPoint(39.4130,-0.4400), GeoPoint(39.4100,-0.4480),
        GeoPoint(39.4070,-0.4560), GeoPoint(39.4040,-0.4630), GeoPoint(39.4000,-0.4660),
        GeoPoint(39.3980,-0.4690),
    ), stations = listOf(
        "Llíria" to 0, "Benaguasil" to 1, "Fondo de Benaguasil" to 3,
        "La Pobla de Vallbona" to 4, "Gallipont-Torre del Virrei" to 6,
        "L'Eliana" to 7, "Montesol" to 9, "El Clot" to 10,
        "Entrepins" to 12, "La Vallesa" to 13, "La Canyada" to 15,
        "Fuente del Jarro" to 16, "Paterna" to 18, "Santa Rita" to 19,
        "Benimàmet" to 21, "Les Carolines-Fira" to 22,
        "Empalme" to 24, "Beniferri" to 25, "Campanar" to 27,
        "Túria" to 28, "Àngel Guimerà" to 30, "Plaça d'Espanya" to 31,
        "Jesús" to 33, "Patraix" to 34, "Safranar" to 36,
        "Sant Isidre" to 37, "València Sud" to 39, "Paiporta" to 40,
        "Picanya" to 42, "Torrent" to 43, "Col·legi El Vedat" to 45,
        "Cantereria" to 46, "Realón" to 48, "Torrent Avinguda" to 51,
    )),
    LineData("L3", AndroidColor.rgb(0, 166, 81), path = listOf(
        GeoPoint(39.5920,-0.3350), GeoPoint(39.5850,-0.3360), GeoPoint(39.5800,-0.3370),
        GeoPoint(39.5730,-0.3380), GeoPoint(39.5670,-0.3390), GeoPoint(39.5610,-0.3400),
        GeoPoint(39.5550,-0.3410), GeoPoint(39.5490,-0.3420), GeoPoint(39.5430,-0.3430),
        GeoPoint(39.5360,-0.3440), GeoPoint(39.5300,-0.3465), GeoPoint(39.5240,-0.3490),
        GeoPoint(39.5180,-0.3525), GeoPoint(39.5120,-0.3560), GeoPoint(39.5090,-0.3600),
        GeoPoint(39.5060,-0.3640), GeoPoint(39.5030,-0.3650), GeoPoint(39.5000,-0.3660),
        GeoPoint(39.4960,-0.3680), GeoPoint(39.4920,-0.3690), GeoPoint(39.4880,-0.3700),
        GeoPoint(39.4840,-0.3690), GeoPoint(39.4800,-0.3680), GeoPoint(39.4760,-0.3670),
        GeoPoint(39.4740,-0.3660), GeoPoint(39.4710,-0.3670), GeoPoint(39.4695,-0.3685),
        GeoPoint(39.4682,-0.3705), GeoPoint(39.4685,-0.3735), GeoPoint(39.4690,-0.3767),
        GeoPoint(39.4680,-0.3780), GeoPoint(39.4670,-0.3790), GeoPoint(39.4664,-0.3813),
        GeoPoint(39.4650,-0.3850), GeoPoint(39.4640,-0.3890), GeoPoint(39.4660,-0.3950),
        GeoPoint(39.4680,-0.4000), GeoPoint(39.4700,-0.4060), GeoPoint(39.4720,-0.4110),
        GeoPoint(39.4735,-0.4180), GeoPoint(39.4750,-0.4250), GeoPoint(39.4765,-0.4340),
        GeoPoint(39.4780,-0.4430), GeoPoint(39.4800,-0.4470), GeoPoint(39.4820,-0.4500),
        GeoPoint(39.4840,-0.4530), GeoPoint(39.4860,-0.4570), GeoPoint(39.4880,-0.4620),
        GeoPoint(39.4885,-0.4670), GeoPoint(39.4890,-0.4730),
    ), stations = listOf(
        "Rafelbunyol" to 0, "La Pobla de Farnals" to 1, "Massamagrell" to 3,
        "Museros" to 5, "Albalat dels Sorells" to 7, "Foios" to 9,
        "Meliana" to 11, "Almàssera" to 13,
        "Alboraia Peris Aragó" to 15, "Alboraia Palmaret" to 17,
        "Machado" to 19, "Benimaclet" to 21,
        "Facultats-Manuel Broseta" to 23, "Alameda" to 25,
        "Colón" to 27, "Xàtiva" to 29, "Àngel Guimerà" to 31,
        "Avinguda del Cid" to 33, "Nou d'Octubre" to 35,
        "Mislata" to 37, "Mislata-Almassil" to 39,
        "Faitanar" to 41, "Quart de Poblet" to 43,
        "Salt de l'Aigua" to 45, "Manises" to 47,
        "Roses" to 48, "Aeroport" to 49,
    )),
    LineData("L4", AndroidColor.rgb(255, 209, 0), path = listOf(
        GeoPoint(39.5080,-0.3400), GeoPoint(39.5060,-0.3420), GeoPoint(39.5030,-0.3510),
        GeoPoint(39.5000,-0.3440), GeoPoint(39.4950,-0.3470), GeoPoint(39.4910,-0.3500),
        GeoPoint(39.4870,-0.3550), GeoPoint(39.4830,-0.3580), GeoPoint(39.4800,-0.3620),
        GeoPoint(39.4775,-0.3670), GeoPoint(39.4750,-0.3710), GeoPoint(39.4725,-0.3715),
        GeoPoint(39.4680,-0.3660), GeoPoint(39.4660,-0.3640), GeoPoint(39.4640,-0.3620),
        GeoPoint(39.4632,-0.3595), GeoPoint(39.4625,-0.3570), GeoPoint(39.4622,-0.3555),
        GeoPoint(39.4620,-0.3540), GeoPoint(39.4620,-0.3530), GeoPoint(39.4620,-0.3520),
        GeoPoint(39.4615,-0.3510), GeoPoint(39.4612,-0.3505), GeoPoint(39.4610,-0.3500),
        GeoPoint(39.4610,-0.3496), GeoPoint(39.4610,-0.3492), GeoPoint(39.4610,-0.3488),
        GeoPoint(39.4610,-0.3485),
    ), stations = listOf(
        "Mas del Rosari" to 0, "Parc Científic" to 1, "La Coma" to 2,
        "Tomás y Valiente" to 3, "Lloma Llarga-Terramelar" to 4,
        "À Punt" to 5, "Campus" to 6, "Vicent Andrés Estellés" to 7,
        "Sant Joan" to 8, "La Granja" to 9, "Palau de Congressos" to 10,
        "Trànsits" to 11, "Marxalenes" to 12, "Reus" to 13,
        "Sagunt" to 14, "Pont de Fusta" to 15, "Trinitat" to 16,
        "Benimaclet" to 17, "Florista" to 18, "Tarongers-Ernest Lluch" to 19,
        "Universitat Politècnica" to 20, "La Carrasca" to 21,
        "La Cadena" to 22, "Garbí" to 23, "Beteró" to 24,
        "Doctor Lluch" to 26,
    ), tram = true),
    LineData("L5", AndroidColor.rgb(0, 63, 127), path = listOf(
        GeoPoint(39.4567,-0.3350), GeoPoint(39.4600,-0.3380), GeoPoint(39.4620,-0.3440),
        GeoPoint(39.4650,-0.3490), GeoPoint(39.4680,-0.3550), GeoPoint(39.4710,-0.3670),
        GeoPoint(39.4682,-0.3705), GeoPoint(39.4690,-0.3767), GeoPoint(39.4670,-0.3790),
        GeoPoint(39.4664,-0.3813), GeoPoint(39.4650,-0.3850), GeoPoint(39.4640,-0.3890),
        GeoPoint(39.4660,-0.3950), GeoPoint(39.4680,-0.4000), GeoPoint(39.4700,-0.4060),
        GeoPoint(39.4720,-0.4110), GeoPoint(39.4735,-0.4180), GeoPoint(39.4750,-0.4250),
        GeoPoint(39.4765,-0.4340), GeoPoint(39.4780,-0.4430), GeoPoint(39.4800,-0.4470),
        GeoPoint(39.4820,-0.4500), GeoPoint(39.4840,-0.4530), GeoPoint(39.4860,-0.4570),
        GeoPoint(39.4880,-0.4620), GeoPoint(39.4885,-0.4670), GeoPoint(39.4890,-0.4730),
    ), stations = listOf(
        "Marítim" to 0, "Amistat-Casa de Salud" to 1, "Ayora" to 2,
        "Alameda" to 3, "Colón" to 4, "Xàtiva" to 5,
        "Àngel Guimerà" to 6, "Avinguda del Cid" to 7,
        "Nou d'Octubre" to 8, "Mislata" to 9, "Mislata-Almassil" to 10,
        "Faitanar" to 11, "Quart de Poblet" to 12,
        "Salt de l'Aigua" to 13, "Manises" to 14, "Roses" to 15,
        "Aeroport" to 16,
    )),
    LineData("L6", AndroidColor.rgb(123, 45, 142), path = listOf(
        GeoPoint(39.4910,-0.3900), GeoPoint(39.4870,-0.3880), GeoPoint(39.4830,-0.3850),
        GeoPoint(39.4790,-0.3800), GeoPoint(39.4750,-0.3750), GeoPoint(39.4725,-0.3715),
        GeoPoint(39.4680,-0.3660), GeoPoint(39.4640,-0.3620), GeoPoint(39.4625,-0.3570),
        GeoPoint(39.4620,-0.3540), GeoPoint(39.4620,-0.3520), GeoPoint(39.4610,-0.3500),
        GeoPoint(39.4600,-0.3480), GeoPoint(39.4590,-0.3450), GeoPoint(39.4580,-0.3420),
        GeoPoint(39.4570,-0.3380), GeoPoint(39.4567,-0.3350),
    ), stations = listOf(
        "Tossal del Rei" to 0, "Estadi Ciutat de València" to 1,
        "Sant Miquel dels Reis" to 2, "Alfauir" to 3,
        "Orriols" to 4, "Benimaclet" to 5, "Trinitat" to 6,
        "Universitat Politècnica" to 7, "Vicent Zaragozá" to 8,
        "Tarongers-Ernest Lluch" to 9, "La Carrasca" to 10,
        "La Cadena" to 11, "Beteró" to 12, "Cabanyal" to 13,
        "Grau-La Marina" to 14, "Marítim" to 15,
    ), tram = true),
    LineData("L7", AndroidColor.rgb(243, 146, 0), path = listOf(
        GeoPoint(39.4567,-0.3350), GeoPoint(39.4600,-0.3380), GeoPoint(39.4620,-0.3440),
        GeoPoint(39.4650,-0.3490), GeoPoint(39.4680,-0.3550), GeoPoint(39.4710,-0.3670),
        GeoPoint(39.4682,-0.3705), GeoPoint(39.4690,-0.3767), GeoPoint(39.4670,-0.3790),
        GeoPoint(39.4664,-0.3813), GeoPoint(39.4645,-0.3804), GeoPoint(39.4630,-0.3795),
        GeoPoint(39.4600,-0.3795), GeoPoint(39.4580,-0.3805), GeoPoint(39.4545,-0.3825),
        GeoPoint(39.4500,-0.3845), GeoPoint(39.4480,-0.3870), GeoPoint(39.4440,-0.3900),
        GeoPoint(39.4420,-0.3930), GeoPoint(39.4370,-0.3970), GeoPoint(39.4330,-0.4000),
        GeoPoint(39.4300,-0.4070), GeoPoint(39.4275,-0.4165), GeoPoint(39.4240,-0.4200),
        GeoPoint(39.4220,-0.4230), GeoPoint(39.4190,-0.4280), GeoPoint(39.4160,-0.4330),
        GeoPoint(39.4130,-0.4400), GeoPoint(39.4100,-0.4480), GeoPoint(39.4070,-0.4560),
        GeoPoint(39.4040,-0.4630), GeoPoint(39.4000,-0.4660), GeoPoint(39.3980,-0.4690),
    ), stations = listOf(
        "Marítim" to 0, "Amistat-Casa de Salud" to 1, "Ayora" to 2,
        "Alameda" to 3, "Aragó" to 4, "Bailén" to 5,
        "Colón" to 6, "Patraix" to 8, "Jesús" to 9,
        "Sant Isidre" to 11, "Safranar" to 12,
        "Paiporta" to 14, "València Sud" to 15,
        "Torrent" to 17, "Picanya" to 18, "Torrent Avinguda" to 20,
    )),
    LineData("L8", AndroidColor.rgb(229, 0, 127), path = listOf(
        GeoPoint(39.4567,-0.3350), GeoPoint(39.4570,-0.3320), GeoPoint(39.4570,-0.3280),
        GeoPoint(39.4570,-0.3260),
    ), stations = listOf(
        "Marítim" to 0, "Neptú" to 3,
    ), tram = true),
    LineData("L9", AndroidColor.rgb(0, 150, 136), path = listOf(
        GeoPoint(39.5440,-0.5600), GeoPoint(39.5400,-0.5550), GeoPoint(39.5350,-0.5500),
        GeoPoint(39.5300,-0.5450), GeoPoint(39.5250,-0.5400), GeoPoint(39.5230,-0.5360),
        GeoPoint(39.5210,-0.5310), GeoPoint(39.5180,-0.5200), GeoPoint(39.5150,-0.5100),
        GeoPoint(39.5120,-0.5000), GeoPoint(39.5100,-0.4900), GeoPoint(39.5080,-0.4800),
        GeoPoint(39.5060,-0.4700), GeoPoint(39.5030,-0.4630), GeoPoint(39.5000,-0.4580),
        GeoPoint(39.4950,-0.4530), GeoPoint(39.4900,-0.4480), GeoPoint(39.4840,-0.4460),
        GeoPoint(39.4780,-0.4430), GeoPoint(39.4750,-0.4250), GeoPoint(39.4720,-0.4110),
        GeoPoint(39.4700,-0.4060), GeoPoint(39.4675,-0.3970), GeoPoint(39.4660,-0.3950),
        GeoPoint(39.4640,-0.3890), GeoPoint(39.4650,-0.3850), GeoPoint(39.4664,-0.3813),
        GeoPoint(39.4670,-0.3790), GeoPoint(39.4690,-0.3767), GeoPoint(39.4682,-0.3705),
        GeoPoint(39.4695,-0.3685), GeoPoint(39.4710,-0.3670), GeoPoint(39.4740,-0.3660),
        GeoPoint(39.4760,-0.3670), GeoPoint(39.4800,-0.3680), GeoPoint(39.4840,-0.3690),
        GeoPoint(39.4880,-0.3700), GeoPoint(39.4920,-0.3690), GeoPoint(39.4960,-0.3680),
        GeoPoint(39.5000,-0.3660), GeoPoint(39.5030,-0.3650), GeoPoint(39.5060,-0.3640),
    ), stations = listOf(
        "Riba-roja de Túria" to 0, "La Presa" to 1, "Masia de Traver" to 2,
        "La Cova" to 3, "Benissanó" to 5, "Font del Barranc" to 7,
        "València la Vella" to 9, "Roses" to 12, "Salt de l'Aigua" to 13,
        "Quart de Poblet" to 14, "Faitanar" to 15, "Mislata-Almassil" to 16,
        "Mislata" to 17, "Nou d'Octubre" to 18, "Avinguda del Cid" to 19,
        "Àngel Guimerà" to 20, "Xàtiva" to 22, "Colón" to 23,
        "Alameda" to 24, "Facultats-Manuel Broseta" to 26,
        "Benimaclet" to 27, "Machado" to 28,
        "Alboraia Palmaret" to 29, "Alboraia Peris Aragó" to 30,
    )),
    LineData("L10", AndroidColor.rgb(0, 180, 216), path = listOf(
        GeoPoint(39.4590,-0.3760), GeoPoint(39.4600,-0.3720), GeoPoint(39.4605,-0.3650),
        GeoPoint(39.4610,-0.3580), GeoPoint(39.4610,-0.3540), GeoPoint(39.4590,-0.3500),
        GeoPoint(39.4570,-0.3450), GeoPoint(39.4550,-0.3400), GeoPoint(39.4520,-0.3340),
        GeoPoint(39.4490,-0.3280),
    ), stations = listOf(
        "Alacant" to 0, "Russafa" to 1, "Amado Granell-Montolivet" to 2,
        "Moreres" to 3, "Quatre Carreres" to 4,
        "Ciutat Arts i Ciències-Justícia" to 5, "Oceanogràfic" to 6,
        "Natzaret" to 7,
    ), tram = true),
)

private val stationPositions = mapOf(
    "Aeroport" to GeoPoint(39.492190, -0.474374),
    "Alacant" to GeoPoint(39.464898, -0.377451),
    "Alameda" to GeoPoint(39.473108, -0.366018),
    "Albalat dels Sorells" to GeoPoint(39.545339, -0.347981),
    "Alberic" to GeoPoint(39.117012, -0.523655),
    "Alboraia Palmaret" to GeoPoint(39.495820, -0.355561),
    "Alboraia Peris Aragó" to GeoPoint(39.500891, -0.352361),
    "Alfauir" to GeoPoint(39.489709, -0.365424),
    "Alginet" to GeoPoint(39.262932, -0.474863),
    "Almàssera" to GeoPoint(39.512224, -0.354204),
    "Amado Granell-Montolivet" to GeoPoint(39.459367, -0.365168),
    "Amistat-Casa de Salud" to GeoPoint(39.470333, -0.350490),
    "Aragó" to GeoPoint(39.472407, -0.357540),
    "Ausiàs March" to GeoPoint(39.249372, -0.492210),
    "Avinguda del Cid" to GeoPoint(39.468547, -0.397934),
    "Ayora" to GeoPoint(39.467775, -0.343127),
    "Bailén" to GeoPoint(39.463596, -0.379276),
    "Benaguasil" to GeoPoint(39.615000, -0.590000),
    "Beniferri" to GeoPoint(39.490871, -0.399391),
    "Benimaclet" to GeoPoint(39.484998, -0.363051),
    "Benimodo" to GeoPoint(39.216471, -0.519595),
    "Benimàmet" to GeoPoint(39.501850, -0.418720),
    "Benissanó" to GeoPoint(39.500000, -0.485000),
    "Beteró" to GeoPoint(39.476702, -0.334468),
    "Burjassot" to GeoPoint(39.508294, -0.406792),
    "Burjassot-Godella" to GeoPoint(39.513126, -0.411667),
    "Bétera" to GeoPoint(39.590729, -0.457496),
    "Cabanyal" to GeoPoint(39.472274, -0.327575),
    "Campanar" to GeoPoint(39.483551, -0.394109),
    "Campus" to GeoPoint(39.507068, -0.417369),
    "Cantereria" to GeoPoint(39.502538, -0.411944),
    "Carlet" to GeoPoint(39.227393, -0.525128),
    "Castelló" to GeoPoint(39.078500, -0.515300),
    "Ciutat Arts i Ciències-Justícia" to GeoPoint(39.452691, -0.353344),
    "Col·legi El Vedat" to GeoPoint(39.411275, -0.460251),
    "Colón" to GeoPoint(39.470185, -0.370838),
    "Doctor Lluch" to GeoPoint(39.468438, -0.328127),
    "El Clot" to GeoPoint(39.549904, -0.527975),
    "Empalme" to GeoPoint(39.499441, -0.402286),
    "Entrepins" to GeoPoint(39.543260, -0.514020),
    "Espioca" to GeoPoint(39.326419, -0.468678),
    "Estadi Ciutat de València" to GeoPoint(39.495042, -0.365185),
    "Facultats-Manuel Broseta" to GeoPoint(39.478104, -0.361884),
    "Faitanar" to GeoPoint(39.477678, -0.433410),
    "Florista" to GeoPoint(39.495725, -0.397910),
    "Foios" to GeoPoint(39.537273, -0.353825),
    "Fondo de Benaguasil" to GeoPoint(39.605000, -0.587000),
    "Font Almaguer" to GeoPoint(39.289331, -0.464540),
    "Font del Barranc" to GeoPoint(39.517550, -0.469747),
    "Fuente del Jarro" to GeoPoint(39.511157, -0.464076),
    "Gallipont-Torre del Virrei" to GeoPoint(39.568928, -0.542100),
    "Garbí" to GeoPoint(39.492154, -0.394133),
    "Godella" to GeoPoint(39.519185, -0.414100),
    "Grau-La Marina" to GeoPoint(39.463159, -0.330008),
    "Horta Vella" to GeoPoint(39.581997, -0.442839),
    "Jesús" to GeoPoint(39.459362, -0.384649),
    "L'Alcúdia" to GeoPoint(39.194711, -0.504241),
    "L'Eliana" to GeoPoint(39.561624, -0.535952),
    "La Cadena" to GeoPoint(39.475245, -0.329417),
    "La Canyada" to GeoPoint(39.526957, -0.487104),
    "La Carrasca" to GeoPoint(39.479595, -0.344500),
    "La Coma" to GeoPoint(39.521575, -0.431729),
    "La Cova" to GeoPoint(39.498913, -0.484703),
    "La Granja" to GeoPoint(39.503911, -0.412349),
    "La Pobla de Farnals" to GeoPoint(39.578913, -0.330055),
    "La Pobla de Vallbona" to GeoPoint(39.550000, -0.540000),
    "La Presa" to GeoPoint(39.517079, -0.515991),
    "La Vallesa" to GeoPoint(39.535989, -0.495404),
    "Les Carolines-Fira" to GeoPoint(39.499015, -0.424924),
    "Lloma Llarga-Terramelar" to GeoPoint(39.509865, -0.430454),
    "Llíria" to GeoPoint(39.624000, -0.595000),
    "Machado" to GeoPoint(39.492086, -0.358713),
    "Manises" to GeoPoint(39.489583, -0.459426),
    "Marxalenes" to GeoPoint(39.487955, -0.383859),
    "Marítim" to GeoPoint(39.464835, -0.338506),
    "Mas del Rosari" to GeoPoint(39.524993, -0.435492),
    "Masia de Traver" to GeoPoint(39.538265, -0.546633),
    "Masies" to GeoPoint(39.566508, -0.405263),
    "Massamagrell" to GeoPoint(39.570499, -0.332612),
    "Massarrojos" to GeoPoint(39.536441, -0.402960),
    "Meliana" to GeoPoint(39.528017, -0.351626),
    "Mislata" to GeoPoint(39.473986, -0.418117),
    "Mislata-Almassil" to GeoPoint(39.475487, -0.422783),
    "Moncada-Alfara" to GeoPoint(39.543501, -0.388571),
    "Montesol" to GeoPoint(39.555439, -0.531805),
    "Moreres" to GeoPoint(39.449970, -0.338455),
    "Museros" to GeoPoint(39.561747, -0.340704),
    "Natzaret" to GeoPoint(39.449931, -0.334764),
    "Neptú" to GeoPoint(39.463232, -0.325854),
    "Nou d'Octubre" to GeoPoint(39.470762, -0.406872),
    "Oceanogràfic" to GeoPoint(39.451952, -0.347381),
    "Orriols" to GeoPoint(39.493161, -0.367532),
    "Paiporta" to GeoPoint(39.432220, -0.417878),
    "Palau de Congressos" to GeoPoint(39.497236, -0.400068),
    "Parc Científic" to GeoPoint(39.515211, -0.422457),
    "Paterna" to GeoPoint(39.498891, -0.441940),
    "Patraix" to GeoPoint(39.456467, -0.390342),
    "Picanya" to GeoPoint(39.433113, -0.437142),
    "Picassent" to GeoPoint(39.363056, -0.464670),
    "Plaça d'Espanya" to GeoPoint(39.466220, -0.382088),
    "Pont de Fusta" to GeoPoint(39.482188, -0.372913),
    "Quart de Poblet" to GeoPoint(39.480827, -0.441785),
    "Quatre Carreres" to GeoPoint(39.452403, -0.360211),
    "Rafelbunyol" to GeoPoint(39.587968, -0.330870),
    "Realón" to GeoPoint(39.393988, -0.464049),
    "Reus" to GeoPoint(39.485931, -0.381717),
    "Riba-roja de Túria" to GeoPoint(39.544000, -0.560000),
    "Rocafort" to GeoPoint(39.529191, -0.407305),
    "Roses" to GeoPoint(39.492673, -0.467162),
    "Russafa" to GeoPoint(39.464108, -0.369483),
    "Safranar" to GeoPoint(39.455085, -0.397769),
    "Sagunt" to GeoPoint(39.486581, -0.374191),
    "Salt de l'Aigua" to GeoPoint(39.485307, -0.450439),
    "Sant Isidre" to GeoPoint(39.451070, -0.402617),
    "Sant Joan" to GeoPoint(39.505313, -0.416420),
    "Sant Miquel dels Reis" to GeoPoint(39.497640, -0.369894),
    "Sant Pau" to GeoPoint(39.488309, -0.403980),
    "Santa Rita" to GeoPoint(39.505904, -0.455317),
    "Seminari-CEU" to GeoPoint(39.549749, -0.389610),
    "Tarongers-Ernest Lluch" to GeoPoint(39.478163, -0.339534),
    "Tomás y Valiente" to GeoPoint(39.519726, -0.425709),
    "Torrent" to GeoPoint(39.436098, -0.460926),
    "Torrent Avinguda" to GeoPoint(39.431683, -0.472632),
    "Tossal del Rei" to GeoPoint(39.495836, -0.372416),
    "Trinitat" to GeoPoint(39.486315, -0.367537),
    "Trànsits" to GeoPoint(39.489553, -0.387039),
    "Túria" to GeoPoint(39.478254, -0.390276),
    "Universitat Politècnica" to GeoPoint(39.481184, -0.350014),
    "València Sud" to GeoPoint(39.440820, -0.410513),
    "València la Vella" to GeoPoint(39.530481, -0.534227),
    "Vicent Andrés Estellés" to GeoPoint(39.508570, -0.419887),
    "Vicent Zaragozá" to GeoPoint(39.483305, -0.357290),
    "Xàtiva" to GeoPoint(39.467667, -0.377580),
    "À Punt" to GeoPoint(39.512165, -0.424693),
    "Àngel Guimerà" to GeoPoint(39.470118, -0.384919),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSMNetworkMapRoute(
    onBack: () -> Unit,
    onOpenPdf: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.getPackageName()
            osmdroidTileCache = context.cacheDir
        }
    }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    val visibleLines = remember { mutableStateListOf(*metroLines.map { it.name }.toTypedArray()) }
    var selectedStation by remember { mutableStateOf<StationInfo?>(null) }
    var showLegend by remember { mutableStateOf(true) }

    val stationLines = remember {
        val map = mutableMapOf<String, MutableList<String>>()
        metroLines.forEach { line ->
            line.stations.forEach { (name, _) ->
                map.getOrPut(name) { mutableListOf() }.add(line.name)
            }
        }
        map
    }
    val stationPoints = remember {
        val map = mutableMapOf<String, GeoPoint>()
        metroLines.forEach { line ->
            line.stations.forEach { (name, idx) ->
                map.putIfAbsent(name, stationPositions[name]
                    ?: if (idx in line.path.indices) line.path[idx]
                    else null)
            }
        }
        map
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa Metrovalencia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showLegend = !showLegend }) {
                        Icon(Icons.Outlined.Layers, contentDescription = "Leyenda")
                    }
                    IconButton(onClick = onOpenPdf) {
                        Icon(Icons.Outlined.Map, contentDescription = "Plano PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Column {
                selectedStation?.let { info ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(info.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    info.lines.forEach { lineName ->
                                        val line = metroLines.find { it.name == lineName }
                                        if (line != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(line.color))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                            ) {
                                                Text(lineName, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { selectedStation = null }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Cerrar")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    metroLines.forEach { line ->
                        val selected = line.name in visibleLines
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color(line.color).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (selected) {
                                        visibleLines.remove(line.name)
                                        removeLineOverlays(mapView, line.color)
                                    } else {
                                        visibleLines.add(line.name)
                                        addLineOverlay(mapView, line)
                                    }
                                    mapView?.invalidate()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) Color(line.color) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(line.name, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Color(line.color) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        controller.setZoom(11.5)
                        controller.setCenter(GeoPoint(39.47, -0.38))

                        overlays.add(ScaleBarOverlay(this).apply {
                            setAlignBottom(true)
                            setAlignRight(true)
                        })

                        val res = ctx.resources
                        val density = ctx.resources.displayMetrics.density

                        metroLines.forEach { line ->
                            addLineOverlay(this, line)
                            line.stations.forEach { (name, _) ->
                                val point = stationPoints[name] ?: return@forEach
                                val primaryColor = stationLines[name]?.firstOrNull()
                                    ?.let { ln -> metroLines.find { it.name == ln }?.color }
                                    ?: line.color
                                val lineCount = stationLines[name]?.size ?: 0
                                val lineNumber = stationLines[name]?.firstOrNull()
                                    ?.removePrefix("L") ?: ""
                                overlays.add(Marker(this).apply {
                                    position = point
                                    title = name
                                    snippet = stationLines[name]?.joinToString(",") ?: ""
                                    icon = createLineCircleMarker(res, primaryColor, density, lineNumber, lineCount > 1)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    setOnMarkerClickListener { m, _ ->
                                        selectedStation = StationInfo(
                                            name = m.title ?: "",
                                            lines = stationLines[m.title] ?: emptyList(),
                                        )
                                        m.showInfoWindow()
                                        true
                                    }
                                })
                            }
                        }

                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (showLegend) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .width(150.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Líneas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        metroLines.forEach { line ->
                            val active = line.name in visibleLines
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (active) Color(line.color) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = line.name + if (line.tram) " T" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Toca una estación", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun addLineOverlay(map: MapView?, line: LineData) {
    val m = map ?: return
    m.overlays.add(Polyline().apply {
        outlinePaint.apply {
            color = line.color
            strokeWidth = if (line.tram) 5f else 7f
            isAntiAlias = true
            alpha = 200
        }
        setPoints(line.path)
    })
}

private fun removeLineOverlays(map: MapView?, color: Int) {
    map?.overlays?.removeAll { it is Polyline && it.outlinePaint.color == color }
}

private fun createLineCircleMarker(res: Resources, color: Int, density: Float, lineNumber: String, multiLine: Boolean): BitmapDrawable {
    val size = (28 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    val r = size / 2f - 2f
    val ri = r - 3f * density

    Paint(Paint.ANTI_ALIAS_FLAG).let { p ->
        p.color = AndroidColor.WHITE
        p.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, r + 0.5f, p)
    }

    Paint(Paint.ANTI_ALIAS_FLAG).let { p ->
        p.color = color
        p.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, ri, p)
    }

    Paint(Paint.ANTI_ALIAS_FLAG).let { p ->
        p.color = AndroidColor.WHITE
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * density
        canvas.drawCircle(cx, cy, r, p)
    }

    Paint(Paint.ANTI_ALIAS_FLAG).let { p ->
        p.color = AndroidColor.WHITE
        p.style = Paint.Style.FILL
        p.textSize = 13f * density
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = true
        val fm = p.fontMetrics
        canvas.drawText(lineNumber, cx, cy - (fm.ascent + fm.descent) / 2f, p)
    }

    if (multiLine) {
        Paint(Paint.ANTI_ALIAS_FLAG).let { p ->
            p.color = AndroidColor.WHITE
            p.style = Paint.Style.FILL
            p.textSize = 7f * density
            p.textAlign = Paint.Align.CENTER
            val label = "+"
            canvas.drawText(label, cx + ri * 0.5f, cy - ri * 0.5f - 2f * density, p)
        }
    }

    return BitmapDrawable(res, bitmap)
}
