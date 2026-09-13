package com.smtutt.app.data.parser

import android.content.Context
import com.smtutt.app.ui.viewmodel.WeekFilter
import java.util.Calendar
import java.util.TimeZone

object WeekDetector {

    private const val PREFS_NAME = "smtu_week_prefs"
    private const val KEY_ANCHOR_WEEK_NUM = "anchor_week_num"
    private const val KEY_ANCHOR_WEEK_TYPE = "anchor_week_type"
    private const val KEY_LAST_UPDATE_TIME = "last_update_time"

    private val moscowTz = TimeZone.getTimeZone("Europe/Moscow")

    /**
     * Save the week detected from SMTU website.
     */
    fun saveDetectedWeek(context: Context, detectedWeek: WeekFilter) {
        val cal = getMoscowCalendar(System.currentTimeMillis())
        val weekNum = getAbsoluteWeekNumber(cal)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_ANCHOR_WEEK_NUM, weekNum)
            .putString(KEY_ANCHOR_WEEK_TYPE, detectedWeek.name)
            .putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Calculate current week (Upper or Lower) based on saved anchor and current date.
     */
    fun getCurrentWeek(context: Context): WeekFilter {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val anchorWeekNum = prefs.getInt(KEY_ANCHOR_WEEK_NUM, -1)
        val anchorWeekTypeStr = prefs.getString(KEY_ANCHOR_WEEK_TYPE, null)

        val nowCal = getMoscowCalendar(System.currentTimeMillis())
        val isSunday = nowCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

        // If today is Sunday, students prepare for tomorrow (Monday), so advance by 1 day
        val effectiveCal = getMoscowCalendar(
            if (isSunday) System.currentTimeMillis() + 86400000L else System.currentTimeMillis()
        )
        val currentWeekNum = getAbsoluteWeekNumber(effectiveCal)

        if (anchorWeekNum != -1 && anchorWeekTypeStr != null) {
            val anchorWeekType = try {
                WeekFilter.valueOf(anchorWeekTypeStr)
            } catch (e: Exception) {
                WeekFilter.LOWER
            }

            val weekDiff = Math.abs(currentWeekNum - anchorWeekNum)
            return if (weekDiff % 2 == 0) {
                anchorWeekType
            } else {
                if (anchorWeekType == WeekFilter.UPPER) WeekFilter.LOWER else WeekFilter.UPPER
            }
        }

        // Fallback calculation: In SMTU September 2026, week starting Monday Sep 14 is UPPER (week starting Sep 7 is LOWER).
        // Standard academic semester: 1st week of September (starting Sep 1 or first Monday) is usually Lower/1 or Upper.
        // On Sep 13, 2026 smtu.ru stated LOWER. Sep 14 starts UPPER.
        val fallbackAnchorWeekNum = getAbsoluteWeekNumber(getMoscowCalendar(1789344000000L)) // mid Sep 2026
        val diff = Math.abs(currentWeekNum - fallbackAnchorWeekNum)
        return if (diff % 2 == 0) WeekFilter.LOWER else WeekFilter.UPPER
    }

    /**
     * Returns 0 for Monday, 1 for Tuesday, ..., 5 for Saturday.
     * On Sunday, returns 0 (Monday) to show the upcoming week's first day!
     */
    fun getDefaultDayIndex(): Int {
        val cal = getMoscowCalendar(System.currentTimeMillis())
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 0 // Tomorrow is Monday!
            else -> 0
        }
    }

    private fun getMoscowCalendar(timestamp: Long): Calendar {
        val cal = Calendar.getInstance(moscowTz)
        cal.timeInMillis = timestamp
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.minimalDaysInFirstWeek = 4
        return cal
    }

    private fun getAbsoluteWeekNumber(cal: Calendar): Int {
        val year = cal.get(Calendar.YEAR)
        val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
        return year * 53 + weekOfYear
    }
}
