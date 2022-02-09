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
import com.example.hnreader.databinding.FragmentNewstoriesBinding

class NewStoriesFragment : Fragment(), RecyclerViewClickListener<HackerNewsItem> {

    companion object {
        fun newInstance() = NewStoriesFragment()
        private const val TAG = "NewStoriesFragment"
    }

    // Use the 'by activityViewModels()' Kotlin property delegate from the fragment-ktx artifact
    private val storyModel: StoriesViewModel by activityViewModels()
    private lateinit var binding: FragmentNewstoriesBinding
    private lateinit var adapter: StoryItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_newstories, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StoryItemAdapter(requireContext(), this)
        binding.newStoriesList.layoutManager = LinearLayoutManager(context)
        binding.newStoriesList.adapter = adapter

        storyModel.isNewStoriesLoading().observe(viewLifecycleOwner,
            Observer<Boolean> { isLoading -> binding.swipeToRefresh.isRefreshing = isLoading!! }
        )

        storyModel.getNewStories().observe(viewLifecycleOwner
        ) { stories ->
            Log.d(TAG, "New stories data change!!")
            if (stories!!.isNotEmpty()) {
                Log.d(TAG, "New stories size: ${stories.size}")
                binding.swipeToRefresh.isRefreshing = false

                adapter.setStories(stories)
            }
        }

        binding.swipeToRefresh.setOnRefreshListener {
            storyModel.fetchNewStories()
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