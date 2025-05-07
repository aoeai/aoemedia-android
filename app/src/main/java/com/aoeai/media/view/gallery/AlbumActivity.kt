package com.aoeai.media.view.gallery

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aoeai.media.data.model.gallery.AlbumInfo
import com.aoeai.media.data.repository.gallery.AlbumRepository
import android.content.Intent
import androidx.compose.foundation.clickable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Scale

class AlbumActivity : AppCompatActivity() {

}

@Composable
fun AlbumView() {
    val context = LocalContext.current
    val albumRepository = AlbumRepository(context)
    var albums by remember { mutableStateOf(emptyList<AlbumInfo>()) }

    LaunchedEffect(Unit) {
        albums = albumRepository.getAlbums()
    }

    // 显示相册列表
    LazyColumn {
        items(albums) { album ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            // 创建 Intent 跳转到 PhotoListActivity 并传递相册 ID
                            val intent = Intent(context, PhotoListActivity::class.java).apply {
                                putExtra(PhotoListActivity.EXTRA_ALBUM_ID, album.id)
                            }
                            context.startActivity(intent)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 相册封面预览
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(album.coverUri)
                            .size(180) // 告诉 Coil 精确的像素尺寸 (60dp 大约是 180px)
                            .scale(Scale.FILL) // 根据目标大小缩放图片
                            .build(),
                        contentDescription = album.name,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${album.count} 张照片",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
