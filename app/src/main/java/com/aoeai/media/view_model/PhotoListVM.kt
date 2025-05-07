package com.aoeai.media.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aoeai.media.data.model.gallery.PhotoItem
import com.aoeai.media.data.repository.gallery.PhotoRepository
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotoListVM(
    private val photoRepository: PhotoRepository
) : ViewModel() {
    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos = _photos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var currentPage = 1
    private val pageSize = 30

    fun loadInitialPhotos(albumId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _photos.value = photoRepository.getPhotosByAlbumId(albumId, currentPage, pageSize)
            _isLoading.value = false
        }
    }

    fun loadMorePhotos(albumId: String) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            currentPage++
            val newPhotos = photoRepository.getPhotosByAlbumId(albumId, currentPage, pageSize)
            _photos.value = _photos.value + newPhotos
            _isLoading.value = false
        }
    }

    fun clearResources() {
        _photos.value = emptyList()
        currentPage = 1
        _isLoading.value = false
        // 取消正在进行的协程任务
        viewModelScope.coroutineContext.cancelChildren()
    }

    override fun onCleared() {
        super.onCleared()
        clearResources()
    }
}