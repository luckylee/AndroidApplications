package com.example.hnreader.data

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.hnreader.network.HNRestApi
import com.example.hnreader.ui.StoriesViewModel
import retrofit2.HttpException
import java.io.IOException

/**
 * The main hacker news items data getter and management
 *
 * The repository will fetch the hacker news via Web API in background task and then insert/update
 * to database. ViewModel will get the  hacker news live data from Room and observe their status.
 *
 * @param hackerNewsDataBase  The context for Database or Web API creation
 *
 * @see [StoriesViewModel]
 * @see [HNRestApi]
 */
class HackerNewsRepository(hackerNewsDataBase: HackerNewsDatabase) {
    companion object {
        private const val CACHE_DURATION = 60 * 60 * 1000 // 1 hour
        private const val TAG = "HackerNewsRepository"
    }

    private val hackerNewsItemDao: HackerNewsItemDao = hackerNewsDataBase.storyDao()

    private lateinit var newstoryIds : List<Int>
    private lateinit var topstoryIds : List<Int>

    private val webApi: HNRestApi = HNRestApi()


    fun getItemDetail(itemId: Int): LiveData<HackerNewsItem> {
        return hackerNewsItemDao.getItemDetail(itemId)
    }

    fun allStories(): LiveData<List<HackerNewsItem>> {
        return hackerNewsItemDao.getAllStories()
    }

    fun topStories(): LiveData<List<HackerNewsItem>> {
        return hackerNewsItemDao.getStories(topstoryIds)
    }

    fun newStories(): LiveData<List<HackerNewsItem>> {
        return hackerNewsItemDao.getStories(newstoryIds)
    }

    suspend fun fetchNewStoryIDs() : List<Int> {

        try {
            newstoryIds = webApi.getNewStories()
            Log.d(TAG, "Get new stories size: ${newstoryIds.size}")
            Log.d(TAG, "Get new stories IDs: $newstoryIds")
            insertStories(newstoryIds)
        } catch (e: HttpException) {
            //handles exception with the request
            Log.e(TAG, "request error: " + e.message)
        } catch (e: IOException) {
            //handles no internet exception
            Log.e(TAG, "internet error: " + e.message)
        }

        //isNewLoading.value = false
        return newstoryIds
    }

    suspend fun fetchTopStoryIDs() : List<Int> {

        try {
            topstoryIds = webApi.getTopStories()
            Log.d(TAG, "Get top stories size: ${topstoryIds.size}")
            Log.d(TAG, "Get top stories IDs: $topstoryIds")

            insertStories(topstoryIds)
        } catch (e: HttpException) {
            //handles exception with the request
            Log.e(TAG, "request error: " + e.message)
        } catch (e: IOException) {
            //handles no internet exception
            Log.e(TAG, "internet error: " + e.message)
        }

        return topstoryIds
    }

    suspend fun fetchStoryDetail(storyId: Int) = webApi.getStoryDetail(storyId)


    private fun insertStories(storyIds: List<Int>) : Boolean {
        storyIds.forEach {
            hackerNewsItemDao.insert(HackerNewsItem(it, "story", null))
        }
        return true
    }

    fun insertStoryDetail(item: HackerNewsItem) : Boolean {
        item.kidCount = item.kids?.size
        Log.d(TAG, "Item id: ${item.id}, title: ${item.title}, url: ${item.url}")
        hackerNewsItemDao.update(item)

        // TODO() Handle comments in later stage
        // item.kids?.forEach { hackerNewsItemDao.insert(HackerNewsItem(it, "comment", item.id)) }
        return true
    }

}