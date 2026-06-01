# Runner

App Android que programa **alarmas** y ejecuta tu **JavaScript** o **TypeScript** de forma nativa con QuickJS.

## Cómo funciona

1. **Subes un script** (.js o .ts) desde la pestaña Scripts.
2. **Creas una alarma** con hora y script asociado.
3. Cuando llega la hora, Android dispara la alarma (como un despertador del sistema).
4. Runner verifica **internet** y, si hay conexión, ejecuta el script con QuickJS.
5. El resultado queda en **Historial**.

## Restricciones (por diseño)

| Condición | Comportamiento |
|-----------|----------------|
| Teléfono **apagado** | La alarma no se dispara. Al encender, se reprograman alarmas futuras. |
| **Sin internet** | El script **no se ejecuta** (se registra en historial como cancelado). |
| App cerrada | Sí funciona — usa `AlarmManager` del sistema, no requiere la app abierta. |

## TypeScript

Los archivos `.ts` se transpilan en el dispositivo (strip de tipos básico). Para TS avanzado, compila a `.js` en tu PC y súbelo.

## fetch / HTTP

Runner **no es Node.js**. QuickJS no trae `fetch` nativo, pero Runner lo inyecta usando HTTP de Android.

- Puedes usar `fetch(url, { method, headers, body })` como en tu script de GitHub Actions.
- `response.ok`, `response.status`, `response.text()`, `response.json()` funcionan.
- Si usas `async/await`, Runner lo convierte a síncrono automáticamente (QuickJS no tiene event loop).
- **No existen** `setTimeout`, `require`, ni módulos npm.

## Ejemplo de script

```javascript
console.log("Hola desde Runner!");
console.log("Hora: " + new Date().toISOString());
var sum = 2 + 2;
console.log("2+2 = " + sum);
```

## Compilar

1. Abre la carpeta en **Android Studio** (Ladybug o newer).
2. Sync Gradle y conecta un dispositivo o emulador (API 26+).
3. Run ▶

Desde terminal (con Android SDK instalado):

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Permisos

- Alarmas exactas (`SCHEDULE_EXACT_ALARM`) — Android 12+ pedirá habilitarlo en ajustes.
- Notificaciones — para mostrar ejecución en segundo plano.
- Internet / estado de red — verificar conectividad antes de ejecutar.

## Estructura

```
app/src/main/java/com/runner/app/
├── alarm/          AlarmManager, receivers, boot
├── script/         QuickJS, transpiler TS, servicio
├── data/           Room DB, scripts en filesDir
└── ui/             Jetpack Compose
```
