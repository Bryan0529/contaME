<div align="center">
<img width="1200" height="475" alt="contaME" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# contaME

**contaME** es una aplicación Android para organizar las finanzas personales. Permite registrar ingresos y gastos, consultar el balance, planificar presupuestos mensuales y dar seguimiento a metas de ahorro.

## Funciones principales

- **Resumen financiero:** balance actual, ingresos y gastos del mes, movimientos recientes y estado del presupuesto.
- **Movimientos:** registrar, editar y eliminar ingresos y gastos; buscar y filtrar por tipo y categoría.
- **Presupuesto mensual:** definir un límite y un umbral de alerta; consultar el progreso y recibir avisos cuando corresponda.
- **Metas de ahorro:** crear y administrar metas, y agregar fondos.
- **Gráficos:** consultar visualizaciones de ingresos y gastos.
- **Cuenta:** iniciar sesión con Google y sincronizar información con los servicios Firebase configurados para la aplicación.

## Tecnologías

- **Kotlin** y **Android** (minSdk 24; compileSdk 36.1).
- **Jetpack Compose** y **Material 3** para la interfaz.
- **AndroidX ViewModel**, Lifecycle y Kotlin Coroutines para el estado y las tareas asíncronas.
- **Room** para persistencia local y **DataStore** para preferencias.
- **Firebase Authentication**, **Cloud Firestore**, **Firebase AI** y **Firebase App Check** para servicios integrados.
- **Credential Manager** y Google Identity para el inicio de sesión con Google.
- **Retrofit**, **OkHttp** y **Moshi** para comunicación y serialización; **Coil** para imágenes.
- **Gradle Kotlin DSL**, Android Gradle Plugin y KSP para la compilación.

## Requisitos

- Android Studio actualizado, con soporte para Android SDK 36.1 y Gradle del proyecto.
- JDK compatible con la versión de Android Gradle Plugin configurada (Android Studio incluye un JDK integrado).
- Emulador Android o dispositivo físico con **Android 7.0 (API 24) o superior**.
- Conexión a Internet para las funciones que utilizan Firebase o servicios remotos.
- Para usar autenticación y sincronización, acceso a un proyecto Firebase configurado para Android. Las claves de servicios opcionales solo son necesarias si se utilizan esas funciones.

## Instalación

1. Clona el repositorio y ábrelo en Android Studio:

   ```bash
   git clone https://github.com/Bryan0529/contaME.git
   cd contaME
   ```

2. En Android Studio, selecciona **Open** y elige la carpeta `contaME`. Espera a que termine la sincronización de Gradle e instala los SDK que solicite el IDE.
3. Configura Firebase si vas a utilizar inicio de sesión o sincronización (ver [Configuración](#configuración)). Para compilar y probar la interfaz, no añadas claves reales al repositorio.
4. Selecciona un emulador o dispositivo y pulsa **Run** para ejecutar la variante `app` / `debug`.

## Configuración

### Firebase (inicio de sesión y sincronización)

1. Crea o selecciona un proyecto Firebase y registra una aplicación Android con el identificador `com.uma.contame`.
2. Habilita los servicios que vayas a utilizar, como Authentication con Google y Cloud Firestore, y configura sus reglas y proveedores desde Firebase Console.
3. Descarga el archivo `google-services.json` de **tu** aplicación Firebase y colócalo en `app/google-services.json`.
4. Configura la huella SHA correspondiente en Firebase si el inicio de sesión de Google lo requiere.

No compartas claves privadas, archivos de cuentas de servicio, contraseñas ni tokens. No subas credenciales personales al repositorio.

### Claves opcionales

El proyecto incluye `.env.example` como plantilla para el plugin de secretos de Gradle. Si una función de IA requiere una clave de Gemini, crea un `.env` local en la raíz y configura allí la clave usando el nombre esperado por esa función. La plantilla contiene solo un ejemplo comentado; no pegues una clave real en este README, en el código ni en archivos versionados. Consulta la configuración de secretos de AI Studio/Firebase que corresponda a tu entorno.

Para firmar una versión de publicación, configura el almacén de claves y las variables `KEYSTORE_PATH`, `STORE_PASSWORD` y `KEY_PASSWORD` en el entorno local o seguro de CI. No compartas ni subas el archivo `.jks` o sus contraseñas. Para desarrollo local, ejecuta la variante `debug` desde Android Studio.

## Ejecución y compilación

- **Android Studio:** abre el proyecto, espera la sincronización, selecciona un emulador/dispositivo y ejecuta `app` con la configuración `debug`.
- **Pruebas:** ejecuta las pruebas desde Android Studio o, si tienes Gradle instalado y configurado para este proyecto, usa `gradle test` y `gradle connectedAndroidTest` para pruebas unitarias e instrumentadas, respectivamente.
- **APK de depuración:** desde Android Studio, selecciona **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## Seguridad

- Mantén `.env`, keystores, contraseñas y tokens fuera del control de versiones.
- Usa credenciales propias y permisos mínimos; configura las reglas de Firebase antes de usar datos reales.
- Revisa las políticas de privacidad y los avisos de seguridad de los proveedores antes de desplegar la aplicación.

## AI Studio

[Ver la aplicación en Google AI Studio](https://ai.studio/apps/ade35c78-1589-44f9-a981-5922cba52c01)
