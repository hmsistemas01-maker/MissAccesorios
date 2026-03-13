# Miss Accesorios — Código Fuente Android

## Requisitos
- Android Studio Hedgehog (2023.1) o superior
- JDK 8 o superior
- SDK Android 24+ (Android 7.0)

## Cómo abrir el proyecto

1. Abre **Android Studio**
2. Selecciona **File → Open**
3. Navega hasta la carpeta `missaccesorios/` y ábrela
4. Espera a que Gradle sincronice (puede tardar unos minutos la primera vez)
5. Conecta un dispositivo Android o crea un emulador (API 24+)
6. Presiona ▶ **Run** o usa `Shift+F10`

## Cómo generar el APK

### Modo debug (para pruebas):
- **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- El APK quedará en: `app/build/outputs/apk/debug/app-debug.apk`

### Modo release (para distribución):
- **Build → Generate Signed Bundle / APK**
- Elige APK, crea o selecciona tu keystore
- El APK quedará en: `app/build/outputs/apk/release/app-release.apk`

## Estructura del proyecto

```
missaccesorios/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/missaccesorios/app/
│   │   ├── activities/          ← 11 pantallas
│   │   ├── adapters/            ← 6 adaptadores
│   │   ├── database/            ← DatabaseHelper + Contract
│   │   └── models/              ← Proveedor, Venta, DetalleVenta
│   └── res/layout/              ← 11 layouts XML
├── app/build.gradle
├── build.gradle
└── settings.gradle
```

## Funcionalidades incluidas

- ✅ Gestión de proveedores (alta y edición)
- ✅ Nueva venta con múltiples productos/proveedores
- ✅ Métodos de pago: efectivo (cambio), tarjeta (comisión %), transferencia (referencia)
- ✅ Historial de ventas con filtros (hoy/ayer/semana/mes)
- ✅ Detalle de venta al tocar
- ✅ Corte de caja diario con desglose por proveedor
- ✅ Historial de cortes con detalle
- ✅ Reportes por proveedor con cálculo de comisión proporcional
- ✅ Registro de pagos a proveedores con validaciones
- ✅ Reporte de pagos (detalle + resumen por proveedor)
- ✅ Base de datos SQLite versión 4

## Base de datos

La BD se crea automáticamente al primer arranque en el almacenamiento interno del dispositivo.
Versión actual: **4** (fuerza recreación si existe versión anterior)
