# contaME – Control de Gastos, Ingresos y Ahorro

**contaME** es una aplicación nativa de Android desarrollada con **Kotlin**, **Jetpack Compose**, **Architecture Components (MVVM)**, **Room Database** y **Firebase Authentication / Firestore**.

---

## 🚀 Funcionalidades Principales

- **Gestión Financiera:** Registro de ingresos, gastos, categorías y cálculo de balance en tiempo real.
- **Presupuestos y Notificaciones:** Configuración de límite mensual con alertas preventivas y notificaciones push en tiempo real (`POST_NOTIFICATIONS`) utilizando la API de Result de Actividades y diálogo Rationale de UX (Android 13+).
- **Metas de Ahorro:** Creación de metas con seguimiento porcentual e incorporación directa desde gastos/ingresos.
- **Exportación de Reportes (`export/`):**
  - **Excel (`.xlsx`):** Generador nativo de libros de 5 hojas (*Resumen*, *Movimientos*, *Historial mensual*, *Metas*, *Registro de acciones*) sin librerías pesadas.
  - **Resumen por WhatsApp:** Formato de texto estilizado con bloques de código, emojis y barras de progreso.
  - **Tarjeta como Imagen PNG:** Renderizado de tarjeta gráfica (1080 × 1350 px) con los colores de contaME para compartir.
  - **Envío con Intents de Android:** Uso de `Intent.ACTION_SEND` y `FileProvider` sin requerir servidor ni credenciales expuestas en la APK.
- **Seguridad y Privacidad de Datos:**
  - Exclusión de bases de datos de Room y preferencias en copias de seguridad de Android y transferencias entre dispositivos (`backup_rules.xml` y `data_extraction_rules.xml`).
  - Firma de lanzamiento de producción configurada con Play App Signing (`upload-keystore.jks`).
  - Protección de secretos en `.gitignore`.

---

## 🛠️ Tecnologías y Arquitectura

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose (Material Design 3)
- **Arquitectura:** MVVM (Model-View-ViewModel) + Coroutines & Flow
- **Base de Datos Local:** Room Database
- **Servicios en la Nube:** Firebase Auth & Firestore
- **Documentación:** KDoc en español para todas las clases y funciones principales

---

## 📄 Documentación Adicional

- [CAMBIOS_CONTAME.md](file:///C:/Users/ester/Downloads/contaME-master/CAMBIOS_CONTAME.md): Registro detallado de cambios, estructura de archivos y guía de pruebas paso a paso.
- [AGENTS.md](file:///C:/Users/ester/Downloads/contaME-master/AGENTS.md): Reglas del agente de desarrollo eficiente Ponytail (YAGNI).

---

## 📱 Compilación y Pruebas Locales

1. Abre el proyecto en **Android Studio**.
2. Ejecuta la aplicación en un emulador o dispositivo físico.
3. Para generar el paquete de distribución firmado (AAB):
   ```bash
   ./gradlew :app:bundleRelease
   ```
   El paquete estará disponible en `app/build/outputs/bundle/release/app-release.aab`.
