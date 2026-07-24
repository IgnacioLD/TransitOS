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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TopAppBar
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
import app.transitos.core.ui.EmptyState
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
    onOpenMap: () -> Unit = {},
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
        onOpenMap = onOpenMap,
        modifier = modifier,
    )
}

@Composable
fun PrefilledPlannerRoute(
    originStopId: String,
    destinationStopId: String,
    modifier: Modifier = Modifier,
    viewModel: PlannerViewModel = koinViewModel(),
    onOpenMap: () -> Unit = {},
) {
    LaunchedEffect(originStopId, destinationStopId) {
        viewModel.prefillRoute(originStopId, destinationStopId)
    }
    PlannerRoute(modifier = modifier, viewModel = viewModel, onOpenMap = onOpenMap)
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
    onOpenMap: () -> Unit = {},
) {
    val spacing = LocalSpacing.current
    var picking by rememberSaveable { mutableStateOf<PickingTarget?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val timeZone = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.todayIn(timeZone) }
    val tomorrow = remember { today.plus(DatePeriod(days = 1)) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.planner_title)) },
                actions = {
                    IconButton(onClick = onOpenMap) {
                        Icon(Icons.Outlined.Map, contentDescription = stringResource(R.string.planner_view_network_map))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = spacing.screenGutter,
                end = spacing.screenGutter,
                bottom = spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item { SearchBarEndpoints(
                origin = state.origin,
                destination = state.destination,
                onPickOrigin = { picking = PickingTarget.ORIGIN },
                onPickDestination = { picking = PickingTarget.DESTINATION },
                onSwap = onSwap,
            ) }

            item { TimeOptionsSection(
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
            ) }

            if (state.canPlan) {
                item {
                    Button(
                        onClick = onSearch,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(spacing.sm))
                        Text(stringResource(R.string.planner_search_route))
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

                !state.canPlan -> item { QuickPicksSection(stops = stops, onPick = onOriginSelected) }

                state.errorMessage != null -> item {
                    EmptyState(
                        icon = Icons.Outlined.Search,
                        title = stringResource(R.string.planner_error_title),
                        subtitle = state.errorMessage,
                    )
                }

                state.hasSearched -> item {
                    EmptyState(
                        icon = Icons.Outlined.Search,
                        title = stringResource(R.string.planner_no_route_title),
                        subtitle = stringResource(R.string.planner_no_route_subtitle),
                    )
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
private fun SearchBarEndpoints(
    origin: Stop?,
    destination: Stop?,
    onPickOrigin: () -> Unit,
    onPickDestination: () -> Unit,
    onSwap: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Box {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                SearchEndpointRow(
                    icon = "●",
                    iconColor = MaterialTheme.colorScheme.primary,
                    text = origin?.name ?: stringResource(R.string.planner_pick_station),
                    isPlaceholder = origin == null,
                    onClick = onPickOrigin,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = spacing.lg + 20.dp),
                )
                SearchEndpointRow(
                    icon = "●",
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    text = destination?.name ?: stringResource(R.string.planner_pick_station),
                    isPlaceholder = destination == null,
                    onClick = onPickDestination,
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = spacing.sm),
        ) {
            IconButton(
                onClick = onSwap,
                modifier = Modifier.size(36.dp),
            ) {
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
private fun SearchEndpointRow(
    icon: String,
    iconColor: Color,
    text: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = icon,
                color = iconColor,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPlaceholder) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TimeOptionsSection(
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

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        RadioButtonRow(
            selected = leaveNow,
            onClick = {
                onTimeModeSelected(TimeMode.DEPARTURE)
                onTravelTimeSelected(null)
            },
            label = stringResource(R.string.planner_leave_now),
        )
        RadioButtonRow(
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
                    .padding(start = spacing.xl),
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
private fun RadioButtonRow(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
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
private fun QuickPicksSection(
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
                journey.firstDeparture?.let { append(" · $it") }
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
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f), height = 24.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 18.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.9f), height = 18.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 56.dp)
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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            JourneySummaryRow(journey = journey)

            journey.legs.forEachIndexed { i, leg ->
                if (i > 0) {
                    val transferStation = journey.legs[i - 1].destinationName
                    TransferIndicator(stationName = transferStation)
                }
                JourneyLegSection(leg = leg, index = i, isLast = i == journey.legs.lastIndex)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                HighlightBox(
                    label = stringResource(R.string.planner_first_departure),
                    value = journey.firstDeparture ?: "—",
                    modifier = Modifier.weight(1f),
                )
                HighlightBox(
                    label = stringResource(R.string.planner_last_departure),
                    value = journey.lastDeparture ?: "—",
                    modifier = Modifier.weight(1f),
                )
                HighlightBox(
                    label = stringResource(R.string.planner_departures),
                    value = journey.legs.firstOrNull()?.departures?.count()?.toString() ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = stringResource(R.string.planner_full_schedule),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            journey.legs.firstOrNull()?.let { leg -> DepartureSchedule(departures = leg.departures) }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
private fun JourneySummaryRow(journey: Journey) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${journey.durationMinutes} min",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (journey.hasTransfers) {
                Text(
                    text = "${journey.legs.size} ${stringResource(R.string.planner_legs_unit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (journey.distanceMeters > 0) {
            StatChip(value = "${"%.1f".format(journey.distanceMeters / 1000.0)} km", label = stringResource(R.string.planner_distance))
        }
        journey.fareZone?.let {
            StatChip(value = stringResource(R.string.planner_fare_zone, it), label = stringResource(R.string.planner_fare))
        }
        val carbonKg = journey.carbonKg
        if (carbonKg != null && carbonKg > 0.0) {
            StatChip(value = "${"%.1f".format(carbonKg)} kg", label = stringResource(R.string.planner_co2))
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = LocalSpacing.current.sm, vertical = LocalSpacing.current.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun JourneyLegSection(leg: JourneyLeg, index: Int, isLast: Boolean = false) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (leg.lineNames.isNotEmpty()) {
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
            }
            Text(
                text = "${leg.originName} → ${leg.destinationName}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (leg.headsigns.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.planner_direction, leg.headsigns.joinToString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.planner_departures_count, leg.departures.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            leg.departures.firstOrNull()?.let { first ->
                Text(
                    text = stringResource(R.string.planner_first_train, first),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun TransferIndicator(stationName: String) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.SwapVert,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = stringResource(R.string.planner_transfer_at, stationName),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun HighlightBox(label: String, value: String, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DepartureSchedule(departures: List<String>) {
    val spacing = LocalSpacing.current
    val byHour = departures.groupBy { it.substringBefore(':') }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        byHour.keys.sorted().forEach { hour ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${hour}h",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(44.dp),
                )
                Text(
                    text = byHour[hour]!!.joinToString("    "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
                    .padding(horizontal = spacing.lg, vertical = spacing.md),
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
