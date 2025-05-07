package com.aoeai.media.data.model.gallery

import android.net.Uri

// 照片数据模型
data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val dateTaken: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val albumName: String,
    val path: String? = null
)