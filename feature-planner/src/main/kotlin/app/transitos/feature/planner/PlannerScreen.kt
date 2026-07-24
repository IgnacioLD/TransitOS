package app.transitos.feature.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transitos.core.design.theme.LocalSpacing
import app.transitos.core.model.Journey
import app.transitos.core.model.JourneyLeg
import app.transitos.core.model.Stop
import app.transitos.core.ui.R as coreUiR
import app.transitos.core.ui.SkeletonBlock
import app.transitos.feature.planner.R
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

    val timeZone = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.todayIn(timeZone) }
    val tomorrow = remember { today.plus(DatePeriod(days = 1)) }

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = spacing.screenGutter,
                end = spacing.screenGutter,
                top = spacing.xl,
                bottom = spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            item {
                Text(
                    text = stringResource(R.string.planner_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                EndpointFields(
                    origin = state.origin,
                    destination = state.destination,
                    onPickOrigin = { picking = PickingTarget.ORIGIN },
                    onPickDestination = { picking = PickingTarget.DESTINATION },
                    onSwap = onSwap,
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
                        onClick = onSearch,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = spacing.md),
                    ) {
                        Text(
                            stringResource(R.string.planner_search_route),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            when {
                state.isPlanning -> item { PlanningSkeleton() }

                state.journeys.isNotEmpty() -> {
                    if (state.journeys.size > 1) {
                        item {
                            JourneyAlternatives(
                                journeys = state.journeys,
                                selectedIndex = state.selectedJourneyIndex,
                                onSelect = onSelectJourney,
                            )
                        }
                    }
                    item {
                        JourneyResultCard(
                            journey = state.selectedJourney!!,
                            isSaved = state.isSaved,
                            onSaveRoute = onSaveRoute,
                            onRemoveRoute = onRemoveRoute,
                        )
                    }
                }

                !state.canPlan -> item {
                    QuickPicks(stops = stops, onPick = onOriginSelected)
                }
            }
        }
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
) {
    val spacing = LocalSpacing.current
    Box {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            EndpointField(
                label = stringResource(R.string.planner_origin),
                stopName = origin?.name,
                onClick = onPickOrigin,
            )
            EndpointField(
                label = stringResource(R.string.planner_destination),
                stopName = destination?.name,
                onClick = onPickDestination,
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = spacing.sm),
        ) {
            IconButton(onClick = onSwap, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = stringResource(R.string.planner_swap_cd),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun EndpointField(
    label: String,
    stopName: String?,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stopName ?: stringResource(R.string.planner_pick_station),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (stopName == null) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
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
        RadioRow(
            selected = leaveNow,
            onClick = {
                onTimeModeSelected(TimeMode.DEPARTURE)
                onTravelTimeSelected(null)
            },
            label = stringResource(R.string.planner_leave_now),
        )
        RadioRow(
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
            label = stringResource(R.string.planner_arrive_by),
        )

        AnimatedVisibility(visible = !leaveNow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.xl, top = spacing.xs),
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
private fun RadioRow(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = LocalSpacing.current.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

@Composable
private fun PlanningSkeleton() {
    val spacing = LocalSpacing.current
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.5f), height = 28.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 18.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.8f), height = 18.dp)
        }
    }
}

@Composable
private fun JourneyResultCard(
    journey: Journey,
    isSaved: Boolean = false,
    onSaveRoute: () -> Unit = {},
    onRemoveRoute: () -> Unit = {},
) {
    val spacing = LocalSpacing.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            JourneyHeader(journey = journey)

            JourneyTimeline(journey = journey)

            TextButton(
                onClick = if (isSaved) onRemoveRoute else onSaveRoute,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(spacing.sm))
                Text(
                    if (isSaved) stringResource(R.string.planner_route_saved)
                    else stringResource(R.string.planner_save_route)
                )
            }
        }
    }
}

@Composable
private fun JourneyHeader(journey: Journey) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = "${journey.durationMinutes} min",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (journey.hasTransfers) {
                Text(
                    text = stringResource(R.string.planner_transfers_count, journey.legs.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }

        val stats = buildList {
            if (journey.distanceMeters > 0) {
                add("${"%.1f".format(journey.distanceMeters / 1000.0)} km")
            }
            journey.fareZone?.let { add(stringResource(R.string.planner_fare_zone, it)) }
            val carbonKg = journey.carbonKg
            if (carbonKg != null && carbonKg > 0.0) {
                add("${"%.1f".format(carbonKg)} kg CO₂")
            }
        }
        if (stats.isNotEmpty()) {
            Text(
                text = stats.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun JourneyTimeline(journey: Journey) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        journey.legs.forEachIndexed { i, leg ->
            LegStationRow(leg = leg, isFirst = i == 0)
            if (i < journey.legs.lastIndex) {
                LegConnector(
                    transferName = leg.destinationName,
                    nextLeg = journey.legs[i + 1],
                )
            }
        }
        val lastLeg = journey.legs.last()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = lastLeg.destinationName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LegStationRow(leg: JourneyLeg, isFirst: Boolean) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = if (isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = leg.originName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (leg.lineNames.isNotEmpty() || leg.headsigns.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leg.lineNames.forEach { lineName ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = lineName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (leg.headsigns.isNotEmpty()) {
                        Text(
                            text = leg.headsigns.joinToString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        leg.departures.firstOrNull()?.let { firstDep ->
            Text(
                text = firstDep,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LegConnector(
    transferName: String,
    nextLeg: JourneyLeg,
) {
    val spacing = LocalSpacing.current
    Column(modifier = Modifier.padding(start = 10.dp)) {
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 20.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.planner_transfer_at, transferName),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.tertiary,
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
        else stops.filter { it.name.contains(query, ignoreCase = true) }
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
