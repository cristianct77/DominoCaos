# Plan de commits – Dominó Caos (3 commits, uno por estudiante)

Cada estudiante hace **un solo commit** con toda su parte. Se suben en orden: **E1 → E2 → E3**. Cada commit tiene su zip numerado (`Commit_1_...`, `Commit_2_...`, `Commit_3_...`) con los archivos listos para extraer en la raíz del repositorio y un `_LEEME_COMMIT_N.txt` con los pasos (ese archivo está en `.gitignore`, no se sube).

## Orden

| Orden | Est. | Rama | Zip | Mensaje |
|---|---|---|---|---|
| **1** | E1 | `main` | `Commit_1_E1_logica-juego.zip` | `feat: proyecto base y lógica del juego (reglas, poderes, eventos, puntaje) con pruebas` |
| **2** | E2 | `feature/firebase-datos` | `Commit_2_E2_firebase-datos.zip` | `feat(data): Firebase, autenticación, salas en tiempo real y ViewModels` |
| **3** | E3 | `feature/interfaz` | `Commit_3_E3_interfaz.zip` | `feat(ui): componentes, pantallas y navegación completa de Dominó Caos` |

## Qué contiene cada commit

**Commit 1 – E1, lógica del juego**
- Proyecto base: Gradle (todavía sin Firebase), manifest, recursos, tema "Caos nocturno", `strings.xml`, `MainActivity` inicial, `TypeScreen`.
- `model/`: Domino, Player, PowerType, ChaosEvent, GameState.
- `logic/`: DominoRules, MoveResult, TurnManager, ScoreCalculator, PowerManager, EventGenerator, GameEngine.
- Pruebas: DominoRulesTest, PowerManagerTest, GameEngineTest.
- `docs/ARQUITECTURA.md`.

**Commit 2 – E2, Firebase y datos**
- Gradle con Firebase, manifest con internet, `database.rules.json`, `app/google-services.json`.
- `logic/`: AuthValidator, RoomCodeGenerator. `model/`: MatchRecord.
- `data/`: FirebaseMapper, AuthRepository, GameRepository (tiempo real con transacciones), HistoryRepository.
- `viewmodel/`: AuthViewModel, LobbyViewModel, GameViewModel, HistoryViewModel.
- Pruebas: AuthValidatorTest, FirebaseMapperTest.
- `README.md`.

**Commit 3 – E3, interfaz**
- `composables/components/`: CommonComponents, DominoTile, BoardView, PlayerBadge, PowerComponents, GameControls, UiMappings.
- Pantallas: Login, Register, Lobby, CreateGame, JoinGame, WaitingRoom, Game, Result, History.
- `AppNavigation`, `MainActivity` final, todos los textos (`strings_*.xml`).
- `docs/PLAN_COMMITS.md`, `docs/GUIA_INSTALACION_Y_COMMITS.md`.

## Pasos

1. E1 crea el repositorio **público y vacío** en GitHub e invita a E2 y E3.
2. **Commit 1 (E1):** directo a `main`; luego crea la rama `develop` y el tag `v0.1`.
3. **Commit 2 (E2):** rama `feature/firebase-datos` desde `develop` → Pull Request a `develop` → otro integrante aprueba → merge → PR `develop → main` + tag `v0.2`.
4. **Commit 3 (E3):** empieza cuando el PR de E2 ya está fusionado. Rama `feature/interfaz` → Pull Request a `develop` → merge → PR `develop → main` + tag `v1.0` + Release.

- `app/google-services.json` se sube en el **commit 2** (E2).
- Cada uno hace su commit con **su propia cuenta** (`git config user.email` = correo de GitHub).
- La app se puede usar (login, salas, juego) cuando se fusiona el **commit 3**. Después de los commits 1 y 2 se demuestra con `./gradlew test`.

## Sustentación e informe IEEE

| Estudiante | Explica | Escribe en el informe |
|---|---|---|
| **E1** | Reglas, poderes, eventos, puntaje, fin de partida, pruebas | Diseño del motor y diagrama de clases del modelo/lógica |
| **E2** | MVVM, login, sincronización en tiempo real con Firebase | Arquitectura MVVM, diagrama de secuencia, estructura de datos |
| **E3** | Componentes reutilizables, pantallas, navegación | Especificación de la interfaz (capturas) y catálogo de componentes |
