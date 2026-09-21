package com.glossostudio.transitos.map

import android.Manifest
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glossostudio.transitos.R
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.ui.AnimatedMinutes
import com.glossostudio.transitos.core.ui.LineBadge
import com.glossostudio.transitos.core.ui.RealtimeDot
import com.glossostudio.transitos.core.ui.StatusPill
import com.glossostudio.transitos.core.ui.R as coreUiR
import org.koin.androidx.compose.koinViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/** Zoom level at or above which station markers are worth drawing. */
private const val MARKER_ZOOM = 13.0

/** How often the approximate train positions are recomputed and redrawn. */
private const val TRAIN_TICK_MS = 400L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSMNetworkMapRoute(
    onBack: () -> Unit,
    onOpenPdf: () -> Unit,
    viewModel: MapViewModel = koinViewModel(),
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.getPackageName()
            osmdroidTileCache = context.cacheDir
        }
    }

    val network by viewModel.network.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val alertedLineNames = remember(alerts) { viewModel.alertedLineNames }
    val liveTrainsEnabled by viewModel.liveTrainsEnabled.collectAsStateWithLifecycle()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var focusedLine by remember { mutableStateOf<String?>(null) }
    var selectedStation by remember { mutableStateOf<MapStation?>(null) }
    var showLocation by remember { mutableStateOf(false) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var trainOverlay by remember { mutableStateOf<LiveTrainsOverlay?>(null) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    val trainSimulator = remember(network.lines) { TrainSimulator(network.lines) }
    // Station markers only make sense once the map is zoomed in; at overview
    // zoom they overlap into an unreadable blob, so the map shows just lines.
    var markersVisible by remember { mutableStateOf(false) }

    val nearestStation = remember(userLocation, network) {
        userLocation?.let { loc ->
            var best: Pair<MapStation, Float>? = null
            val results = FloatArray(1)
            network.stations.forEach { station ->
                android.location.Location.distanceBetween(
                    loc.latitude, loc.longitude,
                    station.position.latitude, station.position.longitude,
                    results,
                )
                if (best == null || results[0] < best!!.second) {
                    best = station to results[0]
                }
            }
            best?.let { (station, dist) ->
                Triple(station, dist.toDouble(), station.position)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            showLocation = true
            mapView?.let { map ->
                locationOverlay?.let { overlay ->
                    overlay.enableMyLocation()
                    overlay.lastFix?.let { fix ->
                        map.controller.animateTo(GeoPoint(fix.latitude, fix.longitude))
                    }
                }
            }
        }
    }

    val onLocationClick: () -> Unit = {
        if (showLocation) {
            locationOverlay?.let { overlay ->
                overlay.lastFix?.let { fix ->
                    mapView?.controller?.animateTo(GeoPoint(fix))
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(mapView, network, focusedLine, markersVisible, trainOverlay) {
        rebuildOverlays(
            map = mapView,
            network = network,
            focusedLine = focusedLine,
            alertedLineNames = alertedLineNames,
            showMarkers = markersVisible,
            trainOverlay = trainOverlay,
        ) { info ->
            selectedStation = info
            mapView?.let { map ->
                if (map.zoomLevelDouble < 15) {
                    map.controller.setZoom(15.5)
                    map.controller.animateTo(info.position)
                } else {
                    map.controller.animateTo(info.position)
                }
            }
        }
    }

    // Approximate train movement. The simulation is advanced from a wall clock
    // so it stays smooth and deterministic without holding per-train state.
    LaunchedEffect(liveTrainsEnabled, trainOverlay, trainSimulator, mapView) {
        val overlay = trainOverlay ?: return@LaunchedEffect
        val map = mapView ?: return@LaunchedEffect
        if (!liveTrainsEnabled) {
            overlay.markers = emptyList()
            map.invalidate()
            return@LaunchedEffect
        }
        while (true) {
            val clockSeconds = System.currentTimeMillis() / 1000.0
            overlay.markers = trainSimulator.markersAt(clockSeconds)
            map.invalidate()
            kotlinx.coroutines.delay(TRAIN_TICK_MS)
        }
    }

    LaunchedEffect(showLocation) {
        while (showLocation) {
            locationOverlay?.lastFix?.let { fix ->
                userLocation = GeoPoint(fix.latitude, fix.longitude)
            }
            kotlinx.coroutines.delay(2000)
        }
    }

    Scaffold(
        // The host Scaffold already insets content for the system bars and the
        // bottom navigation, so this nested Scaffold must not re-apply them.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.map_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(coreUiR.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPdf) {
                        Icon(Icons.Outlined.Map, contentDescription = stringResource(R.string.map_pdf_cd))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = selectedStation != null,
                    enter = slideInVertically { it } + androidx.compose.animation.fadeIn(),
                    exit = slideOutVertically { it } + androidx.compose.animation.fadeOut(),
                ) {
                    selectedStation?.let { info ->
                        StationArrivalsPanel(
                            stationInfo = info,
                            viewModel = viewModel,
                            onClose = { selectedStation = null },
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            network.lines.forEach { line ->
                                val isFocused = focusedLine == line.name
                                val isDimmed = focusedLine != null && !isFocused
                                val hasAlert = line.name.removePrefix("L") in
                                    alertedLineNames.map { it.removePrefix("L") }

                                Row(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .background(if (isFocused) Color(line.color) else Color.Transparent)
                                        .clickable { focusedLine = if (isFocused) null else line.name }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isDimmed) Color(line.color).copy(alpha = 0.3f)
                                                else Color(line.color),
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
                                    if (hasAlert) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Outlined.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isFocused) Color.White
                                            else if (isDimmed) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
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

                        val locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        overlays.add(locOverlay)
                        locationOverlay = locOverlay

                        val trains = LiveTrainsOverlay()
                        overlays.add(trains)
                        trainOverlay = trains

                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean = false

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                val visible = zoomLevelDouble >= MARKER_ZOOM
                                if (visible != markersVisible) markersVisible = visible
                                return false
                            }
                        })

                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            FilledTonalIconButton(
                onClick = { viewModel.setLiveTrainsEnabled(!liveTrainsEnabled) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
                    .size(52.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (liveTrainsEnabled) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (liveTrainsEnabled) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Train,
                    contentDescription = stringResource(R.string.map_trains_cd),
                )
            }

            FilledTonalIconButton(
                onClick = onLocationClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .size(52.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (showLocation) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
            ) {
                Icon(
                    imageVector = if (showLocation) Icons.Outlined.MyLocation else Icons.Outlined.LocationOff,
                    contentDescription = stringResource(R.string.map_location_cd),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (liveTrainsEnabled) {
                    LiveTrainsExperimentalBanner()
                }

                nearestStation?.let { (station, distance, pos) ->
                    Surface(
                        modifier = Modifier
                            .clickable {
                                selectedStation = station
                                mapView?.let { map ->
                                    if (map.zoomLevelDouble < 15) {
                                        map.controller.setZoom(15.5)
                                        map.controller.animateTo(pos)
                                    } else {
                                        map.controller.animateTo(pos)
                                    }
                                }
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        shadowElevation = 4.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Outlined.NearMe,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (distance < 1000) "${distance.toInt()} m"
                                else "${"%.1f".format(distance / 1000)} km",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The honest disclaimer for the simulated trains. Shown whenever they are on,
 * so the user never mistakes the animation for a real GPS feed.
 */
@Composable
private fun LiveTrainsExperimentalBanner() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.map_trains_experimental),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun StationArrivalsPanel(
    stationInfo: MapStation,
    viewModel: MapViewModel,
    onClose: () -> Unit,
) {
    val stopId = stationInfo.stopId
    val arrivalsLoaded by remember(stopId) {
        if (stopId != null) {
            kotlinx.coroutines.flow.flow {
                emit(false)
                viewModel.observeArrivals(stopId).collect { emit(true) }
            }
        } else {
            kotlinx.coroutines.flow.flowOf(true)
        }
    }.collectAsStateWithLifecycle(initialValue = false)

    val arrivals by remember(stopId) {
        if (stopId != null) viewModel.observeArrivals(stopId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stationInfo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        stationInfo.lines.forEach { lineName ->
                            LineBadge(
                                label = lineName.removePrefix("L"),
                                colorArgb = viewModel.network.value.lineColor(lineName)?.toLong(),
                                size = 26.dp,
                            )
                        }
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(coreUiR.string.cd_close))
                }
            }

            if (stopId == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(coreUiR.string.arrival_unconfirmed_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (!arrivalsLoaded) {
                Spacer(Modifier.height(8.dp))
                repeat(3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SkeletonBox(modifier = Modifier.size(24.dp), cornerRadius = 4.dp)
                        SkeletonBox(modifier = Modifier.weight(1f).height(14.dp), cornerRadius = 4.dp)
                        SkeletonBox(modifier = Modifier.width(40.dp).height(14.dp), cornerRadius = 4.dp)
                    }
                }
            } else if (arrivals.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(coreUiR.string.arrival_unconfirmed_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                arrivals.take(4).forEach { arrival ->
                    ArrivalRowCompact(arrival)
                }
            }
        }
    }
}

@Composable
private fun SkeletonBox(modifier: Modifier, cornerRadius: Dp = 4.dp) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)),
    )
}

@Composable
private fun ArrivalRowCompact(arrival: Arrival) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LineBadge(
            label = arrival.lineShortName ?: stringResource(coreUiR.string.arrival_no_line),
            colorArgb = arrival.lineColor,
            size = 28.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = arrival.destination,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (arrival.isRealTime) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    RealtimeDot(diameter = 6.dp)
                    Text(
                        text = stringResource(coreUiR.string.arrival_live),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        val minutes = arrival.minutesAway
        if (minutes != null && minutes > 0) {
            AnimatedMinutes(
                minutes = minutes,
                numberStyle = MaterialTheme.typography.titleMedium,
                unitStyle = MaterialTheme.typography.labelSmall,
            )
        } else {
            StatusPill(
                text = stringResource(coreUiR.string.arrival_boarding),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

private fun rebuildOverlays(
    map: MapView?,
    network: MapNetwork,
    focusedLine: String?,
    alertedLineNames: Set<String>,
    showMarkers: Boolean,
    trainOverlay: LiveTrainsOverlay?,
    onStationTap: (MapStation) -> Unit,
) {
    val m = map ?: return
    val isAnyFocused = focusedLine != null

    m.overlays.retainAll { it is ScaleBarOverlay || it is MyLocationNewOverlay }

    val res = m.context.resources
    val density = res.displayMetrics.density

    network.lines.forEach { line ->
        val isFocused = focusedLine == line.name
        val lineAlpha = when {
            !isAnyFocused -> 220
            isFocused -> 255
            else -> 50
        }
        val strokeWidth = when {
            isFocused -> 10f
            line.isTram -> 5f
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

        if (showMarkers) line.stationNames.forEach { name ->
            val station = network.stationFor(name) ?: return@forEach
            val point = station.position
            val linesForStation = station.lines
            val stationOnFocused = isAnyFocused && focusedLine in linesForStation
            val markerAlpha = when {
                !isAnyFocused -> 1f
                stationOnFocused -> 1f
                else -> 0.2f
            }

            val primaryColor = linesForStation.firstOrNull()
                ?.let { ln -> network.lineColor(ln) }
                ?: line.color
            val lineColors = linesForStation.mapNotNull { ln -> network.lineColor(ln) }
            val lineNumbers = linesForStation.map { it.removePrefix("L") }

            m.overlays.add(Marker(m).apply {
                position = point
                title = name
                snippet = linesForStation.joinToString(", ")
                icon = createLineCircleMarker(res, primaryColor, density, lineNumbers, lineColors).apply {
                    alpha = (markerAlpha * 255).toInt()
                }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ ->
                    onStationTap(station)
                    true
                }
            })
        }
    }

    // Drawn last so trains always sit above the lines and station markers.
    trainOverlay?.let { m.overlays.add(it) }

    m.invalidate()
}

private fun createLineCircleMarker(res: Resources, color: Int, density: Float, lineNumbers: List<String>, lineColors: List<Int>): BitmapDrawable {
    val multiLine = lineNumbers.size > 1
    val size = (24 * density).toInt()
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
