package com.udistrital.dominocaos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udistrital.dominocaos.data.repository.GameRepository
import com.udistrital.dominocaos.data.repository.HistoryRepository
import com.udistrital.dominocaos.logic.DominoRules
import com.udistrital.dominocaos.logic.GameEngine
import com.udistrital.dominocaos.logic.MoveError
import com.udistrital.dominocaos.logic.MoveResult
import com.udistrital.dominocaos.logic.PowerManager
import com.udistrital.dominocaos.model.ChaosEvent
import com.udistrital.dominocaos.model.Domino
import com.udistrital.dominocaos.model.GameState
import com.udistrital.dominocaos.model.GameStatus
import com.udistrital.dominocaos.model.MatchRecord
import com.udistrital.dominocaos.model.Player
import com.udistrital.dominocaos.model.PowerType
import com.udistrital.dominocaos.model.Side
import com.udistrital.dominocaos.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado que la pantalla de juego necesita para dibujarse. */
data class GameUiState(
    val game: GameState? = null,
    val myId: String = "",
    val selectedTile: Domino? = null,
    val error: MoveError? = null,
    /** Evento aleatorio pendiente de mostrar en el diálogo. */
    val pendingEvent: ChaosEvent? = null,
    /** La sala fue eliminada (el anfitrión canceló). */
    val roomClosed: Boolean = false
) {
    val me: Player? get() = game?.playerById(myId)
    val isHost: Boolean get() = game?.hostId == myId
    val isMyTurn: Boolean
        get() {
            val g = game ?: return false
            return g.status == GameStatus.PLAYING && g.isTurnOf(myId)
        }

    /** Extremos válidos para la ficha seleccionada (todos si hay Comodín). */
    val validSides: List<Side>
        get() {
            val g = game ?: return emptyList()
            val tile = selectedTile ?: return emptyList()
            if (!isMyTurn) return emptyList()
            return if (g.wildActive && g.board.isNotEmpty()) Side.entries
            else DominoRules.validSides(tile, g.board)
        }

    /** Fichas de mi mano que se pueden jugar (para resaltarlas). */
    val playableTiles: Set<String>
        get() {
            val g = game ?: return emptySet()
            if (!isMyTurn) return emptySet()
            return me?.hand.orEmpty()
                .filter { g.wildActive || DominoRules.validSides(it, g.board).isNotEmpty() }
                .map { it.key }.toSet()
        }

    val canDraw: Boolean
        get() = isMyTurn && playableTiles.isEmpty() && game?.pool?.isNotEmpty() == true

    val canPass: Boolean
        get() = isMyTurn && playableTiles.isEmpty() && game?.pool?.isEmpty() == true

    fun canUsePower(power: PowerType): Boolean {
        val g = game ?: return false
        val player = me ?: return false
        return isMyTurn && PowerManager.canUse(g, player, power)
    }
}

/**
 * ViewModel de la sala de espera, la partida y el resultado.
 * Observa la sala en Firebase y envía cada acción al [GameEngine]
 * dentro de una transacción.
 */
class GameViewModel(
    private val repository: GameRepository = GameRepository(),
    private val history: HistoryRepository = HistoryRepository(),
    private val engine: GameEngine = GameEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var lastEventSeen = -1
    private val savedMatches = mutableSetOf<String>()

    /** Empieza a escuchar la sala [code] en tiempo real. */
    fun observe(code: String, user: UserProfile) {
        observeJob?.cancel()
        lastEventSeen = -1
        _uiState.value = GameUiState(myId = user.uid)
        observeJob = viewModelScope.launch {
            repository.observeRoom(code)
                .catch { _uiState.update { it.copy(roomClosed = true) } }
                .collect { state -> onNewState(state) }
        }
    }

    private fun onNewState(state: GameState?) {
        if (state == null) {
            _uiState.update { it.copy(game = null, roomClosed = true) }
            return
        }
        // Mostrar el diálogo solo para eventos nuevos
        val newEvent = if (lastEventSeen >= 0 && state.eventCounter > lastEventSeen &&
            state.status == GameStatus.PLAYING
        ) state.lastEvent else null
        lastEventSeen = state.eventCounter

        _uiState.update { ui ->
            val myHand = state.playerById(ui.myId)?.hand.orEmpty()
            ui.copy(
                game = state,
                selectedTile = ui.selectedTile?.takeIf { sel -> myHand.any { it.sameAs(sel) } },
                pendingEvent = newEvent ?: ui.pendingEvent
            )
        }
        if (state.status == GameStatus.FINISHED) saveHistory(state)
    }

    // -------------------------------------------------------- Acciones UI

    fun selectTile(tile: Domino) = _uiState.update {
        it.copy(selectedTile = if (it.selectedTile?.sameAs(tile) == true) null else tile, error = null)
    }

    fun play(side: Side) {
        val tile = _uiState.value.selectedTile ?: return
        act { engine.playTile(it, myId(), tile, side) }
        _uiState.update { it.copy(selectedTile = null) }
    }

    fun usePower(power: PowerType) =
        act { engine.usePower(it, myId(), power, _uiState.value.selectedTile) }

    fun draw() = act { engine.drawTile(it, myId()) }

    fun pass() = act { engine.passTurn(it, myId()) }

    fun start() = act { engine.startGame(it, myId()) }

    fun restart() = act { engine.restart(it, myId()) }

    /** Abandonar: en espera sale de la sala; jugando, la partida termina. */
    fun leave() {
        val game = _uiState.value.game ?: return reset()
        val id = myId()
        viewModelScope.launch {
            when {
                game.status == GameStatus.WAITING && game.hostId == id -> repository.deleteRoom(game.code)
                game.status == GameStatus.WAITING -> repository.update(game.code) { MoveResult.Ok(engine.removePlayer(it, id)) }
                game.status == GameStatus.PLAYING -> repository.update(game.code) { MoveResult.Ok(engine.abandon(it, id)) }
                else -> Unit
            }
            reset()
        }
    }

    fun dismissEvent() = _uiState.update { it.copy(pendingEvent = null) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun reset() {
        observeJob?.cancel()
        observeJob = null
        _uiState.value = GameUiState()
    }

    // ------------------------------------------------------------ Helpers

    private fun myId() = _uiState.value.myId

    private fun act(action: (GameState) -> MoveResult) {
        val code = _uiState.value.game?.code ?: return
        viewModelScope.launch {
            val result = repository.update(code, action)
            if (result is MoveResult.Invalid) _uiState.update { it.copy(error = result.error) }
        }
    }

    /** Guarda la partida en el historial del usuario (una sola vez). */
    private fun saveHistory(state: GameState) {
        val me = state.playerById(myId()) ?: return
        val matchKey = "${state.code}_${state.createdAt}_${state.turnCount}"
        if (!savedMatches.add(matchKey)) return
        val record = MatchRecord(
            won = state.winnerId == me.id,
            score = me.score,
            round = state.round.coerceAtMost(state.maxRounds),
            maxRounds = state.maxRounds,
            opponents = state.players.filter { it.id != me.id }.joinToString { it.name },
            tilesPlaced = me.tilesPlaced,
            powersUsed = me.powersUsed,
            date = System.currentTimeMillis()
        )
        viewModelScope.launch { runCatching { history.save(me.id, matchKey, record) } }
    }
}
