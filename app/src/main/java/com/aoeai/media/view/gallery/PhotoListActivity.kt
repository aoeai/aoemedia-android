package com.aoeai.media.view.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.request.crossfade
import com.aoeai.media.data.model.gallery.PhotoItem
import com.aoeai.media.data.repository.gallery.PhotoRepository
import kotlinx.coroutines.launch

class PhotoListActivity : ComponentActivity() {
    companion object {
        const val EXTRA_ALBUM_ID = "album_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val albumId = intent.getStringExtra(EXTRA_ALBUM_ID) ?: ""

        setContent {
            PhotoListView(albumId = albumId)
        }
    }
}

@Composable
fun PhotoListView(albumId: String) {
    val context = LocalContext.current
    val photoRepository = remember { PhotoRepository(context) }
    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 30
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // 检测是否滚动到底部
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisibleItem >= totalItems - 5 && !isLoading && totalItems > 0
        }
    }

    // 首次加载照片
    LaunchedEffect(albumId) {
        isLoading = true
        photos = photoRepository.getPhotosByAlbumId(albumId, currentPage, pageSize)
        isLoading = false
    }

    // 滚动到底部加载更多
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            coroutineScope.launch {
                isLoading = true
                currentPage++
                val newPhotos = photoRepository.getPhotosByAlbumId(albumId, currentPage, pageSize)
                photos = photos + newPhotos
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(photos.size) { index ->
                val photo = photos[index]

                // 检查项目是否在可视区域内
                val isVisible = remember(gridState.firstVisibleItemIndex, gridState.layoutInfo.visibleItemsInfo.size) {
                    val firstVisible = gridState.firstVisibleItemIndex
                    val lastVisible = firstVisible + gridState.layoutInfo.visibleItemsInfo.size
                    index in firstVisible..lastVisible
                }

                // 根据可见性加载图片
                AsyncImage(
                    model = if (isVisible) {
                        ImageRequest.Builder(LocalContext.current)
                            .data(photo.uri)
                            .size(300)
                            .crossfade(true)
                            .memoryCacheKey("${photo.id}_${index}")
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .allowRgb565(true)
                            .allowHardware(false)
                            .build()
                    } else null,  // 不可见时加载null
                    contentDescription = photo.displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { },
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }
    }
}