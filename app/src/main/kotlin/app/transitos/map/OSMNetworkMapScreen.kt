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
import androidx.compose.material.icons.outlined.Warning
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
import com.glossostudio.transitos.core.ui.R as coreUiR
import org.koin.androidx.compose.koinViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
private data class StationInfo(
    val name: String,
    val lines: List<String>,
    val position: GeoPoint,
)

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

    val stops by viewModel.stops.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val alertedLineNames = remember(alerts) { viewModel.alertedLineNames }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var focusedLine by remember { mutableStateOf<String?>(null) }
    var selectedStation by remember { mutableStateOf<StationInfo?>(null) }
    var showLocation by remember { mutableStateOf(false) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }

    val nearestStation = remember(userLocation) {
        userLocation?.let { loc ->
            var best: Pair<String, Float>? = null
            val results = FloatArray(1)
            stationPoints.forEach { (name, pos) ->
                android.location.Location.distanceBetween(
                    loc.latitude, loc.longitude,
                    pos.latitude, pos.longitude,
                    results,
                )
                if (best == null || results[0] < best!!.second) {
                    best = name to results[0]
                }
            }
            best?.let { (name, dist) ->
                Triple(name, dist.toDouble(), stationPoints[name]!!)
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

    LaunchedEffect(mapView, focusedLine) {
        rebuildOverlays(mapView, focusedLine, alertedLineNames) { info ->
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

    LaunchedEffect(showLocation) {
        while (showLocation) {
            locationOverlay?.lastFix?.let { fix ->
                userLocation = GeoPoint(fix.latitude, fix.longitude)
            }
            kotlinx.coroutines.delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(coreUiR.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
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
                    }) {
                        Icon(
                            imageVector = if (showLocation) Icons.Outlined.MyLocation
                            else Icons.Outlined.LocationOff,
                            contentDescription = null,
                            tint = if (showLocation) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                        val stop = remember(info) { viewModel.stopByName(info.name) }
                        StationArrivalsPanel(
                            stationInfo = info,
                            stopId = stop?.id,
                            viewModel = viewModel,
                            onClose = { selectedStation = null },
                        )
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
                            val hasAlert = line.name.removePrefix("L") in
                                alertedLineNames.map { it.removePrefix("L") }

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

                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            nearestStation?.let { (name, distance, pos) ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .clickable {
                            val lines = stationLines[name] ?: emptyList()
                            selectedStation = StationInfo(
                                name = name,
                                lines = lines,
                                position = pos,
                            )
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
                            text = name,
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

@Composable
private fun StationArrivalsPanel(
    stationInfo: StationInfo,
    stopId: String?,
    viewModel: MapViewModel,
    onClose: () -> Unit,
) {
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
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stationInfo.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        stationInfo.lines.forEach { lineName ->
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
    val lineColor = arrival.lineColor?.let { Color(it) } ?: MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(lineColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                arrival.lineShortName ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            arrival.destination,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if ((arrival.minutesAway ?: 1) <= 0) stringResource(coreUiR.string.arrival_boarding)
                   else "${arrival.minutesAway} ${stringResource(coreUiR.string.unit_minutes)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if ((arrival.minutesAway ?: 99) <= 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun rebuildOverlays(
    map: MapView?,
    focusedLine: String?,
    alertedLineNames: Set<String>,
    onStationTap: (StationInfo) -> Unit,
) {
    val m = map ?: return
    val isAnyFocused = focusedLine != null

    m.overlays.retainAll { it is ScaleBarOverlay || it is MyLocationNewOverlay }

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
                        position = marker.position,
                    ))
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
