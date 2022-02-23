package com.example.hnreader.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.example.hnreader.MainActivity
import com.example.hnreader.data.HackerNewsItem
import com.example.hnreader.data.HackerNewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException


class StoriesViewModel(private val repository: HackerNewsRepository) : ViewModel() {

    companion object {
        private const val TAG = "StoriesViewModel"
    }

    private var isNewLoading: MutableLiveData<Boolean> = MutableLiveData()
    private var isTopLoading: MutableLiveData<Boolean> = MutableLiveData()


    fun isNewStoriesLoading() : LiveData<Boolean>{
        return  isNewLoading
    }

    fun isTopStoriesLoading() : LiveData<Boolean>{
        return  isTopLoading
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val item: HackerNewsItem = repository.fetchStoryDetail(id)
                repository.insertStoryDetail(item)

            } catch (e: HttpException) {
                //handles exception with the request
                Log.e(TAG, "request error: " + e.message)
            } catch (e: IOException) {
                //handles no internet exception
                Log.e(TAG, "internet error: " + e.message)
            }
        }

        return repository.getItemDetail(id)
    }

    fun fetchNewStories()  {
        // TODO() Refactor update flow for isNewLoading and isTopLoading
        isNewLoading = liveData(Dispatchers.IO) {
            repository.fetchNewStoryIDs()
            emit(false)
        } as MutableLiveData<Boolean>

        isNewLoading.value = true
    }

    fun fetchTopStories() {
        // TODO() Refactor update flow for isNewLoading and isTopLoading
        isTopLoading = liveData(Dispatchers.IO) {
            repository.fetchTopStoryIDs()
            emit(false)
        } as MutableLiveData<Boolean>

        isTopLoading.value = true
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