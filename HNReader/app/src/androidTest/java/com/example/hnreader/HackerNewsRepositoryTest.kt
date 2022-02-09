package com.example.hnreader

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.hnreader.data.HackerNewsDatabase
import com.example.hnreader.data.HackerNewsRepository
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HackerNewsRepositoryTest {

    private val mContext: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: HackerNewsDatabase
    private lateinit var repo : HackerNewsRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(mContext, HackerNewsDatabase::class.java).build()
        repo  = HackerNewsRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }



}