package com.smtutt.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smtutt.app.data.model.TeacherSearchResult
import com.smtutt.app.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherSearchUiState(
    val query: String = "",
    val results: List<TeacherSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasSearched: Boolean = false
)

class TeacherSearchViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherSearchUiState())
    val uiState: StateFlow<TeacherSearchUiState> = _uiState.asStateFlow()

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery, errorMessage = null)
    }

    fun search() {
        val q = _uiState.value.query.trim()
        if (q.length < 2) {
            _uiState.value = _uiState.value.copy(errorMessage = "Введите не менее 2 букв фамилии")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, hasSearched = true)
            try {
                val teachers = repository.searchTeachers(q)
                _uiState.value = _uiState.value.copy(
                    results = teachers,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка поиска преподавателя"
                )
            }
        }
    }
}
