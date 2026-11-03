package com.example

import com.example.engine.nlp.BengaliDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun testBengaliDigitConversion() {
        assertEquals("০", BengaliDateUtils.toBengaliDigits(0))
        assertEquals("১", BengaliDateUtils.toBengaliDigits(1))
        assertEquals("১২", BengaliDateUtils.toBengaliDigits(12))
        assertEquals("২০২৬", BengaliDateUtils.toBengaliDigits(2026))
    }

    @Test
    fun testBengaliTimeFormatting() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        val spokenTime = BengaliDateUtils.getSpokenBengaliTime(calendar)
        assertTrue(spokenTime.contains("দুপুর"))
        assertTrue(spokenTime.contains("সাড়ে ২ টা"))
    }

    @Test
    fun testEnglishTimeFormatting() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 15)
        }
        val spokenTime = BengaliDateUtils.getSpokenEnglishTime(calendar)
        assertEquals("The time is 9:15 AM.", spokenTime)
    }

    @Test
    fun testBengaliDayFormatting() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        val day = BengaliDateUtils.getSpokenBengaliDay(calendar)
        assertEquals("আজ রবিবার।", day)
    }
}
