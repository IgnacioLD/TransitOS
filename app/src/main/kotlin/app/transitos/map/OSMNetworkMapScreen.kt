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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
    LineData("L1", AndroidColor.rgb(254, 198, 1), path = listOf(
        GeoPoint(39.590607,-0.457531),
        GeoPoint(39.586887,-0.449452),
        GeoPoint(39.581932,-0.443144),
        GeoPoint(39.576385,-0.428553),
        GeoPoint(39.565868,-0.411000),
        GeoPoint(39.566582,-0.405378),
        GeoPoint(39.566959,-0.402203),
        GeoPoint(39.565041,-0.398555),
        GeoPoint(39.549988,-0.389744),
        GeoPoint(39.543530,-0.388461),
        GeoPoint(39.540455,-0.388813),
        GeoPoint(39.536610,-0.402878),
        GeoPoint(39.528709,-0.407578),
        GeoPoint(39.519505,-0.414456),
        GeoPoint(39.513531,-0.411997),
        GeoPoint(39.508415,-0.406736),
        GeoPoint(39.499577,-0.402108),
        GeoPoint(39.491032,-0.399337),
        GeoPoint(39.484631,-0.395036),
        GeoPoint(39.478867,-0.391206),
        GeoPoint(39.470303,-0.385036),
        GeoPoint(39.466202,-0.381633),
        GeoPoint(39.459202,-0.384542),
        GeoPoint(39.456451,-0.390508),
        GeoPoint(39.454571,-0.398383),
        GeoPoint(39.451035,-0.402803),
        GeoPoint(39.440811,-0.410647),
        GeoPoint(39.432262,-0.418061),
        GeoPoint(39.430458,-0.423403),
        GeoPoint(39.433121,-0.437158),
        GeoPoint(39.437321,-0.457735),
        GeoPoint(39.436939,-0.460235),
        GeoPoint(39.434647,-0.460986),
        GeoPoint(39.423134,-0.460653),
        GeoPoint(39.393906,-0.464014),
        GeoPoint(39.384865,-0.467261),
        GeoPoint(39.373821,-0.472026),
        GeoPoint(39.363037,-0.464858),
        GeoPoint(39.358822,-0.467262),
        GeoPoint(39.353245,-0.473687),
        GeoPoint(39.339577,-0.476747),
        GeoPoint(39.321884,-0.466747),
        GeoPoint(39.317566,-0.464044),
        GeoPoint(39.289406,-0.464086),
        GeoPoint(39.277515,-0.465374),
        GeoPoint(39.262939,-0.474886),
        GeoPoint(39.249557,-0.491977),
        GeoPoint(39.235012,-0.509405),
        GeoPoint(39.232819,-0.522881),
        GeoPoint(39.226799,-0.524942),
        GeoPoint(39.216293,-0.519619),
        GeoPoint(39.200466,-0.509748),
        GeoPoint(39.193813,-0.510264),
        GeoPoint(39.173798,-0.515886),
        GeoPoint(39.143806,-0.518761),
        GeoPoint(39.117050,-0.523526),
        GeoPoint(39.108017,-0.525541),
        GeoPoint(39.102024,-0.522881),
        GeoPoint(39.095032,-0.515070),
        GeoPoint(39.084007,-0.516014),
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
    LineData("L2", AndroidColor.rgb(230, 0, 150), path = listOf(
        GeoPoint(39.622841,-0.590278),
        GeoPoint(39.614395,-0.595912),
        GeoPoint(39.613392,-0.596234),
        GeoPoint(39.607540,-0.594893),
        GeoPoint(39.598881,-0.583928),
        GeoPoint(39.592724,-0.578081),
        GeoPoint(39.582764,-0.562278),
        GeoPoint(39.572052,-0.545368),
        GeoPoint(39.568665,-0.541917),
        GeoPoint(39.561832,-0.535919),
        GeoPoint(39.555573,-0.531697),
        GeoPoint(39.549866,-0.527986),
        GeoPoint(39.543533,-0.522537),
        GeoPoint(39.542690,-0.520649),
        GeoPoint(39.542458,-0.518954),
        GeoPoint(39.543335,-0.513954),
        GeoPoint(39.543980,-0.510864),
        GeoPoint(39.544079,-0.509083),
        GeoPoint(39.543930,-0.507903),
        GeoPoint(39.543419,-0.506508),
        GeoPoint(39.537842,-0.497978),
        GeoPoint(39.532578,-0.491295),
        GeoPoint(39.526806,-0.487122),
        GeoPoint(39.523491,-0.481617),
        GeoPoint(39.523052,-0.480276),
        GeoPoint(39.519402,-0.471039),
        GeoPoint(39.512733,-0.466104),
        GeoPoint(39.511173,-0.464517),
        GeoPoint(39.505657,-0.455106),
        GeoPoint(39.501625,-0.448680),
        GeoPoint(39.498810,-0.441972),
        GeoPoint(39.497375,-0.439410),
        GeoPoint(39.496193,-0.436803),
        GeoPoint(39.495846,-0.435323),
        GeoPoint(39.495083,-0.432308),
        GeoPoint(39.495110,-0.431439),
        GeoPoint(39.495380,-0.430752),
        GeoPoint(39.496384,-0.428681),
        GeoPoint(39.497509,-0.426546),
        GeoPoint(39.498737,-0.425419),
        GeoPoint(39.500397,-0.423446),
        GeoPoint(39.501408,-0.421751),
        GeoPoint(39.501804,-0.420688),
        GeoPoint(39.501877,-0.419411),
        GeoPoint(39.501915,-0.417641),
        GeoPoint(39.501534,-0.415946),
        GeoPoint(39.501423,-0.414594),
        GeoPoint(39.501740,-0.413661),
        GeoPoint(39.502380,-0.412072),
        GeoPoint(39.502518,-0.409970),
        GeoPoint(39.502716,-0.407717),
        GeoPoint(39.502716,-0.406537),
        GeoPoint(39.502552,-0.405786),
        GeoPoint(39.502022,-0.404863),
        GeoPoint(39.499577,-0.402108),
        GeoPoint(39.491032,-0.399337),
        GeoPoint(39.484631,-0.395036),
        GeoPoint(39.478867,-0.391206),
        GeoPoint(39.470303,-0.385036),
        GeoPoint(39.466202,-0.381633),
        GeoPoint(39.459202,-0.384542),
        GeoPoint(39.456451,-0.390508),
        GeoPoint(39.454571,-0.398383),
        GeoPoint(39.451035,-0.402803),
        GeoPoint(39.448597,-0.404949),
        GeoPoint(39.442799,-0.409069),
        GeoPoint(39.440811,-0.410647),
        GeoPoint(39.437790,-0.412953),
        GeoPoint(39.434975,-0.415045),
        GeoPoint(39.433380,-0.416311),
        GeoPoint(39.433113,-0.416579),
        GeoPoint(39.432869,-0.416880),
        GeoPoint(39.432598,-0.417293),
        GeoPoint(39.432262,-0.418061),
        GeoPoint(39.430477,-0.422974),
        GeoPoint(39.430443,-0.424433),
        GeoPoint(39.433121,-0.437158),
        GeoPoint(39.437386,-0.457735),
        GeoPoint(39.437286,-0.459237),
        GeoPoint(39.436939,-0.460246),
        GeoPoint(39.434647,-0.460986),
        GeoPoint(39.426746,-0.461769),
        GeoPoint(39.425949,-0.463486),
        GeoPoint(39.425884,-0.464559),
        GeoPoint(39.426514,-0.466661),
        GeoPoint(39.431812,-0.472833),
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
    LineData("L3", AndroidColor.rgb(221, 5, 44), path = listOf(
        GeoPoint(39.588524,-0.331058),
        GeoPoint(39.579418,-0.330436),
        GeoPoint(39.576675,-0.330148),
        GeoPoint(39.575264,-0.330105),
        GeoPoint(39.570560,-0.333033),
        GeoPoint(39.565701,-0.335984),
        GeoPoint(39.561577,-0.340856),
        GeoPoint(39.560471,-0.342110),
        GeoPoint(39.559246,-0.343226),
        GeoPoint(39.546909,-0.347228),
        GeoPoint(39.545265,-0.348289),
        GeoPoint(39.543633,-0.349250),
        GeoPoint(39.537224,-0.353869),
        GeoPoint(39.535656,-0.354652),
        GeoPoint(39.534267,-0.354824),
        GeoPoint(39.533249,-0.354717),
        GeoPoint(39.532875,-0.354438),
        GeoPoint(39.528030,-0.351819),
        GeoPoint(39.524700,-0.349932),
        GeoPoint(39.523739,-0.349803),
        GeoPoint(39.522812,-0.349932),
        GeoPoint(39.516621,-0.353150),
        GeoPoint(39.514568,-0.354095),
        GeoPoint(39.512257,-0.354267),
        GeoPoint(39.509785,-0.353950),
        GeoPoint(39.505199,-0.353622),
        GeoPoint(39.500763,-0.352328),
        GeoPoint(39.499340,-0.352163),
        GeoPoint(39.498974,-0.352120),
        GeoPoint(39.497948,-0.352464),
        GeoPoint(39.496822,-0.353365),
        GeoPoint(39.496181,-0.354196),
        GeoPoint(39.495663,-0.355222),
        GeoPoint(39.494774,-0.356914),
        GeoPoint(39.493740,-0.358171),
        GeoPoint(39.492432,-0.358794),
        GeoPoint(39.484852,-0.362333),
        GeoPoint(39.483841,-0.363064),
        GeoPoint(39.481091,-0.359716),
        GeoPoint(39.480392,-0.360661),
        GeoPoint(39.478004,-0.361906),
        GeoPoint(39.473900,-0.363922),
        GeoPoint(39.473156,-0.365317),
        GeoPoint(39.471813,-0.368729),
        GeoPoint(39.470146,-0.370928),
        GeoPoint(39.466976,-0.374951),
        GeoPoint(39.467186,-0.377375),
        GeoPoint(39.468071,-0.380144),
        GeoPoint(39.468933,-0.381904),
        GeoPoint(39.470158,-0.383534),
        GeoPoint(39.470303,-0.385036),
        GeoPoint(39.468105,-0.395293),
        GeoPoint(39.468220,-0.397575),
        GeoPoint(39.470657,-0.407631),
        GeoPoint(39.473824,-0.418306),
        GeoPoint(39.476009,-0.424369),
        GeoPoint(39.477619,-0.433183),
        GeoPoint(39.481087,-0.441881),
        GeoPoint(39.484833,-0.450568),
        GeoPoint(39.489590,-0.459065),
        GeoPoint(39.492649,-0.467236),
        GeoPoint(39.492367,-0.474919),
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
    LineData("L4", AndroidColor.rgb(1, 74, 153), path = listOf(
        GeoPoint(39.469307,-0.328153),
        GeoPoint(39.472855,-0.327583),
        GeoPoint(39.475204,-0.329375),
        GeoPoint(39.476593,-0.334203),
        GeoPoint(39.478138,-0.339622),
        GeoPoint(39.479660,-0.344825),
        GeoPoint(39.481316,-0.350500),
        GeoPoint(39.483372,-0.357947),
        GeoPoint(39.484852,-0.362333),
        GeoPoint(39.486271,-0.367764),
        GeoPoint(39.481781,-0.373181),
        GeoPoint(39.486500,-0.374972),
        GeoPoint(39.485966,-0.381707),
        GeoPoint(39.487972,-0.383837),
        GeoPoint(39.489563,-0.387258),
        GeoPoint(39.490028,-0.390928),
        GeoPoint(39.492290,-0.394511),
        GeoPoint(39.494434,-0.396817),
        GeoPoint(39.497181,-0.400142),
        GeoPoint(39.499577,-0.402108),
        GeoPoint(39.504032,-0.412478),
        GeoPoint(39.505291,-0.416324),
        GeoPoint(39.507221,-0.417458),
        GeoPoint(39.508560,-0.419884),
        GeoPoint(39.512203,-0.424749),
        GeoPoint(39.515141,-0.422622),
        GeoPoint(39.519772,-0.425636),
        GeoPoint(39.521572,-0.431707),
        GeoPoint(39.524960,-0.435825),
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
    LineData("L5", AndroidColor.rgb(0, 143, 113), path = listOf(
        GeoPoint(39.492367,-0.474919),
        GeoPoint(39.492649,-0.467236),
        GeoPoint(39.489590,-0.459065),
        GeoPoint(39.484833,-0.450568),
        GeoPoint(39.481087,-0.441881),
        GeoPoint(39.477619,-0.433183),
        GeoPoint(39.476009,-0.424369),
        GeoPoint(39.473824,-0.418306),
        GeoPoint(39.470657,-0.407631),
        GeoPoint(39.468220,-0.397575),
        GeoPoint(39.468063,-0.395443),
        GeoPoint(39.470112,-0.385895),
        GeoPoint(39.470303,-0.385036),
        GeoPoint(39.470165,-0.383599),
        GeoPoint(39.468941,-0.381871),
        GeoPoint(39.468147,-0.380316),
        GeoPoint(39.467186,-0.377375),
        GeoPoint(39.467026,-0.375166),
        GeoPoint(39.467102,-0.374844),
        GeoPoint(39.470146,-0.370928),
        GeoPoint(39.471981,-0.368600),
        GeoPoint(39.473156,-0.365317),
        GeoPoint(39.472626,-0.358117),
        GeoPoint(39.470333,-0.350394),
        GeoPoint(39.466427,-0.342969),
        GeoPoint(39.464939,-0.338237),
    ), stations = listOf(
        "Marítim" to 0, "Amistat-Casa de Salud" to 1, "Ayora" to 2,
        "Alameda" to 3, "Colón" to 4, "Xàtiva" to 5,
        "Àngel Guimerà" to 6, "Avinguda del Cid" to 7,
        "Nou d'Octubre" to 8, "Mislata" to 9, "Mislata-Almassil" to 10,
        "Faitanar" to 11, "Quart de Poblet" to 12,
        "Salt de l'Aigua" to 13, "Manises" to 14, "Roses" to 15,
        "Aeroport" to 16,
    )),
    LineData("L6", AndroidColor.rgb(136, 132, 191), path = listOf(
        GeoPoint(39.495953,-0.372537),
        GeoPoint(39.496418,-0.372234),
        GeoPoint(39.497391,-0.371861),
        GeoPoint(39.497856,-0.371652),
        GeoPoint(39.497967,-0.371550),
        GeoPoint(39.498028,-0.371454),
        GeoPoint(39.498043,-0.371298),
        GeoPoint(39.497707,-0.370005),
        GeoPoint(39.497219,-0.368495),
        GeoPoint(39.497066,-0.367908),
        GeoPoint(39.496769,-0.366926),
        GeoPoint(39.496544,-0.365818),
        GeoPoint(39.496418,-0.365320),
        GeoPoint(39.496159,-0.364432),
        GeoPoint(39.496109,-0.364298),
        GeoPoint(39.496044,-0.364228),
        GeoPoint(39.495987,-0.364196),
        GeoPoint(39.495926,-0.364188),
        GeoPoint(39.495838,-0.364217),
        GeoPoint(39.495468,-0.364652),
        GeoPoint(39.494919,-0.365542),
        GeoPoint(39.494507,-0.365859),
        GeoPoint(39.493401,-0.367237),
        GeoPoint(39.493149,-0.367664),
        GeoPoint(39.492970,-0.367782),
        GeoPoint(39.492817,-0.367951),
        GeoPoint(39.492725,-0.367983),
        GeoPoint(39.492657,-0.367986),
        GeoPoint(39.492542,-0.367913),
        GeoPoint(39.492378,-0.367683),
        GeoPoint(39.492111,-0.367326),
        GeoPoint(39.491745,-0.366816),
        GeoPoint(39.490547,-0.365236),
        GeoPoint(39.490173,-0.364893),
        GeoPoint(39.489300,-0.366033),
        GeoPoint(39.488358,-0.367854),
        GeoPoint(39.487858,-0.368640),
        GeoPoint(39.487259,-0.369386),
        GeoPoint(39.487125,-0.369523),
        GeoPoint(39.487003,-0.369547),
        GeoPoint(39.486820,-0.369429),
        GeoPoint(39.486488,-0.368246),
        GeoPoint(39.486271,-0.367764),
        GeoPoint(39.484852,-0.362333),
        GeoPoint(39.483803,-0.358933),
        GeoPoint(39.483372,-0.357947),
        GeoPoint(39.482727,-0.355301),
        GeoPoint(39.481731,-0.352319),
        GeoPoint(39.481125,-0.351691),
        GeoPoint(39.481316,-0.350500),
        GeoPoint(39.479660,-0.344825),
        GeoPoint(39.478138,-0.339622),
        GeoPoint(39.476593,-0.334203),
        GeoPoint(39.475204,-0.329375),
        GeoPoint(39.474655,-0.327353),
        GeoPoint(39.474541,-0.325867),
        GeoPoint(39.474266,-0.325615),
        GeoPoint(39.473690,-0.325728),
        GeoPoint(39.468929,-0.325728),
        GeoPoint(39.468018,-0.325679),
        GeoPoint(39.467625,-0.325593),
        GeoPoint(39.467453,-0.325792),
        GeoPoint(39.466522,-0.327932),
        GeoPoint(39.467068,-0.327476),
        GeoPoint(39.466019,-0.328056),
        GeoPoint(39.464619,-0.327927),
        GeoPoint(39.463593,-0.328088),
        GeoPoint(39.463512,-0.329002),
        GeoPoint(39.463516,-0.329316),
        GeoPoint(39.463100,-0.329472),
        GeoPoint(39.463009,-0.330244),
        GeoPoint(39.462921,-0.330451),
        GeoPoint(39.462955,-0.331406),
        GeoPoint(39.463245,-0.333973),
        GeoPoint(39.463390,-0.334828),
        GeoPoint(39.463535,-0.335571),
        GeoPoint(39.463615,-0.336172),
        GeoPoint(39.463661,-0.336376),
        GeoPoint(39.464142,-0.336413),
        GeoPoint(39.464939,-0.338237),
    ), stations = listOf(
        "Tossal del Rei" to 0, "Estadi Ciutat de València" to 1,
        "Sant Miquel dels Reis" to 2, "Alfauir" to 3,
        "Orriols" to 4, "Benimaclet" to 5, "Trinitat" to 6,
        "Universitat Politècnica" to 7, "Vicent Zaragozá" to 8,
        "Tarongers-Ernest Lluch" to 9, "La Carrasca" to 10,
        "La Cadena" to 11, "Beteró" to 12, "Cabanyal" to 13,
        "Grau-La Marina" to 14, "Marítim" to 15,
    ), tram = true),
    LineData("L7", AndroidColor.rgb(242, 141, 1), path = listOf(
        GeoPoint(39.464939,-0.338237),
        GeoPoint(39.466427,-0.342969),
        GeoPoint(39.470333,-0.350394),
        GeoPoint(39.472626,-0.358117),
        GeoPoint(39.473156,-0.365317),
        GeoPoint(39.471848,-0.368707),
        GeoPoint(39.470146,-0.370928),
        GeoPoint(39.467094,-0.374823),
        GeoPoint(39.467010,-0.375166),
        GeoPoint(39.467094,-0.375853),
        GeoPoint(39.466988,-0.376732),
        GeoPoint(39.466820,-0.377269),
        GeoPoint(39.466557,-0.377805),
        GeoPoint(39.466106,-0.378234),
        GeoPoint(39.463978,-0.379422),
        GeoPoint(39.461800,-0.380058),
        GeoPoint(39.461071,-0.380745),
        GeoPoint(39.460575,-0.381560),
        GeoPoint(39.460079,-0.382676),
        GeoPoint(39.459202,-0.384542),
        GeoPoint(39.458553,-0.385959),
        GeoPoint(39.457577,-0.386882),
        GeoPoint(39.456863,-0.387504),
        GeoPoint(39.456631,-0.388019),
        GeoPoint(39.456516,-0.388727),
        GeoPoint(39.456451,-0.390508),
        GeoPoint(39.456234,-0.394628),
        GeoPoint(39.456112,-0.395658),
        GeoPoint(39.455681,-0.396591),
        GeoPoint(39.455597,-0.396903),
        GeoPoint(39.454571,-0.398383),
        GeoPoint(39.451035,-0.402803),
        GeoPoint(39.440811,-0.410647),
        GeoPoint(39.436886,-0.413688),
        GeoPoint(39.435207,-0.414943),
        GeoPoint(39.434574,-0.415415),
        GeoPoint(39.433422,-0.416316),
        GeoPoint(39.433010,-0.416719),
        GeoPoint(39.432644,-0.417207),
        GeoPoint(39.432262,-0.418061),
        GeoPoint(39.430527,-0.423338),
        GeoPoint(39.430641,-0.425227),
        GeoPoint(39.433121,-0.437158),
        GeoPoint(39.437366,-0.457633),
        GeoPoint(39.437405,-0.458121),
        GeoPoint(39.437386,-0.458636),
        GeoPoint(39.437347,-0.459082),
        GeoPoint(39.437168,-0.459672),
        GeoPoint(39.436924,-0.460128),
        GeoPoint(39.436424,-0.460605),
        GeoPoint(39.435539,-0.460873),
        GeoPoint(39.434647,-0.460986),
        GeoPoint(39.433029,-0.460895),
        GeoPoint(39.429539,-0.460964),
        GeoPoint(39.427162,-0.461200),
        GeoPoint(39.426655,-0.461555),
        GeoPoint(39.426140,-0.462327),
        GeoPoint(39.425793,-0.463260),
        GeoPoint(39.425793,-0.464269),
        GeoPoint(39.426010,-0.465642),
        GeoPoint(39.426846,-0.466919),
        GeoPoint(39.429333,-0.469902),
        GeoPoint(39.431812,-0.472833),
    ), stations = listOf(
        "Marítim" to 0, "Amistat-Casa de Salud" to 1, "Ayora" to 2,
        "Alameda" to 3, "Aragó" to 4, "Bailén" to 5,
        "Colón" to 6, "Patraix" to 8, "Jesús" to 9,
        "Sant Isidre" to 11, "Safranar" to 12,
        "Paiporta" to 14, "València Sud" to 15,
        "Torrent" to 17, "Picanya" to 18, "Torrent Avinguda" to 20,
    )),
    LineData("L8", AndroidColor.rgb(130, 206, 230), path = listOf(
        GeoPoint(39.464939,-0.338237),
        GeoPoint(39.463245,-0.333973),
        GeoPoint(39.463100,-0.329472),
        GeoPoint(39.463253,-0.325851),
    ), stations = listOf(
        "Marítim" to 0, "Neptú" to 3,
    ), tram = true),
    LineData("L9", AndroidColor.rgb(184, 128, 79), path = listOf(
        GeoPoint(39.543335,-0.559444),
        GeoPoint(39.538055,-0.546667),
        GeoPoint(39.517223,-0.515833),
        GeoPoint(39.498890,-0.484444),
        GeoPoint(39.492649,-0.467236),
        GeoPoint(39.489590,-0.459065),
        GeoPoint(39.484833,-0.450568),
        GeoPoint(39.481087,-0.441881),
        GeoPoint(39.477619,-0.433183),
        GeoPoint(39.476009,-0.424369),
        GeoPoint(39.473824,-0.418306),
        GeoPoint(39.470657,-0.407631),
        GeoPoint(39.468220,-0.397575),
        GeoPoint(39.467995,-0.395293),
        GeoPoint(39.470303,-0.385036),
        GeoPoint(39.470058,-0.383534),
        GeoPoint(39.467987,-0.380133),
        GeoPoint(39.467186,-0.377375),
        GeoPoint(39.466869,-0.374941),
        GeoPoint(39.470146,-0.370928),
        GeoPoint(39.471706,-0.368729),
        GeoPoint(39.473156,-0.365317),
        GeoPoint(39.473793,-0.363922),
        GeoPoint(39.478004,-0.361906),
        GeoPoint(39.480286,-0.360661),
        GeoPoint(39.480984,-0.359706),
        GeoPoint(39.483788,-0.363064),
        GeoPoint(39.484852,-0.362333),
        GeoPoint(39.492432,-0.358794),
        GeoPoint(39.493713,-0.358169),
        GeoPoint(39.494804,-0.356911),
        GeoPoint(39.495663,-0.355222),
        GeoPoint(39.496235,-0.354202),
        GeoPoint(39.496773,-0.353365),
        GeoPoint(39.497753,-0.352592),
        GeoPoint(39.499031,-0.352120),
        GeoPoint(39.500763,-0.352328),
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
    LineData("L10", AndroidColor.rgb(183, 221, 121), path = listOf(
        GeoPoint(39.464722,-0.377477),
        GeoPoint(39.463913,-0.369531),
        GeoPoint(39.459373,-0.365110),
        GeoPoint(39.452446,-0.360125),
        GeoPoint(39.452591,-0.353216),
        GeoPoint(39.452038,-0.347342),
        GeoPoint(39.450150,-0.338408),
        GeoPoint(39.449890,-0.334674),
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

private val stationLines: Map<String, List<String>> by lazy {
    val map = mutableMapOf<String, MutableList<String>>()
    metroLines.forEach { line ->
        line.stations.forEach { (name, _) ->
            map.getOrPut(name) { mutableListOf() }.add(line.name)
        }
    }
    map
}

private val stationPoints: Map<String, GeoPoint> by lazy {
    val map = mutableMapOf<String, GeoPoint>()
    metroLines.forEach { line ->
        line.stations.forEach { (name, idx) ->
            val pos = stationPositions[name]
                ?: if (idx in line.path.indices) line.path[idx]
                else null
            if (pos != null) map.putIfAbsent(name, pos)
        }
    }
    map
}

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
    var focusedLine by remember { mutableStateOf<String?>(null) }
    var selectedStation by remember { mutableStateOf<StationInfo?>(null) }

    LaunchedEffect(mapView, focusedLine) {
        rebuildOverlays(mapView, focusedLine) { info -> selectedStation = info }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
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
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    info.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    info.lines.forEach { lineName ->
                                        val line = metroLines.find { it.name == lineName }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(line?.color ?: AndroidColor.GRAY))
                                                .padding(horizontal = 8.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                lineName.removePrefix("L"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                            )
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

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        metroLines.forEach { line ->
                            val isFocused = focusedLine == line.name
                            val isDimmed = focusedLine != null && !isFocused

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFocused) Color(line.color) else Color.Transparent)
                                    .clickable { focusedLine = if (isFocused) null else line.name }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDimmed) Color(line.color).copy(alpha = 0.3f)
                                            else Color(line.color)
                                        ),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    line.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isFocused -> Color.White
                                        isDimmed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
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

                    mapView = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

private fun rebuildOverlays(
    map: MapView?,
    focusedLine: String?,
    onStationTap: (StationInfo) -> Unit,
) {
    val m = map ?: return
    val isAnyFocused = focusedLine != null

    m.overlays.retainAll { it is ScaleBarOverlay }

    val res = m.context.resources
    val density = res.displayMetrics.density

    metroLines.forEach { line ->
        val isFocused = focusedLine == line.name
        val lineAlpha = when {
            !isAnyFocused -> 220
            isFocused -> 255
            else -> 50
        }
        val strokeWidth = when {
            isFocused -> 10f
            line.tram -> 5f
            else -> 7f
        }

        if (line.path.size >= 2) {
            m.overlays.add(Polyline().apply {
                outlinePaint.apply {
                    color = line.color
                    this.strokeWidth = strokeWidth
                    isAntiAlias = true
                    alpha = lineAlpha
                }
                setPoints(line.path)
            })
        }

        line.stations.forEach { (name, _) ->
            val point = stationPoints[name] ?: return@forEach
            val linesForStation = stationLines[name] ?: emptyList()
            val stationOnFocused = isAnyFocused && focusedLine in linesForStation
            val markerAlpha = when {
                !isAnyFocused -> 1f
                stationOnFocused -> 1f
                else -> 0.2f
            }

            val primaryColor = linesForStation.firstOrNull()
                ?.let { ln -> metroLines.find { it.name == ln }?.color }
                ?: line.color
            val lineColors = linesForStation.mapNotNull { ln ->
                metroLines.find { it.name == ln }?.color
            }
            val lineNumbers = linesForStation.map { it.removePrefix("L") }

            m.overlays.add(Marker(m).apply {
                position = point
                title = name
                snippet = linesForStation.joinToString(", ")
                icon = createLineCircleMarker(res, primaryColor, density, lineNumbers, lineColors).apply {
                    alpha = (markerAlpha * 255).toInt()
                }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { marker, _ ->
                    onStationTap(StationInfo(
                        name = marker.title ?: "",
                        lines = linesForStation,
                    ))
                    marker.showInfoWindow()
                    true
                }
            })
        }
    }

    m.invalidate()
}

private fun createLineCircleMarker(res: Resources, color: Int, density: Float, lineNumbers: List<String>, lineColors: List<Int>): BitmapDrawable {
    val multiLine = lineNumbers.size > 1
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

    val colors = if (multiLine) lineColors else listOf(color)

    if (multiLine) {
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val anglePerSegment = 360f / colors.size
        colors.forEachIndexed { i, c ->
            arcPaint.color = c
            canvas.drawArc(
                cx - ri, cy - ri, cx + ri, cy + ri,
                i * anglePerSegment - 90f, anglePerSegment - 0.5f, true, arcPaint,
            )
        }
    } else {
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
    }

    Paint(Paint.ANTI_ALIAS_FLAG).let { p ->
        p.color = AndroidColor.WHITE
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * density
        canvas.drawCircle(cx, cy, r, p)
    }

    return BitmapDrawable(res, bitmap)
}
