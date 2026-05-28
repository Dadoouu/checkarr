package com.checkarr.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.checkarr.data.models.Instance
import com.checkarr.data.models.Movie
import com.checkarr.data.models.SeriesCalendarItem
import com.checkarr.navigation.Screen
import com.checkarr.ui.shared.EmptyState
import com.checkarr.ui.shared.LoadingOverlay
import com.checkarr.ui.shared.NoInstanceConfigured
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    radarrInstance: Instance?,
    sonarrInstance: Instance?,
    navController: NavController,
    viewModel: CalendarViewModel = viewModel()
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val calendarDays by viewModel.calendarDays.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load(radarrInstance, sonarrInstance)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " ${selectedMonth.year}")
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.previousMonth(radarrInstance, sonarrInstance) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.nextMonth(radarrInstance, sonarrInstance) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                radarrInstance == null && sonarrInstance == null -> NoInstanceConfigured("Radarr or Sonarr") {
                    navController.navigate(Screen.Settings.route)
                }
                isLoading -> LoadingOverlay()
                else -> CalendarGrid(
                    month = selectedMonth,
                    calendarDays = calendarDays,
                    navController = navController
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: LocalDate,
    calendarDays: Map<LocalDate, CalendarDay>,
    navController: NavController
) {
    val today = LocalDate.now()
    val firstDayOfMonth = month.withDayOfMonth(1)
    val daysInMonth = month.lengthOfMonth()
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value % 7)

    val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val allDays = buildList {
        repeat(firstDayOfWeek) { add(null) }
        for (day in 1..daysInMonth) {
            add(month.withDayOfMonth(day))
        }
        while (size % 7 != 0) add(null)
    }

    val weeks = allDays.chunked(7)

    val datesWithContent = calendarDays.keys.filter { it.month == month.month }
    val selectedDate = remember(month) { mutableStateOf<LocalDate?>(datesWithContent.firstOrNull() ?: today.takeIf { it.month == month.month }) }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayHeaders.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    CalendarCell(
                        date = date,
                        isToday = date == today,
                        isSelected = date == selectedDate.value,
                        hasContent = date != null && calendarDays.containsKey(date),
                        modifier = Modifier.weight(1f),
                        onClick = { if (date != null) selectedDate.value = date }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        selectedDate.value?.let { date ->
            val day = calendarDays[date]
            if (day != null) {
                LazyColumn {
                    item {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(day.movies) { movie ->
                        CalendarMovieRow(movie = movie, onClick = {
                            navController.navigate(Screen.MovieDetail.createRoute(movie.id))
                        })
                    }
                    items(day.episodes) { episode ->
                        CalendarEpisodeRow(episode = episode, onClick = {
                            episode.series?.let { s ->
                                navController.navigate(Screen.SeriesDetail.createRoute(s.id))
                            }
                        })
                    }
                }
            } else {
                EmptyState(
                    icon = Icons.Default.EventAvailable,
                    title = "No Releases",
                    subtitle = "Nothing scheduled for this day."
                )
            }
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate?,
    isToday: Boolean,
    isSelected: Boolean,
    hasContent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (hasContent) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMovieRow(movie: Movie, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(movie.title) },
        supportingContent = {
            val label = when {
                movie.inCinemas != null -> "In Cinemas"
                movie.digitalRelease != null -> "Digital"
                movie.physicalRelease != null -> "Physical"
                else -> ""
            }
            Text(label)
        },
        leadingContent = {
            movie.remotePoster?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(40.dp).height(60.dp).clip(RoundedCornerShape(4.dp))
                )
            } ?: Icon(Icons.Default.Movie, contentDescription = null)
        },
        trailingContent = {
            if (movie.hasFile) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun CalendarEpisodeRow(episode: SeriesCalendarItem, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text("${episode.series?.title ?: "Unknown"} · S%02dE%02d".format(episode.seasonNumber, episode.episodeNumber)) },
        supportingContent = { Text(episode.title) },
        leadingContent = {
            episode.series?.remotePoster?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(40.dp).height(60.dp).clip(RoundedCornerShape(4.dp))
                )
            } ?: Icon(Icons.Default.Tv, contentDescription = null)
        },
        trailingContent = {
            if (episode.hasFile) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
