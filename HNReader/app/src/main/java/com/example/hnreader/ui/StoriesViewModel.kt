package com.example.hnreader.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hnreader.data.HackerNewsItem
import com.example.hnreader.data.HackerNewsRepository


class StoriesViewModel(private val repository: HackerNewsRepository) : ViewModel() {

    fun isNewStoriesLoading() : LiveData<Boolean>{
        return  repository.isNewLoading
    }

    fun isTopStoriesLoading() : LiveData<Boolean>{
        return  repository.isTopLoading
    }

    fun getAllStories() : LiveData<List<HackerNewsItem>> {
        // TODO() Add error handle when get null or no IDs return
        return repository.allStories()
    }

    fun getNewStories() : LiveData<List<HackerNewsItem>> {
        // TODO() Add error handle when get null or no IDs return
        return repository.newStories()
    }

    fun getTopStories() : LiveData<List<HackerNewsItem>> {
        // TODO() Add error handle when get null or no IDs return
        return repository.topStories()
    }

    fun getStoryItem(id: Int) : LiveData<HackerNewsItem> {
        // TODO() Add error handle when get null or no IDs return
        return repository.getItemDetail(id)
    }

    fun fetchNewStories() {
        return repository.fetchNewStoryIDs()
    }

    fun fetchTopStories() {
        return repository.fetchTopStoryIDs()
    }
}

class StoriesViewModelFactory(private val repository: HackerNewsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoriesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}