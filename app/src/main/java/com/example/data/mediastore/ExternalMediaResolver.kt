package com.example.data.mediastore

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.model.MediaItemModel
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * "Open with As Player" se aayi file ko MediaStore mein dhoondta hai aur usi folder ki
 * baaqi media files ka queue banata hai, taake Next / Previous us folder mein chalein.
 */
class ExternalMediaResolver(private val context: Context) {

    data class Result(val queue: List<MediaItemModel>, val index: Int)

    /** Null ka matlab: file library mein nahi mili ya folder mein koi aur file nahi; caller single item chalaye. */
    suspend fun resolve(repository: MediaRepository, uri: Uri, isVideo: Boolean): Result? =
        withContext(Dispatchers.IO) {
            try {
                var all = load(repository, isVideo)
                var current = findMatch(all, uri)
                if (current == null) {
                    // Library abhi load na hui ho ya nayi file ho: ek baar refresh karke dobara dekhein
                    repository.refreshMedia()
                    all = load(repository, isVideo)
                    current = findMatch(all, uri)
                }
                if (current == null || current.filePath.isEmpty()) return@withContext null

                val folder = File(current.filePath).parent ?: return@withContext null
                val siblings = all
                    .filter { it.filePath.isNotEmpty() && File(it.filePath).parent == folder }
                    .sortedWith { a, b -> naturalCompare(File(a.filePath).name, File(b.filePath).name) }

                val index = siblings.indexOfFirst { it.id == current.id }
                if (index < 0 || siblings.size < 2) null else Result(siblings, index)
            } catch (e: Exception) {
                null
            }
        }

    private suspend fun load(repository: MediaRepository, isVideo: Boolean): List<MediaItemModel> =
        if (isVideo) repository.videosFlow.first() else repository.audiosFlow.first()

    private fun findMatch(all: List<MediaItemModel>, uri: Uri): MediaItemModel? {
        if (all.isEmpty()) return null

        // 1. MediaStore ka apna URI: content://media/external/video/media/123
        if (uri.authority == "media") {
            val id = uri.lastPathSegment
            all.firstOrNull { it.id == id }?.let { return it }
        }

        // 2. MediaStore documents URI: content://com.android.providers.media.documents/document/video:123
        if (uri.authority == "com.android.providers.media.documents") {
            val id = runCatching { DocumentsContract.getDocumentId(uri).substringAfter(':') }.getOrNull()
            all.firstOrNull { it.id == id }?.let { return it }
        }

        // 3. File path (file:// ya external storage documents ya DATA column)
        pathFromUri(uri)?.let { path ->
            all.firstOrNull { it.filePath == path }?.let { return it }
        }

        // 4. Aakhri koshish: file ka naam aur size
        val (name, size) = queryNameAndSize(uri)
        if (name != null && size > 0) {
            all.firstOrNull { it.sizeBytes == size && File(it.filePath).name == name }?.let { return it }
        }
        return null
    }

    private fun pathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path

        if (uri.authority == "com.android.externalstorage.documents") {
            val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return null
            val volume = docId.substringBefore(':')
            val relative = docId.substringAfter(':', "")
            return if (volume.equals("primary", ignoreCase = true)) {
                File(Environment.getExternalStorageDirectory(), relative).path
            } else {
                "/storage/$volume/$relative"
            }
        }

        return runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun queryNameAndSize(uri: Uri): Pair<String?, Long> {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIdx != -1) c.getString(nameIdx) else null
                    val size = if (sizeIdx != -1 && !c.isNull(sizeIdx)) c.getLong(sizeIdx) else 0L
                    name to size
                } else null
            }
        }.getOrNull() ?: (null to 0L)
    }

    /** "Episode 2" "Episode 10" se pehle aaye: numbers ko number ki tarah compare karta hai. */
    private fun naturalCompare(a: String, b: String): Int {
        val splitter = Regex("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)")
        val pa = a.split(splitter)
        val pb = b.split(splitter)
        for (i in 0 until minOf(pa.size, pb.size)) {
            val x = pa[i]
            val y = pb[i]
            val nx = x.toBigIntegerOrNull()
            val ny = y.toBigIntegerOrNull()
            val cmp = if (nx != null && ny != null) nx.compareTo(ny) else x.compareTo(y, ignoreCase = true)
            if (cmp != 0) return cmp
        }
        return pa.size - pb.size
    }
}
