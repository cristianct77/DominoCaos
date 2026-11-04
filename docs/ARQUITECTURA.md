# Arquitectura de Dominó Caos

Base para las secciones de **especificaciones de diseño, arquitectura MVVM, diagrama de clases y componentes** del informe IEEE.

## 1. Capas (MVVM)

```mermaid
flowchart TB
    subgraph View["View · Jetpack Compose (composables/)"]
        Screens["Pantallas<br/>Login · Register · Lobby · CreateGame · JoinGame<br/>WaitingRoom · Game · Result · History"]
        Components["Componentes reutilizables<br/>DominoTile · BoardView · PlayerBadge<br/>PowerCard · GameControls · EventDialog"]
        Nav["AppNavigation + TypeScreen"]
    end
    subgraph VM["ViewModel (viewmodel/)"]
        AuthVM[AuthViewModel]
        LobbyVM[LobbyViewModel]
        GameVM[GameViewModel]
        HistVM[HistoryViewModel]
    end
    subgraph Model["Model"]
        subgraph Logic["logic/ · Kotlin puro (probado con JUnit)"]
            Engine[GameEngine]
            Rules[DominoRules]
            Powers[PowerManager]
            Events[EventGenerator]
            Score[ScoreCalculator]
        end
        subgraph Data["data/ · Firebase"]
            AuthRepo[AuthRepository]
            GameRepo[GameRepository]
            HistRepo[HistoryRepository]
            Mapper[FirebaseMapper]
        end
        Entities["model/<br/>Domino · Player · GameState · PowerType · ChaosEvent"]
    end
    Firebase[(Firebase Auth +<br/>Realtime Database)]

    Screens --> Nav
    Screens --> Components
    Nav -- observa StateFlow --> VM
    Nav -- eventos de usuario --> VM
    VM --> Engine
    VM --> Data
    Engine --> Rules & Powers & Events & Score
    Data --> Mapper
    Data <--> Firebase
    Logic --> Entities
    Data --> Entities
```

- **View**: funciones `@Composable` sin lógica de juego. Reciben un `UiState` y callbacks.
- **ViewModel**: expone `StateFlow<UiState>`; traduce acciones de la UI a llamadas del motor y del repositorio.
- **Model**: `model/` (entidades inmutables), `logic/` (reglas puras, sin Android) y `data/` (Firebase).

## 2. Flujo de una jugada en tiempo real

```mermaid
sequenceDiagram
    actor J1 as Jugador 1
    participant UI as GameScreen
    participant VM as GameViewModel
    participant Repo as GameRepository
    participant DB as Firebase RTDB
    participant E as GameEngine
    actor J2 as Jugador 2 (otro celular)

    J1->>UI: selecciona ficha + "Derecha"
    UI->>VM: play(Side.RIGHT)
    VM->>Repo: update(code) { engine.playTile(...) }
    Repo->>DB: runTransaction
    DB-->>Repo: estado actual
    Repo->>E: playTile(estado, jugador, ficha, lado)
    E-->>Repo: MoveResult.Ok(nuevoEstado) / Invalid(error)
    Repo->>DB: guarda nuevoEstado (o aborta)
    DB-->>VM: onDataChange (ambos dispositivos)
    DB-->>J2: onDataChange
    VM-->>UI: GameUiState actualizado (tablero, turno, puntaje, evento)
```

## 3. Diagrama de clases (modelo y lógica)

```mermaid
classDiagram
    class Domino {
        +Int left
        +Int right
        +Boolean isDouble
        +Int points
        +String key
        +has(value) Boolean
        +flipped() Domino
        +sameAs(other) Boolean
    }
    class BoardTile {
        +Domino domino
        +Boolean wild
    }
    class Player {
        +String id
        +String name
        +List~Domino~ hand
        +Int score
        +Map~PowerType,Int~ powers
        +Int powersUsed
        +Int tilesPlaced
        +Int eventsReceived
        +usesOf(power) Int
    }
    class GameState {
        +String code
        +String hostId
        +GameStatus status
        +Int maxPlayers
        +Int maxRounds
        +List~Player~ players
        +List~BoardTile~ board
        +List~Domino~ pool
        +Int currentTurn
        +Int direction
        +Int turnCount
        +Boolean wildActive
        +Boolean skipPending
        +Boolean doubleScoreNext
        +ChaosEvent lastEvent
        +String winnerId
        +FinishReason finishReason
        +round Int
    }
    class PowerType {
        <<enumeration>>
        COMODIN
        SALTO
        INTERCAMBIO
        INVERSION
    }
    class ChaosEvent {
        <<enumeration>>
        TORMENTA_FICHAS
        DOBLE_PUNTAJE
        GIRO_CAOTICO
        RECARGA_PODER
        IMPUESTO_LIDER
    }
    class GameEngine {
        +createRoom(...) GameState
        +addPlayer(state, id, name) MoveResult
        +startGame(state, requester) MoveResult
        +playTile(state, id, tile, side) MoveResult
        +usePower(state, id, power, tile) MoveResult
        +drawTile(state, id) MoveResult
        +passTurn(state, id) MoveResult
        +abandon(state, id) GameState
        +restart(state, requester) MoveResult
        -endTurn(state) GameState
        -finish(state, reason) GameState
    }
    class DominoRules {
        <<object>>
        +fullDeck() List~Domino~
        +deal(n, random)
        +canPlace(tile, board, side) Boolean
        +validSides(tile, board) List~Side~
        +place(board, tile, side, wild) List~BoardTile~
    }
    class PowerManager {
        <<object>>
        +canUse(state, player, power) Boolean
        +apply(state, id, power, tile, random) MoveResult
    }
    class EventGenerator {
        +roll() ChaosEvent?
        +apply(state, event) GameState
    }
    class ScoreCalculator {
        <<object>>
        +pointsForPlay(tile, board, double) Int
        +dominoBonus(players, winner) Int
        +determineWinner(players) Player
    }
    class MoveResult {
        <<sealed>>
    }
    class Ok { +GameState state }
    class Invalid { +MoveError error }

    GameState "1" *-- "2..4" Player
    GameState "1" *-- "*" BoardTile
    GameState "1" o-- "*" Domino : pool
    Player "1" o-- "*" Domino : hand
    BoardTile --> Domino
    Player ..> PowerType
    GameState ..> ChaosEvent
    GameEngine ..> DominoRules
    GameEngine ..> PowerManager
    GameEngine --> EventGenerator
    GameEngine ..> ScoreCalculator
    GameEngine ..> MoveResult
    MoveResult <|-- Ok
    MoveResult <|-- Invalid
```

## 4. Diagrama de clases (datos y ViewModels)

```mermaid
classDiagram
    class AuthRepository {
        +currentUser() UserProfile?
        +login(email, pass) Result
        +register(name, email, pass) Result
        +logout()
    }
    class GameRepository {
        +createRoom(build) Result~GameState~
        +observeRoom(code) Flow~GameState?~
        +observeOpenRooms() Flow~List~GameState~~
        +update(code, action) MoveResult
        +deleteRoom(code)
    }
    class HistoryRepository {
        +save(uid, key, record)
        +observe(uid) Flow~List~MatchRecord~~
    }
    class FirebaseMapper {
        <<object>>
        +toMap(GameState) Map
        +toGameState(Map) GameState?
    }
    class AuthViewModel { +uiState StateFlow~AuthUiState~ }
    class LobbyViewModel {
        +uiState StateFlow~LobbyUiState~
        +openRooms StateFlow
    }
    class GameViewModel { +uiState StateFlow~GameUiState~ }
    class HistoryViewModel { +records StateFlow }

    AuthViewModel --> AuthRepository
    LobbyViewModel --> GameRepository
    LobbyViewModel --> GameEngine
    GameViewModel --> GameRepository
    GameViewModel --> HistoryRepository
    GameViewModel --> GameEngine
    HistoryViewModel --> HistoryRepository
    GameRepository ..> FirebaseMapper
    HistoryRepository ..> FirebaseMapper
```

## 5. Componentes reutilizables (UI)

| Componente | Responsabilidad | Usado en |
|---|---|---|
| `DominoTile` | Dibuja una ficha con Canvas (horizontal/vertical, seleccionada, jugable, comodín) | Login (logo), Tablero, Mano |
| `BoardView` | Mesa de juego con la cadena de fichas | GameScreen |
| `PlayerBadge` | Avatar, nombre, puntaje, fichas y resaltado de turno | GameScreen |
| `PowerCard`, `PowersButton`, `PowerSelectionDialog` | Selección y control de poderes con usos restantes | GameScreen |
| `HandView`, `GameControls`, `StatusChip` | Mano, botones Izquierda/Derecha/Robar/Pasar, efectos activos | GameScreen |
| `EventDialog` | Anuncio de evento aleatorio | GameScreen |
| `PrimaryButton`, `AppTextField`, `ScreenHeader`, `GlassCard`, `MenuCard`, `ChoiceChips`, `Avatar` | Controles comunes | Todas las pantallas |
| `UiMappings` | Traduce enums de la lógica a textos/iconos/colores (la lógica no depende de Android) | Todas |

## 6. Decisiones de diseño

1. **Estado inmutable + funciones puras** en `GameEngine`: cada acción devuelve un nuevo `GameState`, lo que permite probarlo sin Android y reutilizarlo dentro de una transacción de Firebase.
2. **Transacciones** (`runTransaction`) para todas las jugadas: si dos dispositivos escriben a la vez, Firebase reintenta con el estado más reciente.
3. **Fichas como texto `"a|b"`** en Firebase para que el tablero sea legible en la consola.
4. **Navegación por estado** con `TypeScreen` (misma metodología del proyecto HockeyGame), conectada a los `StateFlow` de los ViewModels.
5. **Eventos con contador** (`eventCounter`): todos los dispositivos muestran el diálogo una sola vez por evento.
