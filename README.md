# Dominó Caos 🁫

Aplicación Android (Kotlin + Jetpack Compose) para jugar **Dominó "Caos"** en una sala multijugador sincronizada en tiempo real con **Firebase**. Los jugadores colocan fichas de dominó, pero la partida cambia con **poderes especiales** y **eventos aleatorios**.

Taller 02 – Programación por Componentes 301 · Universidad Distrital · 2026-3

## Funcionalidades

| Requisito del taller | Dónde está |
|---|---|
| Login y creación de cuenta (Firebase Auth) | `AuthRepository`, `AuthViewModel`, `LoginScreen`, `RegisterScreen` |
| Sala con código + partidas disponibles | `GameRepository`, `LobbyViewModel`, `LobbyScreen`, `CreateGameScreen`, `JoinGameScreen`, `WaitingRoomScreen` |
| Sincronización en tiempo real (Realtime Database) | `GameRepository.observeRoom()` + transacciones en `GameRepository.update()` |
| 7 fichas por jugador, turnos, validación de movimientos | `DominoRules`, `GameEngine.playTile()` |
| Robar del pozo / perder turno | `GameEngine.drawTile()`, `GameEngine.passTurn()` |
| 4 poderes con usos limitados (Comodín, Salto, Intercambio, Inversión) | `PowerType`, `PowerManager`, `PowerSelectionDialog` |
| Eventos aleatorios | `ChaosEvent`, `EventGenerator`, `EventDialog` |
| Cálculo de puntaje | `ScoreCalculator` |
| Fin automático (sin fichas, trancado, máximo de rondas) y desempate por menos poderes | `GameEngine.endTurn()`, `GameEngine.finish()`, `ScoreCalculator.determineWinner()` |
| Abandonar y reiniciar | `GameEngine.abandon()`, `GameEngine.restart()` |
| Turno y puntaje visibles siempre | `GameScreen` + `PlayerBadge` |
| Historial de partidas | `HistoryRepository`, `HistoryViewModel`, `HistoryScreen` |
| Componentes reutilizables | `composables/components/` (DominoTile, BoardView, PlayerBadge, PowerCard, GameControls…) |
| Arquitectura MVVM, lógica separada de la UI | `model/` + `logic/` (Kotlin puro) · `data/` · `viewmodel/` · `composables/` |
| Pruebas unitarias | `app/src/test/` (35 pruebas) |

## Reglas implementadas

- Cada jugador inicia con 7 fichas (2 a 4 jugadores, dominó doble seis). Empieza quien tenga el doble más alto.
- En su turno el jugador selecciona una ficha y la coloca en el extremo izquierdo o derecho si coincide.
- **Poderes** (1 uso de cada uno por jugador, máximo 1 poder por turno, se eligen antes de mover):
  - **Comodín:** la siguiente ficha puede colocarse sin coincidir con el extremo.
  - **Salto:** el siguiente jugador pierde su turno.
  - **Intercambio:** cambia la ficha seleccionada por una aleatoria del oponente.
  - **Inversión:** cambia el sentido de los turnos.
- **Eventos aleatorios** (25 % de probabilidad al terminar cada turno): Tormenta de fichas, Doble puntaje, Giro caótico, Recarga de poder, Impuesto al líder.
- **Puntaje:** puntos de la ficha + bono si la suma de los extremos es múltiplo de 5 (×2 con Doble puntaje). Quien se queda sin fichas suma las fichas de los rivales.
- Sin movimiento válido → robar del pozo. Pozo vacío → pierde el turno.
- La partida termina cuando alguien se queda sin fichas, nadie puede mover o se supera el máximo de rondas. Gana el mayor puntaje; empate → gana quien usó menos poderes.

## Configurar Firebase (un solo proyecto para todo el grupo)

Los tres integrantes usan **el mismo proyecto de Firebase**; si no, no pueden unirse a las salas de los demás.

1. Un integrante crea el proyecto en <https://console.firebase.google.com> e invita a los otros dos (*Configuración del proyecto → Usuarios y permisos*).
2. **Authentication → Sign-in method:** habilitar *Correo electrónico/contraseña*.
3. **Realtime Database → Crear base de datos** (modo bloqueado) y en *Reglas* pegar el contenido de `database.rules.json`.
4. **Después** de crear la base de datos: *Configuración del proyecto → Agregar app → Android* con el paquete **`com.udistrital.dominocaos`** y descargar `google-services.json` (si se descarga antes, no trae la URL de la base de datos).
5. Copiar `google-services.json` en la carpeta `app/`. Se sube al repositorio en el commit 2, así los tres lo reciben con `git pull`.
6. Abrir el proyecto en Android Studio, sincronizar Gradle y ejecutar.

La guía completa de instalación, Gradle y commits está en [`docs/GUIA_INSTALACION_Y_COMMITS.md`](docs/GUIA_INSTALACION_Y_COMMITS.md).

Para probar el tiempo real: ejecutar en dos emuladores (o emulador + celular), crear la sala en uno y unirse con el código en el otro.

## Estructura de datos en Firebase

```
rooms/
  K7Q2/
    code, hostId, status (WAITING|PLAYING|FINISHED), maxPlayers, maxRounds,
    players: [ {id, name, hand:["2|5",...], score, powers:{COMODIN:1,...}, powersUsed, tilesPlaced, eventsReceived} ],
    board: [ {tile:"6|3", wild:false}, ... ],
    pool: ["0|0", ...], currentTurn, direction, turnCount, lastEvent, eventCounter, lastAction, winnerId, finishReason
users/
  {uid}/profile {name, email}
  {uid}/history/{matchKey} {won, score, round, maxRounds, opponents, tilesPlaced, powersUsed, date}
```

## Ejecutar pruebas

```bash
./gradlew test
```

## Documentación

- [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md): MVVM, diagrama de clases y componentes (base para el informe IEEE).
- [`docs/PLAN_COMMITS.md`](docs/PLAN_COMMITS.md): reparto de trabajo y commits por estudiante.
