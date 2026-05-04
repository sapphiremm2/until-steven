package com.saph.countdown

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class MainActivity : AppCompatActivity() {

    private lateinit var editLabel: EditText
    private lateinit var previewDisplay: TextView
    private lateinit var permissionBanner: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFF080808.toInt())
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(56, 80, 56, 80)
        }

        // ── Title ──────────────────────────────────────────────
        root.addView(TextView(this).apply {
            text = "until wednesday"
            textSize = 26f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        })

        root.addView(space(24))

        // ── Live preview card ──────────────────────────────────
        previewDisplay = TextView(this).apply {
            textSize = 32f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setBackgroundResource(0)
            setPadding(0, 28, 0, 4)
        }

        val previewLabel = TextView(this).apply {
            textSize = 10f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(0x44FFFFFF)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
            setPadding(0, 0, 0, 28)
        }

        val previewCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF111111.toInt())
            // Round corners via outline — simple color bg for compatibility
            elevation = 0f
            setPadding(0, 0, 0, 0)
        }
        previewCard.addView(previewDisplay)
        previewCard.addView(previewLabel)

        val cardWrapper = FrameLayout(this).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 40
            layoutParams = lp
            setBackgroundColor(0xFF111111.toInt())
        }
        cardWrapper.addView(previewCard)
        root.addView(cardWrapper)

        // Store reference to update label text from prefs
        this.previewLabelView = previewLabel

        // ── Label editor ───────────────────────────────────────
        root.addView(TextView(this).apply {
            text = "widget label"
            textSize = 11f
            setTextColor(0x66FFFFFF)
            letterSpacing = 0.1f
            setPadding(4, 0, 0, 8)
        })

        editLabel = EditText(this).apply {
            hint = CountdownWidget.DEFAULT_LABEL
            setHintTextColor(0x33FFFFFF)
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setBackgroundColor(0xFF1A1A1A.toInt())
            setPadding(20, 16, 20, 16)
            filters = arrayOf(InputFilter.LengthFilter(32))
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_DONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
        }
        root.addView(editLabel)

        root.addView(space(12))

        // ── Save button ────────────────────────────────────────
        val saveBtn = Button(this).apply {
            text = "save & update widget"
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFFFFFFFF.toInt())
            letterSpacing = 0.05f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
            setOnClickListener { saveLabel() }
        }
        root.addView(saveBtn)

        root.addView(space(40))

        // ── Hint ───────────────────────────────────────────────
        root.addView(TextView(this).apply {
            text = "long-press home screen → widgets to add"
            textSize = 11f
            setTextColor(0x33FFFFFF)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        })

        // cover screen hint
        root.addView(TextView(this).apply {
            text = "cover screen: settings → cover screen → widgets"
            textSize = 11f
            setTextColor(0x33FFFFFF)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        })

        // lock screen hint
        root.addView(TextView(this).apply {
            text = "lock screen: settings → lock screen → edit lock screen → widgets"
            textSize = 11f
            setTextColor(0x33FFFFFF)
            gravity = Gravity.CENTER
        })

        root.addView(space(32))

        // ── Exact alarm permission banner ──────────────────────
        permissionBanner = TextView(this).apply {
            text = "⚠  tap to grant exact alarm permission\n(needed for per-minute updates)"
            textSize = 11f
            setTextColor(0xFFFFCC00.toInt())
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
            setBackgroundColor(0xFF1A1500.toInt())
            visibility = android.view.View.GONE
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            }
        }
        root.addView(permissionBanner)

        scroll.addView(root)
        setContentView(scroll)
    }

    // Held so onResume can update both the number display and label text
    private lateinit var previewLabelView: TextView

    override fun onResume() {
        super.onResume()
        editLabel.setText(CountdownWidget.getLabel(this))
        updatePreviewDisplay()
        checkExactAlarmPermission()
    }

    private fun saveLabel() {
        val newLabel = editLabel.text.toString().trim().ifEmpty { CountdownWidget.DEFAULT_LABEL }
        CountdownWidget.setLabel(this, newLabel)

        // Push update to all active widget instances and restart alarm chain
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, CountdownWidget::class.java))
        ids.forEach { CountdownWidget.updateAppWidget(this, manager, it) }
        CountdownWidget.scheduleNextUpdate(this)

        // Dismiss keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editLabel.windowToken, 0)

        Toast.makeText(this, "widget updated", Toast.LENGTH_SHORT).show()
        updatePreviewDisplay()
    }

    private fun updatePreviewDisplay() {
        val now = LocalDateTime.now()
        val targetTime = LocalTime.of(18, 30)
        val windowEnd = LocalTime.of(21, 0)
        val time = now.toLocalTime()

        val numbers = if (now.dayOfWeek == DayOfWeek.WEDNESDAY && time >= targetTime && time <= windowEnd) {
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

        previewDisplay.text = numbers
        previewLabelView.text = CountdownWidget.getLabel(this)
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            permissionBanner.visibility =
                if (am.canScheduleExactAlarms()) android.view.View.GONE
                else android.view.View.VISIBLE
        }
    }

    private fun space(dp: Int) = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (dp * resources.displayMetrics.density).toInt()
        )
    }
}
