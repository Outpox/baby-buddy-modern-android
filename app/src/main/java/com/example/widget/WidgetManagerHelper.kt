package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

object WidgetManagerHelper {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Update Quick Log Widgets
        val quickLogComponent = ComponentName(context, BabyBuddyQuickLogWidget::class.java)
        val quickLogIds = appWidgetManager.getAppWidgetIds(quickLogComponent)
        if (quickLogIds.isNotEmpty()) {
            val intent = Intent(context, BabyBuddyQuickLogWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, quickLogIds)
            }
            context.sendBroadcast(intent)
        }

        // Update Timer Widgets
        val timerComponent = ComponentName(context, BabyBuddyTimerWidget::class.java)
        val timerIds = appWidgetManager.getAppWidgetIds(timerComponent)
        if (timerIds.isNotEmpty()) {
            val intent = Intent(context, BabyBuddyTimerWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, timerIds)
            }
            context.sendBroadcast(intent)
        }

        // Update Summary Widgets
        val summaryComponent = ComponentName(context, BabyBuddySummaryWidget::class.java)
        val summaryIds = appWidgetManager.getAppWidgetIds(summaryComponent)
        if (summaryIds.isNotEmpty()) {
            val intent = Intent(context, BabyBuddySummaryWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, summaryIds)
            }
            context.sendBroadcast(intent)
        }
    }

    /**
     * Request the system launcher to pin a widget to the home screen.
     * Returns true if requestPinAppWidget is supported and dispatched.
     */
    fun pinWidget(context: Context, providerClass: Class<*>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java) ?: return false
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = ComponentName(context, providerClass)
                val successCallbackIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                    action = WidgetActionReceiver.ACTION_WIDGET_PINNED
                }
                val successCallback = PendingIntent.getBroadcast(
                    context,
                    0,
                    successCallbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                return appWidgetManager.requestPinAppWidget(provider, null, successCallback)
            }
        }
        return false
    }
}
