package com.example.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.AppInfo
import com.example.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads and caches the real launcher icon of an installed app.
 *
 * Icons are decoded once per package at a small size and kept in an [LruCache], so scrolling the
 * drawer does not hit the package manager for the same icon again. Everything happens off the
 * main thread: [iconFor] suspends on [Dispatchers.IO].
 */
object AppIconLoader {

    private const val MAX_ENTRIES = 220

    private val cache = object : LruCache<String, ImageBitmap>(MAX_ENTRIES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = 1
    }

    /** Icon for [packageName], or null when the app has none (or was uninstalled). */
    suspend fun iconFor(context: Context, packageName: String, sizePx: Int): ImageBitmap? {
        val key = "$packageName@$sizePx"
        cache.get(key)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val drawable = pm.getApplicationIcon(packageName)
                // getApplicationIcon may return a drawable with no intrinsic size; a fallback
                // size keeps toBitmap() from throwing.
                val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else sizePx
                val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else sizePx
                drawable.toBitmap(width = w, height = h).asImageBitmap()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            } catch (_: Exception) {
                null
            }
        }
        if (bitmap != null) cache.put(key, bitmap)
        return bitmap
    }

    /** Drops a package's cached icon (used when the app list is reloaded). */
    fun evict(packageName: String) {
        val snapshot = cache.snapshot()
        for (key in snapshot.keys) {
            if (key.startsWith("$packageName@")) cache.remove(key)
        }
    }
}

/**
 * One app icon at [size]: the real launcher icon when the app is installed, otherwise the old
 * coloured circle with the app's initial, so nothing ever renders as a blank square.
 */
@Composable
fun RealAppIcon(
    app: AppInfo,
    size: Dp,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targetPx = with(androidx.compose.ui.platform.LocalDensity.current) { size.roundToPx() }
    val icon by produceState<ImageBitmap?>(initialValue = null, app.packageName, targetPx) {
        value = AppIconLoader.iconFor(context, app.packageName, targetPx)
    }

    val bitmap = icon
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        val palette = AppColors.getPaletteFor(app.packageName + app.label)
        val circleColor = if (isDarkTheme) palette.iconCircleDark else palette.iconCircleLight
        val initial = app.label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Small square-ish icon used by the favourites bar and its group sheet. */
@Composable
fun RealAppIconSmall(app: AppInfo, isDarkTheme: Boolean, modifier: Modifier = Modifier) {
    RealAppIcon(app = app, size = 26.dp, isDarkTheme = isDarkTheme, modifier = modifier)
}
