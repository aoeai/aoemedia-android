package com.aoeai.media.data.repository.gallery

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.aoeai.media.data.model.gallery.PhotoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.paging.PagingSource
import androidx.paging.PagingState

class PhotoRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // 定义常用的投影列
    private val photoProjection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.DATA
    )

    // 获取所有照片
    suspend fun getAllPhotos(page: Int = 1, pageSize: Int = 50): List<PhotoItem> =
        withContext(ioDispatcher) {
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            val selection = "${MediaStore.Images.Media.MIME_TYPE} LIKE ?"
            val selectionArgs = arrayOf("image/%")
            val offset = (page - 1) * pageSize

            queryPhotos(
                selection,
                selectionArgs,
                "$sortOrder LIMIT $pageSize OFFSET $offset"
            )
        }

    // 获取所有照片的总数
    suspend fun getAllPhotosCount(): Int = withContext(ioDispatcher) {
        try {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val ids = HashSet<Long>()

            // 1. 标准图片查询（不限制MIME类型，捕获更多图片格式）
            val standardSelection = "${MediaStore.MediaColumns.MIME_TYPE} LIKE ?"
            val standardArgs = arrayOf("image/%")

            // 查询外部存储图片
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                standardSelection,
                standardArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(idColumn))
                }
                Log.d("PhotoRepository", "外部存储图片: ${cursor.count}张")
            }

            // 查询内部存储图片
            context.contentResolver.query(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                projection,
                standardSelection,
                standardArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val initialSize = ids.size
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(idColumn))
                }
                Log.d(
                    "PhotoRepository",
                    "内部存储图片: ${cursor.count}张, 新增: ${ids.size - initialSize}张"
                )
            }

            // Android 10+: 查询Files表
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 2. 基于扩展名查询（可能捕获到被错误分类的图片）
                val extensionSelection =
                    "${MediaStore.MediaColumns.DATA} LIKE ? OR ${MediaStore.MediaColumns.DATA} LIKE ? OR ${MediaStore.MediaColumns.DATA} LIKE ? OR ${MediaStore.MediaColumns.DATA} LIKE ? OR ${MediaStore.MediaColumns.DATA} LIKE ? OR ${MediaStore.MediaColumns.DATA} LIKE ? OR ${MediaStore.MediaColumns.DATA} LIKE ? OR ${MediaStore.MediaColumns.DATA} LIKE ?"
                val extensionArgs = arrayOf(
                    "%.jpg",
                    "%.jpeg",
                    "%.png",
                    "%.webp",
                    "%.gif",
                    "%.bmp",
                    "%.heic",
                    "%.heif"
                )

                context.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    extensionSelection,
                    extensionArgs,
                    null
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val initialSize = ids.size
                    while (cursor.moveToNext()) {
                        ids.add(cursor.getLong(idColumn))
                    }
                    Log.d(
                        "PhotoRepository",
                        "Files表扩展名查询: ${cursor.count}张, 新增: ${ids.size - initialSize}张"
                    )
                }

                // 3. 无MIME类型限制的查询（尝试捕获所有可能的图片）
                val rawSelection =
                    "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
                val rawArgs = arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString()
                )

                context.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    rawSelection,
                    rawArgs,
                    null
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val initialSize = ids.size
                    while (cursor.moveToNext()) {
                        ids.add(cursor.getLong(idColumn))
                    }
                    Log.d(
                        "PhotoRepository",
                        "Files表媒体类型查询: ${cursor.count}张, 新增: ${ids.size - initialSize}张"
                    )
                }

                // 4. 检查内部存储的Files表
                context.contentResolver.query(
                    MediaStore.Files.getContentUri("internal"),
                    projection,
                    standardSelection,
                    standardArgs,
                    null
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val initialSize = ids.size
                    while (cursor.moveToNext()) {
                        ids.add(cursor.getLong(idColumn))
                    }
                    Log.d(
                        "PhotoRepository",
                        "Files内部表图片: ${cursor.count}张, 新增: ${ids.size - initialSize}张"
                    )
                }
            }

            // 5. 尝试刷新媒体库（对于某些设备可能有帮助）
            try {
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, null, null, null
                )?.close()
            } catch (ignored: Exception) {
            }

            Log.d("PhotoRepository", "最终总照片数: ${ids.size}张")
            ids.size
        } catch (e: Exception) {
            Log.e("PhotoRepository", "获取照片计数失败: ${e.message}", e)
            0
        }
    }

    // 根据ID获取单张照片
    suspend fun getPhotoById(id: Long): PhotoItem? = withContext(ioDispatcher) {
        val selection = "${MediaStore.Images.Media._ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        queryPhotos(selection, selectionArgs, null).firstOrNull()
    }

    /**
     * 根据相册 ID 获取照片列表
     */
    suspend fun getPhotosByAlbumId(
        albumId: String,
        page: Int = 1,
        pageSize: Int = 50
    ): List<PhotoItem> = withContext(ioDispatcher) {
        val selection =
            "${MediaStore.Images.Media.BUCKET_ID} = ? AND ${MediaStore.Images.Media.MIME_TYPE} LIKE ?"
        val selectionArgs = arrayOf(albumId, "image%")
        val offset = (page - 1) * pageSize
        val sortOrder =
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC LIMIT $pageSize OFFSET $offset"

        queryPhotos(selection, selectionArgs, sortOrder)
    }

    // 按日期范围查询照片
    suspend fun getPhotosByDateRange(startDate: Long, endDate: Long): List<PhotoItem> =
        withContext(ioDispatcher) {
            val selection =
                "${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} <= ? AND ${MediaStore.Images.Media.MIME_TYPE} LIKE ?"
            val selectionArgs = arrayOf(startDate.toString(), endDate.toString(), "image%")
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            queryPhotos(selection, selectionArgs, sortOrder)
        }

    // 提取的公共查询方法
    private suspend fun queryPhotos(
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): List<PhotoItem> {
        val photos = mutableListOf<PhotoItem>()

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            photoProjection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            photos.addAll(extractPhotosFromCursor(cursor))
        }

        return photos
    }

    // 从游标中提取照片数据
    private fun extractPhotosFromCursor(cursor: Cursor): List<PhotoItem> {
        val photos = mutableListOf<PhotoItem>()

        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val albumNameColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val dateAdded = cursor.getLong(dateAddedColumn)
            val dateTaken = cursor.getLong(dateTakenColumn)
            val size = cursor.getLong(sizeColumn)
            val width = cursor.getInt(widthColumn)
            val height = cursor.getInt(heightColumn)
            val mimeType = cursor.getString(mimeTypeColumn)
            val albumName = cursor.getString(albumNameColumn) ?: ""
            val path = cursor.getString(dataColumn)

            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
            )

            photos.add(
                PhotoItem(
                    id = id,
                    uri = contentUri,
                    displayName = name,
                    dateAdded = dateAdded,
                    dateTaken = dateTaken,
                    size = size,
                    width = width,
                    height = height,
                    mimeType = mimeType,
                    albumName = albumName,
                    path = path
                )
            )
        }

        return photos
    }
}