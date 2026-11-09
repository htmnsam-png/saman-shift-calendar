package ir.samansaz.shiftcalendar

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ir.samansaz.shiftcalendar.databinding.ActivityMainBinding

/**
 * نسخه‌ی اول (MVP) — تقویم جلالی + ثبت شیفت + محاسبه‌گر ساعت کاری.
 * مخصوص کارکنان راه‌آهن و لوکوموتیوران‌ها؛ ساخته‌ی «سامان‌ساز».
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private var shownYear: Int = 1405
    private var shownMonth: Int = 8

    private val gregorianMonthNames = arrayOf(
        "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("shifts", MODE_PRIVATE)
        if (prefs.contains("dark_mode")) {
            delegate.localNightMode = if (prefs.getBoolean("dark_mode", false))
                AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.layoutDirection = View.LAYOUT_DIRECTION_RTL

        prefs = getSharedPreferences("shifts", MODE_PRIVATE)

        val today = JalaliDate.today()
        shownYear = savedInstanceState?.getInt("year") ?: today.year
        shownMonth = savedInstanceState?.getInt("month") ?: today.month

        binding.btnPrev.setOnClickListener { moveMonth(-1) }
        binding.btnNext.setOnClickListener { moveMonth(1) }
        binding.btnToday.setOnClickListener {
            val t = JalaliDate.today()
            shownYear = t.year
            shownMonth = t.month
            renderCalendar()
        }
        binding.calcButton.setOnClickListener { calculateHours() }
        binding.patternButton.setOnClickListener { showPatternEditor() }
        binding.shareButton.setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, monthReport())
            }, "اشتراک‌گذاری گزارش ماه"))
        }
        binding.themeButton.text = if (isDark()) "حالت روشن" else "حالت تیره"
        binding.themeButton.setOnClickListener {
            val dark = !isDark()
            prefs.edit().putBoolean("dark_mode", dark).apply()
            delegate.localNightMode = if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        }

        // اطلاعات امروز
        val (gy, gm, gd) = JalaliDate.toGregorian(today.year, today.month, today.day)
        val weekday = JalaliDate.weekdayNames[(JalaliDate.firstWeekdayOfMonth(today.year, today.month) + today.day - 1) % 7]
        binding.todayInfo.text =
            "امروز: $weekday ${JalaliDate.faDigits(today.day.toString())} " +
            "${JalaliDate.monthNames[today.month - 1]} ${JalaliDate.faDigits(today.year.toString())}" +
            " — ${JalaliDate.faDigits(gd.toString())} ${gregorianMonthNames[gm - 1]} $gy"

        renderCalendar()
    }

    private fun moveMonth(delta: Int) {
        shownMonth += delta
        if (shownMonth < 1) {
            shownMonth = 12
            shownYear -= 1
        } else if (shownMonth > 12) {
            shownMonth = 1
            shownYear += 1
        }
        renderCalendar()
    }

    private fun shiftKey(year: Int, month: Int, day: Int) = "$year-$month-$day"

    private fun getShift(year: Int, month: Int, day: Int): ShiftType? {
        val code = prefs.getString(shiftKey(year, month, day), null) ?: return null
        return ShiftType.entries.firstOrNull { it.name == code }
    }

    private fun setShift(year: Int, month: Int, day: Int, shift: ShiftType?) {
        prefs.edit().apply {
            if (shift == null) remove(shiftKey(year, month, day)) else putString(shiftKey(year, month, day), shift.name)
        }.apply()
    }

    private fun renderCalendar() {
        binding.monthTitle.text =
            "${JalaliDate.monthNames[shownMonth - 1]} ${JalaliDate.faDigits(shownYear.toString())}"

        val grid = binding.calendarGrid
        grid.removeAllViews()
        grid.columnCount = 7
        grid.rowCount = 6

        val today = JalaliDate.today()
        val monthLength = JalaliDate.jalaliMonthLength(shownYear, shownMonth)
        val firstWeekday = JalaliDate.firstWeekdayOfMonth(shownYear, shownMonth) // ۰=شنبه

        var day = 1
        for (row in 0 until 6) {
            for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(4, 10, 4, 10)
                }
                val params = GridLayout.LayoutParams(
                    GridLayout.spec(row, 1f),
                    GridLayout.spec(col, 1f)
                ).apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(2, 2, 2, 2)
                }
                cell.layoutParams = params

                if (cellIndex >= firstWeekday && day <= monthLength) {
                    val d = day
                    val dayNum = TextView(this).apply {
                        text = JalaliDate.faDigits(d.toString())
                        textSize = 17f
                        gravity = Gravity.CENTER
                    }
                    val shiftLabel = TextView(this).apply {
                        textSize = 11f
                        gravity = Gravity.CENTER
                    }
                    cell.addView(dayNum)
                    cell.addView(shiftLabel)

                    val isToday = shownYear == today.year && shownMonth == today.month && d == today.day
                    val shift = getShift(shownYear, shownMonth, d)
                    if (shift != null) {
                        cell.setBackgroundColor(shift.color)
                        dayNum.setTextColor(Color.WHITE)
                        shiftLabel.setTextColor(Color.WHITE)
                        shiftLabel.text = shift.short
                    } else if (isToday) {
                        cell.setBackgroundColor(0xFFFFF176.toInt()) // زرد روشن برای امروز
                    } else {
                        cell.setBackgroundColor(if (isDark()) 0xFF303030.toInt() else 0xFFEEEEEE.toInt())
                    }
                    if (isToday && shift == null) {
                        dayNum.setTextColor(Color.BLACK)
                    }

                    cell.setOnClickListener { showShiftPicker(d) }
                    day++
                } else {
                    cell.setBackgroundColor(Color.TRANSPARENT)
                }
                grid.addView(cell)
            }
        }
        renderSummary()
    }

    private fun showShiftPicker(day: Int) {
        val labels = ShiftType.entries.map { "${it.label} (${JalaliDate.faDigits(it.hours.toString())} ساعت)" }.toTypedArray()
        val items = labels + "حذف شیفت"
        AlertDialog.Builder(this)
            .setTitle("شیفت ${JalaliDate.faDigits(day.toString())} ${JalaliDate.monthNames[shownMonth - 1]} چیست؟")
            .setItems(items) { _, which ->
                if (which < ShiftType.entries.size) {
                    setShift(shownYear, shownMonth, day, ShiftType.entries[which])
                } else {
                    setShift(shownYear, shownMonth, day, null)
                }
                renderCalendar()
            }
            .setNegativeButton("بستن", null)
            .show()
    }

    private fun isDark() = resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("year", shownYear)
        outState.putInt("month", shownMonth)
        super.onSaveInstanceState(outState)
    }

    private fun monthReport() = monthlyReport(shownYear, shownMonth,
        (1..JalaliDate.jalaliMonthLength(shownYear, shownMonth)).mapNotNull {
            getShift(shownYear, shownMonth, it)
        })

    private fun renderSummary() {
        binding.summaryText.text = monthReport()
    }

    private fun showPatternEditor() {
        val input = EditText(this).apply {
            hint = "روز، شب، استراحت"
            setText(prefs.getString("pattern", "روز، شب، استراحت"))
            contentDescription = "الگوی شیفت با جداکننده ویرگول"
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("الگو از روز اول ماه تکرار می‌شود")
            .setMessage("شیفت‌ها را به ترتیب با ویرگول جدا کن: " +
                ShiftType.entries.joinToString("، ") { it.label })
            .setView(input)
            .setPositiveButton("پرکردن ماه", null)
            .setNegativeButton("انصراف", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pattern = try {
                    parseShiftPattern(input.text.toString())
                } catch (_: IllegalArgumentException) {
                    input.error = "نام شیفت‌ها و جداکننده را بررسی کن"
                    return@setOnClickListener
                }
                val days = repeatShifts(pattern, JalaliDate.jalaliMonthLength(shownYear, shownMonth))
                val applyPattern = {
                    val editor = prefs.edit().putString("pattern", pattern.joinToString("، ") { it.label })
                    days.forEachIndexed { index, shift ->
                        editor.putString(shiftKey(shownYear, shownMonth, index + 1), shift.name)
                    }
                    editor.apply()
                    renderCalendar()
                    dialog.dismiss()
                }
                if (days.indices.any { getShift(shownYear, shownMonth, it + 1) != null }) {
                    AlertDialog.Builder(this)
                        .setTitle("جایگزینی شیفت‌های ماه؟")
                        .setMessage("شیفت‌های ثبت‌شده این ماه با الگوی جدید جایگزین می‌شوند.")
                        .setPositiveButton("جایگزین کن") { _, _ -> applyPattern() }
                        .setNegativeButton("انصراف", null).show()
                } else applyPattern()
            }
        }
        dialog.show()
    }

    private fun parseTime(text: String): Int? {
        // ارقام فارسی را به لاتین تبدیل می‌کند؛ قالب مورد قبول: HH:MM
        val latin = text.trim().map { c ->
            when (c) {
                in '۰'..'۹' -> ('0' + (c - '۰'))
                in '٠'..'٩' -> ('0' + (c - '٠'))
                else -> c
            }
        }.joinToString("")
        val parts = latin.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    private fun calculateHours() {
        val start = parseTime(binding.calcStart.text.toString())
        val end = parseTime(binding.calcEnd.text.toString())
        if (start == null || end == null) {
            Toast.makeText(this, "ساعت را به شکل ۰۷:۰۰ وارد کن", Toast.LENGTH_SHORT).show()
            return
        }
        var diff = end - start
        if (diff < 0) diff += 24 * 60 // شیفت شب که از نیمه‌شب می‌گذرد
        if (diff == 0) diff = 24 * 60
        val h = diff / 60
        val m = diff % 60
        binding.calcResult.text =
            "مدت شیفت: ${JalaliDate.faDigits(h.toString())} ساعت" +
            if (m > 0) " و ${JalaliDate.faDigits(m.toString())} دقیقه" else ""
    }
}
