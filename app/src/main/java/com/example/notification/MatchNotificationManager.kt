package com.example.notification

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
import com.example.MainActivity
import com.example.model.MatchAlertEvent
import com.example.model.MatchAlertEventType

object MatchNotificationManager {

    const val CHANNEL_ID = "spor_toto_live_matches"
    private const val CHANNEL_NAME = "Spor Toto Canlı Maç & Gol Bildirimleri"
    private const val CHANNEL_DESC = "Favoriye eklediğiniz maçlarda gol olduğunda ve maç başladığında anlık bildirimler gönderir."

    /**
     * Initializes the Android Notification Channel (Required on Android 8.0+).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Checks whether the app has permission to post notifications.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Sends an Android System Tray Push Notification for a match event (Match Start, Goal, Match End).
     */
    fun sendNotification(context: Context, event: MatchAlertEvent): Boolean {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        // Tap action: Launch MainActivity and route to Live Results
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAV_TAB", "RESULTS")
            putExtra("EXTRA_MATCH_NUMBER", event.matchNumber)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            event.matchNumber,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = when (event.eventType) {
            MatchAlertEventType.GOAL_SCORED -> android.R.drawable.ic_media_play
            MatchAlertEventType.MATCH_STARTED -> android.R.drawable.ic_dialog_info
            MatchAlertEventType.HALF_TIME -> android.R.drawable.ic_media_pause
            MatchAlertEventType.MATCH_FINISHED -> android.R.drawable.ic_menu_agenda
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(event.title)
            .setContentText(event.message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${event.message}\nCanlı skoru ve kupon durumunu görmek için dokunun.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .build()

        try {
            val notificationId = 1000 + event.matchNumber * 10 + event.eventType.ordinal
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            return true
        } catch (e: SecurityException) {
            return false
        }
    }
}
