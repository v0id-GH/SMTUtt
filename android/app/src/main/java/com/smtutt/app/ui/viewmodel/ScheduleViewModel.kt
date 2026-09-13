package com.smtutt.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smtutt.app.SmtuApplication
import com.smtutt.app.data.local.FavoriteEntity
import com.smtutt.app.data.model.ScheduleResponse
import com.smtutt.app.data.parser.WeekDetector
import com.smtutt.app.data.repository.Resource
import com.smtutt.app.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class WeekFilter(val title: String) {
    ALL("Все"),
    UPPER("Верхняя"),
    LOWER("Нижняя")
}

data class ScheduleUiState(
    val id: String = "",
    val title: String = "",
    val isTeacher: Boolean = false,
    val weekFilter: WeekFilter = WeekFilter.ALL,
    val detectedCurrentWeek: WeekFilter = WeekFilter.ALL,
    val selectedDayIndex: Int = 0,
    val scheduleResource: Resource<ScheduleResponse> = Resource.Loading,
    val isFavorite: Boolean = false,
    val isOfflineAvailable: Boolean = false,
    val isRefreshing: Boolean = false
)

class ScheduleViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = (application as SmtuApplication).repository
    val favorites: Flow<List<FavoriteEntity>> = repository.getFavorites()

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private var defaultOrLastFavoriteId: String = ""

    fun isViewingDefaultOrFavorite(): Boolean {
        val currentId = _uiState.value.id
        if (currentId.isBlank()) return true
        if (_uiState.value.isTeacher) return false
        if (defaultOrLastFavoriteId.isNotBlank() && currentId != defaultOrLastFavoriteId) {
            return false
        }
        return true
    }

    fun returnToDefaultOrLastFavorite() {
        viewModelScope.launch {
            val defaultFav = repository.getDefaultFavorite()
            if (defaultFav != null) {
                defaultOrLastFavoriteId = defaultFav.id
                if (_uiState.value.id != defaultFav.id || _uiState.value.isTeacher) {
                    loadSchedule(defaultFav.id, defaultFav.isTeacher)
                }
            } else {
                val allFavs = repository.getFavorites().firstOrNull() ?: emptyList()
                val target = allFavs.firstOrNull { !it.isTeacher } ?: allFavs.firstOrNull()
                if (target != null) {
                    defaultOrLastFavoriteId = target.id
                    if (_uiState.value.id != target.id || _uiState.value.isTeacher) {
                        loadSchedule(target.id, target.isTeacher)
                    }
                } else if (defaultOrLastFavoriteId.isNotBlank() && (_uiState.value.id != defaultOrLastFavoriteId || _uiState.value.isTeacher)) {
                    loadSchedule(defaultOrLastFavoriteId, isTeacher = false)
                }
            }
        }
    }

    init {
        val appContext = getApplication<Application>()
        val initialWeek = WeekDetector.getCurrentWeek(appContext)
        val initialDay = WeekDetector.getDefaultDayIndex()

        _uiState.value = _uiState.value.copy(
            weekFilter = initialWeek,
            detectedCurrentWeek = initialWeek,
            selectedDayIndex = initialDay
        )

        // 1. Sync current week in background from website
        viewModelScope.launch {
            val detected = repository.detectAndSyncWeek()
            if (detected != null) {
                WeekDetector.saveDetectedWeek(appContext, detected)
                val updatedWeek = WeekDetector.getCurrentWeek(appContext)
                _uiState.value = _uiState.value.copy(
                    weekFilter = updatedWeek,
                    detectedCurrentWeek = updatedWeek
                )
            }
        }

        // 2. Load default group with automatic background refresh if enabled in settings
        viewModelScope.launch {
            val defaultGroup = repository.getDefaultFavorite()
            val targetGroup = defaultGroup ?: run {
                val allFavs = repository.getFavorites().firstOrNull() ?: emptyList()
                allFavs.firstOrNull { !it.isTeacher } ?: allFavs.firstOrNull()
            }
            if (targetGroup != null) {
                val shouldAutoRefresh = ThemePreferences.isAutoRefreshEnabled(appContext)
                loadSchedule(
                    id = targetGroup.id,
                    isTeacher = targetGroup.isTeacher,
                    forceRefresh = false,
                    autoBackgroundUpdate = shouldAutoRefresh
                )
            }
        }
    }

    fun loadSchedule(
        id: String,
        isTeacher: Boolean,
        forceRefresh: Boolean = false,
        autoBackgroundUpdate: Boolean = false
    ) {
        if (id.isBlank()) return
        if (!isTeacher) {
            defaultOrLastFavoriteId = id
        }
        viewModelScope.launch {
            val isFav = repository.isFavorite(id)
            _uiState.value = _uiState.value.copy(
                id = id,
                isTeacher = isTeacher,
                isFavorite = isFav,
                isRefreshing = forceRefresh
            )

            var hadCachedData = false

            repository.getSchedule(id, isTeacher, forceRefresh).collect { resource ->
                val currentTitle = when (resource) {
                    is Resource.Success -> {
                        if (resource.isOffline) hadCachedData = true
                        resource.data.title
                    }
                    else -> _uiState.value.title
                }

                _uiState.value = _uiState.value.copy(
                    scheduleResource = resource,
                    title = currentTitle,
                    isOfflineAvailable = resource is Resource.Success && resource.isOffline,
                    isRefreshing = false
                )

                if (resource is Resource.Error && hadCachedData) {
                    _userMessage.value = "Не удалось обновить расписание (нет сети). Показана сохранённая версия."
                }
            }

            // If we loaded from cache and autoBackgroundUpdate is true, attempt background refresh
            if (autoBackgroundUpdate && hadCachedData) {
                try {
                    repository.getSchedule(id, isTeacher, forceRefresh = true).collect { resource ->
                        if (resource is Resource.Success && !resource.isOffline) {
                            _uiState.value = _uiState.value.copy(
                                scheduleResource = resource,
                                title = resource.data.title,
                                isOfflineAvailable = false
                            )
                        }
                    }
                } catch (e: Exception) {
                    _userMessage.value = "Не удалось обновить расписание (нет сети). Показана сохранённая версия."
                }
            }
        }
    }

    fun setWeekFilter(filter: WeekFilter) {
        _uiState.value = _uiState.value.copy(weekFilter = filter)
    }

    fun selectDay(index: Int) {
        _uiState.value = _uiState.value.copy(selectedDayIndex = index)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun toggleFavorite() {
        val currentState = _uiState.value
        val scheduleData = (currentState.scheduleResource as? Resource.Success)?.data
        val name = scheduleData?.title ?: currentState.title
        if (currentState.id.isNotBlank()) {
            viewModelScope.launch {
                repository.toggleFavorite(currentState.id, name, currentState.isTeacher)
                _uiState.value = currentState.copy(isFavorite = !currentState.isFavorite)
            }
        }
    }

    fun saveOffline() {
        val scheduleData = (_uiState.value.scheduleResource as? Resource.Success)?.data ?: return
        viewModelScope.launch {
            repository.saveScheduleOffline(scheduleData)
            _uiState.value = _uiState.value.copy(isFavorite = true, isOfflineAvailable = true)
            _userMessage.value = "Расписание сохранено для доступа офлайн"
        }
    }

    fun setDefaultGroup(groupId: String, groupName: String) {
        viewModelScope.launch {
            repository.setDefaultGroup(groupId, groupName)
            loadSchedule(groupId, isTeacher = false, forceRefresh = false, autoBackgroundUpdate = true)
        }
    }
}
