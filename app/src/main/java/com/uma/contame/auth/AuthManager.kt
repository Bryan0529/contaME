package com.uma.contame.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
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

data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean = false
)

class AuthManager private constructor(private val context: Context) {

    private val tag = "ContaMeAuth"
    private val credentialManager = CredentialManager.create(context)

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(tag, "FirebaseAuth no disponible: ${e.message}")
            null
        }
    }

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        firebaseAuth?.addAuthStateListener { auth ->
            val user = auth.currentUser
            _currentUser.value = user?.toUserProfile()
            Log.d(tag, "AuthStateListener: user = ${user?.email}")
        }
        _currentUser.value = firebaseAuth?.currentUser?.toUserProfile()
    }

    suspend fun signInWithGoogle(activity: Activity): Result<UserProfile> {
        _isLoading.value = true
        _authError.value = null

        return try {
            val serverClientId = getWebClientId()

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Authenticate with Firebase Auth
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
            _isLoading.value = false
            val msg = "Inicio de sesión con Google cancelado"
            _authError.value = msg
            Log.d(tag, msg)
            Result.failure(e)
        } catch (e: NoCredentialException) {
            _isLoading.value = false
            val msg = "No hay cuentas de Google disponibles en este dispositivo/emulador. Puedes ingresar tu correo de Google directamente abajo para autenticar en Firebase."
            _authError.value = msg
            Log.w(tag, msg)
            Result.failure(e)
        } catch (e: Exception) {
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
     * Authenticates directly with Firebase Authentication using Email & Password.
     * If user does not exist in Firebase Auth yet, it automatically creates the account.
     * This guarantees the user appears in Firebase Authentication Console under 'Authentication -> Users'.
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
                // Try signing in first
                val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
                result.user
            } catch (e: Exception) {
                // If not found or wrong password, try creating new account
                Log.d(tag, "signInWithEmailAndPassword falló (${e.message}), intentando crear cuenta nueva...")
                val createResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
                createResult.user?.let { newUser ->
                    try {
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

    private fun getWebClientId(): String {
        return "252590588787-vu9hk7mh4cil9ris3sjvfp8l7m57rl73.apps.googleusercontent.com"
    }

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

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
