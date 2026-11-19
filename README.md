## 1. Propósito y funciones principales

**contaME** es una aplicación Android en Kotlin para organizar las finanzas personales. Permite registrar ingresos y gastos, controlar presupuestos y metas de ahorro, y consultar la actividad financiera.

- Registrar ingresos y gastos, buscar movimientos y filtrarlos por tipo y categoría.
- Clasificar movimientos con categorías predeterminadas de ingresos y gastos.
- Definir un presupuesto mensual y recibir alertas locales cuando se acerca al límite o se supera.
- Crear metas de ahorro, seguir su progreso y agregar fondos.
- Consultar gráficos y estadísticas de ingresos, gastos, balance y categorías.
- Exportar un reporte Excel (.xlsx) con hojas de resumen, movimientos, historial mensual, metas y registro de acciones; compartir un resumen con formato para WhatsApp y una tarjeta de resumen en imagen PNG; y enviar archivos por correo o WhatsApp con el selector de Android. Si no hay acciones registradas, la hoja de acciones lista los movimientos.
- Guardar y sincronizar el perfil y los datos con Firebase.

La pantalla de inicio ofrece acceso con Google; el método de correo y contraseña existe en el código, pero no está conectado a la interfaz.

## 2. Tecnologías usadas

| Área | Tecnologías y uso |
|---|---|
| Aplicación Android | Kotlin 2.2.10 y Jetpack Compose (BOM 2024.09.00) para la interfaz. |
| Datos locales | Room 2.7.0 para guardar movimientos, metas y presupuestos en el dispositivo. |
| Autenticación y datos en la nube | Firebase Authentication y Cloud Firestore (Firebase BoM 34.17.0); AndroidX Credential Manager y Google Identity Services (`googleid`) para iniciar sesión con Google. |
| Exportación y compartir | Intents de Android (`Intent.ACTION_SEND`) con `FileProvider` para compartir por WhatsApp y correo; generador propio de archivos .xlsx sin librerías externas; `android.graphics` (`Canvas`) para crear la tarjeta de resumen en PNG. |
| Compilación | Android Gradle Plugin (AGP) 9.1.1, Gradle Wrapper 9.3.1 y JDK 25 para Gradle. |

No se usa la API de WhatsApp Business: la app delega el envío a WhatsApp o al cliente de correo del usuario mediante Intents.

## 3. Requisitos e instalación sin secretos

- Android Studio compatible con AGP 9.1.1 y Gradle 9.3.1.
- JDK 25 para ejecutar Gradle.
- Android SDK Platform 36, minor API 1 (`compileSdk`).
- Android 7.0 (API 24) o posterior; `minSdk` 24 y `targetSdk` 36.
- Conexión a Internet para descargar dependencias y usar Firebase.
- Para probar Google en un emulador, usa una imagen de sistema con Google Play y agrega una cuenta de Google al emulador.

Clona el repositorio y abre la carpeta `contaME` en Android Studio:

```bash
git clone https://github.com/Bryan0529/contaME.git
cd contaME
```

El repositorio es público; `google-services.json`, `*.jks`, `*.keystore` y `.env` están ignorados por Git. Cada persona debe usar su propia configuración de Firebase y su propio keystore de firma.

## 4. Configuración y ejecución

1. En Firebase Console, crea un proyecto y agrega una aplicación Android con el ID de paquete `com.uma.contame`.
2. Descarga el archivo de configuración de esa aplicación y guárdalo como `app/google-services.json`.
3. En **Authentication**, habilita el proveedor **Google**.
4. En la raíz del proyecto, ejecuta `./gradlew signingReport` (Windows: `gradlew.bat signingReport`) y registra en la configuración de Firebase las huellas SHA del certificado de depuración.
5. Crea la base de datos de **Cloud Firestore** y publica estas reglas de seguridad:

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

La estructura de Firestore es la siguiente:

| Ruta | Datos y campos principales |
|---|---|
| `users/{uid}` | Perfil: `usuarioId`, `propietarioUid`, `email`, `nombreMostrado`, `fotoUrl`, `ultimoInicioSesion`, `actualizadoEn`. |
| `users/{uid}/transacciones/{id}` | Movimientos: `id`, `titulo`, `monto`, `tipo`, `categoriaId`, `nombreCategoria`, `iconoCategoria`, `colorCategoria`, `fechaHora`, `notas`, `actualizadoEn`. |
| `users/{uid}/metas_ahorro/{id}` | Metas: `id`, `titulo`, `montoObjetivo`, `montoActual`, `fechaLimite`, `colorHex`, `nombreIcono`, `actualizadoEn`. |
| `users/{uid}/presupuestos_mensuales/{claveMesAno}` | Presupuestos: `claveMesAno`, `limitePresupuesto`, `porcentajeAlerta`, `notificacionesHabilitadas`, `actualizadoEn`. |

Room guarda los datos locales y su base de datos se crea automáticamente.

Para ejecutar la app, abre el proyecto en Android Studio, espera a que termine la sincronización de Gradle, selecciona un emulador o dispositivo y pulsa **Run**. También puedes compilar e instalar la versión de depuración desde la raíz:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

En Windows, usa `gradlew.bat :app:assembleDebug` y `gradlew.bat :app:installDebug`.
