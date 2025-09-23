package com.cm.naviconnector.feature.audio

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AudioRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun pagedAudio(
        query: String? = null,
        sort: AudioSort = AudioSort.ByNameAsc,
        pageSize: Int = 40
    ): Flow<PagingData<AudioFile>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize * 2,
                prefetchDistance = pageSize / 2,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                MediaStoreAudioPagingSource(
                    context = context,
                    query = query,
                    sort = sort
                )
            }
        ).flow
    }
}