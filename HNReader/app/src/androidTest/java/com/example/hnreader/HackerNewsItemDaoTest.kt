package com.example.hnreader

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.hnreader.data.HackerNewsDatabase
import com.example.hnreader.data.HackerNewsItem
import com.example.hnreader.data.HackerNewsItemDao
import org.junit.Assert.*
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HackerNewsItemDaoTest {

    private lateinit var database: HackerNewsDatabase
    private lateinit var hackerNewsItemDao: HackerNewsItemDao
    private val mContext: Context = ApplicationProvider.getApplicationContext()


    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()


    @Before
    fun createDb() {


        database = Room.inMemoryDatabaseBuilder(
            mContext, HackerNewsDatabase::class.java).build()
        hackerNewsItemDao = database.storyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testHackerNewsItemInsert(){
        hackerNewsItemDao.insert(HackerNewsItem(1234, "story", 5678))
        hackerNewsItemDao.insert(HackerNewsItem(3454, "story", 98792))
        hackerNewsItemDao.insert(HackerNewsItem(19097, "story", 6517))
        hackerNewsItemDao.insert(HackerNewsItem(32443, "comment", 6517))

        val allStoriesLiveData = hackerNewsItemDao.getAllStories()
        val storiesList = getValueFromLiveData(allStoriesLiveData)

        assertEquals(3, storiesList.size)

        val storyLiveData = hackerNewsItemDao.getItemDetail(1234)
        val story = getValueFromLiveData(storyLiveData)
        assertEquals(story.type, "story")
        assertEquals(story.parent, 5678)

        val commentLiveData = hackerNewsItemDao.getCommentsForItem(6517)
        val comment = getValueFromLiveData(commentLiveData)
        assertEquals(comment[0].type, "comment")
        assertEquals(comment[0].id, 32443)
    }

    private fun <T> getValueFromLiveData(liveData: LiveData<T>): T {
        val data = mutableListOf<T>()
        val countDownLatch = CountDownLatch(1)
        val observer = object : Observer<T> {
            override fun onChanged(it: T?) {
                it?.let { it1 -> data.add(it1) }
                countDownLatch.countDown()
                liveData.removeObserver(this)
            }
        }

        liveData.observeForever(observer)
        countDownLatch.await(1, TimeUnit.SECONDS)
        return data[0]
    }

}
