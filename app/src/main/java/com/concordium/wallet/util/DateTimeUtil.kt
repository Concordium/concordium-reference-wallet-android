package com.concordium.wallet.util

import android.text.format.DateFormat
import com.concordium.wallet.App
import com.concordium.wallet.R
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtil {

    fun nowPlusMinutes(minutes: Int): Date {
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, minutes)
        return now.time
    }

    /**
     *
     * @param item Time string in the format yyyyMM
     */
    fun convertLongDate(time: String): String {
        val date = parseLongDate(time)
        if(date == null){
            return time
        }
        return formatDateAsLocalMedium(date)
    }

    /**
     *
     * @param item Time string in the format yyyyMM
     */
    fun convertShortDate(time: String): String {
        val date = parseShortDate(time)
        if(date == null){
            return time
        }
        return formatDateAsLocalMediumWithoutDay(date)
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        // Comparing in local time (because the dates are show in local time)
        val fmt = SimpleDateFormat("yyyyMMdd")
        return fmt.format(date1) == fmt.format(date2)
    }

    fun isToday(date: Date): Boolean {
        return isSameDay(Date(), date)
    }

    fun isYesterday(date: Date): Boolean {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(cal.time, date)
    }

    //region Parsing
    //************************************************************

    /**
     *
     * @param item Time string in the format YYYYMMDD
     */
    fun parseLongDate(time: String): Date? {
        try {
            val year = time.substring(0, 4)
            val month = time.substring(4, 6)
            val day = time.substring(6, time.length)
            val date = Calendar.getInstance()
            date.clear()
            date.set(year.toInt(), month.toInt() - 1, day.toInt())
            return date.time
        }
        catch(e:Exception){
            return null;
        }
    }

    /**
     *
     * @param item Time string in the format YYYYMM
     */
    fun parseShortDate(time: String): Date? {
        try {
            val year = time.substring(0, 4)
            val month = time.substring(4, time.length)
            val date = Calendar.getInstance()
            date.clear()
            date.set(year.toInt(), month.toInt() - 1, 1)
            return date.time
        }
        catch(e:Exception){
            return null;
        }
    }

    //endregion

    //region Formatting
    //************************************************************

    fun formatDate(date: Date): String {
        //Log.d(DateFormat.getDateFormat(App.appContext).format(date))          // 15/06/2018       6/15/18
        //Log.d(DateFormat.getMediumDateFormat(App.appContext).format(date))    // 15. jun. 2018    Jun 15, 2018
        //Log.d(DateFormat.getLongDateFormat(App.appContext).format(date))      // 15. juni 2018    June 15, 2018
        //Log.d(DateFormat.getTimeFormat(App.appContext).format(date))          // 14.50            2:50 PM

        // 15/06/2018       6/15/18
        return DateFormat.getDateFormat(App.appContext).format(date)
    }

    fun formatDateAsLocalMedium(date: Date?): String {
        // 15. jun. 2018    Jun 15, 2018
        if(date == null){
            return ""
        }
        return DateFormat.getMediumDateFormat(App.appContext).format(date)
    }

    fun formatDateAsLocalMediumWithoutDay(date: Date?): String {
        // jun. 2018    Jun 2018
        if(date == null){
            return ""
        }
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMyyyy")
        val dateStr = DateFormat.format(pattern, date).toString()
        return "${dateStr}"
    }

    fun formatTimeAsLocal(date: Date?): String {
        // 14.50            2:50 PM
        if(date == null){
            return ""
        }
        return DateFormat.getTimeFormat(App.appContext).format(date)
    }

    fun formatDateAsLocalMediumWithTime(date: Date?): String {
        // 15. jun. 2018  14.50    Jun 15, 2018 2:50 PM
        if(date == null){
            return ""
        }
        val dateString = DateFormat.getMediumDateFormat(App.appContext).format(date)
        val timeString = DateFormat.getTimeFormat(App.appContext).format(date)
        return "$dateString $timeString"
    }

    fun formatDateAsLocalMediumWithAltTexts(date: Date?): String {
        // 15. jun. 2018    Jun 15, 2018
        if(date == null){
            return ""
        }
        if (isToday(date)) {
            return App.appContext.getString(R.string.account_details_header_today)
        } else if (isYesterday(date)) {
            return App.appContext.getString(R.string.account_details_header_yesterday)
        }
        return DateFormat.getMediumDateFormat(App.appContext).format(date)
    }

    fun String.toDate(dateFormat: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timeZone: TimeZone = TimeZone.getTimeZone("UTC")): Date? {
        val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
        parser.timeZone = timeZone
        return parser.parse(this)
    }

    fun Date.formatTo(dateFormat: String, timeZone: TimeZone = TimeZone.getDefault()): String {
        val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
        formatter.timeZone = timeZone
        return formatter.format(this)
    }

    //endregion
}