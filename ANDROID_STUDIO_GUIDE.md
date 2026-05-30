# 📱 Guía Completa de Android Studio - OAuth2 Testing App

Esta guía te llevará paso a paso desde abrir el proyecto hasta publicar la aplicación en Google Play Store.

---

## 📋 Tabla de Contenidos

1. [Requisitos Previos](#requisitos-previos)
2. [Abrir el Proyecto en Android Studio](#abrir-el-proyecto-en-android-studio)
3. [Compilar la Aplicación](#compilar-la-aplicación)
4. [Probar en Emulador](#probar-en-emulador)
5. [Instalar en Dispositivo Físico](#instalar-en-dispositivo-físico)
6. [Debug de la Aplicación](#6-debug-de-la-aplicación) ⭐ NUEVO
7. [Configurar Certificado de Firma](#7-configurar-certificado-de-firma)
8. [Generar APK para Distribución](#8-generar-apk-para-distribución)
9. [Publicar en Google Play Store](#9-publicar-en-google-play-store)

---

## 1. Requisitos Previos

### ✅ Software Necesario

- **Android Studio** (versión Arctic Fox o superior)
  - Descarga: https://developer.android.com/studio
- **Java JDK 17 o 21** (ya incluido con Android Studio)
- **Android SDK** (se instala automáticamente con Android Studio)

### ✅ Verificar Instalación

1. Abrir Android Studio
2. Ve a **Android Studio > Preferences** (macOS) o **File > Settings** (Windows/Linux)
3. Navega a **Appearance & Behavior > System Settings > Android SDK**
4. Verifica que esté instalado:
   - **Android 14.0 (API 34)** ✓
   - **Android SDK Build-Tools**
   - **Android SDK Platform-Tools**

---

## 2. Abrir el Proyecto en Android Studio

### Paso 1: Abrir Android Studio

Inicia Android Studio desde tus aplicaciones.

### Paso 2: Abrir el Proyecto

1. En la pantalla de bienvenida, click en **"Open"**
2. Navega a la ruta del proyecto:
   ```
   <project-root>
   ```
3. Selecciona la carpeta `android-authn` y click **"Open"**

### Paso 3: Sincronizar Gradle (Automático)

Android Studio automáticamente sincronizará el proyecto con Gradle. Verás en la parte inferior:
```
Gradle sync in progress...
```

Espera a que termine (puede tomar 1-5 minutos la primera vez).

### Paso 4: Verificar que No Hay Errores

- En la pestaña **"Build"** (parte inferior) no deben aparecer errores
- Si aparecen warnings (advertencias), no te preocupes, son normales

### ⚠️ Posibles Problemas

**Problema**: "SDK location not found"
- **Solución**: Ve a **File > Project Structure > SDK Location** y verifica que apunte a tu SDK de Android (normalmente `~/Library/Android/sdk`)

**Problema**: "Gradle sync failed"
- **Solución**: Click en **File > Invalidate Caches and Restart**

---

## 3. Compilar la Aplicación

### Opción A: Compilar desde Android Studio (Recomendado)

1. En el menú superior, click en **Build > Make Project** (o `Cmd + F9` en Mac)
2. Espera a que termine la compilación
3. Verás en la parte inferior:
   ```
   BUILD SUCCESSFUL in 42s
   ```

### Opción B: Compilar desde Terminal

1. Abre la terminal integrada en Android Studio: **View > Tool Windows > Terminal**
2. Ejecuta:
   ```bash
   ./gradlew assembleDebug
   ```
3. El APK se generará en:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### 📦 Entender los Build Types

- **Debug**: Para desarrollo y testing (sin firmar, más grande)
- **Release**: Para producción (firmado, optimizado, más pequeño)

---

## 4. Probar en Emulador

### Paso 1: Crear un Emulador (Primera Vez)

1. Click en **Tools > Device Manager** (o el icono de celular en la barra superior)
2. Click en **"Create Device"**
3. Selecciona un dispositivo (recomendado: **Pixel 5**)
4. Click **"Next"**
5. Selecciona una **System Image**:
   - Recomendado: **API 34 (Android 14.0)** - arquitectura x86_64
   - Si no está descargada, click en el botón **"Download"** junto a la imagen
6. Click **"Next"**
7. Nombra tu emulador (ej: "Pixel_5_API_34")
8. Click **"Finish"**

### Paso 2: Ejecutar la App en el Emulador

1. Asegúrate de que el emulador esté seleccionado en la barra superior (junto al botón Run)
2. Click en el botón **Run** ▶️ (verde) o presiona `Shift + F10`
3. El emulador se iniciará (puede tomar 1-2 minutos la primera vez)
4. La app se instalará y abrirá automáticamente

### 📝 Notas sobre el Emulador

- **WebAuthn NO funcionará** en el emulador (requiere dispositivo físico con biometría)
- **Authorization Code y CIBA** funcionarán correctamente

### 🎮 Controles del Emulador

- **Rotar**: `Ctrl + Left/Right Arrow`
- **Volver**: `Esc`
- **Home**: `Cmd + H` (Mac) o `Ctrl + H` (Windows)
- **Menú de opciones**: `...` en el panel lateral del emulador

---

## 5. Instalar en Dispositivo Físico

### Paso 1: Habilitar Opciones de Desarrollador en tu Móvil

#### En Android:

1. Ve a **Ajustes > Acerca del teléfono**
2. Busca **"Número de compilación"** o **"Versión de MIUI"**
3. Toca **7 veces** sobre ese número
4. Verás el mensaje: _"Ahora eres desarrollador"_

### Paso 2: Habilitar Depuración USB

1. Ve a **Ajustes > Sistema > Opciones de desarrollador**
2. Activa **"Depuración USB"**
3. (Opcional) Activa **"Instalación vía USB"** si está disponible

### Paso 3: Conectar el Dispositivo

1. Conecta tu móvil al Mac con un cable USB
2. En el móvil, aparecerá un popup: **"¿Permitir depuración USB?"**
3. Marca **"Permitir siempre desde este ordenador"**
4. Click **"Permitir"**

### Paso 4: Verificar Conexión en Android Studio

1. En Android Studio, verás tu dispositivo en el selector superior
2. Debería aparecer algo como: **"Samsung Galaxy S21"** o el modelo de tu móvil

### Paso 5: Instalar y Ejecutar

1. Selecciona tu dispositivo en el selector
2. Click en el botón **Run** ▶️
3. La app se instalará en tu móvil

### ⚠️ Problemas Comunes

**Problema**: "Device not recognized"
- **Mac**: Puede que necesites instalar Android File Transfer
- **Solución**: Desconecta y vuelve a conectar el cable
- **Solución 2**: Prueba con otro cable USB

**Problema**: "Unauthorized device"
- **Solución**: Revoca autorizaciones USB en el móvil y vuelve a autorizar

---

## 6. Debug de la Aplicación

El debugging es esencial para encontrar y solucionar errores. Android Studio ofrece herramientas potentes para inspeccionar tu app en tiempo real.

### 🐛 Conceptos Básicos de Debugging

**¿Qué es debuggear?**
- Ejecutar la app paso a paso
- Inspeccionar valores de variables
- Ver el flujo de ejecución del código
- Encontrar y corregir errores

### Modo Debug vs Modo Run

| Característica | Run (▶️) | Debug (🐞) |
|----------------|---------|-----------|
| Velocidad | Rápido | Más lento |
| Breakpoints | Ignorados | Se detiene |
| Inspección de variables | No | Sí |
| Step-by-step | No | Sí |
| Uso típico | Probar app | Encontrar bugs |

---

### Paso 1: Colocar Breakpoints

Un **breakpoint** es un punto donde la ejecución se detendrá para que puedas inspeccionar el estado.

#### Cómo Añadir un Breakpoint:

1. Abre el archivo que quieres debuggear (ej: `MainActivity.kt`)
2. Encuentra la línea de código donde quieres pausar
3. Click en el margen izquierdo (junto al número de línea)
4. Aparecerá un círculo rojo 🔴

**Ejemplo**:
```kotlin
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {  // ← Coloca un breakpoint aquí (línea 34)
        MaterialTheme {
            MainScreen(...)
        }
    }
}
```

#### Tipos de Breakpoints:

- **Line breakpoint** (🔴): Se detiene en esa línea
- **Conditional breakpoint**: Se detiene solo si se cumple una condición
- **Exception breakpoint**: Se detiene cuando ocurre una excepción

**Breakpoint Condicional**:
1. Click derecho en el breakpoint (círculo rojo)
2. Añade una condición, ej: `username == "test"`
3. Solo se detendrá cuando `username` sea "test"

---

### Paso 2: Iniciar Modo Debug

#### Opción A: Desde el Botón Debug

1. Selecciona tu dispositivo/emulador en la barra superior
2. Click en el botón **Debug** 🐞 (junto al botón Run verde)
3. O presiona `Ctrl + D` (Mac: `Cmd + D`)

#### Opción B: Attach Debugger a una App en Ejecución

Si la app ya está corriendo:

1. Ve a **Run > Attach Debugger to Android Process**
2. Selecciona el proceso: `com.masstack.authn`
3. Click **"OK"**

La app seguirá ejecutándose normalmente hasta que llegue a un breakpoint.

---

### Paso 3: Usar las Herramientas de Debug

Cuando la ejecución se detiene en un breakpoint, verás varias ventanas:

#### 🔍 Debug Tool Window (Ventana de Debug)

Aparece automáticamente en la parte inferior. Contiene varias pestañas:

**1. Debugger**:
- Muestra el stack trace (pila de llamadas)
- Variables locales y sus valores
- Campos del objeto actual

**2. Frames**:
- Pila de llamadas de funciones
- Puedes navegar entre diferentes niveles

**3. Variables**:
- Todas las variables accesibles en el punto actual
- Puedes expandir objetos para ver sus propiedades
- Click derecho > "Set Value" para cambiar valores en tiempo real

**4. Watches**:
- Expresiones personalizadas que quieres monitorear
- Ej: `settings.clientId`, `tokenResponse?.accessToken`

---

### Paso 4: Controles de Navegación Durante el Debug

Una vez detenido en un breakpoint, usa estos controles:

| Icono/Atajo | Acción | Descripción |
|-------------|--------|-------------|
| ▶️ **Resume** (`F9`) | Continuar | Continúa hasta el siguiente breakpoint |
| ⏸️ **Pause** | Pausar | Pausa la ejecución |
| ⏹️ **Stop** (`Cmd+F2`) | Detener | Termina la sesión de debug |
| 👣 **Step Over** (`F8`) | Paso siguiente | Ejecuta la línea actual, pasa a la siguiente |
| ⬇️ **Step Into** (`F7`) | Entrar en función | Entra dentro de la función llamada |
| ⬆️ **Step Out** (`Shift+F8`) | Salir de función | Sale de la función actual |
| ⏭️ **Run to Cursor** | Ejecutar hasta cursor | Ejecuta hasta donde está el cursor |

#### Ejemplo Práctico:

```kotlin
fun authenticateUser(username: String) {  // ← Breakpoint aquí
    val settings = settingsRepository.getSettings()  // Step Over (F8)
    val result = authRepository.exchangeCodeForToken(...)  // Step Into (F7) para entrar
    if (result.isSuccess) {  // Step Over (F8)
        showSuccess()
    }
}
```

**Flujo de debug típico**:
1. Breakpoint en la línea 1
2. `F8` (Step Over) para ejecutar línea 2
3. `F7` (Step Into) para entrar en `exchangeCodeForToken`
4. Inspeccionar variables dentro
5. `Shift+F8` (Step Out) para volver
6. `F9` (Resume) para continuar

---

### Paso 5: Inspeccionar Variables y Expresiones

#### Ver Valores de Variables:

En la ventana **Variables**, verás todas las variables disponibles:

```
this = MainActivity@12345
  settingsRepository = SettingsRepository@67890
  authRepository = AuthRepository@54321

username = "testuser"
settings = Settings
  serverUrl = "https://authn.sta.masstack.com/v1"
  clientId = "my-client-id"
  scope = "openid profile"
```

Puedes:
- **Expandir objetos**: Click en la flecha ▶️
- **Ver valores inline**: Aparecen junto al código
- **Copiar valores**: Click derecho > "Copy Value"

#### Evaluar Expresiones:

1. Selecciona una expresión en el código (ej: `settings.clientId`)
2. Click derecho > **"Evaluate Expression"** (o `Alt+F8`)
3. Escribe cualquier expresión Kotlin válida
4. Click **"Evaluate"**

**Ejemplos de expresiones**:
```kotlin
settings.clientId.length
tokenResponse?.accessToken?.take(10)
username.startsWith("test")
```

#### Watches (Vigilancia de Variables):

Para monitorear expresiones automáticamente:

1. En la ventana **Debug**, ve a la pestaña **Watches**
2. Click en `+`
3. Añade expresión: `settings.isValid()`
4. Se actualizará automáticamente en cada paso

---

### Paso 6: Logcat - Ver Logs en Tiempo Real

**Logcat** muestra todos los logs de la aplicación.

#### Abrir Logcat:

**View > Tool Windows > Logcat** (o click en pestaña inferior)

#### Filtrar Logs:

En la barra de filtros, puedes:

1. **Por nivel**:
   - Verbose (V) - Todo
   - Debug (D) - Mensajes de debug
   - Info (I) - Información
   - Warn (W) - Advertencias
   - Error (E) - Errores
   - Assert (A) - Fallos críticos

2. **Por tag**: Filtra por clase
   ```
   tag:MainActivity
   tag:AuthRepository
   ```

3. **Por paquete**: Solo logs de tu app
   ```
   package:com.masstack.authn
   ```

4. **Por texto**: Busca texto específico
   ```
   OAuth
   token
   error
   ```

#### Añadir Logs en tu Código:

```kotlin
import android.util.Log

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "onCreate called")  // Debug
        Log.i("MainActivity", "Settings valid: ${settings.isValid()}")  // Info
        Log.w("MainActivity", "No client ID configured")  // Warning
        Log.e("MainActivity", "Failed to load settings", exception)  // Error
    }
}
```

**Formato de Log**:
```kotlin
Log.d(TAG, mensaje)
//  │   │    └─ Mensaje a mostrar
//  │   └────── Tag (normalmente nombre de la clase)
//  └────────── Nivel (d=debug, i=info, w=warn, e=error)
```

**Consejo**: Define un TAG constante:
```kotlin
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    fun someFunction() {
        Log.d(TAG, "Function called")
    }
}
```

---

### Paso 7: Debug de Problemas Comunes en la App OAuth

#### Problema 1: Token no se recibe

**Añade breakpoints en**:
```kotlin
// AuthorizationCodeActivity.kt
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    handleOAuthCallback(intent)  // ← Breakpoint aquí
}

private fun handleOAuthCallback(intent: Intent?) {
    val uri = intent?.data  // ← Inspecciona uri
    val code = uri?.getQueryParameter("code")  // ← ¿code es null?
}
```

**Watches útiles**:
- `intent?.data?.toString()`
- `uri?.getQueryParameter("code")`
- `uri?.getQueryParameter("error")`

#### Problema 2: PKCE Challenge no coincide

**Añade breakpoints en**:
```kotlin
// PKCEUtil.kt
fun generateCodeChallenge(codeVerifier: String): String {
    val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val digest = messageDigest.digest(bytes)  // ← Inspecciona digest
    return base64UrlEncode(digest)  // ← Inspecciona resultado
}
```

**Evalúa expresiones**:
- `codeVerifier.length` (debe ser 43-128)
- `digest.size` (debe ser 32 bytes)

#### Problema 3: Settings no se guardan

**Añade breakpoints en**:
```kotlin
// SettingsRepository.kt
fun saveSettings(settings: Settings) {
    sharedPreferences.edit().apply {
        putString("server_url", settings.serverUrl)  // ← Breakpoint
        putString("client_id", settings.clientId)
        apply()
    }
}
```

**Watches**:
- `settings.isValid()`
- `settings.clientId.isBlank()`

---

### Paso 8: Debuggear Network Requests (Retrofit)

#### Opción A: Network Profiler

1. Ve a **View > Tool Windows > Profiler**
2. Click en el botón `+` y selecciona tu dispositivo
3. Click en **"NETWORK"**
4. Verás todas las peticiones HTTP en tiempo real

#### Opción B: OkHttp Logging Interceptor

Ya está configurado en el proyecto. Los logs aparecen en Logcat:

```
Filtro en Logcat: tag:OkHttp
```

Verás:
```
--> POST https://authn.sta.masstack.com/v1/oauth/token
Content-Type: application/x-www-form-urlencoded
Content-Length: 245

grant_type=authorization_code&code=abc123...

<-- 200 OK (247ms)
Content-Type: application/json

{"access_token":"eyJ...", "expires_in":3600}
```

#### Añadir Breakpoint en Repository:

```kotlin
// AuthRepository.kt
suspend fun exchangeCodeForToken(...): Result<TokenResponse> {
    return try {
        val response = authApi.exchangeCodeForToken(...)  // ← Breakpoint

        if (response.isSuccessful && response.body() != null) {  // ← Inspecciona response
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception(parseError(response)))
        }
    } catch (e: Exception) {  // ← Breakpoint para errores
        Result.failure(e)
    }
}
```

**Variables a inspeccionar**:
- `response.code()` - Código HTTP (200, 400, 500...)
- `response.message()` - Mensaje HTTP
- `response.body()` - El TokenResponse
- `response.errorBody()?.string()` - Mensaje de error del servidor

---

### Paso 9: Debug de Compose UI

Para debuggear problemas de UI en Jetpack Compose:

#### Layout Inspector (Inspector de Layout):

1. Ejecuta la app en modo debug
2. Ve a **View > Tool Windows > Layout Inspector**
3. Click en **"Live Updates"** para ver cambios en tiempo real
4. Selecciona elementos de la UI para ver sus propiedades

#### Compose Recomposition Highlighting:

Añade esta configuración en `MainActivity.kt`:

```kotlin
@Composable
fun MainScreen(...) {
    // Ver cuándo se recompone la UI
    SideEffect {
        Log.d("Compose", "MainScreen recomposed")
    }

    // Tu código UI...
}
```

#### Debug de Estados:

```kotlin
@Composable
fun SettingsScreen() {
    var clientId by remember { mutableStateOf("") }

    // Añade logs para ver cambios de estado
    LaunchedEffect(clientId) {
        Log.d("SettingsScreen", "ClientId changed: $clientId")
    }

    TextField(
        value = clientId,
        onValueChange = {
            clientId = it  // ← Breakpoint aquí
        }
    )
}
```

---

### Paso 10: Trucos Avanzados de Debugging

#### 1. Conditional Breakpoints (Breakpoints Condicionales)

Solo detener cuando se cumple una condición:

1. Click derecho en el breakpoint (círculo rojo)
2. En **"Condition"**, escribe: `username == "admin"`
3. Solo se detendrá si username es "admin"

#### 2. Temporary Breakpoints (Breakpoints Temporales)

Se eliminan automáticamente después de activarse una vez:

- `Cmd+Alt+Shift+F8` (Mac) o `Ctrl+Alt+Shift+F8` (Windows/Linux)
- O click derecho en línea > "Run to Cursor"

#### 3. Evaluate and Log (Sin Detener la Ejecución)

En lugar de detener:

1. Click derecho en breakpoint
2. Desmarca "Suspend"
3. Activa "Evaluate and log"
4. Escribe expresión: `"ClientId: ${settings.clientId}"`
5. Los logs aparecerán en consola sin detener la app

#### 4. Method Breakpoints

Para detener al entrar o salir de una función:

1. Click en el margen junto al nombre de la función
2. Aparecerá un rombo rojo ♦️
3. Click derecho > Configurar para entrar/salir

#### 5. Field Watchpoints

Detener cuando un campo cambia de valor:

1. Click en el margen junto a una propiedad
2. Se detendrá cuando el valor cambie

```kotlin
class SettingsRepository {
    private var cachedSettings: Settings? = null  // ← Watchpoint aquí
}
```

---

### Paso 11: Memory Profiler (Detector de Memory Leaks)

Si la app consume mucha memoria:

1. Ve a **View > Tool Windows > Profiler**
2. Click en `+` y selecciona tu dispositivo
3. Click en **"MEMORY"**
4. Interactúa con tu app
5. Click en **"Dump Java Heap"**
6. Busca objetos que no deberían estar en memoria

---

### Paso 12: Database Inspector (Para EncryptedSharedPreferences)

Android Studio permite inspeccionar SharedPreferences:

1. Ejecuta la app en modo debug
2. Ve a **View > Tool Windows > App Inspection**
3. Selecciona la pestaña **"Database Inspector"**
4. Verás los datos guardados (si usas Room Database)

**Nota**: EncryptedSharedPreferences no es directamente visible, pero puedes verificar el archivo:

```bash
adb shell
run-as com.masstack.authn
cd shared_prefs
cat encrypted_prefs.xml
```

---

### 🎯 Resumen de Teclas Rápidas

| Acción | Mac | Windows/Linux |
|--------|-----|---------------|
| Debug | `Cmd + D` | `Ctrl + D` |
| Resume | `F9` | `F9` |
| Step Over | `F8` | `F8` |
| Step Into | `F7` | `F7` |
| Step Out | `Shift + F8` | `Shift + F8` |
| Evaluate Expression | `Alt + F8` | `Alt + F8` |
| Toggle Breakpoint | `Cmd + F8` | `Ctrl + F8` |
| View Breakpoints | `Cmd + Shift + F8` | `Ctrl + Shift + F8` |

---

### 📝 Checklist de Debugging

Cuando encuentres un bug:

- [ ] Reproduce el error consistentemente
- [ ] Identifica la funcionalidad afectada
- [ ] Añade breakpoints en puntos clave
- [ ] Ejecuta en modo debug
- [ ] Inspecciona variables paso a paso
- [ ] Verifica logs en Logcat
- [ ] Verifica network requests si aplica
- [ ] Anota el estado de las variables cuando falla
- [ ] Corrige el código
- [ ] Verifica que el fix funciona
- [ ] Elimina los breakpoints temporales

---

### 🔍 Debugging Específico para OAuth2

#### Verificar PKCE:

```kotlin
// Añade logs en PKCEUtil.kt
fun generateCodeVerifier(): String {
    val verifier = base64UrlEncode(bytes)
    Log.d("PKCE", "Code Verifier: $verifier (length: ${verifier.length})")
    return verifier
}

fun generateCodeChallenge(codeVerifier: String): String {
    val challenge = base64UrlEncode(digest)
    Log.d("PKCE", "Code Challenge: $challenge")
    Log.d("PKCE", "Verifier -> Challenge mapping stored")
    return challenge
}
```

#### Verificar OAuth Callbacks:

```kotlin
// AuthorizationCodeActivity.kt
override fun onNewIntent(intent: Intent?) {
    Log.d("OAuth", "onNewIntent called")
    Log.d("OAuth", "Intent data: ${intent?.data}")

    intent?.data?.let { uri ->
        Log.d("OAuth", "Full URI: $uri")
        Log.d("OAuth", "Code: ${uri.getQueryParameter("code")}")
        Log.d("OAuth", "State: ${uri.getQueryParameter("state")}")
        Log.d("OAuth", "Error: ${uri.getQueryParameter("error")}")
    }
}
```

#### Verificar Token Exchange:

```kotlin
// AuthRepository.kt
suspend fun exchangeCodeForToken(...) {
    Log.d("OAuth", "Exchanging code for token")
    Log.d("OAuth", "Code: ${code.take(10)}...")
    Log.d("OAuth", "Verifier: ${codeVerifier.take(10)}...")

    val response = authApi.exchangeCodeForToken(...)

    Log.d("OAuth", "Response code: ${response.code()}")
    if (response.isSuccessful) {
        Log.d("OAuth", "Token received: ${response.body()?.accessToken?.take(20)}...")
    } else {
        Log.e("OAuth", "Error: ${response.errorBody()?.string()}")
    }
}
```

---

**¡Ahora estás listo para debuggear como un profesional! 🐛🔍**

---

## 7. Configurar Certificado de Firma

Tu certificado está en: `/path/to/your/keystore.jks`

### Paso 1: Crear archivo de configuración de firma

Crea un archivo llamado `keystore.properties` en la raíz del proyecto:

```bash
cd <project-root>
touch keystore.properties
```

Abre el archivo y añade:

```properties
storePassword=TU_PASSWORD_DEL_KEYSTORE
keyPassword=TU_PASSWORD_DE_LA_KEY
keyAlias=TU_ALIAS
storeFile=/path/to/your/keystore.jks
```

**⚠️ IMPORTANTE**:
- Reemplaza `TU_PASSWORD_DEL_KEYSTORE` con tu contraseña del keystore
- Reemplaza `TU_PASSWORD_DE_LA_KEY` con tu contraseña de la key (puede ser la misma)
- Reemplaza `TU_ALIAS` con el alias de tu certificado
- **NO subas este archivo a git** (ya está en `.gitignore`)

### Paso 2: Configurar build.gradle para usar el certificado

Abre `app/build.gradle` y añade ANTES de `android {`:

```gradle
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}
```

Dentro del bloque `android {`, añade DESPUÉS de `buildTypes {`:

```gradle
android {
    // ... código existente ...

    signingConfigs {
        release {
            if (keystorePropertiesFile.exists()) {
                keyAlias keystoreProperties['keyAlias']
                keyPassword keystoreProperties['keyPassword']
                storeFile file(keystoreProperties['storeFile'])
                storePassword keystoreProperties['storePassword']
            }
        }
    }

    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release  // ← Añade esta línea
        }

        debug {
            applicationIdSuffix ".debug"
            debuggable true
        }
    }

    // ... resto del código ...
}
```

### Paso 3: Verificar el Certificado

Puedes verificar la información de tu certificado con:

```bash
keytool -list -v -keystore /path/to/your/keystore.jks -alias TU_ALIAS
```

Esto mostrará:
- Fecha de creación
- Fecha de expiración
- Algoritmo de firma (debería ser SHA256withRSA)
- Información del propietario

---

## 8. Generar APK para Distribución

### Opción A: APK Firmado (para instalar manualmente)

#### Desde Terminal:

```bash
./gradlew assembleRelease
```

El APK firmado se generará en:
```
app/build/outputs/apk/release/app-release.apk
```

#### Desde Android Studio:

1. Ve a **Build > Generate Signed Bundle / APK**
2. Selecciona **APK**
3. Click **"Next"**
4. **Key store path**: `/path/to/your/keystore.jks`
5. Introduce tu **Key store password**
6. Introduce tu **Key alias**
7. Introduce tu **Key password**
8. Click **"Next"**
9. Selecciona **release** build variant
10. Marca **V1 (Jar Signature)** y **V2 (Full APK Signature)**
11. Click **"Finish"**

### Opción B: Android App Bundle (AAB) - Para Google Play

El formato AAB es **obligatorio** para publicar en Google Play Store.

#### Desde Terminal:

```bash
./gradlew bundleRelease
```

El AAB se generará en:
```
app/build/outputs/bundle/release/app-release.aab
```

#### Desde Android Studio:

1. Ve a **Build > Generate Signed Bundle / APK**
2. Selecciona **Android App Bundle**
3. Sigue los mismos pasos que para APK (desde el paso 3)

### 📦 Diferencias APK vs AAB

| Característica | APK | AAB |
|----------------|-----|-----|
| Tamaño | Más grande | Más pequeño |
| Google Play | ❌ (deprecated) | ✅ Obligatorio |
| Instalación manual | ✅ | ❌ |
| Optimización | Manual | Automática por Google |

---

## 9. Publicar en Google Play Store

### Requisitos Previos

1. **Cuenta de Google Play Console**
   - Costo: $25 USD (pago único)
   - Registro: https://play.google.com/console

2. **Información Requerida**:
   - Nombre de la aplicación
   - Descripción corta (80 caracteres)
   - Descripción completa (4000 caracteres)
   - Capturas de pantalla (mínimo 2)
   - Icono de alta resolución (512x512 px)
   - Gráfico de encabezado (1024x500 px)
   - Categoría de la app
   - Política de privacidad (URL)

### Paso 1: Crear la Aplicación en Google Play Console

1. Ve a https://play.google.com/console
2. Click en **"Crear aplicación"**
3. Completa:
   - **Nombre**: "OAuth2 Flow Tester" (o el que prefieras)
   - **Idioma predeterminado**: Español
   - **Tipo**: Aplicación o juego
   - **Gratis o de pago**: Gratis
4. Acepta las políticas
5. Click **"Crear aplicación"**

### Paso 2: Completar la Ficha de la Tienda

#### 2.1 Detalles de la Aplicación

Ve a **Presencia en Play Store > Ficha de la tienda principal**:

- **Nombre de la app**: OAuth2 Flow Tester
- **Descripción corta**:
  ```
  Prueba flujos OAuth2: Authorization Code, WebAuthn y CIBA
  ```
- **Descripción completa**:
  ```
  OAuth2 Flow Tester es una aplicación de desarrollo que permite probar
  diferentes flujos de autenticación OAuth2:

  ✓ Authorization Code con PKCE
  ✓ WebAuthn (autenticación biométrica)
  ✓ CIBA (Client Initiated Backchannel Authentication)

  Características:
  - Configuración flexible de servidor OAuth2
  - Almacenamiento seguro de tokens
  - Soporte para biometría (huella digital, reconocimiento facial)
  - Interfaz intuitiva con Material Design 3

  Ideal para desarrolladores que necesitan probar integraciones OAuth2.
  ```

#### 2.2 Assets Gráficos

**Icono de la aplicación** (512x512 px):
- Formato: PNG de 32 bits
- Sin transparencia
- Circular o cuadrado redondeado

**Gráfico de encabezado** (1024x500 px):
- Formato: PNG o JPEG
- Sin transparencia

**Capturas de pantalla** (mínimo 2, máximo 8):
- Teléfono: 16:9 aspect ratio
- Recomendado: 1080x1920 px
- Puedes tomarlas desde el emulador o dispositivo real

**Cómo tomar capturas con Android Studio**:
1. Ejecuta la app en emulador
2. Click en el icono de cámara 📷 en el panel del emulador
3. Se guardarán automáticamente

#### 2.3 Categorización

- **Categoría**: Herramientas
- **Tags**: OAuth2, Desarrollo, Testing

### Paso 3: Clasificación del Contenido

1. Ve a **Políticas > Clasificación de contenido**
2. Click **"Iniciar cuestionario"**
3. Selecciona:
   - Categoría: **Utilidades y productividad**
   - Completa el cuestionario (todas las respuestas deberían ser "No" si solo prueba OAuth2)
4. Guarda y aplica la clasificación

### Paso 4: Audiencia y Contenido

1. **Público objetivo**:
   - Ve a **Políticas > Público objetivo y contenido**
   - Selecciona: **Solo adultos** o **13 años o más**
   - La app NO está diseñada para niños

2. **Declaración de privacidad**:
   - Necesitas una URL con tu política de privacidad
   - Puedes usar GitHub Pages o crear una simple

### Paso 5: Subir el AAB

1. Ve a **Producción > Versiones de producción**
2. Click en **"Crear nueva versión"**
3. Arrastra y suelta tu archivo `app-release.aab`
4. Completa:
   - **Nombre de la versión**: 1.0 (debe coincidir con `versionName` en `build.gradle`)
   - **Notas de la versión** (español):
     ```
     Primera versión:
     - Soporte para Authorization Code con PKCE
     - Soporte para WebAuthn
     - Soporte para CIBA
     - Almacenamiento seguro de tokens
     - Interfaz Material Design 3
     ```

5. Click **"Guardar"**

### Paso 6: Revisión y Publicación

1. Ve al **Panel de control**
2. Revisa todas las secciones con ✅
3. Una vez completadas todas, verás el botón **"Enviar a revisión"**
4. Click en **"Enviar a revisión"**

### ⏱️ Proceso de Revisión

- **Tiempo**: 1-7 días (normalmente 1-2 días)
- **Estado**: Puedes verlo en el Panel de control
- **Notificación**: Recibirás un email cuando se apruebe

### 🔄 Actualizaciones Futuras

Para publicar una nueva versión:

1. Incrementa `versionCode` y `versionName` en `app/build.gradle`:
   ```gradle
   versionCode 2        // incrementa en 1
   versionName "1.1"    // nueva versión
   ```

2. Genera nuevo AAB:
   ```bash
   ./gradlew bundleRelease
   ```

3. Ve a **Producción > Versiones de producción > Crear nueva versión**
4. Sube el nuevo AAB y añade notas de la versión

---

## 10. Instalar APK Manualmente (Sin Google Play)

### En el Mismo Dispositivo donde Compilaste

Si generaste el APK en tu ordenador:

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### Compartir APK a Otros Usuarios

1. **Envía el APK** por email, Drive, Dropbox, etc.
2. **En el móvil del usuario**:
   - Descarga el APK
   - Abre el archivo
   - Android preguntará: _"¿Instalar aplicación desconocida?"_
   - Ve a **Configuración** y permite **"Instalar aplicaciones desconocidas"** para esa fuente
   - Vuelve e instala

### 📲 Distribución Beta con Firebase App Distribution

Para distribución interna (testers):

1. Ve a https://console.firebase.google.com
2. Crea un proyecto o selecciona uno existente
3. Ve a **App Distribution**
4. Sube tu APK
5. Añade emails de testers
6. Los testers recibirán un link para descargar

---

## 11. Comandos Útiles de Gradle

### Limpiar el Proyecto

```bash
./gradlew clean
```

### Ver Todas las Tareas Disponibles

```bash
./gradlew tasks
```

### Ejecutar Tests

```bash
./gradlew test
```

### Ver Dependencias

```bash
./gradlew app:dependencies
```

### Compilar Todo (Debug + Release)

```bash
./gradlew assemble
```

### Info del Build

```bash
./gradlew assembleRelease --info
```

---

## 12. Troubleshooting

### Problema: "Installed Build Tools revision X.X.X is corrupted"

**Solución**:
```bash
cd ~/Library/Android/sdk/build-tools/X.X.X
mv dx dx_backup
mv lib/dx.jar .
cat dx_backup | sed 's/^eval set.*/eval set -- "$javaOpts" "-jar  $(dirname "$0")\/dx.jar" "$@"/' > dx
chmod +x dx
```

### Problema: "Execution failed for task ':app:lintVitalRelease'"

**Solución**: Añade en `app/build.gradle`:
```gradle
android {
    lintOptions {
        checkReleaseBuilds false
        abortOnError false
    }
}
```

### Problema: Error de firma "keystore was tampered with"

**Solución**: Verifica tu contraseña del keystore y que el archivo `.jks` no esté corrupto

### Problema: "Manifest merger failed"

**Solución**: Añade en `AndroidManifest.xml`:
```xml
<application
    tools:replace="android:icon,android:label"
    ...>
```

Y en la parte superior:
```xml
<manifest xmlns:tools="http://schemas.android.com/tools">
```

---

## 13. Checklist Final Antes de Publicar

### Código

- [ ] Tests pasan correctamente
- [ ] No hay warnings críticos
- [ ] Versión actualizada en `build.gradle`
- [ ] ProGuard configurado (ya incluido)
- [ ] Certificado configurado correctamente

### Recursos

- [ ] Iconos de alta resolución
- [ ] Todas las strings traducidas
- [ ] Capturas de pantalla actualizadas
- [ ] Política de privacidad creada

### Google Play

- [ ] Ficha de la tienda completa
- [ ] Clasificación de contenido realizada
- [ ] AAB firmado generado
- [ ] Notas de la versión escritas

### Legal

- [ ] Política de privacidad publicada
- [ ] Términos de servicio (si aplica)
- [ ] Permisos justificados en la descripción

---

## 📚 Recursos Adicionales

- **Documentación oficial de Android**: https://developer.android.com/docs
- **Google Play Console ayuda**: https://support.google.com/googleplay/android-developer
- **Firebase App Distribution**: https://firebase.google.com/docs/app-distribution
- **Material Design 3**: https://m3.material.io

---

## 🆘 Soporte

Si encuentras problemas:

1. Verifica los logs en Android Studio: **View > Tool Windows > Logcat**
2. Busca el error específico en Stack Overflow
3. Revisa la documentación oficial de Android

---

**¡Buena suerte con tu aplicación OAuth2 Testing! 🚀**
