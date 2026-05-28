package com.checkarr.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.checkarr.data.models.Instance
import com.checkarr.data.models.Movie
import com.checkarr.navigation.Screen
import com.checkarr.ui.shared.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: Int,
    instance: Instance?,
    navController: NavController,
    viewModel: MoviesViewModel = viewModel()
) {
    val movies by viewModel.movies.collectAsState()
    val toast by viewModel.toast.collectAsState()
    val error by viewModel.error.collectAsState()

    val movie = movies.firstOrNull { it.id == movieId }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteFiles by remember { mutableStateOf(false) }

    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(movie?.title ?: "Movie") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    movie?.let { m ->
                        instance?.let { inst ->
                            IconButton(onClick = { viewModel.toggleMonitor(inst, m) }) {
                                Icon(
                                    if (m.monitored) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (m.monitored) "Unmonitor" else "Monitor",
                                    tint = if (m.monitored) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { viewModel.searchMovie(inst, m.id) }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("View Releases") },
                                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            navController.navigate(Screen.MovieReleases.createRoute(m.id))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Movie") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = { showMenu = false; showDeleteDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = {
            if (toast != null) {
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(toast!!) }
            }
        }
    ) { padding ->
        if (movie == null) {
            LoadingOverlay()
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                error?.let {
                    ErrorBanner(message = it, onDismiss = { viewModel.clearError() })
                }
                MovieHero(movie = movie)
                MovieInfoSection(movie = movie)
                if (movie.hasFile) {
                    MovieFileSection(movie = movie)
                }
                MovieRatingsSection(movie = movie)
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Movie") },
            text = {
                Column {
                    Text("Are you sure you want to delete \"${movie?.title}\"?")
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = deleteFiles, onCheckedChange = { deleteFiles = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Delete movie files")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        instance?.let { inst ->
                            movie?.let { m ->
                                viewModel.deleteMovie(inst, m.id, deleteFiles)
                            }
                        }
                        showDeleteDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MovieHero(movie: Movie) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        movie.remoteFanart?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier
                    .width(90.dp)
                    .height(135.dp)
            ) {
                movie.remotePoster?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${movie.year}${if (movie.studio != null) " · ${movie.studio}" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (movie.monitored) {
                        StatusBadge("Monitored", Color(0xFF22C55E))
                    } else {
                        StatusBadge("Unmonitored", Color(0xFF6C6C70))
                    }
                    if (movie.hasFile) {
                        StatusBadge("Downloaded", Color(0xFF3B82F6))
                    } else if (movie.isAvailable) {
                        StatusBadge("Missing", Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieInfoSection(movie: Movie) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (movie.overview.isNotBlank()) {
            Text(
                text = movie.overview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }

        if (movie.genres.isNotEmpty()) {
            Text(
                text = movie.genres.joinToString(", "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
        }

        SectionHeader("Details")
        HorizontalDivider()
        if (movie.runtime > 0) MetadataRow("Runtime", movie.runtimeFormatted)
        movie.certification?.let { MetadataRow("Certification", it) }
        if (movie.status.isNotBlank()) MetadataRow("Status", movie.status.replaceFirstChar { it.uppercase() })
        movie.inCinemas?.let { MetadataRow("In Cinemas", it.take(10)) }
        movie.digitalRelease?.let { MetadataRow("Digital Release", it.take(10)) }
        movie.physicalRelease?.let { MetadataRow("Physical Release", it.take(10)) }
        if (movie.sizeOnDisk > 0) MetadataRow("Size on Disk", movie.fileSizeFormatted)
        MetadataRow("Path", movie.path)
    }
}

@Composable
private fun MovieFileSection(movie: Movie) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader("File")
        HorizontalDivider()
        movie.movieFile?.let { file ->
            file.quality?.let { q -> MetadataRow("Quality", q.quality.name) }
            file.mediaInfo?.let { info ->
                info.videoCodec?.let { MetadataRow("Video Codec", it) }
                info.audioCodec?.let { MetadataRow("Audio Codec", it) }
                info.resolution?.let { MetadataRow("Resolution", it) }
                if (info.audioChannels > 0) MetadataRow("Audio Channels", info.audioChannels.toString())
            }
            MetadataRow("Path", file.relativePath)
        }
    }
}

@Composable
private fun MovieRatingsSection(movie: Movie) {
    val ratings = movie.ratings
    val hasRatings = ratings.imdb != null || ratings.tmdb != null || ratings.rottenTomatoes != null
    if (!hasRatings) return

    Column(modifier = Modifier.padding(16.dp)) {
        SectionHeader("Ratings")
        HorizontalDivider()
        ratings.imdb?.let {
            MetadataRow("IMDb", "%.1f / 10  (${it.votes} votes)".format(it.value))
        }
        ratings.tmdb?.let {
            MetadataRow("TMDb", "%.0f%%".format(it.value * 10))
        }
        ratings.rottenTomatoes?.let {
            MetadataRow("Rotten Tomatoes", "%.0f%%".format(it.value))
        }
    }
}
