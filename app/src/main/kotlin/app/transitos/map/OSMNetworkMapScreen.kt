package app.transitos.map

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSMNetworkMapRoute(
    onBack: () -> Unit,
    onOpenPdf: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = context.cacheDir
        }
    }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    val selectedLines = remember { mutableStateListOf(*metrovalenciaLines.map { it.name }.toTypedArray()) }
    var selectedMarkerTitle by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa interactivo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPdf) {
                        Icon(
                            imageVector = Icons.Outlined.Map,
                            contentDescription = "Plano PDF",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (selectedMarkerTitle != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                ) {
                    Text(
                        text = selectedMarkerTitle ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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

                        val scaleBar = ScaleBarOverlay(this)
                        scaleBar.setAlignBottom(true)
                        scaleBar.setAlignRight(true)
                        overlays.add(scaleBar)

                        metrovalenciaLines.forEach { line ->
                            val polyline = Polyline().apply {
                                outlinePaint.apply {
                                    color = line.color
                                    strokeWidth = if (line.name in listOf("L4", "L6", "L8", "L10")) 6f else 8f
                                    isAntiAlias = true
                                    alpha = 200
                                }
                                setPoints(line.linePoints)
                            }
                            overlays.add(polyline)

                            line.stations.forEach { (name, point) ->
                                val marker = Marker(this).apply {
                                    position = point
                                    title = name
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    setOnMarkerClickListener { m, _ ->
                                        selectedMarkerTitle = m.title
                                        m.showInfoWindow()
                                        true
                                    }
                                }
                                overlays.add(marker)
                            }
                        }

                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Line toggle chips
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                metrovalenciaLines.forEach { line ->
                    val isSelected = line.name in selectedLines
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selectedLines.remove(line.name)
                                mapView?.overlays?.forEach { overlay ->
                                    if (overlay is Polyline && overlay.outlinePaint.color == line.color) {
                                        mapView?.overlays?.remove(overlay)
                                    }
                                }
                            } else {
                                selectedLines.add(line.name)
                                val polyline = Polyline().apply {
                                    outlinePaint.apply {
                                        color = line.color
                                        strokeWidth = if (line.name in listOf("L4", "L6", "L8", "L10")) 6f else 8f
                                        isAntiAlias = true
                                        alpha = 200
                                    }
                                    setPoints(line.linePoints)
                                }
                                mapView?.overlays?.add(0, polyline)
                            }
                            mapView?.invalidate()
                        },
                        label = { Text(line.name, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(line.color),
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}
