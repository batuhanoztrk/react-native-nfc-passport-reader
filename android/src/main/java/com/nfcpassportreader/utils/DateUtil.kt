package com.nfcpassportreader.utils

import android.annotation.SuppressLint
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("SimpleDateFormat")
class DateUtil {
  private fun stringToDate(dateStr: String, dateFormat: DateFormat): Date? {
    return dateFormat.parse(dateStr)
  }

  private fun dateToString(date: Date?, dateFormat: DateFormat): String? {
    return date?.let { dateFormat.format(it) }
  }

  fun convertFromMrzDate(mrzDate: String): String? {
    val date = stringToDate(mrzDate, SimpleDateFormat("yyMMdd"))
    return dateToString(date, SimpleDateFormat("yyyy-MM-dd"))
  }

  fun convertFromNfcDate(nfcDate: String): String? {
    val date = stringToDate(nfcDate, SimpleDateFormat("yyMMdd"))
    return dateToString(date, SimpleDateFormat("yyyy-MM-dd"))
  }
}
