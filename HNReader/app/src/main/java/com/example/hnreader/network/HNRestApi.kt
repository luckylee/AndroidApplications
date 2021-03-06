package com.example.hnreader.network


import com.example.hnreader.data.HackerNewsItem
import com.example.hnreader.utils.DateTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.*

/** HackerNews Web API */
class HNRestApi {

    private val hackerNewsApi: HackerNewsApi
    private val baseUrl = "https://hacker-news.firebaseio.com/v0/"

    init {

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        // Must use the self-define DataTypeAdapter, unable use com.google.gson.GsonBuilder.DateTypeAdapter
        val gson : Gson = GsonBuilder().registerTypeAdapter(Date::class.java, DateTypeAdapter()).create()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        hackerNewsApi = retrofit.create(HackerNewsApi::class.java)
    }

    suspend fun getTopStories(): List<Int> {
        return hackerNewsApi.getTopStories("pretty")
    }

    suspend fun getNewStories(): List<Int> {
        return hackerNewsApi.getNewStories("pretty")
    }

    suspend fun getStoryDetail(storyId: Int): HackerNewsItem {
        return hackerNewsApi.getStoryDetail(storyId, "pretty")
    }

    interface HackerNewsApi {
        // Before Retrofit 2.6, use Callback to get data in REST Api, now change to use
        // Coroutine suspend

        @GET("topstories.json")
        suspend fun getTopStories(@Query("print") printParam: String): List<Int>

        @GET("newstories.json")
        suspend fun getNewStories(@Query("print") printParam: String): List<Int>

        @GET("item/{item_id}.json")
        suspend fun getStoryDetail(@Path("item_id") itemId: Int, @Query("print") printParam: String): HackerNewsItem

    }
}