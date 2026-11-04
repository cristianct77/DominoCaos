# Guía: abrir el proyecto, hacer funcionar Gradle y subir los commits

Dominó Caos · Taller 2 · Programación por Componentes 301

---

## Parte 0. Lo que necesita cada estudiante

| Herramienta | Notas |
|---|---|
| **Android Studio** actualizado | El mismo con el que abren HockeyGame. El proyecto usa las mismas versiones (Gradle 9.5.0, AGP 9.3.2, Kotlin 2.2.10): si HockeyGame abre, este también. |
| **Git** | En Windows instalar *Git for Windows* (incluye **Git Bash**). Todos los comandos de esta guía se escriben en Git Bash. |
| **Cuenta de GitHub** | Una por estudiante. |
| **Internet** | La primera sincronización de Gradle descarga varias dependencias. |

---

## Parte 1. Abrir bien el proyecto

1. **Descomprimir** `DominoCaos_Taller2.zip` en una ruta **corta, sin espacios ni tildes y fuera de OneDrive**, por ejemplo:

   ```
   C:\Taller2\referencia\DominoCaos\        <- el proyecto completo (referencia)
   C:\Taller2\referencia\Commits_en_orden\         <- los 3 zips numerados (1 = E1, 2 = E2, 3 = E3)
   ```
   Las rutas como `C:\Users\Juan\OneDrive\Escritorio\Programación por componentes\` dan errores raros de Gradle.

2. En Android Studio: **File → Open** y seleccionar la carpeta **`DominoCaos`**, la que contiene `settings.gradle.kts`.
   - ❌ No abrir la carpeta `referencia` ni la carpeta `app`.
   - Si pregunta *Trust project*, elegir **Trust Project**.

3. Esperar la **sincronización de Gradle** (barra inferior). La primera vez puede tardar de 5 a 15 minutos porque descarga Gradle 9.5.0, el JDK y las librerías.

4. El proyecto **no compila hasta tener `app/google-services.json`** (Parte 2). Es normal ver el error `File google-services.json is missing` antes de ese paso.

> La carpeta `referencia` es solo para consultar y copiar. **Nadie trabaja ni hace commits dentro de ella.** El repositorio de GitHub se clona en otra carpeta (Parte 4).

---

## Parte 2. Firebase (un solo proyecto para los tres)

Los tres usan **el mismo proyecto de Firebase**. Si cada uno usa el suyo, sus celulares no ven las salas de los demás.

Lo hace **un solo integrante** (sugerido: E2, que es quien integra Firebase):

1. Entrar a <https://console.firebase.google.com> → **Agregar proyecto** → nombre `DominoCaos` (Google Analytics se puede desactivar).
2. **Compilación → Authentication → Comenzar → Sign-in method → Correo electrónico/contraseña → Habilitar → Guardar.**
3. **Compilación → Realtime Database → Crear base de datos** → ubicación (la que sugiera) → *Comenzar en modo bloqueado*.
4. En Realtime Database, pestaña **Reglas**: borrar todo, pegar el contenido de `database.rules.json` del proyecto y **Publicar**.
5. **Recién ahora** (después de crear la base de datos): ⚙️ **Configuración del proyecto → General → Tus apps → ícono de Android**.
   - Nombre del paquete: **`com.udistrital.dominocaos`** (exacto).
   - Apodo: `DominoCaos`. El SHA-1 no es necesario.
   - **Registrar app → Descargar `google-services.json`** y saltar los demás pasos del asistente (el proyecto ya tiene configuradas las dependencias).
6. ⚙️ **Configuración del proyecto → Usuarios y permisos → Agregar miembro**: invitar a los otros dos como *Editor*.
7. `google-services.json` se sube al repositorio en el **commit 2** (E2). Los demás lo reciben con `git pull`. No es una contraseña: la seguridad la dan las reglas de la base de datos.

Cada estudiante copia el archivo en la carpeta `app/` de su proyecto:

```
DominoCaos/
 ├─ app/
 │   ├─ google-services.json   <- aquí
 │   ├─ build.gradle.kts
 │   └─ src/
 ├─ build.gradle.kts
 └─ settings.gradle.kts
```

Luego, en Android Studio: **File → Sync Project with Gradle Files** (o el ícono del elefante 🐘).

> ⚠️ Si descargaron el `google-services.json` **antes** de crear la Realtime Database, vuelvan a descargarlo. El archivo viejo no trae la URL de la base de datos y la app se cierra con `Failed to get FirebaseDatabase instance: Specify DatabaseURL`.

---

## Parte 3. Hacer funcionar Gradle (problemas comunes)

Siempre que cambien algo de la configuración: **File → Sync Project with Gradle Files**. Para ver el error completo: pestaña **Build** (abajo) → clic en el error rojo de más arriba.

| Mensaje de error | Solución |
|---|---|
| `File google-services.json is missing` | Falta el archivo en `app/` (Parte 2). |
| `Failed to get FirebaseDatabase instance: Specify DatabaseURL` (al abrir la app) | Volver a descargar `google-services.json` **después** de crear la Realtime Database. |
| `Could not resolve ...` / `Plugin [id: ...] was not found` | 1) Revisar internet. 2) En la pestaña **Gradle** (derecha) desactivar el modo **Offline** (ícono de nube tachada). 3) Volver a sincronizar. |
| `This version of the Android Gradle plugin requires a newer version of Android Studio` | **Help → Check for Updates** y actualizar Android Studio. |
| `Failed to find target with hash string 'android-37'` o `SDK ... not installed` | Clic en el enlace azul *Install missing SDK* del error, o **Tools → SDK Manager → SDK Platforms** e instalar la versión que pida. |
| Errores de JDK o *toolchain* (`Cannot find a Java installation`, `Unsupported class file major version`) | **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** → elegir *JetBrains Runtime (jbr-21)* o un JDK 21+. Si sigue fallando, borrar el archivo `gradle/gradle-daemon-jvm.properties` y sincronizar. |
| Error relacionado con *configuration cache* y `google-services` | En `gradle.properties` cambiar `org.gradle.configuration-cache=true` por `false`. |
| Android Studio sugiere otra versión de Firebase o del plugin | Las versiones están en `gradle/libs.versions.toml` (`firebaseBom`, `googleServices`). Se puede aceptar la sugerencia. |
| `Unresolved reference ...` en un archivo `.kt` | **Build → Clean Project** y luego **Build → Rebuild Project**. Si persiste, copiar el mensaje completo de la pestaña Build y enviarlo para corregirlo. |
| Todo raro, errores sin sentido | **File → Invalidate Caches → Invalidate and Restart**. |

Desde la **Terminal** de Android Studio también se puede compilar y ver el error completo:

```bash
./gradlew assembleDebug     # compila la app
./gradlew test              # corre las 35 pruebas unitarias
```

---

## Parte 4. Probar el juego en tiempo real

1. **Device Manager** (derecha) → crear **dos emuladores** (por ejemplo, Pixel 7 con API 34 o superior). También sirve un emulador y un celular con depuración USB.
2. Elegir el emulador 1 en la lista de dispositivos → **Run ▶**. Luego elegir el emulador 2 → **Run ▶**.
3. En cada uno, **Crear cuenta** con un correo distinto.
4. Emulador 1: **Nueva partida → 2 jugadores → Crear partida**. Aparece el código (ej. `K7Q2`).
5. Emulador 2: **Unirse a partida → escribir el código → Unirse** (o tocar **Entrar** en *Partidas disponibles*).
6. Emulador 1: **Iniciar partida**. En la consola de Firebase (**Realtime Database → Datos**) se ve cambiar el tablero en vivo.

---

## Parte 5. Repositorio público y commits

### 5.1 Configuración de Git (cada estudiante, una sola vez)

En Git Bash:

```bash
git config --global user.name "Nombre Apellido"
git config --global user.email "el-mismo-correo-de-su-cuenta-github@ejemplo.com"
```

> Si el correo no coincide con el de GitHub, sus commits no aparecen como suyos en *Contributors*.

La primera vez que hagan `git push`, Windows abre una ventana para iniciar sesión en GitHub. Hay que aceptarla.

### 5.2 Organización de carpetas

```
C:\Taller2\
 ├─ referencia\                      <- el zip descomprimido (NO se toca)
 │   ├─ DominoCaos\                  <- proyecto completo, solo para consultar
 │   └─ Commits_en_orden\
 │       ├─ 00_LEER_PRIMERO_ORDEN.txt
 │       ├─ Commit_1_E1_logica-juego.zip
 │       ├─ Commit_2_E2_firebase-datos.zip
 │       └─ Commit_3_E3_interfaz.zip
 └─ repo\
     └─ DominoCaos\                  <- el clon de GitHub (aquí se trabaja)
```

Son **3 commits en total, uno por estudiante**, en este orden: E1 → E2 → E3.

### 5.3 Crear el repositorio (E1)

1. GitHub → **New repository**.
   - Nombre: `DominoCaos`
   - **Public**
   - ❌ **No** marcar *Add a README*, *.gitignore* ni *license* (debe quedar vacío).
2. **Settings → Collaborators → Add people** → agregar a E2 y E3 (deben aceptar la invitación por correo).

### 5.4 Clonar (cada estudiante)

```bash
mkdir -p /c/Taller2/repo && cd /c/Taller2/repo
git clone https://github.com/USUARIO_E1/DominoCaos.git
cd DominoCaos
```

> En Git Bash, `C:\Taller2` se escribe `/c/Taller2`.

### 5.5 Cómo se usa cada zip

Cada zip trae **solo los archivos de ese commit**, con sus carpetas (`app/src/...`), y un archivo `_LEEME_COMMIT_N.txt` con la rama, el mensaje y los pasos.

1. Abrir el zip con el Explorador de Windows.
2. **Extraer todo** dentro de `C:\Taller2\repo\DominoCaos\` (la carpeta que contiene `.git`, **no** una subcarpeta nueva).
3. Si Windows pregunta, elegir **Reemplazar los archivos en el destino**.

> ⚠️ Al extraer, Windows propone crear una carpeta con el nombre del zip (`...\DominoCaos\Commit_2_E2_firebase-datos\`). **Borren esa última parte** para que quede `C:\Taller2\repo\DominoCaos`. Si no, los archivos quedan en el lugar equivocado.

El `_LEEME_COMMIT_N.txt` está en `.gitignore`: no se sube aunque hagan `git add .`. Lo pueden borrar después del commit.

### 5.6 Commit 1 – Estudiante 1 (directo a `main`)

El repositorio está vacío, así que el primer commit va directo a `main`:

```bash
cd /c/Taller2/repo/DominoCaos
# Extraer Commit_1_E1_logica-juego.zip aquí (paso 5.5)
# Abrir en Android Studio y sincronizar (este commit compila sin google-services.json)
git add .
git commit -m "feat: proyecto base y lógica del juego (reglas, poderes, eventos, puntaje) con pruebas"
git push -u origin main

# Crear develop a partir de main
git checkout -b develop
git push -u origin develop
```

Luego, en GitHub: **Settings → General → Default branch** → cambiar a `develop`.

### 5.7 Commits 2 y 3 – Estudiantes 2 y 3 (con Pull Request)

```bash
cd /c/Taller2/repo/DominoCaos

# 1. Ponerse al día y crear la rama
git checkout develop
git pull
git checkout -b feature/firebase-datos        # E3 usa: feature/interfaz

# 2. Extraer el zip del commit en esta carpeta (paso 5.5)

# 3. Android Studio (abierto sobre C:\Taller2\repo\DominoCaos):
#    Sync Project with Gradle Files  +  Build -> Make Project

# 4. Revisar y hacer el commit con el mensaje del LEEME
git add .
git status        # deben aparecer solo los archivos listados en el LEEME
git commit -m "feat(data): Firebase, autenticación, salas en tiempo real y ViewModels"
#   E3: git commit -m "feat(ui): componentes, pantallas y navegación completa de Dominó Caos"

# 5. Subir la rama
git push -u origin feature/firebase-datos     # E3: feature/interfaz
```

**Pull Request:** en GitHub aparece el botón **Compare & pull request**.
- *base:* `develop` ← *compare:* la rama del estudiante
- Agregar como *Reviewer* a otro integrante. Ese integrante revisa los archivos y aprueba (**Review changes → Approve**).
- **Merge pull request → Confirm merge → Delete branch**.

> **Orden:** E3 empieza **después** de que se fusione el PR de E2, y siempre con `git checkout develop && git pull`.

> **`google-services.json`:** viene dentro del zip del **commit 2** y se sube con ese commit. Desde ahí todos lo tienen al hacer `git pull`.

### 5.8 Orden completo (3 commits, uno por estudiante)

| Orden | Quién | Rama | Zip | Contenido |
|---|---|---|---|---|
| **1** | E1 | `main` | `Commit_1_E1_logica-juego.zip` | Proyecto base, modelo, reglas, motor, poderes, eventos, puntaje, pruebas, `docs/ARQUITECTURA.md` |
| | E1 | **Tag `v0.1`** en `main` | | |
| **2** | E2 | `feature/firebase-datos` | `Commit_2_E2_firebase-datos.zip` | Firebase (incluye `google-services.json`), autenticación, salas en tiempo real, historial, ViewModels, pruebas, `README.md` |
| | E2 | **PR `develop → main` + tag `v0.2`** | | |
| **3** | E3 | `feature/interfaz` | `Commit_3_E3_interfaz.zip` | Componentes, todas las pantallas, navegación, guías |
| | E3 | **PR `develop → main` + tag `v1.0` + Release** | | |

### 5.9 Cerrar un avance (PR a main y tag)

1. GitHub → **Pull requests → New** → *base:* `main` ← *compare:* `develop` → crear y fusionar (**Create a merge commit**).
2. En Git Bash:

```bash
git checkout main
git pull
git tag -a v0.1 -m "Avance 1: proyecto base, modelo y autenticación"
git push origin v0.1
git checkout develop
```

(Para los otros avances: `v0.2` "Avance 2: motor, poderes y salas en tiempo real" y `v1.0` "Entrega final".)

3. Al final: GitHub → **Releases → Draft a new release** → tag `v1.0` → adjuntar el APK (`app/build/outputs/apk/debug/app-debug.apk`, generado con **Build → Generate APKs**).

### 5.10 Si algo sale mal

| Problema | Solución |
|---|---|
| `! [rejected] ... (fetch first)` al hacer push | `git pull --rebase` y luego `git push`. |
| Hice commits en la rama equivocada (por ejemplo en `develop`) | `git branch feature/correcta`, luego `git reset --hard origin/develop`, luego `git checkout feature/correcta` (los commits quedan en la rama nueva). |
| Agregué un archivo que no era | Antes del commit: `git restore --staged ruta/archivo`. |
| Me equivoqué en el mensaje del último commit (sin push) | `git commit --amend -m "mensaje correcto"`. |
| Conflicto al hacer pull | No debería pasar si se respeta el orden. Si pasa: abrir en Android Studio **Git → Resolve Conflicts**, elegir *Accept Theirs* (la versión de `develop`) y luego volver a extraer el zip del commit. |
| Los archivos quedaron en una subcarpeta con el nombre del zip | Borrar esa subcarpeta y volver a extraer directamente en `C:\Taller2\repo\DominoCaos`. `git status` sirve para ver dónde quedaron. |

### 5.11 Alternativa sin consola (Android Studio)

- **Ramas:** abajo a la derecha (nombre de la rama) → *New Branch* / *Checkout*.
- **Commit:** **Git → Commit** (Ctrl+K). Marcar solo los archivos del commit y escribir el mensaje.
- **Push:** **Git → Push** (Ctrl+Shift+K).
- **Pull:** **Git → Pull**.

Los zips se extraen igual con el Explorador de Windows; luego el commit se puede hacer desde Android Studio en lugar de Git Bash.

### 5.12 Para la sustentación

- **Insights → Contributors:** commits de cada integrante.
- **Insights → Network:** gráfico de ramas y merges.
- **Pull requests → Closed:** revisiones entre compañeros.
- **Tags/Releases:** los tres avances.
