package ir.samansaz.shiftcalendar

    /** نوع شیفت: نام نمایشی، ساعت کاری، رنگ، مخفف */
enum class ShiftType(val label: String, val hours: Int, val color: Int, val short: String) {
        DAY("روز", 12, 0xFF2E7D32.toInt(), "روز"),
        NIGHT("شب", 12, 0xFF1565C0.toInt(), "شب"),
        MANEUVER("مانور", 12, 0xFFEF6C00.toInt(), "مانور"),
        RESERVE("رزرو", 12, 0xFF6A1B9A.toInt(), "رزرو"),
        FREIGHT("باری", 12, 0xFF795548.toInt(), "باری"),
        PASSENGER("مسافری", 12, 0xFF00838F.toInt(), "مسافری"),
        REST("استراحت", 0, 0xFF9E9E9E.toInt(), "استراحت"),
        LEAVE("مرخصی", 0, 0xFFC62828.toInt(), "مرخصی")
    }

fun parseShiftPattern(text: String): List<ShiftType> =
    text.split('،', ',').map { label ->
        ShiftType.entries.firstOrNull { it.label == label.trim() }
            ?: throw IllegalArgumentException("نام شیفت نامعتبر است")
    }

fun repeatShifts(pattern: List<ShiftType>, days: Int): List<ShiftType> {
    require(pattern.isNotEmpty() && days in 29..31)
    return List(days) { pattern[it % pattern.size] }
}

fun monthlyReport(year: Int, month: Int, shifts: List<ShiftType>): String = buildString {
    append("گزارش ${JalaliDate.monthNames[month - 1]} ${JalaliDate.faDigits(year.toString())}\n")
    for (shift in ShiftType.entries) {
        append("• ${shift.label}: ${JalaliDate.faDigits(shifts.count { it == shift }.toString())} روز\n")
    }
    append("روزهای کاری: ${JalaliDate.faDigits(shifts.count { it.hours > 0 }.toString())}\n")
    append("مجموع ساعت کاری: ${JalaliDate.faDigits(shifts.sumOf { it.hours }.toString())} ساعت")
}
