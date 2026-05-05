package com.saph.countdown

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
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
        const val ACTION_UPDATE   = "com.saph.countdown.ACTION_UPDATE"
        const val PREFS_NAME      = "com.saph.countdown.widget_prefs"
        const val KEY_LABEL       = "label"
        const val KEY_INTERVAL    = "interval_minutes"   // 1, 5, 15, 30, 60
        const val KEY_THEME       = "theme"              // "dark","light","clear","custom"
        const val KEY_CUSTOM_HUE  = "custom_hue"         // 0-360
        const val KEY_CUSTOM_ALPHA = "custom_alpha"      // 0-255
        const val KEY_TARGET_DAY  = "target_day"         // DayOfWeek ordinal (1=MON … 7=SUN)
        const val KEY_TARGET_HOUR = "target_hour"
        const val KEY_TARGET_MIN  = "target_minute"
        const val KEY_WINDOW_HOURS = "window_hours"      // hours the ♡ shows (default 2.5 → 150 min)

        const val DEFAULT_LABEL   = "until wednesday"
        const val DEFAULT_INTERVAL = 15
        const val DEFAULT_THEME   = "dark"
        const val DEFAULT_DAY     = 3   // Wednesday
        const val DEFAULT_HOUR    = 18
        const val DEFAULT_MINUTE  = 30
        const val DEFAULT_WINDOW_MIN = 150  // 2.5 h

        // ── Prefs helpers ──────────────────────────────────────────────────────────

        fun getLabel(ctx: Context): String =
            prefs(ctx).getString(KEY_LABEL, DEFAULT_LABEL) ?: DEFAULT_LABEL

        fun setLabel(ctx: Context, v: String) =
            prefs(ctx).edit().putString(KEY_LABEL, v.trim().ifEmpty { DEFAULT_LABEL }).apply()

        fun getInterval(ctx: Context): Int =
            prefs(ctx).getInt(KEY_INTERVAL, DEFAULT_INTERVAL)

        fun setInterval(ctx: Context, minutes: Int) =
            prefs(ctx).edit().putInt(KEY_INTERVAL, minutes).apply()

        fun getTheme(ctx: Context): String =
            prefs(ctx).getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME

        fun setTheme(ctx: Context, t: String) =
            prefs(ctx).edit().putString(KEY_THEME, t).apply()

        fun getCustomHue(ctx: Context): Float =
            prefs(ctx).getFloat(KEY_CUSTOM_HUE, 200f)

        fun setCustomHue(ctx: Context, h: Float) =
            prefs(ctx).edit().putFloat(KEY_CUSTOM_HUE, h).apply()

        fun getCustomAlpha(ctx: Context): Int =
            prefs(ctx).getInt(KEY_CUSTOM_ALPHA, 200)

        fun setCustomAlpha(ctx: Context, a: Int) =
            prefs(ctx).edit().putInt(KEY_CUSTOM_ALPHA, a).apply()

        fun getTargetDay(ctx: Context): Int =
            prefs(ctx).getInt(KEY_TARGET_DAY, DEFAULT_DAY)

        fun setTargetDay(ctx: Context, d: Int) =
            prefs(ctx).edit().putInt(KEY_TARGET_DAY, d).apply()

        fun getTargetHour(ctx: Context): Int =
            prefs(ctx).getInt(KEY_TARGET_HOUR, DEFAULT_HOUR)

        fun setTargetHour(ctx: Context, h: Int) =
            prefs(ctx).edit().putInt(KEY_TARGET_HOUR, h).apply()

        fun getTargetMinute(ctx: Context): Int =
            prefs(ctx).getInt(KEY_TARGET_MIN, DEFAULT_MINUTE)

        fun setTargetMinute(ctx: Context, m: Int) =
            prefs(ctx).edit().putInt(KEY_TARGET_MIN, m).apply()

        private fun prefs(ctx: Context) =
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // ── Widget update ──────────────────────────────────────────────────────────

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val now = LocalDateTime.now()
            val intervalMinutes = getInterval(context)

            // Apply theme
            applyTheme(context, views)

            views.setTextViewText(R.id.tv_label, getLabel(context))

            // Minutes column visible unless interval is 60 min
            val showMinutes = intervalMinutes < 60
            views.setViewVisibility(R.id.minutes_col,   if (showMinutes) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.separator_hm,  if (showMinutes) View.VISIBLE else View.GONE)

            when (val target = getTarget(context, now)) {
                null -> {
                    views.setViewVisibility(R.id.countdown_container, View.GONE)
                    views.setViewVisibility(R.id.tv_now, View.VISIBLE)
                }
                else -> {
                    val total   = Duration.between(now, target).toMinutes().coerceAtLeast(0)
                    val days    = total / (24 * 60)
                    val hours   = (total % (24 * 60)) / 60
                    val minutes = total % 60

                    views.setViewVisibility(R.id.countdown_container, View.VISIBLE)
                    views.setViewVisibility(R.id.tv_now, View.GONE)
                    views.setTextViewText(R.id.tv_days,    "%02d".format(days))
                    views.setTextViewText(R.id.tv_hours,   "%02d".format(hours))
                    views.setTextViewText(R.id.tv_minutes, "%02d".format(minutes))
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun applyTheme(ctx: Context, views: RemoteViews) {
            when (getTheme(ctx)) {
                "light" -> {
                    views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background_light)
                    val dark = 0xFF111111.toInt()
                    val med  = 0x99111111.toInt()
                    val dim  = 0x55111111.toInt()
                    views.setTextColor(R.id.tv_days,        dark)
                    views.setTextColor(R.id.tv_hours,       dark)
                    views.setTextColor(R.id.tv_minutes,     dark)
                    views.setTextColor(R.id.tv_unit_days,   dim)
                    views.setTextColor(R.id.tv_unit_hours,  dim)
                    views.setTextColor(R.id.tv_unit_minutes,dim)
                    views.setTextColor(R.id.tv_sep_1,       med)
                    views.setTextColor(R.id.separator_hm,   med)
                    views.setTextColor(R.id.tv_now,         dark)
                    views.setTextColor(R.id.tv_label,       dim)
                }
                "clear" -> {
                    views.setInt(R.id.widget_root, "setBackgroundColor", Color.TRANSPARENT)
                    setWhiteTextColors(views)
                }
                "custom" -> {
                    val hue   = getCustomHue(ctx)
                    val alpha = getCustomAlpha(ctx)
                    val hsv   = floatArrayOf(hue, 0.60f, 0.15f)
                    val bg    = Color.HSVToColor(alpha, hsv)
                    views.setInt(R.id.widget_root, "setBackgroundColor", bg)
                    setWhiteTextColors(views)
                }
                else -> { // "dark" (default)
                    views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background)
                    setWhiteTextColors(views)
                }
            }
        }

        private fun setWhiteTextColors(views: RemoteViews) {
            views.setTextColor(R.id.tv_days,         0xFFFFFFFF.toInt())
            views.setTextColor(R.id.tv_hours,        0xFFFFFFFF.toInt())
            views.setTextColor(R.id.tv_minutes,      0xFFFFFFFF.toInt())
            views.setTextColor(R.id.tv_unit_days,    0x55FFFFFF.toInt())
            views.setTextColor(R.id.tv_unit_hours,   0x55FFFFFF.toInt())
            views.setTextColor(R.id.tv_unit_minutes, 0x55FFFFFF.toInt())
            views.setTextColor(R.id.tv_sep_1,        0x33FFFFFF.toInt())
            views.setTextColor(R.id.separator_hm,    0x33FFFFFF.toInt())
            views.setTextColor(R.id.tv_now,          0xCCFFFFFF.toInt())
            views.setTextColor(R.id.tv_label,        0x33FFFFFF.toInt())
        }

        // ── Scheduling ─────────────────────────────────────────────────────────────

        fun scheduleNextUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = buildPendingIntent(context)
            val intervalMs = getInterval(context) * 60_000L
            val nextFire = ((System.currentTimeMillis() / intervalMs) + 1) * intervalMs

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC, nextFire, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, nextFire, pendingIntent)
            }
        }

        private fun buildPendingIntent(context: Context) = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, CountdownWidget::class.java).apply { action = ACTION_UPDATE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── Target calculation ─────────────────────────────────────────────────────

        fun getTarget(ctx: Context, now: LocalDateTime): LocalDateTime? {
            val targetDay    = DayOfWeek.of(getTargetDay(ctx))
            val targetTime   = LocalTime.of(getTargetHour(ctx), getTargetMinute(ctx))
            val windowEndMin = DEFAULT_WINDOW_MIN
            val windowEnd    = targetTime.plusMinutes(windowEndMin.toLong())

            val time = now.toLocalTime()
            return if (now.dayOfWeek == targetDay) {
                when {
                    time < targetTime -> now.toLocalDate().atTime(targetTime)
                    time <= windowEnd -> null   // show ♡
                    else -> now.toLocalDate().plusDays(7).atTime(targetTime)
                }
            } else {
                val daysUntil = (targetDay.value - now.dayOfWeek.value + 7) % 7
                now.toLocalDate().plusDays(daysUntil.toLong()).atTime(targetTime)
            }
        }
    }
}
