package app.transitos.feature.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transitos.core.design.theme.LocalSpacing
import app.transitos.core.model.Journey
import app.transitos.core.model.JourneyLeg
import app.transitos.core.model.Stop
import app.transitos.core.ui.EmptyState
import app.transitos.core.ui.SkeletonBlock
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
        onSwap = viewModel::swapEndpoints,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlannerScreen(
    state: PlannerUiState,
    stops: List<Stop>,
    onOriginSelected: (Stop) -> Unit,
    onDestinationSelected: (Stop) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    var picking by rememberSaveable { mutableStateOf<PickingTarget?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

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
                bottom = spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item {
                Text(
                    text = "Planificar viaje",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = spacing.md, bottom = spacing.xs),
                )
            }

            item {
                EndpointSelector(
                    origin = state.origin,
                    destination = state.destination,
                    onPickOrigin = { picking = PickingTarget.ORIGIN },
                    onPickDestination = { picking = PickingTarget.DESTINATION },
                    onSwap = onSwap,
                )
            }

            item {
                DateChipRow(
                    selected = state.date,
                    today = today,
                    tomorrow = tomorrow,
                    onSelect = onDateSelected,
                    onPickCustom = { showDatePicker = true },
                )
            }

            when {
                !state.canPlan -> item { HintCard() }
                state.isPlanning -> item { PlanningSkeleton() }
                state.journey != null -> item { JourneyResultCard(journey = state.journey!!) }
                state.errorMessage != null -> item {
                    EmptyState(
                        icon = Icons.Outlined.Search,
                        title = "No se pudo planificar",
                        subtitle = state.errorMessage,
                    )
                }
                else -> item {
                    EmptyState(
                        icon = Icons.Outlined.Search,
                        title = "Sin ruta encontrada",
                        subtitle = "No hay trenes entre estas estaciones en la fecha seleccionada.",
                    )
                }
            }
        }
    }

    val target = picking
    if (target != null) {
        StationPickerSheet(
            title = if (target == PickingTarget.ORIGIN) "Origen" else "Destino",
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
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private enum class PickingTarget { ORIGIN, DESTINATION }

@Composable
private fun EndpointSelector(
    origin: Stop?,
    destination: Stop?,
    onPickOrigin: () -> Unit,
    onPickDestination: () -> Unit,
    onSwap: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Box(contentAlignment = Alignment.CenterEnd) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                EndpointRow(label = "Desde", stop = origin, onClick = onPickOrigin)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                EndpointRow(label = "Hasta", stop = destination, onClick = onPickDestination)
            }
            IconButton(
                onClick = onSwap,
                modifier = Modifier.padding(end = spacing.md),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = "Intercambiar origen y destino",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EndpointRow(label: String, stop: Stop?, onClick: () -> Unit) {
    val spacing = LocalSpacing.current
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stop?.name ?: "Elige estación",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (stop == null) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun DateChipRow(
    selected: LocalDate,
    today: LocalDate,
    tomorrow: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPickCustom: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        FilterChip(
            selected = selected == today,
            onClick = { onSelect(today) },
            label = { Text("Hoy") },
        )
        FilterChip(
            selected = selected == tomorrow,
            onClick = { onSelect(tomorrow) },
            label = { Text("Mañana") },
        )
        FilterChip(
            selected = selected != today && selected != tomorrow,
            onClick = onPickCustom,
            label = {
                Text(
                    if (selected != today && selected != tomorrow) formatHumanDate(selected)
                    else "Elegir fecha",
                )
            },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
        )
    }
}

@Composable
private fun HintCard() {
    val spacing = LocalSpacing.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Text(
            text = "Elige origen y destino para ver horarios, primer y último tren.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(spacing.lg),
        )
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
private fun JourneyResultCard(journey: Journey) {
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
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                JourneyLegSection(leg = leg, index = i)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                HighlightBox(
                    label = "Primero",
                    value = journey.firstDeparture ?: "—",
                    modifier = Modifier.weight(1f),
                )
                HighlightBox(
                    label = "Último",
                    value = journey.lastDeparture ?: "—",
                    modifier = Modifier.weight(1f),
                )
                HighlightBox(
                    label = "Salidas",
                    value = journey.legs.firstOrNull()?.departures?.count()?.toString() ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Horario completo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            journey.legs.firstOrNull()?.let { leg -> DepartureSchedule(departures = leg.departures) }
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
                    text = "${journey.legs.size} tramos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (journey.distanceMeters > 0) {
            StatChip(value = "${"%.1f".format(journey.distanceMeters / 1000.0)} km", label = "Distancia")
        }
        journey.fareZone?.let {
            StatChip(value = "Zona $it", label = "Tarifa")
        }
        val carbonKg = journey.carbonKg
        if (carbonKg != null && carbonKg > 0.0) {
            StatChip(value = "${"%.1f".format(carbonKg)} kg", label = "CO₂")
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
private fun JourneyLegSection(leg: JourneyLeg, index: Int) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = "${leg.originName} → ${leg.destinationName}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${leg.departures.size} salidas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (leg.headsigns.isNotEmpty()) {
            Text(
                text = "Dirección: ${leg.headsigns.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
                text = "Elige $title",
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
                placeholder = { Text("Buscar estación") },
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
