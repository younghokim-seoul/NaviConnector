package com.cm.naviconnector.feature.audio

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreAudioPagingSource(
    private val context: Context,
    private val query: String?,
    private val sort: AudioSort
) : PagingSource<Int, AudioFile>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AudioFile> =
        withContext(Dispatchers.IO) {
            val page = params.key ?: 0
            val limit = params.loadSize
            val offset = page * limit

            val clauses = mutableListOf("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            val args = mutableListOf<String>()
            if (!query.isNullOrBlank()) {
                clauses += "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"
                args += "%$query%"
            }
            val selection = clauses.joinToString(" AND ")
            val selectionArgs = if (args.isEmpty()) null else args.toTypedArray()

            val (sortCols, sortDir) = when (sort) {
                AudioSort.ByNameAsc -> arrayOf(MediaStore.Audio.Media.DISPLAY_NAME) to ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                AudioSort.ByNameDesc -> arrayOf(MediaStore.Audio.Media.DISPLAY_NAME) to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                AudioSort.ByDurationDesc -> arrayOf(MediaStore.Audio.Media.DURATION) to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            }

            runCatching {
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.SIZE
                )
                val bundle = Bundle().apply {
                    putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, sortCols)
                    putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, sortDir)
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    if (selectionArgs != null) {
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    }
                }

                val data = mutableListOf<AudioFile>()

                context.contentResolver.query(uri, projection, bundle, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val contentUri = ContentUris.withAppendedId(uri, id)
                        data += AudioFile(
                            uri = contentUri,
                            name = cursor.getString(nameCol),
                            duration = cursor.getLong(durCol),
                            size = cursor.getLong(sizeCol)
                        )
                    }
                }

                LoadResult.Page(
                    data = data,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (data.size < limit) null else page + 1
                )
            }.getOrElse {
                LoadResult.Error(it)
            }
        }

    override fun getRefreshKey(state: PagingState<Int, AudioFile>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }
}