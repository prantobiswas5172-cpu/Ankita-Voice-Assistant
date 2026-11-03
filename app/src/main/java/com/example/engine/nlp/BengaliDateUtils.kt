package com.example.engine.nlp

import java.util.Calendar
import java.util.Locale

object BengaliDateUtils {

    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    private val bengaliMonths = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    private val bengaliDays = arrayOf(
        "", "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )

    fun toBengaliDigits(number: Int): String {
        val str = number.toString()
        val sb = StringBuilder()
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toBengaliDigits(str: String): String {
        val sb = StringBuilder()
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun getSpokenBengaliTime(calendar: Calendar = Calendar.getInstance()): String {
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val hour12 = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = calendar.get(Calendar.MINUTE)

        val period = when (hour24) {
            in 4..5 -> "ভোর"
            in 6..11 -> "সকাল"
            in 12..15 -> "দুপুর"
            in 16..17 -> "বিকেল"
            in 18..19 -> "সন্ধ্যা"
            else -> "রাত"
        }

        val hourBengali = toBengaliDigits(hour12)
        val minuteBengali = toBengaliDigits(minute)

        return if (minute == 0) {
            "এখন সময় $period $hourBengali টা।"
        } else if (minute == 30) {
            "এখন সময় $period সাড়ে $hourBengali টা।"
        } else if (minute == 15) {
            "এখন সময় $period সোয়া $hourBengali টা।"
        } else if (minute == 45) {
            val nextHour = if (hour12 == 12) 1 else hour12 + 1
            "এখন সময় $period পৌনে ${toBengaliDigits(nextHour)} টা।"
        } else {
            "এখন সময় $period $hourBengali টা বেজে $minuteBengali মিনিট।"
        }
    }

    fun getSpokenBengaliDate(calendar: Calendar = Calendar.getInstance()): String {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        val dayBengali = toBengaliDigits(day)
        val monthBengali = bengaliMonths[month]
        val yearBengali = toBengaliDigits(year)

        return "আজ $dayBengali $monthBengali, $yearBengali সাল।"
    }

    fun getSpokenBengaliDay(calendar: Calendar = Calendar.getInstance()): String {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayName = bengaliDays.getOrElse(dayOfWeek) { "আজকের দিন" }
        return "আজ $dayName।"
    }

    fun getSpokenEnglishTime(calendar: Calendar = Calendar.getInstance()): String {
        val hour12 = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = calendar.get(Calendar.MINUTE)
        val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

        return if (minute == 0) {
            "The time is $hour12 o'clock $amPm."
        } else {
            val minStr = if (minute < 10) "0$minute" else "$minute"
            "The time is $hour12:$minStr $amPm."
        }
    }

    fun getSpokenEnglishDate(calendar: Calendar = Calendar.getInstance()): String {
        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US) ?: ""
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val year = calendar.get(Calendar.YEAR)
        return "Today is $monthName $day, $year."
    }

    fun getSpokenEnglishDay(calendar: Calendar = Calendar.getInstance()): String {
        val dayName = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US) ?: ""
        return "Today is $dayName."
    }
}
