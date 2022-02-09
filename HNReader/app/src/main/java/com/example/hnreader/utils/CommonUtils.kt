package com.example.hnreader.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.*

class CommonUtils {
    companion object {

        private const val SECOND_MILLIS = 1000
        private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
        private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
        private const val DAY_MILLIS = 24 * HOUR_MILLIS
        private const val PREF_STORY_CACHE = "PREF_STORY_CACHE_TIME"

        private fun currentDate(): Date {
            val calendar = Calendar.getInstance()
            return calendar.time
        }

        fun getTimeAgo(date: Date): String {
            var time = date.time
            time /= 1000

            val now = currentDate().time
            if (time > now || time <= 0) {
                return "in the future"
            }

            val diff = now - time
            return when {
                diff < 60 * MINUTE_MILLIS -> "${diff / MINUTE_MILLIS} minutes ago"
                diff < 24 * HOUR_MILLIS -> "${diff / HOUR_MILLIS} hours ago"
                diff < 48 * HOUR_MILLIS -> "yesterday"
                else -> "${diff / DAY_MILLIS} days ago"
            }
        }


        fun setStoryCacheTime(context: Context, time: Long) {
            // val sharedPreferences: SharedPreferences = context.defaultSharedPreferences
            // sharedPreferences.edit().putLong(PREF_STORY_CACHE, time).apply()
        }

        fun getStoryCacheTime(context: Context): Long {
            // val sharedPreferences: SharedPreferences = context.defaultSharedPreferences
            // return sharedPreferences.getLong(PREF_STORY_CACHE, 0L)
            return 0;
        }

    }
}