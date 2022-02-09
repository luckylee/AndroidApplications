package com.example.hnreader.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.hnreader.utils.CommonUtils
import com.example.hnreader.utils.Converters

@Database(entities = [HackerNewsItem::class], version = 1)
@TypeConverters(Converters::class)
/** HackerNews database using Room */
abstract class HackerNewsDatabase : RoomDatabase() {

    abstract fun storyDao(): HackerNewsItemDao

    companion object {

        private const val DATABASE_NAME = "hacker_news_stories.db"

        @Volatile
        private var INSTANCE: HackerNewsDatabase? = null

        fun getDatabase(context: Context): HackerNewsDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HackerNewsDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}