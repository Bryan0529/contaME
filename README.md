## contaME

Aplicación Android para organizar gastos, ingresos y metas de ahorro.

## 1. Propósito y funciones principales

contaME es una aplicación Android desarrollada en Kotlin para organizar las finanzas personales. Permite registrar ingresos y gastos, controlar presupuestos y metas de ahorro, y consultar la actividad financiera. No se conecta a tarjetas de crédito ni a cuentas bancarias: la persona registra sus propios movimientos.

### Funciones principales

- Crear, editar y eliminar movimientos de ingreso o gasto.
- Buscar movimientos por texto y filtrarlos por tipo y categoría.
- Clasificar los movimientos con categorías predeterminadas para ingresos y gastos.
- Configurar un presupuesto mensual y recibir alertas locales al acercarse o exceder el límite.
- Crear, editar y eliminar metas de ahorro; consultar su progreso y agregar fondos.
- Consultar gráficos por categoría, comparar ingresos y gastos mensuales y ver estadísticas como la tasa de ahorro.
- Exportar reportes en Excel y compartir resúmenes por WhatsApp o correo.
- Sincronizar el perfil, los movimientos, las metas y los presupuestos con Firestore mediante una cuenta de Google.

La pantalla de inicio ofrece acceso con Google; el método de correo y contraseña existe en el código, pero no está conectado a la interfaz.

### Pantallas de la aplicación

| Pantalla | Qué permite hacer |
|---|---|
| Inicio de sesión | Entrar con una cuenta de Google. |
| Resumen | Ver el balance total, los ingresos y gastos del mes, registrar un gasto, un ingreso o una meta, y revisar el presupuesto y las metas. |
| Movimientos | Consultar el historial, buscar por texto y filtrar por tipo y categoría. |
| Gráficos | Ver gastos e ingresos por categoría, comparar meses, y consultar la tasa de ahorro y la categoría de mayor gasto. |
| Metas | Ver el total ahorrado, el objetivo total y el avance global; crear metas y agregar fondos. |
| Presupuesto | Definir el límite mensual, el porcentaje de aviso y las notificaciones; revisar la cuenta y cerrar sesión. |

### Categorías predeterminadas

Las categorías están definidas en el código:

- **Gastos:** Alimentación, Transporte, Vivienda, Servicios, Ocio y Salidas, Salud y Cuidado, Educación, Compras y Otros Gastos.
- **Ingresos:** Salario, Negocio, Inversiones, Freelance, Regalos / Extras y Otros Ingresos.

### Reportes y exportación

Desde el botón de compartir se abre un diálogo con cuatro opciones:

- Resumen del mes con formato para WhatsApp.
- Tarjeta de resumen en imagen PNG.
- Reporte Excel (.xlsx) enviado por correo.
- Reporte Excel (.xlsx) enviado por WhatsApp.

El Excel incluye las hojas Resumen, Movimientos, Historial mensual, Metas y Registro de acciones. Si no hay acciones registradas, la hoja de registro lista los movimientos.

## 2. Tecnologías usadas

| Área | Tecnología y uso |
|---|---|
| Aplicación Android | Kotlin 2.2.10. |
| Interfaz | Jetpack Compose, con versiones alineadas por Compose BOM 2024.09.00. |
| Arquitectura | ViewModel y Kotlin Coroutines para estado y operaciones asíncronas. |
| Datos locales | Room 2.7.0 para persistir movimientos, metas y presupuestos. |
| Autenticación y datos en la nube | Firebase Authentication y Cloud Firestore (Firebase BoM 34.17.0); AndroidX Credential Manager 1.5.0 y Google Identity Services (googleid) 1.1.1 para el inicio de sesión con Google. |
| Exportación y compartir | Intents de Android (Intent.ACTION_SEND) con FileProvider para WhatsApp y correo; generador propio de .xlsx sin librerías externas y android.graphics (Canvas) para dibujar la tarjeta de resumen en PNG. |
| Imágenes | Coil Compose 2.7.0 para cargar imágenes en la interfaz (por ejemplo, la foto de perfil). |
| Compilación | Android Gradle Plugin (AGP) 9.1.1, Gradle Wrapper 9.3.1 y JDK 25 para Gradle. |

No se usa la API de WhatsApp Business: el envío se delega a WhatsApp o al cliente de correo del usuario mediante Intents.

### Arquitectura

La aplicación sigue el patrón MVVM:

- **ContaMeViewModel** expone el estado de la interfaz y recibe las acciones de la persona.
- **ContaMeRepository** coordina la base local (Room) y la nube (Firestore).
- **FirebaseFirestoreService** lee y escribe los datos del usuario en Firestore.
- **AuthManager** gestiona el inicio de sesión con Google y Firebase Authentication.
- **NotificationHelper** emite las alertas locales de presupuesto.

### Estructura del código

El código fuente está en `app/src/main/java/com/uma/contame/`:

- `auth/`: inicio de sesión.
- `data/local/`, `data/remote/` y `data/repository/`: Room, Firestore y repositorio.
- `export/`: generación del Excel y de la tarjeta PNG, y envío con Intents.
- `model/`: modelos de dominio y categorías.
- `notification/`: alertas de presupuesto.
- `ui/`: pantallas, componentes y tema visual.
- `viewmodel/`: estado y lógica de la interfaz.

## 3. Requisitos e instalación sin secretos

- Android Studio compatible con Android Gradle Plugin 9.1.1 y Gradle 9.3.1.
- JDK 25 para ejecutar Gradle; el módulo configura la compatibilidad de compilación de Java en 11.
- Android SDK Platform 36, minor API 1 (compileSdk).
- Android 7.0 (API 24) o posterior (minSdk 24); la API objetivo es 36 (targetSdk).
- Conexión a Internet para descargar dependencias y usar las funciones de Firebase.
- Para probar Google en un emulador, elige una imagen de sistema con Google Play y agrega una cuenta de Google al emulador.

Clona el repositorio y abre la carpeta en Android Studio:

```
git clone https://github.com/Bryan0529/contaME.git
cd contaME
```

El repositorio es público. Git ignora `app/google-services.json`, `*.jks`, `*.keystore` y `.env`; usa tu propia configuración de Firebase y tu propio keystore de firma.

### Permisos de la aplicación

- `INTERNET` y `ACCESS_NETWORK_STATE`: sincronización con Firebase.
- `POST_NOTIFICATIONS`: alertas de presupuesto. En Android 13 o posterior, la aplicación solicita este permiso.

## 4. Configuración y ejecución

### Configurar Firebase

1. En Firebase Console, crea un proyecto.
2. Agrega una aplicación Android con el ID de paquete `com.uma.contame`.
3. Descarga el archivo de configuración de la aplicación y guárdalo en `app/google-services.json`.
4. En Authentication, habilita el proveedor de inicio de sesión Google.
5. En Android Studio, abre la ventana Gradle, ve a app > Tasks > android y ejecuta `signingReport`. Copia la huella SHA-1 y regístrala en Firebase.
6. Crea la base de datos de Cloud Firestore.
7. Publica estas reglas de seguridad en Firestore:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Las reglas permiten leer y escribir solo a la persona autenticada, dentro de su propio documento `users/{uid}`.

### Estructura de Firestore

| Ruta | Datos y campos principales |
|---|---|
| `users/{uid}` | Perfil: usuarioId, propietarioUid, email, nombreMostrado, fotoUrl, ultimoInicioSesion, actualizadoEn. |
| `users/{uid}/transacciones/{id}` | Movimientos: id, titulo, monto, tipo, categoriaId, nombreCategoria, iconoCategoria, colorCategoria, fechaHora, notas, actualizadoEn. |
| `users/{uid}/metas_ahorro/{id}` | Metas: id, titulo, montoObjetivo, montoActual, fechaLimite, colorHex, nombreIcono, actualizadoEn. |
| `users/{uid}/presupuestos_mensuales/{claveMesAno}` | Presupuestos: claveMesAno, limitePresupuesto, porcentajeAlerta, notificacionesHabilitadas, actualizadoEn. |

### Datos locales con Room

| Tabla | Campos principales |
|---|---|
| `transactions` | id, userId, title, amount, type, categoryId, categoryName, categoryIcon, categoryColor, timestamp, notes. |
| `savings_goals` | id, userId, title, targetAmount, currentAmount, deadlineTimestamp, colorHex, iconName. |
| `monthly_budgets` | id, userId, monthYearKey, budgetLimit, alertThresholdPercent, notificationsEnabled. |

Room crea la base local al iniciar la aplicación. La sincronización con Firestore solo se realiza con una cuenta de Google iniciada: la aplicación descarga los datos de la nube hacia Room y envía los cambios a Firestore.

### Ejecutar y probar

Abre el proyecto en Android Studio, espera a que Gradle termine la sincronización, selecciona un emulador o dispositivo Android y pulsa Run.

### Exportar un reporte

1. Registra algunos movimientos para que el reporte tenga datos.
2. Pulsa el icono de compartir en la barra superior.
3. Elige una de las cuatro opciones.
4. Elige WhatsApp o la aplicación de correo en el selector de Android.

Los archivos se generan en la carpeta de caché de la aplicación y se comparten con un permiso temporal de lectura (FileProvider). Si WhatsApp o una aplicación de correo no están instalados, Android muestra el selector de aplicaciones.

### Solución de problemas

- **El inicio de sesión con Google falla en un dispositivo de desarrollo:** revisa que la huella SHA-1 esté registrada en Firebase y que `app/google-services.json` sea el de tu proyecto.
- **No llegan las notificaciones de presupuesto:** en Android 13 o posterior, concede el permiso de notificaciones a la aplicación.
