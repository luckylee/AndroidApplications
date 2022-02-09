package com.example.hnreader.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
/** The SQL function/command for HackerNewsItem Dao  */
interface HackerNewsItemDao {

    @Query("select * from ${HackerNewsItem.TABLE_NAME} where ${HackerNewsItem.HACKER_NEWS_ITEM_TYPE} = 'story'")
    fun getAllStories(): LiveData<List<HackerNewsItem>>

    @Query("select * from ${HackerNewsItem.TABLE_NAME} where ${HackerNewsItem.HACKER_NEWS_ITEM_ID} = :itemId")
    fun getItemDetail(itemId: Int): LiveData<HackerNewsItem>

    @Query("select * from ${HackerNewsItem.TABLE_NAME} where ${HackerNewsItem.HACKER_NEWS_ITEM_TYPE} = 'comment' and ${HackerNewsItem.HACKER_NEWS_ITEM_PARENT} = :parentId")
    fun getCommentsForItem(parentId: Int): LiveData<List<HackerNewsItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(hackerNewsItem: HackerNewsItem)

    @Query("select * from ${HackerNewsItem.TABLE_NAME} where " +
            "${HackerNewsItem.HACKER_NEWS_ITEM_ID} "
            + "IN (:ids) and ${HackerNewsItem.HACKER_NEWS_ITEM_TYPE} = 'story' "
            + "ORDER BY ${HackerNewsItem.HACKER_NEWS_ITEM_TIME} DESC"
    )
    fun getStories(ids : List<Int>): LiveData<List<HackerNewsItem>>

    @Update
    fun update(hackerNewsItem: HackerNewsItem)
}