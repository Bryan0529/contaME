package com.uma.contame.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.lang.Exception

/**
 * Modelo de datos que representa el perfil de un usuario autenticado en la aplicación.
 *
 * @property uid Identificador único del usuario asignado por el proveedor de autenticación.
 * @property displayName Nombre visible del usuario (puede ser nulo si no está configurado).
 * @property email Correo electrónico asociado a la cuenta del usuario.
 * @property photoUrl URL de la foto de perfil del usuario.
 * @property isAnonymous Indica si la cuenta del usuario es anónima.
 */
data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean = false
)

/**
 * Administrador centralizado de autenticación para la aplicación `contaME`.
 *
 * Implementa el patrón Singleton para garantizar una única instancia activa en toda la app.
 * Gestiona el inicio y cierre de sesión utilizando **Firebase Authentication** y la API de
 * **Android Credential Manager** con Google Identity Services.
 *
 * @param context Contexto de la aplicación para inicializar servicios como [CredentialManager].
 */
class AuthManager private constructor(private val context: Context) {

    private val tag = "ContaMeAuth"

    // Gestor de credenciales nativo de Android para la integración de Google Sign-In
    private val credentialManager = CredentialManager.create(context)

    // Instancia de Firebase Auth inicializada de forma perezosa y segura
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(tag, "FirebaseAuth no disponible: ${e.message}")
            null
        }
    }

    // Flujos de estado reactivos (StateFlow) para exponer información a la UI
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    /** Estado reactivo que emite el perfil del usuario actualmente autenticado (o null si no hay sesión activa). */
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    /** Estado reactivo que contiene mensajes de error recientes de autenticación para mostrar en la interfaz. */
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    /** Estado reactivo que indica si hay un proceso de autenticación en curso. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Escucha cambios en el estado de autenticación de Firebase en tiempo real
        firebaseAuth?.addAuthStateListener { auth ->
            val user = auth.currentUser
            _currentUser.value = user?.toUserProfile()
            Log.d(tag, "AuthStateListener: user = ${user?.email}")
        }
        // Inicializa el usuario actual con la sesión previamente guardada en Firebase Auth
        _currentUser.value = firebaseAuth?.currentUser?.toUserProfile()
    }

    /**
     * Inicia sesión utilizando la cuenta de Google mediante [CredentialManager] y autentica en Firebase.
     *
     * @param activity Actividad desde la cual se invoca el flujo visual de selección de cuenta de Google.
     * @return [Result] con el [UserProfile] resultante si el proceso es exitoso, o una excepción si falla.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<UserProfile> {
        _isLoading.value = true
        _authError.value = null

        return try {
            val serverClientId = getWebClientId()

            // Configuración de las opciones para solicitar la credencial de ID de Google
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            // Petición enviada al CredentialManager
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Solicita al usuario seleccionar una cuenta mediante la UI del sistema
            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            // Verifica que la credencial recibida sea un token de ID de Google válido
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Autenticar en Firebase Auth utilizando el ID Token de Google recibido
                val userProfile = if (firebaseAuth != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = firebaseAuth!!.signInWithCredential(firebaseCredential).await()
                    val user = authResult.user?.toUserProfile() ?: UserProfile(
                        uid = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName,
                        email = googleIdTokenCredential.id,
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                    user
                } else {
                    // Respaldo si Firebase Auth no se encuentra disponible
                    UserProfile(
                        uid = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName,
                        email = googleIdTokenCredential.id,
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                }

                _currentUser.value = userProfile
                _isLoading.value = false
                Log.d(tag, "Google Sign-In exitoso en Firebase Auth: ${userProfile.email}")
                Result.success(userProfile)
            } else {
                _isLoading.value = false
                val err = "No se pudo obtener la credencial de Google seleccionada"
                _authError.value = err
                Result.failure(Exception(err))
            }
        } catch (e: GetCredentialCancellationException) {
            // El usuario canceló la ventana modal de inicio de sesión con Google
            _isLoading.value = false
            val msg = "Inicio de sesión con Google cancelado"
            _authError.value = msg
            Log.d(tag, msg)
            Result.failure(e)
        } catch (e: NoCredentialException) {
            // No hay cuentas configuradas en el dispositivo o emulador
            _isLoading.value = false
            val msg = "No hay cuentas de Google disponibles en este dispositivo/emulador. Puedes ingresar tu correo de Google directamente abajo para autenticar en Firebase."
            _authError.value = msg
            Log.w(tag, msg)
            Result.failure(e)
        } catch (e: Exception) {
            // Manejo de errores generales o fallos de configuración en Firebase/Google
            _isLoading.value = false
            val errorMsg = e.localizedMessage ?: "Error al conectar con Google"
            Log.e(tag, "Error en Google Sign-In: $errorMsg", e)
            _authError.value = if (errorMsg.contains("10")) {
                "Error 10: Verifica que el proveedor Google esté habilitado en la consola de Firebase. Puedes iniciar sesión ingresando tu correo directamente."
            } else {
                errorMsg
            }
            Result.failure(e)
        }
    }

    /**
     * Autentica directamente con Firebase Authentication usando correo y contraseña.
     *
     * Si el usuario no existe aún en Firebase Auth, intenta automáticamente crear una nueva cuenta
     * y le asigna el nombre proporcionado en su perfil.
     *
     * @param email Correo electrónico del usuario.
     * @param pass Contraseña del usuario.
     * @param name Nombre a asignar al perfil del usuario en caso de creación.
     * @return [Result] con el [UserProfile] registrado/autenticado, o una excepción si ocurre un error.
     */
    suspend fun authenticateWithFirebaseEmail(
        email: String,
        pass: String,
        name: String
    ): Result<UserProfile> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Auth no disponible"))
        _isLoading.value = true
        _authError.value = null

        return try {
            val user = try {
                // Intenta iniciar sesión con el correo y contraseña proporcionados
                val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
                result.user
            } catch (e: Exception) {
                // Si el inicio de sesión falla (ej. cuenta no existente), intenta crear una cuenta nueva
                Log.d(tag, "signInWithEmailAndPassword falló (${e.message}), intentando crear cuenta nueva...")
                val createResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
                createResult.user?.let { newUser ->
                    try {
                        // Actualiza el perfil en Firebase con el nombre proporcionado
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name.trim().ifEmpty { email.substringBefore('@') })
                            .build()
                        newUser.updateProfile(profileUpdates).await()
                    } catch (pe: Exception) {
                        Log.w(tag, "No se pudo actualizar el nombre del perfil: ${pe.message}")
                    }
                }
                createResult.user
            }

            if (user != null) {
                val profile = user.toUserProfile()
                _currentUser.value = profile
                _isLoading.value = false
                Log.d(tag, "Autenticado en Firebase Authentication: ${profile.email} (UID: ${profile.uid})")
                Result.success(profile)
            } else {
                _isLoading.value = false
                val err = "No se pudo obtener el usuario de Firebase Auth"
                _authError.value = err
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            _isLoading.value = false
            val msg = e.localizedMessage ?: "Error de autenticación con Firebase"
            _authError.value = msg
            Log.e(tag, "Error en authenticateWithFirebaseEmail: $msg", e)
            Result.failure(e)
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     *
     * Invalida la sesión activa tanto en Firebase Authentication como en la API de
     * CredentialManager de Android y limpia los estados expuestos a la UI.
     */
    suspend fun signOut() {
        try {
            firebaseAuth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _currentUser.value = null
            _authError.value = null
            Log.d(tag, "Sesión cerrada correctamente")
        } catch (e: Exception) {
            Log.e(tag, "Error cerrando sesión: ${e.message}", e)
        }
    }

    /**
     * Retorna el Web Client ID asociado a la consola de Firebase / Google Cloud
     * necesario para obtener el Google ID Token mediante CredentialManager.
     */
    private fun getWebClientId(): String {
        return "252590588787-vu9hk7mh4cil9ris3sjvfp8l7m57rl73.apps.googleusercontent.com"
    }

    /**
     * Función de extensión para transformar una instancia de [FirebaseUser] en
     * el modelo de datos local [UserProfile].
     */
    private fun FirebaseUser.toUserProfile(): UserProfile {
        return UserProfile(
            uid = uid,
            displayName = displayName ?: email?.substringBefore('@') ?: "Usuario contaME",
            email = email,
            photoUrl = photoUrl?.toString(),
            isAnonymous = isAnonymous
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null

        /**
         * Obtiene la instancia única (Singleton) de [AuthManager].
         *
         * @param context Contexto utilizado para inicializar la instancia (se convierte a `applicationContext`).
         */
        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
