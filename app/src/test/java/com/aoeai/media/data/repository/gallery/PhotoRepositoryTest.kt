package com.aoeai.media.data.repository.gallery

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.aoeai.media.data.repository.gallery.PhotoRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@Config(sdk = [30])
class PhotoRepositoryTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var photoRepository: PhotoRepository
    private lateinit var testDispatcher: TestDispatcher
    private val mockUri = mockk<Uri>()

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        every { context.contentResolver } returns contentResolver

        mockkStatic(ContentUris::class)
        every {
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, any())
        } returns mockUri

        photoRepository = PhotoRepository(context, testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(ContentUris::class)
        clearAllMocks()
    }

    @Test
    fun `getAllPhotos 应返回第一页照片列表`() = runTest {
        val cursor = mockPhotoCursor(
            ids = listOf(1L, 2L),
            names = listOf("测试照片1.jpg", "测试照片2.jpg")
        )
        every {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                any(),
                any(),
                any(),
                match { it.contains("OFFSET 0") }
            )
        } returns cursor

        val result = photoRepository.getAllPhotos(1, 2)

        Assertions.assertEquals(2, result.size)
        Assertions.assertEquals("测试照片1.jpg", result[0].displayName)
        Assertions.assertEquals("测试照片2.jpg", result[1].displayName)
    }

    @Test
    fun `getAllPhotos 应返回第二页照片列表`() = runTest {
        val cursor = mockPhotoCursor(
            ids = listOf(3L, 4L),
            names = listOf("第二页照片1.jpg", "第二页照片2.jpg")
        )
        every {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                any(),
                any(),
                any(),
                match { it.contains("OFFSET 10") }
            )
        } returns cursor

        val result = photoRepository.getAllPhotos(2, 10)

        Assertions.assertEquals(2, result.size)
        Assertions.assertEquals("第二页照片1.jpg", result[0].displayName)
        Assertions.assertEquals("第二页照片2.jpg", result[1].displayName)
    }

    @Test
    fun `getAllPhotos 当没有相片时应返回空列表`() = runTest {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToNext() } returns false
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns cursor

        val result = photoRepository.getAllPhotos(0, 2)

        Assertions.assertEquals(0, result.size)
    }

    private fun mockPhotoCursor(
        ids: List<Long>,
        names: List<String>,
        dateAdded: Long = 1000000000L,
        dateTaken: Long = 1000000000L,
        size: Long = 1024L,
        width: Int = 800,
        height: Int = 600,
        mimeType: String = "image/jpeg",
        albumName: String = "相机",
        path: String = "/storage/emulated/0/Pictures/测试照片.jpg"
    ): Cursor {
        val cursor = mockk<Cursor>(relaxed = true)
        val count = ids.size
        val position = intArrayOf(-1)

        every { cursor.moveToNext() } answers {
            position[0]++
            position[0] < count
        }
        every { cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID) } returns 0
        every { cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME) } returns 1
        every { cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED) } returns 2
        every { cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN) } returns 3
        every { cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE) } returns 4
        every { cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH) } returns 5
        every { cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT) } returns 6
        every { cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE) } returns 7
        every { cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME) } returns 8
        every { cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA) } returns 9

        every { cursor.getLong(0) } answers { ids.getOrNull(position[0]) ?: 0L }
        every { cursor.getString(1) } answers { names.getOrNull(position[0]) ?: "" }
        every { cursor.getLong(2) } returns dateAdded
        every { cursor.getLong(3) } returns dateTaken
        every { cursor.getLong(4) } returns size
        every { cursor.getInt(5) } returns width
        every { cursor.getInt(6) } returns height
        every { cursor.getString(7) } returns mimeType
        every { cursor.getString(8) } returns albumName
        every { cursor.getString(9) } returns path
        every { cursor.close() } just runs

        return cursor
    }
}