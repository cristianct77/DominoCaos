package com.udistrital.dominocaos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udistrital.dominocaos.data.repository.GameRepository
import com.udistrital.dominocaos.logic.GameEngine
import com.udistrital.dominocaos.logic.MoveError
import com.udistrital.dominocaos.logic.MoveResult
import com.udistrital.dominocaos.logic.RoomCodeGenerator
import com.udistrital.dominocaos.model.GameState
import com.udistrital.dominocaos.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del menú principal, crear partida y unirse a partida. */
data class LobbyUiState(
    val maxPlayers: Int = 2,
    val maxRounds: Int = 10,
    val joinCode: String = "",
    val loading: Boolean = false,
    val error: MoveError? = null,
    val invalidCode: Boolean = false,
    /** Código de la sala a la que se acaba de entrar (dispara la navegación). */
    val enteredRoom: String? = null
)

class LobbyViewModel(
    private val repository: GameRepository = GameRepository(),
    private val engine: GameEngine = GameEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    private val _openRooms = MutableStateFlow<List<GameState>>(emptyList())

    /** Partidas disponibles en tiempo real. */
    val openRooms: StateFlow<List<GameState>> = _openRooms.asStateFlow()

    private var roomsJob: Job? = null

    /** Se llama después del login (las reglas de Firebase exigen sesión). */
    fun startObservingRooms() {
        roomsJob?.cancel()
        roomsJob = viewModelScope.launch {
            repository.observeOpenRooms()
                .catch { _openRooms.value = emptyList() }
                .collect { _openRooms.value = it }
        }
    }

    fun onPlayersChange(value: Int) = _uiState.update { it.copy(maxPlayers = value) }
    fun onRoundsChange(value: Int) = _uiState.update { it.copy(maxRounds = value) }
    fun onCodeChange(value: String) = _uiState.update {
        it.copy(joinCode = RoomCodeGenerator.normalize(value).take(RoomCodeGenerator.LENGTH), invalidCode = false, error = null)
    }

    fun createRoom(user: UserProfile) {
        val s = _uiState.value
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.createRoom { code ->
                engine.createRoom(code, user.uid, user.name, s.maxPlayers, s.maxRounds, System.currentTimeMillis())
            }.onSuccess { room ->
                _uiState.update { it.copy(loading = false, enteredRoom = room.code) }
            }.onFailure {
                _uiState.update { it.copy(loading = false, error = MoveError.GAME_NOT_ACTIVE) }
            }
        }
    }

    fun joinRoom(user: UserProfile, code: String = _uiState.value.joinCode) {
        val normalized = RoomCodeGenerator.normalize(code)
        if (!RoomCodeGenerator.isValid(normalized)) {
            _uiState.update { it.copy(invalidCode = true) }
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.update(normalized) { engine.addPlayer(it, user.uid, user.name) }) {
                is MoveResult.Ok -> _uiState.update { it.copy(loading = false, enteredRoom = normalized, joinCode = "") }
                is MoveResult.Invalid -> _uiState.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    /** La vista avisa que ya navegó a la sala. */
    fun onRoomEntered() = _uiState.update { it.copy(enteredRoom = null) }

    fun clearError() = _uiState.update { it.copy(error = null, invalidCode = false) }
}
