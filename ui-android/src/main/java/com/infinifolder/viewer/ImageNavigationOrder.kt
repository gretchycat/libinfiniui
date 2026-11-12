package com.infinifolder.viewer

import android.content.Context
import android.content.Intent
import com.infinifolder.model.ResourceEntry
import com.infinifolder.preview.ImageFormats
import java.io.*
import java.util.UUID

data class ImageNavigationItem(val uri: String, val name: String, val mime: String)

/** App-private display snapshot; only an opaque token crosses the Activity boundary. */
object ImageNavigationOrder {
    private const val EXTRA = "image_display_order"
    private const val IMAGE_ACTIVITY = "com.infinifolder.viewer.ImageViewerActivity"
    private const val MAX_BYTES = 8L * 1024 * 1024

    fun items(entries: List<ResourceEntry>): List<ImageNavigationItem> = entries
        .filter { ImageFormats.isImage(it.metadata) && !ImageFormats.hasDosTextPreview(it.metadata) }
        .map { ImageNavigationItem(it.uri.toString(), it.metadata.name, it.metadata.mimeType) }
        .distinctBy { it.uri }

    fun attach(context: Context, launch: Intent, displayed: List<ResourceEntry>) {
        @Suppress("DEPRECATION")
        val nested = launch.getParcelableExtra<Intent>(ExternalAppChooser.INTERNAL_TARGET)
        val image = listOfNotNull(launch, nested).firstOrNull {
            it.component?.packageName == context.packageName && it.component?.className == IMAGE_ACTIVITY
        } ?: return
        val sequence = items(displayed)
        if (sequence.none { it.uri == image.data?.toString() }) return
        val token = write(File(context.cacheDir, "image-navigation"), sequence) ?: return
        image.putExtra(EXTRA, token)
        if (nested != null && image === nested) launch.putExtra(ExternalAppChooser.INTERNAL_TARGET, nested)
    }

    fun read(context: Context, launch: Intent): List<ImageNavigationItem>? =
        launch.getStringExtra(EXTRA)?.let { read(File(context.cacheDir, "image-navigation"), it) }

    internal fun write(directory: File, items: List<ImageNavigationItem>): String? {
        if (items.isEmpty() || items.size > 100_000) return null
        val token = UUID.randomUUID().toString()
        val temporary = File(directory, "$token.tmp")
        return try {
            directory.mkdirs()
            DataOutputStream(BufferedOutputStream(FileOutputStream(temporary))).use { output ->
                output.writeInt(1); output.writeInt(items.size)
                for (item in items) {
                    output.writeUTF(item.uri); output.writeUTF(item.name); output.writeUTF(item.mime)
                    if (output.size().toLong() > MAX_BYTES) throw IOException("Image navigation snapshot limit")
                }
            }
            check(temporary.renameTo(File(directory, "$token.bin")))
            directory.listFiles()?.filter { it.name.endsWith(".bin") }
                ?.filter { it.name != "$token.bin" }
                ?.sortedByDescending { it.lastModified() }?.drop(31)?.forEach { it.delete() }
            token
        } catch (_: Exception) { temporary.delete(); null }
    }

    internal fun read(directory: File, token: String): List<ImageNavigationItem>? {
        if (!Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matches(token)) return null
        val file = File(directory, "$token.bin")
        if (!file.isFile || file.length() !in 8L..MAX_BYTES) return null
        return try {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                if (input.readInt() != 1) return null
                val count = input.readInt()
                if (count !in 1..100_000) return null
                val items = List(count) { ImageNavigationItem(input.readUTF(), input.readUTF(), input.readUTF()) }
                if (input.read() != -1) null else items
            }
        } catch (_: Exception) { null }
    }
}
