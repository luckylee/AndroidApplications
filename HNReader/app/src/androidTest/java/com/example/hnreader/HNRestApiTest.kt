package com.example.hnreader

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.hnreader.data.HackerNewsItem
import com.example.hnreader.network.HNRestApi
import org.junit.*
import org.junit.runner.RunWith
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
@LargeTest
class HNRestApiTest {

    companion object {
        private const val TIMEOUT_DURATION = 10L
        private const val TAG = "HNRestApiTest"
    }

    private val mContext: Context = ApplicationProvider.getApplicationContext()
    private val webApi: HNRestApi = HNRestApi()
    private val storyId: Int = 30357429
    private lateinit var countDownLatch: CountDownLatch

    @Before
    fun setUp() {
        // Ignore the test if there is not network available
        Assume.assumeTrue(isNetworkAvailable())
        countDownLatch = CountDownLatch(1)
    }

    @After
    fun tearDown() {
        // wait for loading data finish and then close test
        countDownLatch.await(TIMEOUT_DURATION, TimeUnit.SECONDS)
    }

    @Test
    fun testGetNewStories() {
        val call: Call<List<Int>> = webApi.getNewStories()

        call.enqueue(object : Callback<List<Int>> {

            override fun onFailure(call: Call<List<Int>>, t: Throwable) {
                Assert.fail("Call Failed: $t")
                countDownLatch.countDown()
            }

            override fun onResponse(call: Call<List<Int>>, response: Response<List<Int>>) {
                if (response.isSuccessful) {
                    val newstoryIds : List<Int> = response.body()!!

                    Log.d(TAG, "Get new stories size: ${newstoryIds.size}")
                    Log.d(TAG, "Get new stories IDs: $newstoryIds")
                } else {
                    //Log.e(HackerNewsRepository.TAG, "Call Failed: " )
                    Assert.fail("Call Failed: " + response.errorBody()?.string())
                }
                countDownLatch.countDown()
            }
        })

    }

    @Test
    fun testGetStoryByID() {
        val call: Call<HackerNewsItem> = webApi.getStoryDetail(storyId)

        call.enqueue(object : Callback<HackerNewsItem> {
            override fun onFailure(call: Call<HackerNewsItem>, t: Throwable) {
                Assert.fail("Call Failed: $t")
                countDownLatch.countDown()
            }

            override fun onResponse(call: Call<HackerNewsItem>, response: Response<HackerNewsItem>)
            {
                if (response.isSuccessful) {
                    val item : HackerNewsItem = response.body()!!
                    Log.d(TAG, "Item id: ${item.id}, title: ${item.title}, " +
                            "url: ${item.url}, time: ${item.time}")

                } else {
                    Assert.fail("Call Failed: " + response.errorBody()?.string())
                }
                countDownLatch.countDown()
            }
        })

    }


    // Check if network is available or not. There are different ways to complete this.
    // Also study the https://developer.android.com/training/basics/network-ops/reading-network-state
    private fun isNetworkAvailable(): Boolean {
        val cm = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cap: NetworkCapabilities =
                cm.getNetworkCapabilities(cm.activeNetwork) ?: return false

            return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }  else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val networks = cm.getAllNetworks()
            for (net: Network in networks) {
                val nInfo = cm.getNetworkInfo(net)
                if (nInfo != null && nInfo.isConnected())
                    return true
            }
        }

        // Not support on this device
        Log.e(TAG, "")
        return false

    }


}