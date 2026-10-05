package com.uma.contame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.uma.contame.auth.UserProfile

/**
 * Composable que dibuja mediante [Canvas] el logo oficial multicolor de Google.
 *
 * @param modifier Modificador para personalizar el tamaño o distribución del vector.
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val red = Color(0xFFEA4335)
        val blue = Color(0xFF4285F4)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)

        drawArc(
            color = blue,
            startAngle = 0f,
            sweepAngle = -90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = size
        )
        drawArc(
            color = red,
            startAngle = -90f,
            sweepAngle = -90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = size
        )
        drawArc(
            color = yellow,
            startAngle = -180f,
            sweepAngle = -90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = size
        )
        drawArc(
            color = green,
            startAngle = -270f,
            sweepAngle = -90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = size
        )

        drawCircle(
            color = Color.White,
            radius = w * 0.32f,
            center = Offset(w / 2, h / 2)
        )

        drawRect(
            color = blue,
            topLeft = Offset(w * 0.45f, h * 0.38f),
            size = Size(w * 0.55f, h * 0.24f)
        )
    }
}

/**
 * Botón estandarizado para iniciar sesión con Google.
 *
 * @param onClick Acción a ejecutar al hacer clic.
 * @param isLoading Indica si hay un proceso de autenticación en curso para mostrar un spinner.
 * @param text Texto visible del botón.
 * @param modifier Modificador de Compose.
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean = false,
    text: String = "Continuar con Google",
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("google_sign_in_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F2937),
            disabledContainerColor = Color(0xFFE5E7EB)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF4285F4),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Conectando con Google...",
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4B5563)
            )
        } else {
            GoogleLogoIcon(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
        }
    }
}

/**
 * Diálogo modal para visualizar el perfil del usuario autenticado y permitir cambiar de cuenta o cerrar sesión.
 *
 * @param user Datos del perfil del usuario.
 * @param onSignOut Callback invocado al cerrar la sesión.
 * @param onSwitchAccount Callback invocado al solicitar cambio de cuenta.
 * @param onDismiss Callback para cerrar el diálogo.
 */
@Composable
fun UserAccountDialog(
    user: UserProfile,
    onSignOut: () -> Unit,
    onSwitchAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar del usuario
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E7FF)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.photoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = "Avatar de ${user.displayName}",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = (user.displayName ?: user.email ?: "U").take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4338CA)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = user.displayName ?: "Usuario contaME",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!user.email.isNullOrEmpty()) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSwitchAccount,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cambiar a Otra Cuenta", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cerrar")
                        }

                        Button(
                            onClick = {
                                onSignOut()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salir")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Diálogo modal para solicitar inicio de sesión con Google.
 */
@Composable
fun SignInPromptDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignInClick: () -> Unit,
    onEmailSignInClick: ((email: String, pass: String, name: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE0E7FF),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Iniciar Sesión en contaME",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                GoogleSignInButton(
                    onClick = onGoogleSignInClick,
                    isLoading = isLoading,
                    text = "Continuar con Google"
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    }
}
