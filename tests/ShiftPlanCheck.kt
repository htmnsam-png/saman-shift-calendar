package ir.samansaz.shiftcalendar

fun main() {
    val pattern = parseShiftPattern(" روز، شب, استراحت ")
    check(pattern == listOf(ShiftType.DAY, ShiftType.NIGHT, ShiftType.REST))
    for (length in listOf(29, 30, 31)) {
        val shifts = repeatShifts(pattern, length)
        check(shifts.size == length)
        check(shifts.last() == pattern[(length - 1) % 3])
        val report = monthlyReport(1405, 8, shifts)
        check(report.contains("روز: ${JalaliDate.faDigits(((length + 2) / 3).toString())}"))
        check(report.contains("${JalaliDate.faDigits((shifts.sumOf { it.hours }).toString())} ساعت"))
    }
    for (bad in listOf("", "روز،", "ناشناخته", "روز,,شب")) {
        check(runCatching { parseShiftPattern(bad) }.isFailure)
    }
    check(runCatching { repeatShifts(emptyList(), 30) }.isFailure)
    val emptyReport = monthlyReport(1405, 8, emptyList())
    check(emptyReport.contains("مجموع ساعت کاری: ۰ ساعت"))
    check(ShiftType.entries.all { emptyReport.contains("${it.label}: ۰ روز") })
    check(JalaliDate.fromGregorian(2026, 11, 9) == JalaliDate.Jalali(1405, 8, 18))
    check(JalaliDate.toGregorian(1405, 1, 1) == Triple(2026, 3, 21))
    check(JalaliDate.jalaliMonthLength(1405, 12) == 29)
    check(JalaliDate.jalaliMonthLength(1403, 12) == 30)
    println("PASS: patterns, invalid input, 29/30/31 days, reports, Jalali regression")
}
