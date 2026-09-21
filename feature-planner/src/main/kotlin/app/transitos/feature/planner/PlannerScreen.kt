package com.glossostudio.transitos.feature.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.design.theme.PillShape
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.JourneyLeg
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.ui.LineBadge
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
    var swapSpin by rememberSaveable { mutableIntStateOf(0) }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        // The host Scaffold already insets content for the system bars and the
        // bottom navigation, so this nested Scaffold must not re-apply them.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = spacing.screenGutter,
                end = spacing.screenGutter,
                top = spacing.lg,
                bottom = spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = stringResource(R.string.planner_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.planner_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !state.isPlanning,
                        shape = PillShape,
                        contentPadding = PaddingValues(vertical = spacing.md),
                    ) {
                        if (state.isPlanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(spacing.sm))
                            Text(
                                stringResource(R.string.planner_search_route),
                                style = MaterialTheme.typography.titleSmall,
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
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(end = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                EndpointRow(
                    label = stringResource(R.string.planner_from),
                    value = origin?.name,
                    dotColor = MaterialTheme.colorScheme.primary,
                    onClick = onPickOrigin,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )
                EndpointRow(
                    label = stringResource(R.string.planner_to),
                    value = destination?.name,
                    dotColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onPickDestination,
                )
            }
            FilledTonalIconButton(
                onClick = onSwap,
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = stringResource(R.string.planner_swap_cd),
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(swapRotation),
                )
            }
        }
    }
}

@Composable
private fun EndpointRow(
    label: String,
    value: String?,
    dotColor: Color,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Surface(
        onClick = onClick,
        color = Color.Transparent,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value ?: stringResource(R.string.planner_pick_station),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (value != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (value == null) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
            fontWeight = FontWeight.SemiBold,
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xl)
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = journey.durationMinutes.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(coreUiR.string.unit_minutes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 3.dp, bottom = 6.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                journey.departureTime?.let { dep ->
                    Text(
                        text = stringResource(R.string.planner_departs_at, dep),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                journey.arrivalTime?.let { arr ->
                    Text(
                        text = stringResource(R.string.planner_arrives_at, arr),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        FilledTonalIconButton(
            onClick = if (isSaved) onRemove else onSave,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isSaved) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (isSaved) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isSaved) stringResource(R.string.planner_route_saved)
                else stringResource(R.string.planner_save_route),
            )
        }
    }
}

private sealed interface TimelineNode {
    val time: String?

    data class Station(
        override val time: String?,
        val name: String,
        val kind: StationKind,
    ) : TimelineNode

    data class Ride(
        override val time: String?,
        val lines: List<String>,
        val lineColors: List<Long>,
        val headsigns: List<String>,
        val railColor: Color,
    ) : TimelineNode

    data class Wait(
        override val time: String?,
        val minutes: Int,
    ) : TimelineNode
}

private enum class StationKind { ORIGIN, TRANSFER, DESTINATION }

/** Flattens a journey into the ordered nodes the timeline rail renders. */
private fun buildTimelineNodes(journey: Journey): List<TimelineNode> = buildList {
    journey.legs.forEachIndexed { index, leg ->
        if (index == 0) {
            add(TimelineNode.Station(leg.departureTime, leg.originName, StationKind.ORIGIN))
        } else {
            val previous = journey.legs[index - 1]
            add(TimelineNode.Station(previous.arrivalTime, leg.originName, StationKind.TRANSFER))
            leg.waitMinutes?.let { add(TimelineNode.Wait(time = null, minutes = it)) }
        }
        add(
            TimelineNode.Ride(
                time = if (index > 0) leg.departureTime else null,
                lines = leg.lineNames,
                lineColors = leg.lineColors,
                headsigns = leg.headsigns,
                railColor = leg.lineColors.firstOrNull()?.let(::Color) ?: Color.Unspecified,
            ),
        )
    }
    val last = journey.legs.last()
    add(TimelineNode.Station(last.arrivalTime, last.destinationName, StationKind.DESTINATION))
}

@Composable
private fun SheetTimeline(journey: Journey) {
    val nodes = remember(journey) { buildTimelineNodes(journey) }
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outline
    val neutral = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface

    // One rail colour per node; rides carry their line colour, everything else
    // (stations, waits) stays on the neutral track.
    val railColors: List<Color> = nodes.map { node ->
        when (node) {
            is TimelineNode.Ride -> if (node.railColor == Color.Unspecified) primary else node.railColor
            else -> neutral
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        nodes.forEachIndexed { index, node ->
            val isFirst = index == 0
            val isLast = index == nodes.lastIndex
            val previousIsRide = index > 0 && nodes[index - 1] is TimelineNode.Ride
            val nextIsRide = index < nodes.lastIndex && nodes[index + 1] is TimelineNode.Ride

            when (node) {
                is TimelineNode.Ride -> TimelineRow(
                    time = node.time,
                    timeColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    topRail = railColors[index],
                    bottomRail = railColors[index],
                    dotColor = null,
                    ringColor = surface,
                ) {
                    RideContent(node)
                }

                is TimelineNode.Wait -> TimelineRow(
                    time = null,
                    timeColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    topRail = neutral,
                    bottomRail = if (nextIsRide) railColors[index + 1] else neutral,
                    dotColor = null,
                    ringColor = surface,
                ) {
                    WaitContent(node.minutes)
                }

                is TimelineNode.Station -> {
                    val dot = when (node.kind) {
                        StationKind.ORIGIN -> primary
                        StationKind.TRANSFER -> outline
                        StationKind.DESTINATION -> tertiary
                    }
                    TimelineRow(
                        time = node.time,
                        timeColor = MaterialTheme.colorScheme.onSurface,
                        topRail = when {
                            isFirst -> null
                            previousIsRide -> railColors[index - 1]
                            else -> neutral
                        },
                        bottomRail = when {
                            isLast -> null
                            nextIsRide -> railColors[index + 1]
                            else -> neutral
                        },
                        dotColor = dot,
                        ringColor = surface,
                    ) {
                        StationContent(node.name, node.kind)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(
    time: String?,
    timeColor: Color,
    topRail: Color?,
    bottomRail: Color?,
    dotColor: Color?,
    ringColor: Color,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = time ?: "",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = timeColor,
            modifier = Modifier.width(52.dp),
        )
        Rail(
            topRail = topRail,
            bottomRail = bottomRail,
            dotColor = dotColor,
            ringColor = ringColor,
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, top = 9.dp, bottom = 9.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun Rail(
    topRail: Color?,
    bottomRail: Color?,
    dotColor: Color?,
    ringColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val stroke = 4.dp.toPx()
        topRail?.let {
            drawLine(
                color = it,
                start = Offset(cx, 0f),
                end = Offset(cx, cy),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        bottomRail?.let {
            drawLine(
                color = it,
                start = Offset(cx, cy),
                end = Offset(cx, size.height),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        if (dotColor != null) {
            drawCircle(color = ringColor, radius = 8.dp.toPx(), center = Offset(cx, cy))
            drawCircle(color = dotColor, radius = 5.5.dp.toPx(), center = Offset(cx, cy))
        }
    }
}

@Composable
private fun RideContent(node: TimelineNode.Ride) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        node.lines.forEachIndexed { index, lineName ->
            LineBadge(
                label = lineName,
                colorArgb = node.lineColors.getOrNull(index),
                size = 32.dp,
            )
        }
        if (node.headsigns.isNotEmpty()) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = node.headsigns.joinToString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StationContent(name: String, kind: StationKind) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (kind == StationKind.DESTINATION) FontWeight.SemiBold else FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun WaitContent(minutes: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(15.dp),
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
        add(transfersLabel(journey))
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stats.joinToString("  ·  "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun transfersLabel(journey: Journey): String =
    if (journey.hasTransfers) {
        pluralStringResource(
            R.plurals.planner_transfers_count,
            journey.legs.size - 1,
            journey.legs.size - 1,
        )
    } else {
        stringResource(R.string.planner_no_transfers)
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
        journeys.forEachIndexed { index, journey ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                shape = MaterialTheme.shapes.large,
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                border = if (selected) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val onCard = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = journey.durationMinutes.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = onCard,
                        )
                        Text(
                            text = stringResource(coreUiR.string.unit_minutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = onCard.copy(alpha = 0.75f),
                            modifier = Modifier.padding(start = 3.dp, bottom = 3.dp),
                        )
                    }
                    Text(
                        text = transfersLabel(journey),
                        style = MaterialTheme.typography.labelSmall,
                        color = onCard.copy(alpha = 0.75f),
                        maxLines = 1,
                    )
                }
            }
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val spacing = LocalSpacing.current
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.planner_pick_with_title, title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, bottom = spacing.sm),
            )
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.xs),
                placeholder = { Text(stringResource(R.string.planner_search_station)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = PillShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stop.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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
