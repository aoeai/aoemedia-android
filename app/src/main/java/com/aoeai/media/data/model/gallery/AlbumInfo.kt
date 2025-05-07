package com.aoeai.media.data.model.gallery

import android.net.Uri

// 相册信息数据模型
data class AlbumInfo(
    val name: String,
    val id: String,
    val coverUri: Uri?,
    val count: Int
)