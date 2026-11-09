package ir.samansaz.shiftcalendar

import java.util.Calendar

/**
 * تبدیل تاریخ میلادی <-> جلالی (تقویم هجری شمسی).
 * الگوریتم jalaali-js (رووزبه پورنادر / بهرنگ نوروزی‌نیا)؛
 * درستی آن با ۱۰ تاریخ مرجع بررسی شده است، از جمله:
 * ۲۰۲۶-۱۱-۰۹ میلادی = ۱۸ آبان ۱۴۰۵، ۲۰۲۶-۰۳-۲۱ = ۱ فروردین ۱۴۰۵،
 * ۲۰۲۶-۰۹-۲۹ = ۷ مهر ۱۴۰۵، ۲۰۲۶-۱۲-۲۹ = ۸ دی ۱۴۰۵.
 * هیچ وابستگی خارجی ندارد.
 */
object JalaliDate {

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    data class Jalali(val year: Int, val month: Int, val day: Int)

    private fun div(a: Int, b: Int): Int = a / b // تقسیم صحیح کوتلین به سمت صفر گرد می‌شود، مثل ~~(a/b) در جاوااسکریپت

    private fun mod(a: Int, b: Int): Int = a - div(a, b) * b

    private data class JalCal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalCal {
        val bl = breaks.size
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        require(jy >= jp && jy < breaks[bl - 1]) { "Invalid Jalaali year $jy" }
        for (i in 1 until bl) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
        var leap = mod(mod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return JalCal(leap, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
            div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
        d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    private fun d2g(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j += div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return Triple(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
    }

    private fun d2j(jdn: Int): Jalali {
        val gy = d2g(jdn).first
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(gy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                val jm = 1 + div(k, 31)
                val jd = mod(k, 31) + 1
                return Jalali(jy, jm, jd)
            } else {
                k -= 186
            }
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        val jm = 7 + div(k, 30)
        val jd = mod(k, 30) + 1
        return Jalali(jy, jm, jd)
    }

    /** میلادی -> جلالی */
    fun fromGregorian(year: Int, month: Int, day: Int): Jalali = d2j(g2d(year, month, day))

    /** جلالی -> میلادی (سال، ماه، روز) */
    fun toGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> = d2g(j2d(jy, jm, jd))

    fun isLeapJalaliYear(jy: Int): Boolean = jalCal(jy).leap == 0

    fun jalaliMonthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeapJalaliYear(jy)) 30 else 29
    }

    /** امروز به جلالی */
    fun today(): Jalali {
        val c = Calendar.getInstance()
        return fromGregorian(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** روز هفته‌ی اول ماه جلالی: ۰=شنبه، ۱=یکشنبه، ... ۶=جمعه */
    fun firstWeekdayOfMonth(jy: Int, jm: Int): Int {
        val (gy, gm, gd) = toGregorian(jy, jm, 1)
        val c = Calendar.getInstance()
        c.set(gy, gm - 1, gd)
        // Calendar.SATURDAY=7 ... FRIDAY=6؛ شنبه را ۰ می‌کنیم
        return (c.get(Calendar.DAY_OF_WEEK) + 1) % 7
    }

    val monthNames = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    val weekdayNames = arrayOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    /** ارقام فارسی */
    fun faDigits(s: String): String {
        val fa = arrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return s.map { if (it in '0'..'9') fa[it - '0'] else it }.joinToString("")
    }
}
