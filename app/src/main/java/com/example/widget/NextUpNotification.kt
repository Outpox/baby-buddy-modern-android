package com.example.widget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ml.RhythmTarget
import com.example.data.model.TimeFormat

/**
 * Silent, lock-screen-visible notification with what's next ("Feed ~14:30", then the diaper change and sleep),
 * counting down to the first one. Shown only while no feed or sleep is timed (their own notifications cover that);
 * re-posted when the times change, never on a timer.
 */
object NextUpNotification {

    private const val CHANNEL_ID = "next_up"
    private const val CHANNEL_NAME = "What's next"
    const val NOTIFICATION_ID = 4203

    private const val REQUEST_CONTENT = 4230

    /** Shows [next] (see [com.example.data.repository.BabyBuddyRepository.nextUpTimes]), or removes it when empty or off. */
    fun update(context: Context, next: Map<RhythmTarget, Long>, enabled: Boolean) {
        if (!enabled || next.isEmpty()) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val items = next.entries.sortedBy { it.value }.map { (target, at) -> "${label(target)} ~${TimeFormat.clock(at)}" to at }
        val (first, firstAt) = items.first()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_feed)
            .setContentTitle(first)
            .setContentText(items.drop(1).joinToString(" · ") { it.first }.ifEmpty { null })
            // Counts down to the first one (and up again once it's due)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(firstAt)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent(context))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and notify; nothing to do.
        }
    }

    private fun label(target: RhythmTarget): String = when (target) {
        RhythmTarget.FEED_GAP -> "Feed"
        RhythmTarget.DIAPER_GAP -> "Diaper"
        RhythmTarget.SLEEP_LENGTH -> "Wake-up"
        RhythmTarget.AWAKE_WINDOW -> "Nap"
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BabyBuddyQuickLogWidget.EXTRA_NAVIGATE_TO, "TODAY_DASHBOARD")
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CONTENT,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
