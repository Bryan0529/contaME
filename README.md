# contaME — Control de gastos, ingresos y ahorro

## 1. Propósito y funciones principales

**contaME** es una aplicación nativa para Android, desarrollada en Kotlin, destinada a personas que quieren registrar sus movimientos financieros, observar sus hábitos de gasto y organizar presupuestos y objetivos de ahorro. Usa Room para los datos locales y Firebase Authentication y Cloud Firestore para la cuenta y la persistencia en la nube.

### Transacciones: ingresos y gastos

- Crear, editar y eliminar movimientos de ingreso o gasto.
- Registrar concepto, monto, fecha y hora, categoría y notas opcionales.
- Consultar el historial, buscar por texto y filtrar por tipo de movimiento y categoría.
- Ver ingresos, gastos y balance, incluido el balance del mes seleccionado.

### Categorías

- Clasificar los movimientos mediante categorías predeterminadas separadas entre ingresos y gastos, con nombre, icono y color.
- Filtrar la lista de transacciones y consultar los desgloses por categoría.
- Las categorías disponibles están definidas en el código; no se encontró una función para crear o administrar categorías personalizadas.

### Presupuestos mensuales y alertas

- Configurar un límite mensual de gastos, un porcentaje de aviso y si se habilitan las notificaciones.
- Consultar el consumo del presupuesto y los estados de advertencia o límite excedido.
- Emitir notificaciones locales de alerta; en Android 13 o posterior se solicita el permiso `POST_NOTIFICATIONS`.
- **No son notificaciones push**: el código consultado implementa notificaciones locales del dispositivo.

### Metas de ahorro

- Crear, editar y eliminar metas con monto objetivo, saldo acumulado, fecha límite opcional, color e icono.
- Consultar el progreso porcentual y el estado de cada meta.
- Agregar fondos a una meta; la interfaz permite indicar si el aporte también se registra como gasto.

### Gráficos y estadísticas

- Ver gráficos de distribución por categoría para gastos e ingresos.
- Comparar ingresos y gastos mensuales.
- Consultar métricas como tasa de ahorro y categoría con mayor gasto.

### Reportes y exportación

- Generar un libro Excel `.xlsx` con hojas de resumen, movimientos, historial mensual, metas y registro de acciones.
- Compartir un resumen de finanzas con formato de WhatsApp, con indicadores, categorías principales, metas y movimientos recientes.
- Generar una tarjeta de resumen en imagen PNG y compartir archivos/texto mediante el selector de Android (`Intent.ACTION_SEND` y `FileProvider`). También hay opciones para compartir el reporte Excel por correo o WhatsApp.
- El registro de acciones puede estar vacío porque sus llamadas de registro no están conectadas actualmente; en ese caso, el reporte usa los movimientos como alternativa.

### Cuentas de usuario

- Iniciar sesión con Google mediante Android Credential Manager, Google Identity Services y Firebase Authentication.
- El código también implementa autenticación de Firebase con correo y contraseña, incluida la creación de cuenta si el inicio de sesión falla. La pantalla principal de acceso observada ofrece Google; el acceso por correo en la interfaz actual está **por confirmar**.
- Guardar el perfil y los datos del usuario en Firestore, bajo su UID, y sincronizar transacciones, metas y presupuestos.

## 2. Tecnologías usadas

Las versiones son las declaradas en `gradle/libs.versions.toml`, `app/build.gradle.kts` y `gradle/wrapper/gradle-wrapper.properties`. Las dependencias cubiertas por un BOM no declaran una versión individual en el catálogo.

| Tecnología | Versión declarada | Uso |
|---|---:|---|
| Kotlin | 2.2.10 | Lenguaje de la aplicación. |
| Android Gradle Plugin (AGP) | 9.1.1 | Configuración y compilación de la aplicación Android. |
| Gradle Wrapper | 9.3.1 | Sistema de compilación; se ejecuta con `./gradlew` o `gradlew.bat`. |
| JDK para el Gradle Daemon | 25 | Toolchain declarada en `gradle/gradle-daemon-jvm.properties`. |
| Kotlin Compose plugin | 2.2.10 | Integración del compilador de Jetpack Compose con Kotlin. |
| Jetpack Compose BOM | 2024.09.00 | Alinea versiones de Compose UI y bibliotecas asociadas. |
| Jetpack Compose UI | BOM | Construcción de la interfaz declarativa. |
| Material 3 | BOM | Componentes y tema visual de la interfaz. Versión individual: por confirmar (gestionada por el BOM). |
| AndroidX Activity Compose | 1.10.1 | Integración de Activity y APIs de resultados con Compose. |
| AndroidX Lifecycle | 2.8.7 | Estado y ciclo de vida para Compose y ViewModel. |
| Room | 2.7.0 | Persistencia local SQLite para transacciones, metas y presupuestos. |
| KSP | 2.3.5 | Procesamiento de anotaciones de Room y Moshi. |
| Firebase Android BoM | 34.17.0 | Alinea versiones de dependencias Firebase. |
| Firebase Authentication | Firebase BoM | Autenticación de usuarios por Google y correo/contraseña en el código. |
| Cloud Firestore | Firebase BoM | Almacenamiento y sincronización remota de perfiles y datos financieros. |
| AndroidX Credentials | 1.5.0 | Flujo de credenciales para iniciar sesión con Google. |
| Google Identity Services (`googleid`) | 1.1.1 | Obtención de la credencial de Google mediante Credential Manager. |
| Secrets Gradle Plugin | 2.0.1 | Lectura de propiedades locales desde `.env` y valores predeterminados desde `.env.example`. |
| Google Services Gradle Plugin | 4.5.0 | Integración de la configuración de Firebase con el módulo Android. |
| Kotlin Coroutines | 1.10.2 | Operaciones asíncronas y flujos de datos. |
| Moshi / Moshi Kotlin Codegen | 1.15.2 | Serialización JSON y generación de adaptadores. |
| Retrofit | 2.12.0 | Cliente HTTP declarado como dependencia. |
| Coil Compose | 2.7.0 | Carga de imágenes en la interfaz Compose. |

## 3. Requisitos e instalación sin secretos

### Requisitos

| Requisito | Valor |
|---|---|
| Android Studio | Versión compatible con AGP 9.1.1 y Gradle 9.3.1; número exacto recomendado: **por confirmar**. |
| JDK | 25 para el Gradle Daemon, según `gradle/gradle-daemon-jvm.properties`. La compatibilidad de compilación Java del módulo está configurada en Java 11. |
| Android SDK | Android SDK Platform 36, minor API 1 (`compileSdk`). Instala también las herramientas/plataformas que Android Studio solicite. |
| API mínima | Android 7.0, API 24 (`minSdk`). |
| API objetivo | API 36 (`targetSdk`). |
| Conexión | Se necesita conexión a Internet para descargar dependencias y para las funciones de Firebase. |

### Clonar y abrir el proyecto

```bash
git clone https://github.com/Bryan0529/contaME.git
cd contaME
```

Abre la carpeta en Android Studio, permite que Gradle sincronice y descarga las plataformas Android SDK que solicite. También puedes compilar desde la raíz:

```bash
./gradlew :app:assembleDebug
```

En Windows usa `gradlew.bat :app:assembleDebug`.

### Archivos locales y credenciales

Este repositorio es público. No publiques credenciales, claves privadas ni archivos de configuración personales. `.gitignore` excluye `app/google-services.json`, los archivos `*.jks` y `*.keystore`, `.env` y `app/upload-keystore.jks`. Cada desarrollador debe generar/descargar los suyos para su propio proyecto y guardarlos únicamente en su copia local. `google-services.json` contiene la configuración de la aplicación Firebase; los keystores contienen material privado de firma. No se incluyen valores reales de claves en este README.

- **Firebase:** crea o usa un proyecto propio y descarga el archivo de configuración de la aplicación Android, como se explica en la sección 4.
- **Firma de lanzamiento:** crea tu propio keystore de subida y consérvalo en un lugar seguro fuera del control de versiones. Gradle admite las variables de entorno `KEYSTORE_PATH`, `STORE_PASSWORD` (o `KEYSTORE_PASSWORD`), `KEY_ALIAS` y `KEY_PASSWORD`; el alias predeterminado del script es `upload`. No reutilices ni publiques el keystore o sus contraseñas.
- **`.env`:** existe `.env.example`, que solo contiene una entrada de ejemplo comentada para `GEMINI_API_KEY`. Si necesitas configurar esa propiedad para tu entorno, copia el archivo y añade el valor privado localmente; no subas `.env`. El código revisado no permite confirmar que la clave Gemini sea necesaria para el flujo principal de la app.

```bash
cp .env.example .env
```

En Windows, copia `.env.example` como `.env` desde el Explorador o con `Copy-Item .env.example .env` en PowerShell. No pegues claves reales en el repositorio ni en este archivo README.

## 4. Configuración y ejecución

### Configurar Firebase

1. En Firebase Console, crea un proyecto nuevo o selecciona uno que controles.
2. Añade una aplicación Android con el ID de paquete exacto **`com.uma.contame`**.
3. Descarga el archivo `google-services.json` de esa aplicación y colócalo en `app/google-services.json`. El plugin de Google Services está declarado en el proyecto; la configuración permite advertir si falta el archivo, pero las funciones de Firebase requieren una configuración válida.
4. En **Authentication → Sign-in method**, habilita **Google**. El código usa Google Identity Services/Credential Manager para obtener una credencial y Firebase Authentication para validarla. Para iniciar sesión con Google en dispositivos de desarrollo, registra en la configuración de la app Firebase las huellas SHA del certificado de depuración correspondiente (puedes consultarlas con `./gradlew signingReport`) y confirma la configuración del cliente OAuth web requerida por Firebase/Google.
5. Habilita también **Correo electrónico/contraseña** si vas a usar el método `authenticateWithFirebaseEmail` del código. La pantalla principal de inicio de sesión solo muestra Google en la versión examinada.
6. Crea la base de datos de **Cloud Firestore** en modo producción y publica las reglas de seguridad siguientes antes de usar la app. No dejes reglas de prueba abiertas.

### Reglas de seguridad de Firestore

Estas reglas limitan la lectura y escritura de cada documento bajo `users/{userId}` al usuario autenticado cuyo UID coincide con ese `userId`:

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

### Base de datos

#### Room (local)

Room usa nombres de tablas y campos en inglés. La base de datos `ContaMeDatabase` declara la **versión 2**. Tipos Kotlin equivalentes: `String` (texto), `Double` (número decimal), `Long` (entero largo en milisegundos), `Long?` (entero largo opcional) y `Boolean` (verdadero/falso). `id` es la clave primaria de cada tabla.

| Tabla | Campo | Tipo | Propósito |
|---|---|---|---|
| `transactions` | `id` | `String` | Identificador único y clave primaria. |
| `transactions` | `userId` | `String` | UID/identificador local propietario; valor predeterminado `local`. |
| `transactions` | `title` | `String` | Concepto o título del movimiento. |
| `transactions` | `amount` | `Double` | Monto. |
| `transactions` | `type` | `String` | Tipo: `INCOME` o `EXPENSE`. |
| `transactions` | `categoryId` | `String` | Identificador de la categoría. |
| `transactions` | `categoryName` | `String` | Nombre visible de la categoría. |
| `transactions` | `categoryIcon` | `String` | Nombre del icono de categoría. |
| `transactions` | `categoryColor` | `String` | Color hexadecimal de la categoría. |
| `transactions` | `timestamp` | `Long` | Fecha y hora en milisegundos desde epoch. |
| `transactions` | `notes` | `String` | Notas adicionales. |
| `savings_goals` | `id` | `String` | Identificador único y clave primaria. |
| `savings_goals` | `userId` | `String` | UID/identificador local propietario; valor predeterminado `local`. |
| `savings_goals` | `title` | `String` | Nombre de la meta. |
| `savings_goals` | `targetAmount` | `Double` | Monto objetivo. |
| `savings_goals` | `currentAmount` | `Double` | Monto acumulado. |
| `savings_goals` | `deadlineTimestamp` | `Long?` | Fecha límite opcional en milisegundos desde epoch. |
| `savings_goals` | `colorHex` | `String` | Color hexadecimal de la meta. |
| `savings_goals` | `iconName` | `String` | Nombre del icono de la meta. |
| `monthly_budgets` | `id` | `String` | Clave primaria compuesta por `userId-monthYearKey`. |
| `monthly_budgets` | `userId` | `String` | UID/identificador local propietario; valor predeterminado `local`. |
| `monthly_budgets` | `monthYearKey` | `String` | Mes y año en formato `yyyy-MM`. |
| `monthly_budgets` | `budgetLimit` | `Double` | Límite de gasto mensual. |
| `monthly_budgets` | `alertThresholdPercent` | `Int` | Porcentaje para activar la alerta. |
| `monthly_budgets` | `notificationsEnabled` | `Boolean` | Indica si las notificaciones locales están habilitadas. |

#### Cloud Firestore (nube)

Firestore usa documentos por UID y nombres de campos en español. Las colecciones de Room están en inglés (`transactions`, `savings_goals`, `monthly_budgets`); las subcolecciones actuales de Firestore están en español (`transacciones`, `metas_ahorro`, `presupuestos_mensuales`). El servicio también contiene compatibilidad/migración para antiguos nombres de subcolecciones en inglés.

| Ruta | Campo | Tipo Firestore | Propósito |
|---|---|---|---|
| `users/{uid}` | `usuarioId` | String | UID del usuario. |
| `users/{uid}` | `propietarioUid` | String | UID propietario del documento. |
| `users/{uid}` | `email` | String | Correo; se guarda como cadena vacía si no está disponible. |
| `users/{uid}` | `nombreMostrado` | String | Nombre visible; cadena vacía si no está disponible. |
| `users/{uid}` | `fotoUrl` | String | URL de foto; cadena vacía si no está disponible. |
| `users/{uid}` | `ultimoInicioSesion` | String | Fecha y hora formateada por la aplicación. |
| `users/{uid}` | `actualizadoEn` | String | Fecha y hora formateada de actualización. |
| `users/{uid}/transacciones/{id}` | `id` | String | ID de la transacción y documento. |
| `users/{uid}/transacciones/{id}` | `usuarioId`, `propietarioUid` | String | UID del usuario y propietario. |
| `users/{uid}/transacciones/{id}` | `titulo` | String | Concepto del movimiento. |
| `users/{uid}/transacciones/{id}` | `monto` | Number (`Double`) | Monto del movimiento. |
| `users/{uid}/transacciones/{id}` | `tipo` | String | Tipo (`INCOME` o `EXPENSE`). |
| `users/{uid}/transacciones/{id}` | `categoriaId`, `nombreCategoria`, `iconoCategoria`, `colorCategoria` | String | Identidad, nombre, icono y color de categoría. |
| `users/{uid}/transacciones/{id}` | `fechaHora` | Number (`Long`) | Fecha y hora en milisegundos desde epoch. |
| `users/{uid}/transacciones/{id}` | `notas` | String | Notas del movimiento. |
| `users/{uid}/transacciones/{id}` | `actualizadoEn` | String | Fecha y hora formateada de actualización. |
| `users/{uid}/metas_ahorro/{id}` | `id` | String | ID de la meta y documento. |
| `users/{uid}/metas_ahorro/{id}` | `usuarioId`, `propietarioUid` | String | UID del usuario y propietario. |
| `users/{uid}/metas_ahorro/{id}` | `titulo` | String | Nombre de la meta. |
| `users/{uid}/metas_ahorro/{id}` | `montoObjetivo`, `montoActual` | Number (`Double`) | Monto objetivo y monto ahorrado. |
| `users/{uid}/metas_ahorro/{id}` | `fechaLimite` | Number (`Long`) | Fecha límite en milisegundos; se guarda `0` si no hay fecha. |
| `users/{uid}/metas_ahorro/{id}` | `colorHex`, `nombreIcono` | String | Color e icono de la meta. |
| `users/{uid}/metas_ahorro/{id}` | `actualizadoEn` | String | Fecha y hora formateada de actualización. |
| `users/{uid}/presupuestos_mensuales/{claveMesAno}` | `claveMesAno` | String | Clave del mes/año y documento. |
| `users/{uid}/presupuestos_mensuales/{claveMesAno}` | `usuarioId`, `propietarioUid` | String | UID del usuario y propietario. |
| `users/{uid}/presupuestos_mensuales/{claveMesAno}` | `limitePresupuesto` | Number (`Double`) | Límite mensual de gasto. |
| `users/{uid}/presupuestos_mensuales/{claveMesAno}` | `porcentajeAlerta` | Number (`Long`/entero) | Porcentaje de consumo que activa la alerta. |
| `users/{uid}/presupuestos_mensuales/{claveMesAno}` | `notificacionesHabilitadas` | Boolean | Estado de las notificaciones locales de alerta. |
| `users/{uid}/presupuestos_mensuales/{claveMesAno}` | `actualizadoEn` | String | Fecha y hora formateada de actualización. |

### Ejecutar y probar

1. Confirma que `app/google-services.json` corresponde a tu proyecto Firebase y que Authentication y Firestore están configurados.
2. En Android Studio, selecciona una configuración de ejecución de la aplicación y un emulador o dispositivo con Android 7.0 (API 24) o posterior; pulsa **Run**.
3. También puedes instalar la variante debug desde la raíz del proyecto:

   ```bash
   ./gradlew :app:installDebug
   ```

4. Ejecuta las pruebas disponibles con:

   ```bash
   ./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest
   ```

5. Para crear un AAB de lanzamiento:

   ```bash
   ./gradlew :app:bundleRelease
   ```

   El resultado se genera en `app/build/outputs/bundle/release/app-release.aab`. Para firmar con tu clave de subida, configura las variables de entorno de firma documentadas en la sección 3; no compartas ni subas ese keystore.
