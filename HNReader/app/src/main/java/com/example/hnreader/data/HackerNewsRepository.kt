package com.example.hnreader.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.hnreader.network.HNRestApi
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * The main hacker news items data getter and management
 *
 * The repository will fetch the hacker news via Web API in background task and then insert/update
 * to database. ViewModel will get the  hacker news live data from Room and observe their status.
 *
 * @param Context   The context for Database or Web API creation
 *
 * @see [StoriesViewModel]
 * @see [HNRestApi]
 */
class HackerNewsRepository(hackerNewsDataBase: HackerNewsDatabase) {
    companion object {
        private const val CACHE_DURATION = 60 * 60 * 1000 // 1 hour
        private const val TAG = "HackerNewsRepository"
    }

    private val webApi: HNRestApi = HNRestApi()
    private val hackerNewsItemDao: HackerNewsItemDao = hackerNewsDataBase.storyDao()

    val isNewLoading: MutableLiveData<Boolean> = MutableLiveData()
    val isTopLoading: MutableLiveData<Boolean> = MutableLiveData()

    private lateinit var newstoryIds : List<Int>
    private lateinit var topstoryIds : List<Int>

    init {
        isNewLoading.value = false
        isTopLoading.value = false

    }

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

    fun fetchNewStoryIDs()  {
        val call: Call<List<Int>> = webApi.getNewStories()

        isNewLoading.value = true
        call.enqueue(object : Callback<List<Int>> {

            override fun onFailure(call: Call<List<Int>>, t: Throwable) {
                Log.e(TAG, "Call Failed: $t")
                isNewLoading.value = false
            }

            override fun onResponse(call: Call<List<Int>>, response: Response<List<Int>>)
                    = runBlocking<Unit> {
                if (response.isSuccessful) {
                    newstoryIds = response.body()!!

                    // Log.d(TAG, "Get new stories size: ${newstoryIds.size}")
                    // Log.d(TAG, "Get new stories IDs: $newstoryIds")

                    val result: Deferred<Boolean> = GlobalScope.async(Dispatchers.IO) {
                        insertStories(response.body()!!)
                    }

                    result.await()
                } else {
                    Log.e(TAG, "Call Failed: " + response.errorBody()?.string())
                }
                // After all story Ids are inserted into db, we 
                isNewLoading.value = false
            }
        })
    }

    fun fetchTopStoryIDs()  {
        val call: Call<List<Int>> = webApi.getTopStories()

        isTopLoading.value = true
        call.enqueue(object : Callback<List<Int>> {

            override fun onFailure(call: Call<List<Int>>, t: Throwable) {
                Log.e(TAG, "Call Failed: $t")
                isTopLoading.value = false
            }

            override fun onResponse(call: Call<List<Int>>, response: Response<List<Int>>)
                    = runBlocking<Unit> {
                if (response.isSuccessful) {
                    topstoryIds = response.body()!!

                    // Log.d(TAG, "Get new stories size: ${newstoryIds.size}")
                    // Log.d(TAG, "Get new stories IDs: $newstoryIds")

                    val result: Deferred<Boolean> = GlobalScope.async(Dispatchers.IO) {
                        insertStories(response.body()!!)
                    }

                    result.await()
                } else {
                    Log.e(TAG, "Call Failed: " + response.errorBody()?.string())
                }
                // After all story Ids are inserted into db, we
                isTopLoading.value = false
            }
        })
    }

    suspend fun insertStories(storyIds: List<Int>) : Boolean {
        storyIds.forEach {
            hackerNewsItemDao.insert(HackerNewsItem(it, "story", null))
        }
        return true;
    }

    fun fetchStoryDetail(storyId: Int) {
        val call: Call<HackerNewsItem> = webApi.getStoryDetail(storyId)

        call.enqueue(object : Callback<HackerNewsItem> {
            override fun onFailure(call: Call<HackerNewsItem>, t: Throwable) {
                Log.e(TAG, "get item id $storyId Call Failed: $t")
            }

            override fun onResponse(call: Call<HackerNewsItem>, response: Response<HackerNewsItem>)
            {
                if (response.isSuccessful) {
                    val result: Deferred<Boolean> = GlobalScope.async(Dispatchers.IO) {
                        insertStoryDetail(response.body()!!)
                    }

                } else {
                    Log.e(TAG, "Call Failed: " + response.errorBody()?.string())
                }
            }
        })

    }

    suspend fun insertStoryDetail(item: HackerNewsItem) : Boolean {
        item.kidCount = item.kids?.size
        Log.d(TAG, "Item id: ${item.id}, title: ${item.title}, url: ${item.url}")
        hackerNewsItemDao.update(item)

        // TODO() Handle comments in later stage
        // item.kids?.forEach { hackerNewsItemDao.insert(HackerNewsItem(it, "comment", item.id)) }
        return true
    }

}