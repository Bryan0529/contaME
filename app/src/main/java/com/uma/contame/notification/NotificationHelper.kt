package com.uma.contame.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.uma.contame.MainActivity
import com.uma.contame.R
import java.text.NumberFormat
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "contame_budget_channel"
    private const val CHANNEL_NAME = "Alertas de Presupuesto contaME"
    private const val CHANNEL_DESC = "Notificaciones cuando el gasto se acerca o supera el presupuesto mensual"

    private const val NOTIFICATION_ID_WARNING = 1001
    private const val NOTIFICATION_ID_EXCEEDED = 1002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun sendBudgetExceededNotification(
        context: Context,
        totalExpenses: Double,
        budgetLimit: Double,
        topCategory: String = "Gastos Generales"
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "US"))
        val expenseStr = currencyFormat.format(totalExpenses)
        val budgetStr = currencyFormat.format(budgetLimit)
        val diffStr = currencyFormat.format(totalExpenses - budgetLimit)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 ¡Presupuesto mensual superado en contaME!")
            .setContentText("Has gastado $expenseStr de tu límite de $budgetStr ($diffStr de sobregiro).")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "⚠️ ¡Atención! Has superado tu presupuesto mensual establecido en contaME.\n" +
                        "• Total gastado este mes: $expenseStr\n" +
                        "• Límite fijado: $budgetStr\n" +
                        "• Exceso actual: $diffStr\n" +
                        "• Categoría con mayor consumo: $topCategory\n" +
                        "Abre contaME para revisar tus transacciones y recortar gastos."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EXCEEDED, notification)
    }

    fun sendBudgetWarningNotification(
        context: Context,
        percentage: Int,
        totalExpenses: Double,
        budgetLimit: Double
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "US"))
        val expenseStr = currencyFormat.format(totalExpenses)
        val budgetStr = currencyFormat.format(budgetLimit)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ Alerta: Has consumido el $percentage% de tu presupuesto")
            .setContentText("Llevas gastados $expenseStr de $budgetStr este mes en contaME.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Has alcanzado el $percentage% del límite presupuestario mensual.\n" +
                        "Gastado: $expenseStr | Presupuesto: $budgetStr\n" +
                        "Te recomendamos moderar tus consumos en los próximos días."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_WARNING, notification)
    }
}
