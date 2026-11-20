## 1. Propósito y funciones principales

**contaME** es una aplicación Android desarrollada en Kotlin para organizar las finanzas personales. Permite registrar ingresos y gastos, controlar presupuestos y metas de ahorro, y consultar la actividad financiera.

- Crear, editar y eliminar movimientos de ingreso o gasto.
- Buscar movimientos por texto y filtrarlos por tipo y categoría.
- Clasificar los movimientos con categorías predeterminadas para ingresos y gastos.
- Configurar un presupuesto mensual y recibir alertas locales al acercarse o exceder el límite.
- Crear, editar y eliminar metas de ahorro; consultar su progreso y agregar fondos.
- Consultar gráficos por categoría, comparar ingresos y gastos mensuales y ver estadísticas como la tasa de ahorro.
- Generar un reporte Excel (.xlsx) con hojas de resumen, movimientos, historial mensual, metas y registro de acciones; si no hay acciones registradas, la hoja lista los movimientos.
- Compartir un resumen con formato para WhatsApp y una tarjeta de resumen en imagen PNG.
- Sincronizar el perfil, los movimientos, las metas y los presupuestos con Firestore mediante una cuenta de Google.

La pantalla de inicio ofrece acceso con Google; el método de correo y contraseña existe en el código, pero no está conectado a la interfaz.

## 2. Tecnologías usadas

| Área | Tecnología y uso |
|---|---|
| Aplicación Android | Kotlin 2.2.10. |
| Interfaz | Jetpack Compose, con versiones alineadas por Compose BOM 2024.09.00. |
| Arquitectura | ViewModel y Kotlin Coroutines para estado y operaciones asíncronas. |
| Datos locales | Room 2.7.0 para persistir movimientos, metas y presupuestos. |
| Autenticación y datos en la nube | Firebase Authentication y Cloud Firestore (Firebase BoM 34.17.0); AndroidX Credential Manager 1.5.0 y Google Identity Services (`googleid`) 1.1.1 para el inicio de sesión con Google. |
| Exportación y compartir | Intents de Android (`Intent.ACTION_SEND`) con `FileProvider` para WhatsApp y correo; generador propio de .xlsx sin librerías externas. |
| Imágenes | `android.graphics` (`Canvas`) para generar la tarjeta de resumen en PNG. |
| Compilación | Android Gradle Plugin (AGP) 9.1.1, Gradle Wrapper 9.3.1 y JDK 25 para Gradle. |

No se usa la API de WhatsApp Business: el envío se delega a WhatsApp o al cliente de correo del usuario mediante Intents.

## 3. Requisitos e instalación sin secretos

- Android Studio compatible con Android Gradle Plugin 9.1.1 y Gradle 9.3.1.
- JDK 25 para ejecutar Gradle; el módulo configura la compatibilidad de compilación de Java en 11.
- Android SDK Platform 36, minor API 1 (`compileSdk`).
- Android 7.0 (API 24) o posterior (`minSdk` 24); la API objetivo es 36 (`targetSdk`).
- Conexión a Internet para descargar dependencias y usar las funciones de Firebase.
- Para probar Google en un emulador, elige una imagen de sistema con Google Play y agrega una cuenta de Google al emulador.

Clona el repositorio y abre la carpeta en Android Studio:

```bash
git clone https://github.com/Bryan0529/contaME.git
cd contaME
```

El repositorio es público. Git ignora `app/google-services.json`, `*.jks`, `*.keystore` y `.env`; usa tu propia configuración de Firebase y tu propio keystore de firma.

## 4. Configuración y ejecución

### Configurar Firebase

1. En Firebase Console, crea un proyecto.
2. Agrega una aplicación Android con el ID de paquete `com.uma.contame`.
3. Descarga el archivo de configuración de la aplicación y guárdalo en `app/google-services.json`.
4. En **Authentication**, habilita el proveedor de inicio de sesión **Google**.
5. En la raíz del proyecto, ejecuta `./gradlew signingReport` (Windows: `gradlew.bat signingReport`) y registra en Firebase la huella SHA del certificado de depuración.
6. Crea la base de datos de **Cloud Firestore**.
7. Publica estas reglas de seguridad en Firestore:

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

### Estructura de Firestore

| Ruta | Datos y campos principales |
|---|---|
| `users/{uid}` | Perfil: `usuarioId`, `propietarioUid`, `email`, `nombreMostrado`, `fotoUrl`, `ultimoInicioSesion`, `actualizadoEn`. |
| `users/{uid}/transacciones/{id}` | Movimientos: `id`, `titulo`, `monto`, `tipo`, `categoriaId`, `nombreCategoria`, `iconoCategoria`, `colorCategoria`, `fechaHora`, `notas`, `actualizadoEn`. |
| `users/{uid}/metas_ahorro/{id}` | Metas: `id`, `titulo`, `montoObjetivo`, `montoActual`, `fechaLimite`, `colorHex`, `nombreIcono`, `actualizadoEn`. |
| `users/{uid}/presupuestos_mensuales/{claveMesAno}` | Presupuestos: `claveMesAno`, `limitePresupuesto`, `porcentajeAlerta`, `notificacionesHabilitadas`, `actualizadoEn`. |

### Datos locales con Room

| Tabla | Campos principales |
|---|---|
| `transactions` | `id`, `userId`, `title`, `amount`, `type`, `categoryId`, `categoryName`, `categoryIcon`, `categoryColor`, `timestamp`, `notes`. |
| `savings_goals` | `id`, `userId`, `title`, `targetAmount`, `currentAmount`, `deadlineTimestamp`, `colorHex`, `iconName`. |
| `monthly_budgets` | `id`, `userId`, `monthYearKey`, `budgetLimit`, `alertThresholdPercent`, `notificationsEnabled`. |

Room crea la base local al iniciar la aplicación.

### Ejecutar y probar

Abre el proyecto en Android Studio, espera a que Gradle termine la sincronización, selecciona un emulador o dispositivo Android y pulsa **Run**. También puedes compilar e instalar la versión de depuración desde la raíz:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

En Windows, usa `gradlew.bat :app:assembleDebug` y `gradlew.bat :app:installDebug`.

Hay pruebas unitarias; ejecútalas desde la raíz con:

```bash
./gradlew :app:testDebugUnitTest
```
