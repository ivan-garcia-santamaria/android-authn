# 📊 Guía de Logging - Android OAuth2 Testing App

**Fecha**: 30 de Octubre de 2025

---

## 🎯 Objetivo

Esta guía te enseñará cómo ver y filtrar los logs de la aplicación en Android Studio para debug y troubleshooting.

---

## 📝 ¿Qué es Logcat?

**Logcat** es la herramienta de Android Studio que muestra todos los logs del sistema Android y de tu aplicación en tiempo real.

### Tipos de Logs Disponibles:

| Nivel | Color | Uso | Ejemplo |
|-------|-------|-----|---------|
| **Verbose (V)** | Gris | Detalles muy específicos | Parámetros de funciones |
| **Debug (D)** | Azul | Información de debugging | "Token URL: https://..." |
| **Info (I)** | Verde | Información general | "Token exchange successful" |
| **Warn (W)** | Naranja | Advertencias no críticas | "Token expiring soon" |
| **Error (E)** | Rojo | Errores | "Failed to exchange code" |
| **Assert (A)** | Morado | Fallos críticos | Crashes |

---

## 🚀 Cómo Abrir Logcat en Android Studio

### Opción 1: Desde el Menú

1. En Android Studio, ve al menú superior
2. Click en **View** → **Tool Windows** → **Logcat**

### Opción 2: Desde la Barra Inferior

1. Mira la barra inferior de Android Studio
2. Click en la pestaña **Logcat** (normalmente al lado de "Build", "Terminal", etc.)

### Opción 3: Atajo de Teclado

- **Mac**: `Cmd + 6`
- **Windows/Linux**: `Alt + 6`

---

## 🎨 Interfaz de Logcat

Cuando abras Logcat, verás esta interfaz:

```
┌─────────────────────────────────────────────────────────────┐
│ [Dispositivo▼] [Filter: No Filters▼] [🔍 Buscar...]        │
├─────────────────────────────────────────────────────────────┤
│ 2025-10-30 23:45:12.345 12345-12345 I/AuthCodeService:     │
│     Starting Authorization Code flow                        │
│ 2025-10-30 23:45:12.456 12345-12345 D/AuthCodeService:     │
│     Authorize URL: https://authn.sta.masstack.com/v1/...    │
│ 2025-10-30 23:45:12.567 12345-12345 E/AuthRepository:      │
│     Token exchange failed: invalid_grant                    │
└─────────────────────────────────────────────────────────────┘
```

### Componentes:

1. **Selector de dispositivo**: Elige el dispositivo/emulador
2. **Filtro**: Filtra logs por nivel o criterio
3. **Búsqueda**: Busca texto específico
4. **Logs**: Área principal con los logs

---

## 🔍 Filtrar Logs de la Aplicación OAuth2

### Filtro 1: Por Paquete (Recomendado)

Muestra **solo los logs de tu aplicación**, ignorando todo el ruido del sistema Android.

#### Pasos:

1. En Logcat, busca el dropdown **"Filter:"** (arriba a la izquierda)
2. Click en el dropdown → **"Edit Filter Configuration"**
3. En la ventana que aparece:
   - **Filter Name**: `OAuth App`
   - **Package Name**: `com.masstack.authn`
   - Click **OK**

4. Ahora selecciona **"OAuth App"** en el dropdown de filtros

**Resultado**: Solo verás logs de tu aplicación OAuth2.

---

### Filtro 2: Por Tag (Más Específico)

Filtra por componente específico de la app.

#### Tags Disponibles en la App:

| Tag | Componente | Qué muestra |
|-----|-----------|-------------|
| `AuthCodeService` | Authorization Code Flow | PKCE, URLs, callbacks |
| `AuthRepository` | Repositorio OAuth | Llamadas API, tokens |
| `CibaService` | CIBA Flow | Polling, estados |
| `WebAuthnService` | WebAuthn Flow | Biometría, registro |
| `SettingsRepository` | Configuración | Guardar/cargar settings |
| `MainActivity` | Pantalla principal | Navegación, eventos |

#### Pasos para filtrar por Tag:

1. En Logcat, en el campo de búsqueda escribe:
   ```
   tag:AuthRepository
   ```

2. O usa el filtro regex:
   ```
   tag:Auth.*
   ```
   Esto mostrará todos los tags que empiezan con "Auth"

---

### Filtro 3: Por Nivel de Log

Filtra por nivel de severidad.

#### Pasos:

1. En la barra de Logcat, encuentra los botones de nivel:
   ```
   [V] [D] [I] [W] [E] [A]
   ```

2. Click en el nivel que quieras ver:
   - **[E]**: Solo errores (rojo)
   - **[W]**: Warnings y errores (naranja + rojo)
   - **[I]**: Info, warnings y errores (verde + naranja + rojo)
   - **[D]**: Debug y superiores (azul + verde + naranja + rojo)

**Recomendación**: Para debugging, usa **[D]** o **[I]**.

---

## 🎯 Filtros Útiles para OAuth2

### Ver Solo Errores de OAuth:

```
package:com.masstack.authn level:error
```

### Ver Authorization Code Flow Completo:

```
tag:AuthCodeService|tag:AuthRepository
```

### Ver Todas las Llamadas a la API:

```
package:com.masstack.authn "response code"
```

### Ver Problemas de Token:

```
package:com.masstack.authn token
```

### Ver Configuración:

```
tag:SettingsRepository
```

---

## 📖 Ejemplo: Debugging Authorization Code Flow

### Escenario:
El flujo Authorization Code falla al intercambiar el código por el token.

### Paso 1: Ejecutar la App

1. Ejecuta la app en debug: **Run** → **Debug 'app'** (🐞)
2. Navega a la pantalla principal
3. Click en **"Authorization Code Flow"**

### Paso 2: Filtrar Logs

En Logcat, usa el filtro:
```
package:com.masstack.authn
```

### Paso 3: Analizar los Logs

Buscarás esta secuencia de logs:

```
D/AuthCodeService: Starting Authorization Code flow
D/AuthCodeService: Configuration validated successfully
D/AuthCodeService: Authorize URL: https://authn.sta.masstack.com/v1/oauth/authorize
D/AuthCodeService: Token URL: https://authn.sta.masstack.com/v1/oauth/token
D/AuthCodeService: Redirect URI: com.masstack.authn://oauth/callback
D/AuthCodeService: PKCE pair generated - Challenge: E9Melhoa2OwvFrEMTJg...
D/AuthCodeService: State generated: 8f3a2b1c-4d5e-6f7g-8h9i-0j1k2l3m4n5o
I/AuthCodeService: Opening authorization URL: https://...

# Usuario autentica en el navegador...

D/AuthCodeService: Handling OAuth callback
D/AuthCodeService: Callback URI: com.masstack.authn://oauth/callback?code=ABC123...
D/AuthCodeService: Authorization code received: ABC123...
D/AuthCodeService: State validated successfully
D/AuthCodeService: Code verifier available: dBjftJeZ...
I/AuthCodeService: Exchanging authorization code for tokens...

D/AuthRepository: Exchanging authorization code for tokens
D/AuthRepository: Token endpoint: https://authn.sta.masstack.com/v1/oauth/token
D/AuthRepository: Client ID: mobile-app-client
D/AuthRepository: Redirect URI: com.masstack.authn://oauth/callback
D/AuthRepository: Code: ABC123...
D/AuthRepository: Using PKCE code verifier
D/AuthRepository: Token exchange response code: 200
I/AuthRepository: Token exchange successful
D/AuthRepository: Access token received: eyJhbGciOiJSUzI1NiIs...
D/AuthRepository: Token expires in: 3600 seconds
D/AuthRepository: Refresh token: present
I/AuthCodeService: Successfully obtained access token
```

### Paso 4: Identificar Problemas

Si hay un error, verás algo como:

```
E/AuthRepository: Token exchange failed: invalid_grant
E/AuthRepository: Response code: 400
E/AuthRepository: Response message: Bad Request
E/AuthCodeService: Failed to exchange code for token: invalid_grant
```

**Problema identificado**: El servidor rechazó el código. Posibles causas:
- Código ya usado
- Code verifier incorrecto
- Redirect URI no coincide
- Código expirado

---

## 🔧 Características Avanzadas de Logcat

### 1. Búsqueda en Tiempo Real

En el campo de búsqueda (🔍), escribe cualquier texto:
- `token`
- `error`
- `https://`
- `ABC123`

Logcat resaltará todas las líneas que coincidan.

### 2. Clear Logcat

Limpia todos los logs para empezar fresh:
- Click en el icono **🗑️** (Trash) en la barra de Logcat
- O atajo: `Cmd + K` (Mac) / `Ctrl + K` (Windows/Linux)

### 3. Pausar Logs

Pausa el scroll automático para leer logs antiguos:
- Click en el icono **⏸️** (Pause) en la barra de Logcat
- Click nuevamente para reanudar

### 4. Wrap Lines

Activa el wrap para ver líneas largas completas:
- Click en el icono **📄** (Wrap lines) en la barra de Logcat
- Útil para ver URLs completas

### 5. Copiar Logs

1. Selecciona las líneas de log que quieras copiar
2. Click derecho → **Copy**
3. Pega en un archivo de texto para análisis

### 6. Guardar Logs

1. Click derecho en Logcat
2. **Save As...**
3. Guarda el archivo `.txt` con todos los logs

---

## 📊 Logs Específicos por Flujo OAuth

### Authorization Code Flow

**Tags**: `AuthCodeService`, `AuthRepository`

**Logs clave**:
```
D/AuthCodeService: Starting Authorization Code flow
D/AuthCodeService: PKCE pair generated
I/AuthCodeService: Opening authorization URL
D/AuthCodeService: Handling OAuth callback
I/AuthRepository: Token exchange successful
```

**Errores comunes**:
```
E/AuthCodeService: Authorization Code flow is not properly configured
E/AuthCodeService: State mismatch - potential CSRF attack
E/AuthRepository: Token exchange failed: invalid_grant
```

---

### CIBA Flow

**Tags**: `CibaService`, `AuthRepository`

**Logs clave**:
```
D/AuthRepository: Initiating CIBA backchannel authorization
I/AuthRepository: CIBA authorization initiated successfully
D/AuthRepository: Auth request ID: brGkDY0KA9V...
D/AuthRepository: Polling interval: 5 seconds
D/CibaService: Polling attempt 1/120
I/AuthRepository: CIBA token obtained
```

**Errores comunes**:
```
E/AuthRepository: CIBA authorization failed: access_denied
E/CibaService: Maximum polling attempts reached
```

---

### WebAuthn Flow

**Tags**: `WebAuthnService`, `AuthRepository`

**Logs clave**:
```
D/AuthRepository: WebAuthn registration begin
D/WebAuthnService: Challenge received
D/WebAuthnService: Creating credential with Credential Manager
I/AuthRepository: WebAuthn registration complete
D/AuthRepository: Access token received
```

**Errores comunes**:
```
E/AuthRepository: WebAuthn registration failed: invalid_request
E/WebAuthnService: Biometric authentication failed
```

---

## 💡 Tips y Trucos

### Tip 1: Usa Regex para Filtros Avanzados

Filtra múltiples tags:
```
tag:Auth.*|Settings.*
```

Busca URLs específicas:
```
https://.*authn.*
```

### Tip 2: Combina Filtros

```
package:com.masstack.authn level:error|warn
```

### Tip 3: Busca por Código HTTP

```
"response code: 400"
"response code: 401"
"response code: 500"
```

### Tip 4: Monitorea Tokens

```
"Access token" | "Refresh token"
```

### Tip 5: Debugging de Red

El OkHttp logging interceptor ya está configurado en la app. Verás logs detallados de HTTP:

```
tag:OkHttp
```

Mostrará:
```
--> POST https://authn.sta.masstack.com/v1/oauth/token
Content-Type: application/x-www-form-urlencoded
Content-Length: 245

grant_type=authorization_code&code=ABC123...

<-- 200 OK (247ms)
Content-Type: application/json

{"access_token":"eyJ...","expires_in":3600}
```

---

## 🎓 Ejercicio Práctico

### Ejercicio 1: Identificar un Error de Configuración

1. Abre la app
2. NO configures nada en Settings
3. Intenta ejecutar Authorization Code Flow
4. Observa Logcat con filtro: `tag:AuthCodeService`

**Pregunta**: ¿Qué error ves?

**Respuesta esperada**:
```
E/AuthCodeService: Authorization Code flow is not properly configured
```

---

### Ejercicio 2: Seguir un Flujo Exitoso

1. Configura la app correctamente
2. Ejecuta Authorization Code Flow
3. Usa filtro: `package:com.masstack.authn level:info`

**Pregunta**: ¿Cuántos logs de nivel INFO ves?

**Respuesta esperada**: Al menos 3-4 logs INFO durante el flujo.

---

## 📚 Resumen de Comandos

| Acción | Comando/Atajo |
|--------|---------------|
| Abrir Logcat | `Cmd/Ctrl + 6` |
| Limpiar logs | `Cmd/Ctrl + K` |
| Buscar | `Cmd/Ctrl + F` |
| Filtrar por paquete | `package:com.masstack.authn` |
| Filtrar por tag | `tag:AuthRepository` |
| Filtrar por nivel | Click en `[E]`, `[W]`, `[I]`, `[D]` |
| Copiar logs | Click derecho → Copy |
| Pausar scroll | Click en ⏸️ |

---

## 🆘 Troubleshooting Logcat

### Problema 1: No veo ningún log

**Solución**:
1. Asegúrate de que el dispositivo/emulador esté seleccionado en el dropdown
2. Verifica que la app esté ejecutándose
3. Quita todos los filtros (selecciona "No Filters")

### Problema 2: Demasiados logs (ruido del sistema)

**Solución**:
Usa el filtro por paquete: `package:com.masstack.authn`

### Problema 3: Los logs desaparecen muy rápido

**Solución**:
1. Click en ⏸️ para pausar
2. O aumenta el buffer de Logcat:
   - Settings → Logcat → Logcat buffer size → 1MB

---

## ✅ Checklist de Debugging

Cuando encuentres un problema:

- [ ] Abre Logcat
- [ ] Filtra por `package:com.masstack.authn`
- [ ] Reproduce el error
- [ ] Busca logs con nivel **ERROR** (rojo)
- [ ] Lee el contexto (logs anteriores y posteriores)
- [ ] Copia los logs relevantes
- [ ] Identifica el componente (tag)
- [ ] Verifica la configuración en logs **DEBUG**
- [ ] Busca códigos HTTP si es error de red

---

**¡Ahora estás listo para debuggear como un profesional! 🐛🔍**

---

_Generado el 30 de Octubre de 2025_
