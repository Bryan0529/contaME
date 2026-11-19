package com.uma.contame.export

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Envío de reportes por WhatsApp y correo electrónico usando el sistema de **Intents** de Android.
 *
 * No se usa la API de WhatsApp Business ni un servidor: la app le entrega el contenido al sistema
 * y WhatsApp / la app de correo del usuario se encargan del envío. Así no hay claves secretas en
 * el APK ni costos por mensaje.
 */
object ReportShare {

    private const val WHATSAPP = "com.whatsapp"
    private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    /** Devuelve un [Uri] seguro (content://) para compartir [file] con otras apps. */
    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun launch(context: Context, intent: Intent) {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Abre WhatsApp con [text] ya escrito; el usuario elige el contacto o grupo.
     * Si WhatsApp no está instalado, abre el selector de apps de Android.
     */
    fun shareTextToWhatsApp(context: Context, text: String) {
        val base = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        for (pkg in listOf(WHATSAPP, WHATSAPP_BUSINESS)) {
            try {
                launch(context, Intent(base).setPackage(pkg))
                return
            } catch (_: ActivityNotFoundException) {
                // Probar el siguiente paquete
            }
        }
        launch(context, Intent.createChooser(base, "Compartir reporte"))
    }

    /**
     * Comparte el archivo de Excel por WhatsApp (como documento adjunto).
     * Si WhatsApp no está instalado, abre el selector de apps de Android.
     */
    fun shareFileToWhatsApp(context: Context, file: File, caption: String) {
        val base = fileIntent(context, file, subject = null, body = caption)
        for (pkg in listOf(WHATSAPP, WHATSAPP_BUSINESS)) {
            try {
                launch(context, Intent(base).setPackage(pkg))
                return
            } catch (_: ActivityNotFoundException) {
                // Probar el siguiente paquete
            }
        }
        launch(context, Intent.createChooser(base, "Compartir reporte"))
    }

    /**
     * Comparte una imagen PNG (por ejemplo la tarjeta del resumen) por WhatsApp.
     * Si WhatsApp no está instalado, abre el selector de apps de Android.
     */
    fun shareImageToWhatsApp(context: Context, file: File, caption: String) {
        val base = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        for (pkg in listOf(WHATSAPP, WHATSAPP_BUSINESS)) {
            try {
                launch(context, Intent(base).setPackage(pkg))
                return
            } catch (_: ActivityNotFoundException) {
                // Probar el siguiente paquete
            }
        }
        launch(context, Intent.createChooser(base, "Compartir resumen"))
    }

    /**
     * Abre la app de correo con el Excel adjunto, asunto y cuerpo ya escritos.
     *
     * @param to Destinatario opcional (vacío = el usuario lo escribe).
     */
    fun sendEmail(context: Context, file: File, subject: String, body: String, to: String = "") {
        val intent = fileIntent(context, file, subject, body).apply {
            if (to.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        }
        launch(context, Intent.createChooser(intent, "Enviar reporte por correo"))
    }

    /** Intent base `ACTION_SEND` con el archivo adjunto y permiso temporal de lectura. */
    private fun fileIntent(context: Context, file: File, subject: String?, body: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = XLSX_MIME
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            putExtra(Intent.EXTRA_TEXT, body)
            if (subject != null) putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
}
