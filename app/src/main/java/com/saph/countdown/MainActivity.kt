package com.saph.countdown

import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var editLabel: EditText
    private lateinit var previewDisplay: TextView
    private lateinit var previewLabelView: TextView

    // Interval
    private val intervalValues = listOf(1, 5, 15, 30, 60)
    private val intervalLabels = listOf("1m", "5m", "15m", "30m", "1h")
    private val intervalBtns = mutableListOf<TextView>()

    // Day picker
    private val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    private val dayBtns = mutableListOf<TextView>()

    // Time
    private lateinit var timePicker: TextView

    // Theme
    private val themeKeys   = listOf("dark", "light", "clear", "custom")
    private val themeLabels = listOf("Dark", "Light", "Clear", "Color")
    private val themeBtns   = mutableListOf<TextView>()
    private lateinit var colorSection: LinearLayout
    private lateinit var hueSeek: SeekBar
    private lateinit var alphaSeek: SeekBar
    private lateinit var colorPreview: View

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

        // ── Title ──────────────────────────────────────────────────────────────────
        root.addView(TextView(this).apply {
            text = "until wednesday"
            textSize = 26f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        })

        root.addView(space(24))

        // ── Live preview card ──────────────────────────────────────────────────────
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

        // ── Widget label ───────────────────────────────────────────────────────────
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

        root.addView(space(32))

        // ── Update interval ────────────────────────────────────────────────────────
        root.addView(sectionLabel("update interval"))
        root.addView(buildSegmentRow(
            intervalLabels,
            getSelectedIndexForInterval(CountdownWidget.getInterval(this))
        ) { idx ->
            CountdownWidget.setInterval(this, intervalValues[idx])
            refreshIntervalButtons(idx)
            scheduleAndRefresh()
        }.also {
            intervalBtns.addAll(it.second)
        }.first)

        root.addView(space(32))

        // ── Target day ────────────────────────────────────────────────────────────
        root.addView(sectionLabel("target day"))
        root.addView(buildSegmentRow(
            dayLabels,
            CountdownWidget.getTargetDay(this) - 1   // DayOfWeek value is 1-based
        ) { idx ->
            CountdownWidget.setTargetDay(this, idx + 1)
            refreshDayButtons(idx)
            scheduleAndRefresh()
        }.also {
            dayBtns.addAll(it.second)
        }.first)

        root.addView(space(32))

        // ── Target time ───────────────────────────────────────────────────────────
        root.addView(sectionLabel("target time"))
        timePicker = buildPillButton(
            timeLabel(CountdownWidget.getTargetHour(this), CountdownWidget.getTargetMinute(this)),
            selected = false
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { openTimePicker() }
        }
        root.addView(timePicker)

        root.addView(space(32))

        // ── Theme ─────────────────────────────────────────────────────────────────
        root.addView(sectionLabel("theme"))
        root.addView(buildSegmentRow(
            themeLabels,
            themeKeys.indexOf(CountdownWidget.getTheme(this)).coerceAtLeast(0)
        ) { idx ->
            CountdownWidget.setTheme(this, themeKeys[idx])
            refreshThemeButtons(idx)
            colorSection.visibility = if (themeKeys[idx] == "custom") View.VISIBLE else View.GONE
            scheduleAndRefresh()
        }.also {
            themeBtns.addAll(it.second)
        }.first)

        root.addView(space(16))

        // ── Custom color section ───────────────────────────────────────────────────
        colorSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (CountdownWidget.getTheme(this@MainActivity) == "custom") View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        colorPreview = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
            ).also { it.bottomMargin = dp(12) }
            val gd = GradientDrawable()
            gd.cornerRadius = dp(12).toFloat()
            gd.setColor(previewColor())
            background = gd
        }
        colorSection.addView(colorPreview)

        colorSection.addView(sliderLabel("hue"))
        hueSeek = SeekBar(this).apply {
            max = 360
            progress = CountdownWidget.getCustomHue(this@MainActivity).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, v: Int, fromUser: Boolean) {
                    if (fromUser) {
                        CountdownWidget.setCustomHue(this@MainActivity, v.toFloat())
                        updateColorPreview()
                        scheduleAndRefresh()
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(12) }
        }
        colorSection.addView(hueSeek)

        colorSection.addView(sliderLabel("opacity"))
        alphaSeek = SeekBar(this).apply {
            max = 255
            progress = CountdownWidget.getCustomAlpha(this@MainActivity)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, v: Int, fromUser: Boolean) {
                    if (fromUser) {
                        CountdownWidget.setCustomAlpha(this@MainActivity, v)
                        updateColorPreview()
                        scheduleAndRefresh()
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(16) }
        }
        colorSection.addView(alphaSeek)
        root.addView(colorSection)

        root.addView(space(32))

        // ── Save button ────────────────────────────────────────────────────────────
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
            setOnClickListener { saveAll() }
        })

        root.addView(space(40))

        // ── Hints ─────────────────────────────────────────────────────────────────
        listOf(
            "updates every time you unlock your phone  ⚡",
            "long-press home screen → widgets to add",
            "cover screen: settings → cover screen → widgets"
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

    // ── Helpers ────────────────────────────────────────────────────────────────────

    /** Returns pair of (row LinearLayout, list of button TextViews) */
    private fun buildSegmentRow(
        labels: List<String>,
        selectedIdx: Int,
        onSelect: (Int) -> Unit
    ): Pair<LinearLayout, List<TextView>> {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val btns = labels.mapIndexed { i, lbl ->
            buildPillButton(lbl, i == selectedIdx).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .also { it.marginEnd = if (i < labels.size - 1) dp(6) else 0 }
                setOnClickListener { onSelect(i) }
            }
        }
        btns.forEach { row.addView(it) }
        return row to btns
    }

    private fun buildPillButton(label: String, selected: Boolean): TextView {
        return TextView(this).apply {
            text = label
            textSize = 13f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setTextColor(if (selected) 0xFF000000.toInt() else 0x99FFFFFF.toInt())
            setBackgroundColor(if (selected) 0xFFFFFFFF.toInt() else 0xFF1A1A1A.toInt())
        }
    }

    private fun refreshIntervalButtons(selected: Int) {
        intervalBtns.forEachIndexed { i, btn ->
            btn.setTextColor(if (i == selected) 0xFF000000.toInt() else 0x99FFFFFF.toInt())
            btn.setBackgroundColor(if (i == selected) 0xFFFFFFFF.toInt() else 0xFF1A1A1A.toInt())
        }
    }

    private fun refreshDayButtons(selected: Int) {
        dayBtns.forEachIndexed { i, btn ->
            btn.setTextColor(if (i == selected) 0xFF000000.toInt() else 0x99FFFFFF.toInt())
            btn.setBackgroundColor(if (i == selected) 0xFFFFFFFF.toInt() else 0xFF1A1A1A.toInt())
        }
    }

    private fun refreshThemeButtons(selected: Int) {
        themeBtns.forEachIndexed { i, btn ->
            btn.setTextColor(if (i == selected) 0xFF000000.toInt() else 0x99FFFFFF.toInt())
            btn.setBackgroundColor(if (i == selected) 0xFFFFFFFF.toInt() else 0xFF1A1A1A.toInt())
        }
    }

    private fun openTimePicker() {
        val h = CountdownWidget.getTargetHour(this)
        val m = CountdownWidget.getTargetMinute(this)
        TimePickerDialog(this, { _, hour, minute ->
            CountdownWidget.setTargetHour(this, hour)
            CountdownWidget.setTargetMinute(this, minute)
            timePicker.text = timeLabel(hour, minute)
            scheduleAndRefresh()
        }, h, m, true).show()
    }

    private fun timeLabel(h: Int, m: Int) = "%02d:%02d".format(h, m)

    private fun previewColor(): Int {
        val hsv = floatArrayOf(CountdownWidget.getCustomHue(this), 0.60f, 0.15f)
        return Color.HSVToColor(CountdownWidget.getCustomAlpha(this), hsv)
    }

    private fun updateColorPreview() {
        val gd = GradientDrawable()
        gd.cornerRadius = dp(12).toFloat()
        gd.setColor(previewColor())
        colorPreview.background = gd
    }

    private fun scheduleAndRefresh() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, CountdownWidget::class.java))
        ids.forEach { CountdownWidget.updateAppWidget(this, manager, it) }
        CountdownWidget.scheduleNextUpdate(this)
        updatePreviewDisplay()
    }

    private fun saveAll() {
        val newLabel = editLabel.text.toString().trim().ifEmpty { CountdownWidget.DEFAULT_LABEL }
        CountdownWidget.setLabel(this, newLabel)

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editLabel.windowToken, 0)

        scheduleAndRefresh()
        Toast.makeText(this, "widget updated", Toast.LENGTH_SHORT).show()
    }

    private fun updatePreviewDisplay() {
        val now = LocalDateTime.now()
        val targetTime = LocalTime.of(CountdownWidget.getTargetHour(this), CountdownWidget.getTargetMinute(this))
        val windowEnd  = targetTime.plusMinutes(CountdownWidget.DEFAULT_WINDOW_MIN.toLong())
        val targetDay  = DayOfWeek.of(CountdownWidget.getTargetDay(this))
        val time       = now.toLocalTime()
        val interval   = CountdownWidget.getInterval(this)

        val numbers = if (now.dayOfWeek == targetDay && time >= targetTime && time <= windowEnd) {
            "♡"
        } else {
            val target = CountdownWidget.getTarget(this, now)
                ?: return run { previewDisplay.text = "♡" }
            val total   = Duration.between(now, target).toMinutes().coerceAtLeast(0)
            val days    = total / (24 * 60)
            val hours   = (total % (24 * 60)) / 60
            val minutes = total % 60
            if (interval >= 60)
                "%02dd  %02dh".format(days, hours)
            else
                "%02dd  %02dh  %02dm".format(days, hours, minutes)
        }

        previewDisplay.text = numbers
        previewLabelView.text = CountdownWidget.getLabel(this)
    }

    private fun getSelectedIndexForInterval(intervalMinutes: Int): Int =
        intervalValues.indexOf(intervalMinutes).coerceAtLeast(2) // default 15m

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text.uppercase(Locale.getDefault())
        textSize = 10f
        setTextColor(0x55FFFFFF)
        letterSpacing = 0.15f
        setPadding(2, 0, 0, 10)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun sliderLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 10f
        setTextColor(0x55FFFFFF)
        letterSpacing = 0.1f
        setPadding(2, 0, 0, 6)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun space(dp: Int) = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (dp * resources.displayMetrics.density).toInt()
        )
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
