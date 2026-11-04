package com.udistrital.dominocaos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udistrital.dominocaos.data.repository.HistoryRepository
import com.udistrital.dominocaos.model.MatchRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Historial de partidas del usuario. */
class HistoryViewModel(
    private val repository: HistoryRepository = HistoryRepository()
) : ViewModel() {

    private val _records = MutableStateFlow<List<MatchRecord>>(emptyList())
    val records: StateFlow<List<MatchRecord>> = _records.asStateFlow()

    private var job: Job? = null

    fun load(uid: String) {
        job?.cancel()
        job = viewModelScope.launch {
            repository.observe(uid)
                .catch { _records.value = emptyList() }
                .collect { _records.value = it }
        }
    }
}
