package com.saph.countdown

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
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
    private lateinit var previewLabelView: TextView

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
            setPadding(0, 28, 0, 4)
        }

        previewLabelView = TextView(this).apply {
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
        }
        previewCard.addView(previewDisplay)
        previewCard.addView(previewLabelView)

        val cardWrapper = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 40 }
            setBackgroundColor(0xFF111111.toInt())
        }
        cardWrapper.addView(previewCard)
        root.addView(cardWrapper)

        // ── Label editor ───────────────────────────────────────
        root.addView(sectionLabel("widget label"))

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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(editLabel)

        root.addView(space(12))

        root.addView(Button(this).apply {
            text = "save & update widget"
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFFFFFFFF.toInt())
            letterSpacing = 0.05f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { saveLabel() }
        })

        root.addView(space(40))

        // ── Hints ──────────────────────────────────────────────
        listOf(
            "updates every time you unlock your phone  ⚡",
            "long-press home screen → widgets to add",
            "cover screen: settings → cover screen → widgets",
            "lock screen: settings → lock screen → edit lock screen → widgets"
        ).forEach { msg ->
            root.addView(TextView(this).apply {
                text = msg
                textSize = 11f
                setTextColor(0x33FFFFFF)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 8)
            })
        }

        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        editLabel.setText(CountdownWidget.getLabel(this))
        updatePreviewDisplay()
    }

    private fun saveLabel() {
        val newLabel = editLabel.text.toString().trim().ifEmpty { CountdownWidget.DEFAULT_LABEL }
        CountdownWidget.setLabel(this, newLabel)

        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, CountdownWidget::class.java))
        ids.forEach { CountdownWidget.updateAppWidget(this, manager, it) }
        CountdownWidget.scheduleNextUpdate(this)

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

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 11f
        setTextColor(0x66FFFFFF)
        letterSpacing = 0.1f
        setPadding(4, 0, 0, 8)
    }

    private fun space(dp: Int) = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (dp * resources.displayMetrics.density).toInt()
        )
    }
}
