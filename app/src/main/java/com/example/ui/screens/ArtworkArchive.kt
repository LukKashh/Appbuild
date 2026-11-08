package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap
import java.util.zip.ZipInputStream

@Composable
fun ArchiveArtwork(searchTerms: List<String>, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    var bitmap by remember(searchTerms) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(searchTerms) {
        bitmap = withContext(Dispatchers.IO) {
            ArchivedArtworkCatalog.load(context, searchTerms)?.asImageBitmap()
        }
    }

    Surface(
        modifier = modifier,
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val art = bitmap
            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = searchTerms.firstOrNull(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    tint = GoldPrimary.copy(alpha = 0.75f)
                )
            }
        }
    }
}

private object ArchivedArtworkCatalog {
    private const val ARCHIVE_PATH = "aistudio/capitalfortune-nov-8-2026-2-58-06-am_9803089f.zip"

    @Volatile
    private var indexedImages: Map<String, ByteArray>? = null

    private val bitmapCache = object : LinkedHashMap<String, Bitmap>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean =
            size > 24
    }

    fun load(context: Context, terms: List<String>): Bitmap? = runCatching {
        val entries = imageIndex(context)
        val match = entries.entries
            .map { entry -> entry to score(entry.key, terms) }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first ?: return null

        synchronized(bitmapCache) {
            bitmapCache[match.key]?.let { return it }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(match.value, 0, match.value.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(
            match.value, 0, match.value.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null

        synchronized(bitmapCache) { bitmapCache[match.key] = decoded }
        decoded
    }.getOrNull()

    private fun imageIndex(context: Context): Map<String, ByteArray> {
        indexedImages?.let { return it }
        return synchronized(this) {
            indexedImages?.let { return@synchronized it }
            val result = LinkedHashMap<String, ByteArray>()
            ZipInputStream(context.assets.open(ARCHIVE_PATH)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val ext = entry.name.substringAfterLast('.', "").lowercase()
                    if (!entry.isDirectory && ext in setOf("png", "jpg", "jpeg", "webp")) {
                        result[entry.name] = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            result.toMap().also { indexedImages = it }
        }
    }

    private fun score(path: String, terms: List<String>): Int {
        val filename = path.substringAfterLast('/')
            .substringBeforeLast('.')
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
        if (filename.isBlank()) return 0

        val ignored = setOf("item", "items", "asset", "assets", "image", "icon", "collectible")
        var total = 0
        terms.forEach { term ->
            val normalized = term.lowercase().replace(Regex("[^a-z0-9]"), "")
            if (normalized.length >= 4 && filename == normalized) total += 30
            else if (normalized.length >= 4 && filename.contains(normalized)) total += 12

            normalized.split(Regex("[^a-z0-9]+"))
                .filter { it.length >= 3 && it !in ignored }
                .distinct()
                .forEach { token ->
                    if (filename == token) total += 8
                    else if (filename.contains(token)) total += token.length.coerceAtMost(6)
                }
        }
        return total
    }
}