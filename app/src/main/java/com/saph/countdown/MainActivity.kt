package com.saph.countdown

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class MainActivity : AppCompatActivity() {

    private lateinit var previewText: TextView
    private lateinit var permissionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF0A0A0A.toInt())
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "until wednesday"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
        }

        val subtitle = TextView(this).apply {
            text = "widget preview"
            textSize = 11f
            setTextColor(0x66FFFFFF)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 48)
        }

        previewText = TextView(this).apply {
            textSize = 14f
            setTextColor(0xAAFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val hint = TextView(this).apply {
            text = "long-press your home screen\nto add the widget"
            textSize = 12f
            setTextColor(0x55FFFFFF)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        permissionButton = Button(this).apply {
            text = "grant exact alarm permission"
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1A1A1A.toInt())
            visibility = android.view.View.GONE
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(previewText)
        root.addView(hint)
        root.addView(permissionButton)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        updatePreview()
        checkExactAlarmPermission()
    }

    private fun updatePreview() {
        val now = LocalDateTime.now()
        val targetTime = LocalTime.of(18, 30)
        val windowEnd = LocalTime.of(21, 0)
        val time = now.toLocalTime()

        val display = if (now.dayOfWeek == DayOfWeek.WEDNESDAY && time >= targetTime && time <= windowEnd) {
            "♡"
        } else {
            val target = if (now.dayOfWeek == DayOfWeek.WEDNESDAY) {
                when {
                    time < targetTime -> now.toLocalDate().atTime(targetTime)
                    else -> now.toLocalDate().plusDays(7).atTime(targetTime)
                }
            } else {
                val daysUntil = (DayOfWeek.WEDNESDAY.value - now.dayOfWeek.value + 7) % 7
                now.toLocalDate().plusDays(daysUntil.toLong()).atTime(targetTime)
            }
            val total = Duration.between(now, target).toMinutes().coerceAtLeast(0)
            val days = total / (24 * 60)
            val hours = (total % (24 * 60)) / 60
            val minutes = total % 60
            "%02dd  %02dh  %02dm".format(days, hours, minutes)
        }
        previewText.text = display
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            permissionButton.visibility = if (alarmManager.canScheduleExactAlarms())
                android.view.View.GONE else android.view.View.VISIBLE
        }
    }
}
