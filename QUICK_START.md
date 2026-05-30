# 🚀 Quick Start Guide

## Compilar el Proyecto

### 1. Abrir en Android Studio
```bash
cd <project-root>
# Abrir este directorio en Android Studio
```

### 2. Sync Gradle
Android Studio hará sync automático. Si no:
- Click en "Sync Project with Gradle Files" (icono de elefante)

### 3. Añadir Iconos (Opcional)
El proyecto necesita iconos en:
```
app/src/main/res/mipmap-*/
```

**Opción rápida**: Android Studio puede generarlos automáticamente:
- Right-click en `res` → New → Image Asset
- Seleccionar tipo "Launcher Icons"
- Usar imagen por defecto o personalizada

### 4. Compilar
```bash
./gradlew assembleDebug
```

O desde Android Studio:
- Build → Make Project (Ctrl+F9)

### 5. Instalar en Dispositivo/Emulador
```bash
./gradlew installDebug
```

O desde Android Studio:
- Run → Run 'app' (Shift+F10)

---

## Configuración Inicial en la App

### Primera Vez:
1. Abrir app
2. Click en icono Settings (⚙️)
3. Configurar Global Settings:
   - **OpenID Configuration URL**: `https://authn.sta.masstack.com/v1/.well-known/openid-configuration` (por defecto)
   - Pulsar **"Discover Endpoints"** para auto-descubrir todos los endpoints
   - **Scope**: `openid profile api:everything`
   - **Username**: tu usuario de prueba
4. Configurar credenciales por flujo:
   - **Client ID**: (solicitar al equipo de Authn)
   - **Client Secret**: (opcional, según el client)
5. Save Settings

### Registrar Redirect URI en Authn:
Antes de usar, asegúrate de que este redirect URI está registrado en tu client de Authn:
```
com.masstack.authn://oauth/callback
```

---

## Probar Flujos

### Authorization Code (más sencillo para empezar):
1. Main → Authorization Code
2. Start Authorization
3. Login en navegador
4. Ver tokens en la app

### WebAuthn:
1. Main → WebAuthn
2. **Registro** (primera vez):
   - Enter username
   - Register New Credential
   - Usar huella/face
3. **Autenticación** (después del registro):
   - Enter mismo username
   - Authenticate with Credential
   - Usar huella/face

### CIBA:
1. Main → CIBA
2. Enter username
3. (Opcional) Binding message
4. Start CIBA Flow
5. Aprobar en dispositivo secundario
6. App hace polling automático

---

## Troubleshooting

### Error de compilación por iconos:
Añadir iconos o usar:
```xml
<!-- En AndroidManifest.xml, temporalmente: -->
android:icon="@android:drawable/sym_def_app_icon"
android:roundIcon="@android:drawable/sym_def_app_icon"
```

### Error de dependencias:
```bash
./gradlew --refresh-dependencies
```

### Error de Hilt:
Rebuild:
```bash
./gradlew clean build
```

### Deep link no funciona:
Verificar en `adb`:
```bash
adb shell am start -a android.intent.action.VIEW -d "com.masstack.authn://oauth/callback?code=test&state=test"
```

---

## Logs y Debugging

### Ver logs de red:
Los logs de Retrofit aparecen en Logcat con tag `OkHttp`

### Ver estado de CIBA:
Los estados del polling aparecen en la UI en tiempo real

### Debug WebAuthn:
Asegurar que el dispositivo tiene biometría configurada

---

## Estructura Rápida

```
android-authn/
├── app/
│   ├── src/main/
│   │   ├── java/com/masstack/authn/
│   │   │   ├── ui/              # Activities (MainActivity, Settings, flows)
│   │   │   ├── services/        # OAuth services
│   │   │   ├── data/            # Models + Repositories
│   │   │   ├── network/         # API + Interceptors
│   │   │   ├── utils/           # PKCE, Constants, Extensions
│   │   │   └── di/              # Hilt modules
│   │   └── res/                 # UI resources
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

---

## URLs Importantes

- **Documentación Authn**: Ver archivo leído al inicio
- **Material Design 3**: https://m3.material.io/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **PKCE Spec**: https://tools.ietf.org/html/rfc7636
- **WebAuthn Spec**: https://www.w3.org/TR/webauthn/
- **CIBA Spec**: https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html

---

## Checklist de Producción

Antes de usar en producción:

- [ ] Iconos profesionales añadidos
- [ ] Client credentials de producción
- [ ] Redirect URI registrada
- [ ] ProGuard habilitado
- [ ] Certificado de firma configurado
- [ ] Tests ejecutados
- [ ] Error handling verificado
- [ ] Permisos mínimos necesarios

---

¡Listo para probar! 🎉
