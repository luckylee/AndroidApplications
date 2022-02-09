package com.example.hnreader

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.hnreader.data.HackerNewsRepository
import com.example.hnreader.databinding.ActivityMainBinding
import com.example.hnreader.ui.NewStoriesFragment
import com.example.hnreader.ui.StoriesViewModel
import com.example.hnreader.ui.StoriesViewModelFactory
import com.example.hnreader.ui.TopStoriesFragment
import com.google.android.material.tabs.TabLayoutMediator


class MainActivity : AppCompatActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        private const val REQUEST_CODE_PERMISSIONS = 7
        private const val TAG = "HNReaderMainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (allPermissionsGranted()) {
                initDataAndUI()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        } else {
            initDataAndUI()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initDataAndUI()
            } else {
                Log.e(TAG, "Permissions not granted by the user.")
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "TODO() : OnResume to check if need to refresh stories data")
    }

    private fun initDataAndUI() {

        if(!isNetworkAvailable()) {
            // No network, close activity
            Toast.makeText(this, "Network not available, close application",
                Toast.LENGTH_LONG).show()
            finish()
        } else {

            val hnNewsRepo = HackerNewsRepository(this)

            hnNewsRepo.fetchNewStoryIDs()
            // Use the 'by viewModels()' Kotlin property delegate
            // from the activity-ktx artifact
            val model: StoriesViewModel by viewModels() {
                StoriesViewModelFactory(hnNewsRepo)
            }

            model.isNewStoriesLoading().observe(this, Observer { loading ->
                // Complete the 1st time data loading, launch the List UI
                if (!loading) {
                    // Pre-load top stories list.
                    model.fetchTopStories()
                    binding.viewPager2.adapter = ViewPager2Adapter(this@MainActivity)
                    attachViewPagerAndTab()
                }
                model.isNewStoriesLoading().removeObserver(Observer { })
            })
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }

    private fun attachViewPagerAndTab() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            when(position) {
                0 -> tab.text = getString(R.string.title_new)
                1 -> tab.text = getString(R.string.title_top)
                else -> throw IllegalArgumentException()
            }
        }.attach()
    }

    internal class ViewPager2Adapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> NewStoriesFragment.newInstance()
            1 -> TopStoriesFragment.newInstance()
            else -> throw IllegalArgumentException()
        }
    }
}