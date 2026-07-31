package com.glossostudio.transitos.feature.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.JourneyLeg
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.ui.R as coreUiR
import com.glossostudio.transitos.core.ui.SkeletonBlock
import com.glossostudio.transitos.core.util.stripDiacritics
import com.glossostudio.transitos.feature.planner.R
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlannerRoute(
    modifier: Modifier = Modifier,
    viewModel: PlannerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stops by viewModel.stops.collectAsStateWithLifecycle()
    PlannerScreen(
        state = state,
        stops = stops,
        onOriginSelected = viewModel::setOrigin,
        onDestinationSelected = viewModel::setDestination,
        onDateSelected = viewModel::setDate,
        onTravelTimeSelected = viewModel::setTravelTime,
        onTimeModeSelected = viewModel::setTimeMode,
        onSwap = viewModel::swapEndpoints,
        onSearch = viewModel::search,
        onSaveRoute = viewModel::saveCurrentRoute,
        onRemoveRoute = viewModel::removeCurrentRoute,
        onSelectJourney = viewModel::selectJourney,
        modifier = modifier,
    )
}

@Composable
fun PrefilledPlannerRoute(
    originStopId: String,
    destinationStopId: String,
    modifier: Modifier = Modifier,
    viewModel: PlannerViewModel = koinViewModel(),
) {
    LaunchedEffect(originStopId, destinationStopId) {
        viewModel.prefillRoute(originStopId, destinationStopId)
    }
    PlannerRoute(modifier = modifier, viewModel = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlannerScreen(
    state: PlannerUiState,
    stops: List<Stop>,
    onOriginSelected: (Stop) -> Unit,
    onDestinationSelected: (Stop) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTravelTimeSelected: (String?) -> Unit,
    onTimeModeSelected: (TimeMode) -> Unit,
    onSwap: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveRoute: () -> Unit = {},
    onRemoveRoute: () -> Unit = {},
    onSelectJourney: (Int) -> Unit = {},
) {
    val spacing = LocalSpacing.current
    var picking by rememberSaveable { mutableStateOf<PickingTarget?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var resultSheetVisible by rememberSaveable { mutableStateOf(false) }
    var swapSpin by rememberSaveable { mutableStateOf(0) }

    val timeZone = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.todayIn(timeZone) }
    val tomorrow = remember { today.plus(DatePeriod(days = 1)) }

    LaunchedEffect(state.origin, state.destination) {
        if (!state.hasSearched) resultSheetVisible = false
    }

    LaunchedEffect(state.hasSearched) {
        if (state.hasSearched) resultSheetVisible = true
    }

    val swapRotation by animateFloatAsState(
        targetValue = swapSpin.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "swap",
    )

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = spacing.xl,
                end = spacing.xl,
                top = spacing.xl,
                bottom = spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            item {
                Text(
                    text = stringResource(R.string.planner_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            item {
                EndpointFields(
                    origin = state.origin,
                    destination = state.destination,
                    onPickOrigin = { picking = PickingTarget.ORIGIN },
                    onPickDestination = { picking = PickingTarget.DESTINATION },
                    onSwap = {
                        onSwap()
                        swapSpin += 180
                    },
                    swapRotation = swapRotation,
                )
            }

            item {
                TimeOptions(
                    timeMode = state.timeMode,
                    date = state.date,
                    today = today,
                    tomorrow = tomorrow,
                    travelTime = state.travelTime,
                    onTimeModeSelected = onTimeModeSelected,
                    onDateSelected = onDateSelected,
                    onTravelTimeSelected = onTravelTimeSelected,
                    onShowDatePicker = { showDatePicker = true },
                    onShowTimePicker = { showTimePicker = true },
                )
            }

            if (state.canPlan) {
                item {
                    Button(
                        onClick = {
                            onSearch()
                            resultSheetVisible = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isPlanning,
                        contentPadding = PaddingValues(vertical = spacing.md),
                    ) {
                        if (state.isPlanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                stringResource(R.string.planner_search_route),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }

            if (!state.canPlan) {
                item {
                    QuickPicks(stops = stops, onPick = onOriginSelected)
                }
            }
        }
    }

    if (resultSheetVisible) {
        ResultBottomSheet(
            state = state,
            onDismiss = { resultSheetVisible = false },
            onSaveRoute = onSaveRoute,
            onRemoveRoute = onRemoveRoute,
            onSelectJourney = onSelectJourney,
        )
    }

    val target = picking
    if (target != null) {
        StationPickerSheet(
            title = if (target == PickingTarget.ORIGIN) stringResource(R.string.planner_from)
            else stringResource(R.string.planner_to),
            stops = stops,
            onPick = { stop ->
                when (target) {
                    PickingTarget.ORIGIN -> onOriginSelected(stop)
                    PickingTarget.DESTINATION -> onDestinationSelected(stop)
                }
                picking = null
            },
            onDismiss = { picking = null },
        )
    }

    if (showDatePicker) {
        val initialMillis = state.date.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(
                                Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date,
                            )
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(coreUiR.string.action_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(coreUiR.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val initialHour = state.travelTime?.substringBefore(":")?.toIntOrNull()
            ?: java.time.LocalTime.now().hour
        val initialMinute = state.travelTime?.substringAfter(":")?.toIntOrNull()
            ?: java.time.LocalTime.now().minute
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    if (state.timeMode == TimeMode.ARRIVAL)
                        stringResource(R.string.planner_arrival_time_title)
                    else
                        stringResource(R.string.planner_departure_time_title),
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerState.hour.toString().padStart(2, '0')
                    val m = timePickerState.minute.toString().padStart(2, '0')
                    onTravelTimeSelected("$h:$m")
                    showTimePicker = false
                }) { Text(stringResource(coreUiR.string.action_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(coreUiR.string.action_cancel))
                }
            },
        )
    }
}

private enum class PickingTarget { ORIGIN, DESTINATION }

@Composable
private fun EndpointFields(
    origin: Stop?,
    destination: Stop?,
    onPickOrigin: () -> Unit,
    onPickDestination: () -> Unit,
    onSwap: () -> Unit,
    swapRotation: Float,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        EndpointCard(
            stopName = origin?.name,
            onClick = onPickOrigin,
            dotColor = MaterialTheme.colorScheme.primary,
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            IconButton(
                onClick = onSwap,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = stringResource(R.string.planner_swap_cd),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(swapRotation),
                )
            }
        }
        EndpointCard(
            stopName = destination?.name,
            onClick = onPickDestination,
            dotColor = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun EndpointCard(
    stopName: String?,
    onClick: () -> Unit,
    dotColor: Color,
) {
    val spacing = LocalSpacing.current
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = stopName ?: stringResource(R.string.planner_pick_station),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (stopName != null) FontWeight.Medium else FontWeight.Normal,
                color = if (stopName == null) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TimeOptions(
    timeMode: TimeMode,
    date: LocalDate,
    today: LocalDate,
    tomorrow: LocalDate,
    travelTime: String?,
    onTimeModeSelected: (TimeMode) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTravelTimeSelected: (String?) -> Unit,
    onShowDatePicker: () -> Unit,
    onShowTimePicker: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val leaveNow = timeMode == TimeMode.DEPARTURE && travelTime == null

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = leaveNow,
                onClick = {
                    onTimeModeSelected(TimeMode.DEPARTURE)
                    onTravelTimeSelected(null)
                },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                label = { Text(stringResource(R.string.planner_leave_now), maxLines = 1) },
            )
            SegmentedButton(
                selected = !leaveNow,
                onClick = {
                    onTimeModeSelected(TimeMode.ARRIVAL)
                    if (travelTime == null) {
                        val now = java.time.LocalTime.now()
                        val h = now.hour.toString().padStart(2, '0')
                        val m = now.minute.toString().padStart(2, '0')
                        onTravelTimeSelected("$h:$m")
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                label = { Text(stringResource(R.string.planner_arrive_by), maxLines = 1) },
            )
        }

        AnimatedVisibility(visible = !leaveNow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = false,
                    onClick = onShowDatePicker,
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = {
                        Text(
                            when {
                                date == today -> stringResource(R.string.date_today)
                                date == tomorrow -> stringResource(R.string.date_tomorrow)
                                else -> formatHumanDate(date)
                            },
                        )
                    },
                )
                FilterChip(
                    selected = false,
                    onClick = onShowTimePicker,
                    leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text(travelTime ?: stringResource(R.string.time_now)) },
                )
            }
        }
    }
}

@Composable
private fun QuickPicks(
    stops: List<Stop>,
    onPick: (Stop) -> Unit,
) {
    val spacing = LocalSpacing.current
    val quickPicks = remember(stops) {
        val names = listOf("Xàtiva", "Colón", "Benimaclet", "Marítim", "Aeroport")
        names.mapNotNull { name ->
            stops.firstOrNull { it.name.contains(name, ignoreCase = true) }
        }
    }
    if (quickPicks.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = stringResource(R.string.planner_quick_picks),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            quickPicks.forEach { stop ->
                FilterChip(
                    selected = false,
                    onClick = { onPick(stop) },
                    label = { Text(stop.name, maxLines = 1) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultBottomSheet(
    state: PlannerUiState,
    onDismiss: () -> Unit,
    onSaveRoute: () -> Unit,
    onRemoveRoute: () -> Unit,
    onSelectJourney: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacing = LocalSpacing.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xl)
                .padding(top = spacing.md, bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )

            when {
                state.isPlanning -> SheetSkeleton()

                state.journeys.isNotEmpty() -> {
                    if (state.journeys.size > 1) {
                        JourneyAlternatives(
                            journeys = state.journeys,
                            selectedIndex = state.selectedJourneyIndex,
                            onSelect = onSelectJourney,
                        )
                    }
                    val journey = state.selectedJourney!!
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                            SheetHeaderRow(
                                journey = journey,
                                isSaved = state.isSaved,
                                onSave = onSaveRoute,
                                onRemove = onRemoveRoute,
                            )
                            SheetTimeline(journey = journey)
                            SheetStats(journey = journey)
                        }
                    }
                }

                state.errorMessage != null -> {
                    Text(
                        text = stringResource(R.string.planner_error_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.planner_no_route_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.planner_no_route_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f), height = 36.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 20.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 20.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.8f), height = 20.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 20.dp)
    }
}

@Composable
private fun SheetHeaderRow(
    journey: Journey,
    isSaved: Boolean,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = "${journey.durationMinutes} min",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                journey.departureTime?.let { dep ->
                    Text(
                        text = "Sale $dep",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                journey.arrivalTime?.let { arr ->
                    Text(
                        text = "Llega $arr",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        IconButton(onClick = if (isSaved) onRemove else onSave) {
            Icon(
                imageVector = if (isSaved) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isSaved) stringResource(R.string.planner_route_saved)
                else stringResource(R.string.planner_save_route),
                tint = if (isSaved) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SheetTimeline(journey: Journey) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        journey.legs.forEachIndexed { i, leg ->
            if (i == 0) {
                TimelineStationRow(
                    time = leg.departureTime,
                    name = leg.originName,
                    dotColor = MaterialTheme.colorScheme.primary,
                )
            } else {
                val prevLeg = journey.legs[i - 1]
                TimelineStationRow(
                    time = prevLeg.arrivalTime,
                    name = leg.originName,
                    dotColor = MaterialTheme.colorScheme.outline,
                )
                leg.waitMinutes?.let { wait ->
                    TimelineWaitRow(minutes = wait)
                }
            }

            TimelineLegInfo(
                time = if (i > 0) leg.departureTime else null,
                lines = leg.lineNames,
                lineColors = leg.lineColors,
                headsigns = leg.headsigns,
            )
        }
        val lastLeg = journey.legs.last()
        TimelineStationRow(
            time = lastLeg.arrivalTime,
            name = lastLeg.destinationName,
            dotColor = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun TimelineStationRow(
    time: String?,
    name: String,
    dotColor: Color,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = time ?: "",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(50.dp),
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TimelineLegInfo(
    time: String?,
    lines: List<String>,
    lineColors: List<Long>,
    headsigns: List<String>,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = time ?: "",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(50.dp),
        )
        Spacer(Modifier.width(12.dp + spacing.md))
        lines.forEachIndexed { index, lineName ->
            val bgColor = lineColors.getOrNull(index)?.let { Color(it) }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = bgColor ?: MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = lineName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (bgColor != null) Color.White
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        if (headsigns.isNotEmpty()) {
            Text(
                text = "→",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = headsigns.joinToString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimelineWaitRow(minutes: Int) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Spacer(Modifier.width(50.dp + 12.dp + spacing.md))
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.planner_wait_minutes, minutes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun SheetStats(journey: Journey) {
    val stats = buildList {
        journey.fareZone?.let { add(stringResource(R.string.planner_fare_zone, it)) }
        add(
            if (journey.hasTransfers)
                stringResource(R.string.planner_transfers_count, journey.legs.size - 1)
            else
                stringResource(R.string.planner_no_transfers),
        )
    }
    Text(
        text = stats.joinToString("  ·  "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun JourneyAlternatives(
    journeys: List<Journey>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        journeys.forEachIndexed { i, journey ->
            val label = buildString {
                append("${journey.durationMinutes} min")
                if (journey.hasTransfers) append(" · ${journey.legs.size}t")
            }
            FilterChip(
                selected = i == selectedIndex,
                onClick = { onSelect(i) },
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationPickerSheet(
    title: String,
    stops: List<Stop>,
    onPick: (Stop) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, stops) {
        if (query.isBlank()) stops
        else {
            val nq = query.stripDiacritics()
            stops.filter { it.name.stripDiacritics().contains(nq, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        val spacing = LocalSpacing.current
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.planner_pick_with_title, title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = spacing.lg, bottom = spacing.xs),
            )
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                placeholder = { Text(stringResource(R.string.planner_search_station)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            LazyColumn(
                modifier = Modifier.height(420.dp),
                contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(filtered, key = { it.id }) { stop ->
                    Surface(
                        onClick = { onPick(stop) },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.md, vertical = spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text = stop.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatHumanDate(date: LocalDate): String {
    val m = date.monthNumber.toString().padStart(2, '0')
    val d = date.dayOfMonth.toString().padStart(2, '0')
    return "$d/$m"
}
