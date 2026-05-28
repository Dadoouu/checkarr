package com.checkarr.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkarr.data.models.*
import com.checkarr.data.network.ApiClient
import com.checkarr.data.repository.RadarrRepository
import com.checkarr.data.repository.SonarrRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CalendarDay(
    val date: LocalDate,
    val movies: List<Movie>,
    val episodes: List<SeriesCalendarItem>
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val radarrRepo = RadarrRepository(ApiClient())
    private val sonarrRepo = SonarrRepository(ApiClient())

    private val _selectedMonth = MutableStateFlow(LocalDate.now())
    private val _calendarMovies = MutableStateFlow<List<Movie>>(emptyList())
    private val _calendarEpisodes = MutableStateFlow<List<SeriesCalendarItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val selectedMonth: StateFlow<LocalDate> = _selectedMonth
    val isLoading: StateFlow<Boolean> = _isLoading
    val error: StateFlow<String?> = _error

    val calendarDays: StateFlow<Map<LocalDate, CalendarDay>> = combine(
        _calendarMovies, _calendarEpisodes
    ) { movies, episodes ->
        val map = mutableMapOf<LocalDate, CalendarDay>()
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val simpleFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        movies.forEach { movie ->
            val dateStr = movie.inCinemas ?: movie.digitalRelease ?: movie.physicalRelease
            if (dateStr != null) {
                runCatching {
                    val date = try {
                        LocalDate.parse(dateStr, formatter)
                    } catch (e: Exception) {
                        LocalDate.parse(dateStr.take(10), simpleFormatter)
                    }
                    val day = map.getOrPut(date) { CalendarDay(date, emptyList(), emptyList()) }
                    map[date] = day.copy(movies = day.movies + movie)
                }
            }
        }
        episodes.forEach { ep ->
            val dateStr = ep.airDate
            if (dateStr != null) {
                runCatching {
                    val date = LocalDate.parse(dateStr.take(10), simpleFormatter)
                    val day = map.getOrPut(date) { CalendarDay(date, emptyList(), emptyList()) }
                    map[date] = day.copy(episodes = day.episodes + ep)
                }
            }
        }
        map
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun load(radarrInstance: Instance?, sonarrInstance: Instance?) {
        val month = _selectedMonth.value
        val start = month.withDayOfMonth(1).toString()
        val end = month.withDayOfMonth(month.lengthOfMonth()).toString()
        viewModelScope.launch {
            _isLoading.value = true
            radarrInstance?.let { inst ->
                radarrRepo.getCalendar(inst, start, end).onSuccess { _calendarMovies.value = it }
            }
            sonarrInstance?.let { inst ->
                sonarrRepo.getCalendar(inst, start, end).onSuccess { _calendarEpisodes.value = it }
            }
            _isLoading.value = false
        }
    }

    fun previousMonth(radarrInstance: Instance?, sonarrInstance: Instance?) {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
        load(radarrInstance, sonarrInstance)
    }

    fun nextMonth(radarrInstance: Instance?, sonarrInstance: Instance?) {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
        load(radarrInstance, sonarrInstance)
    }

    fun clearError() { _error.value = null }
}
