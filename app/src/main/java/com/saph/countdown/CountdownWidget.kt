package com.saph.countdown

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class CountdownWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        cancelUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_UPDATE,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, CountdownWidget::class.java))
                if (ids.isNotEmpty()) {
                    ids.forEach { updateAppWidget(context, manager, it) }
                    scheduleNextUpdate(context)
                }
            }
        }
    }

    companion object {
        const val ACTION_UPDATE = "com.saph.countdown.ACTION_UPDATE"
        const val PREFS_NAME = "com.saph.countdown.widget_prefs"
        const val KEY_LABEL = "label"
        const val DEFAULT_LABEL = "until wednesday"
        private const val INTERVAL_MS = 15 * 60_000L   // 15 minutes

        private val TARGET_TIME = LocalTime.of(18, 30)
        private val WINDOW_END = LocalTime.of(21, 0)

        fun getLabel(context: Context): String =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LABEL, DEFAULT_LABEL) ?: DEFAULT_LABEL

        fun setLabel(context: Context, label: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_LABEL, label.trim().ifEmpty { DEFAULT_LABEL }).apply()
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val now = LocalDateTime.now()

            views.setTextViewText(R.id.tv_label, getLabel(context))

            when (val target = getTarget(now)) {
                null -> {
                    views.setViewVisibility(R.id.countdown_container, View.GONE)
                    views.setViewVisibility(R.id.tv_now, View.VISIBLE)
                }
                else -> {
                    val total = Duration.between(now, target).toMinutes().coerceAtLeast(0)
                    val days = total / (24 * 60)
                    val hours = (total % (24 * 60)) / 60
                    val minutes = total % 60

                    views.setViewVisibility(R.id.countdown_container, View.VISIBLE)
                    views.setViewVisibility(R.id.tv_now, View.GONE)
                    views.setTextViewText(R.id.tv_days, "%02d".format(days))
                    views.setTextViewText(R.id.tv_hours, "%02d".format(hours))
                    views.setTextViewText(R.id.tv_minutes, "%02d".format(minutes))
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun scheduleNextUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = buildPendingIntent(context)
            // Snap to next clean 15-minute boundary (e.g. :00, :15, :30, :45)
            val nextFire = ((System.currentTimeMillis() / INTERVAL_MS) + 1) * INTERVAL_MS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC, nextFire, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, nextFire, pendingIntent)
            }
        }

        fun cancelUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildPendingIntent(context))
        }

        private fun buildPendingIntent(context: Context) = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, CountdownWidget::class.java).apply { action = ACTION_UPDATE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun getTarget(now: LocalDateTime): LocalDateTime? {
            val time = now.toLocalTime()
            if (now.dayOfWeek == DayOfWeek.WEDNESDAY) {
                return when {
                    time < TARGET_TIME -> now.toLocalDate().atTime(TARGET_TIME)
                    time <= WINDOW_END -> null
                    else -> now.toLocalDate().plusDays(7).atTime(TARGET_TIME)
                }
            }
            val daysUntil = (DayOfWeek.WEDNESDAY.value - now.dayOfWeek.value + 7) % 7
            return now.toLocalDate().plusDays(daysUntil.toLong()).atTime(TARGET_TIME)
        }
    }
}
