package com.example.hnreader.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hnreader.R
import com.example.hnreader.data.HackerNewsItem
import com.example.hnreader.databinding.FragmentTopstoriesBinding

class TopStoriesFragment : Fragment(), RecyclerViewClickListener<HackerNewsItem> {

    companion object {
        fun newInstance() = TopStoriesFragment()
        private const val TAG = "TopStoriesFragment"
    }

    // Use the 'by activityViewModels()' Kotlin property delegate from the fragment-ktx artifact
    private val storyModel: StoriesViewModel by activityViewModels()
    private lateinit var binding: FragmentTopstoriesBinding
    private lateinit var adapter: StoryItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_topstories, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StoryItemAdapter(requireContext(), storyModel, this)
        binding.topStoriesList.layoutManager = LinearLayoutManager(context)
        binding.topStoriesList.adapter = adapter

        storyModel.isTopStoriesLoading().observe(viewLifecycleOwner,
            Observer<Boolean> { isLoading -> binding.swipeToRefreshTop.isRefreshing = isLoading!! }
        )

        storyModel.getTopStories().observe(viewLifecycleOwner
        ) { stories ->
            Log.d(TAG, "top stories data change!!")
            if (stories!!.isNotEmpty()) {
                Log.d(TAG, "top stories size: ${stories.size}")
                binding.swipeToRefreshTop.isRefreshing = false

                adapter.setStories(stories)
            }
        }

        binding.swipeToRefreshTop.setOnRefreshListener {
            storyModel.fetchTopStories()
        }
    }

    override fun onItemClick(item: HackerNewsItem) {
        val intent: Intent
        if (TextUtils.isEmpty(item.url)) {
            // pop toast for no url error
            Toast.makeText(requireContext(), "No URL for the story to check detail!",
                Toast.LENGTH_SHORT).show()
        } else {
            intent = Intent(this.context, WebViewActivity::class.java)
            intent.putExtra(WebViewActivity.EXTRA_URL, item.url)
            startActivity(intent)
        }
    }


}