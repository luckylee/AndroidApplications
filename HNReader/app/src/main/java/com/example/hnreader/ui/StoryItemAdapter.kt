package com.example.hnreader.ui

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.hnreader.R
import com.example.hnreader.data.HackerNewsDatabase
import com.example.hnreader.data.HackerNewsItem
import com.example.hnreader.data.HackerNewsRepository
import com.example.hnreader.databinding.ItemStoriesListBinding
import com.example.hnreader.utils.CommonUtils


/**
 * The HackerNews item view adapter in the RecyclerView.
 *
 * In the each item holder, it will update the UI content from the fields of HackerNewItem in the DB.
 * If the item fields are empty content, it will fetch data from network and then update to DB.
 *
 * @param context   the activity/fragment context to control the lifecycle
 * @param recyclerClickListener Need to handle the recycleView item click Listener
 * @see [NewStoriesFragment] or [TopStoriesFragment]
 *
 */
class StoryItemAdapter (
private val context: Context,
private val viewModel: StoriesViewModel,
private val recyclerClickListener: RecyclerViewClickListener<HackerNewsItem>
) : RecyclerView.Adapter<StoryItemAdapter.StoryItemViewHolder>() {

    companion object {
        private const val TAG = "StoryItemAdapter"
    }

    private var stories = emptyList<HackerNewsItem>()
    private var alreadyRequestedItem: MutableList<Int> = mutableListOf()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StoryItemAdapter.StoryItemViewHolder {
        return StoryItemViewHolder(
            DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.item_stories_list, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: StoryItemViewHolder, position: Int) {
        val story = stories[position]
        Log.d(TAG, "Story Item, position: $position, id: ${story.id}")

        if (!TextUtils.isEmpty(story.title)) {
            holder.binding.title.text = story.title
            holder.binding.score.text = story.score.toString()
            val comments = if (story.kidCount == null) 0 else story.kidCount
            holder.binding.info.text =
                String.format(
                    "$comments Comments | ${
                        story.time?.let {
                            CommonUtils.getTimeAgo(
                                it
                            )
                        }
                    } | by ${story.by}"
                )

            holder.binding.root.setOnClickListener { recyclerClickListener.onItemClick(story) }

            holder.binding.storyTypeIndicator.setBackgroundColor(
                context.getColor(
                    if (TextUtils.isEmpty(story.url))
                        android.R.color.holo_green_dark else android.R.color.holo_blue_dark
                )
            )

            setViewVisibilities(holder.binding, false)

        } else if (!alreadyRequestedItem.contains(story.id)) {
            viewModel.getStoryItem(story.id)

            alreadyRequestedItem.add(story.id)

            holder.binding.root.setOnClickListener(null)
            holder.binding.title.text = context.getString(R.string.string_loading)
            setViewVisibilities(holder.binding, true)
        }

    }

    override fun getItemCount(): Int {
        return stories.size
    }

    inner class StoryItemViewHolder(val binding: ItemStoriesListBinding) : RecyclerView.ViewHolder(binding.root)


    private fun setViewVisibilities(binding: ItemStoriesListBinding, loading: Boolean) {
        if (loading) {
            binding.score.visibility = View.INVISIBLE
            binding.info.visibility = View.INVISIBLE
            binding.storyTypeIndicator.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.score.visibility = View.VISIBLE
            binding.info.visibility = View.VISIBLE
            binding.storyTypeIndicator.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
    }

    fun setStories(hackerNewsItems: List<HackerNewsItem>) {
        this.stories = hackerNewsItems
        notifyDataSetChanged()
    }

}