package app.transitos.map

import android.graphics.Color as AndroidColor
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Layers
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
    val visibleLines = remember {
        mutableStateListOf(*metrovalenciaLines.map { it.name }.toTypedArray())
    }
    var selectedStation by remember { mutableStateOf<StationInfo?>(null) }
    var showLegend by remember { mutableStateOf(true) }

    val stationLines = remember {
        val map = mutableMapOf<String, MutableList<String>>()
        metrovalenciaLines.forEach { line ->
            line.stations.forEach { (name, _) ->
                map.getOrPut(name) { mutableListOf() }.add(line.name)
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
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = info.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                info.lines.forEach { lineName ->
                                    val line = metrovalenciaLines.find { it.name == lineName }
                                    if (line != null && lineName in visibleLines) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(line.color))
                                                .padding(horizontal = 8.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = lineName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
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
                    metrovalenciaLines.forEach { line ->
                        val selected = line.name in visibleLines
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (selected) Color(line.color).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
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
                                        .background(
                                            if (selected) Color(line.color)
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        ),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = line.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color(line.color)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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

                        metrovalenciaLines.forEach { line ->
                            addLineOverlay(this, line)
                            line.stations.forEach { (name, point) ->
                                overlays.add(Marker(this).apply {
                                    position = point
                                    title = name
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
                        .width(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Leyenda",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        metrovalenciaLines.forEach { line ->
                            val active = line.name in visibleLines
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) Color(line.color)
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                        ),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = line.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                                    color = if (active) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Toca una estación para ver líneas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun addLineOverlay(map: MapView?, line: MetroLine) {
    val m = map ?: return
    m.overlays.add(Polyline().apply {
        outlinePaint.apply {
            color = line.color
            strokeWidth = if (line.name in listOf("L4", "L6", "L8", "L10")) 6f else 8f
            isAntiAlias = true
            alpha = 200
        }
        setPoints(line.linePoints)
    })
}

private fun removeLineOverlays(map: MapView?, color: Int) {
    map?.overlays?.removeAll { it is Polyline && it.outlinePaint.color == color }
}
