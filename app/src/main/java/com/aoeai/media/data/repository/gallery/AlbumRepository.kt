package com.aoeai.media.data.repository.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.aoeai.media.data.model.gallery.AlbumInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlbumRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    /**
     * 获取所有相册信息
     */
    suspend fun getAlbums(): List<AlbumInfo> = withContext(ioDispatcher) {
        val albums = mutableMapOf<String, MutableMap<String, Any>>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED  // 添加日期字段用于排序
        )

        // 修改排序：先按照日期降序排列，确保最新照片先被处理
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val bucketId = cursor.getString(bucketIdColumn) ?: continue
                val bucketName = cursor.getString(bucketNameColumn) ?: continue

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )

                val album = albums.getOrPut(bucketId) {
                    mutableMapOf(
                        "name" to bucketName,
                        "id" to bucketId,
                        "coverUri" to contentUri,  // 第一次遇到的就是最新的照片
                        "count" to 0
                    )
                }

                // 只更新计数，封面已经是最新的了
                album["count"] = (album["count"] as Int) + 1
            }
        }

        // 转换为AlbumInfo列表
        albums.values.map { albumData ->
            AlbumInfo(
                name = albumData["name"] as String,
                id = albumData["id"] as String,
                coverUri = albumData["coverUri"] as Uri,
                count = albumData["count"] as Int
            )
        }
    }
}