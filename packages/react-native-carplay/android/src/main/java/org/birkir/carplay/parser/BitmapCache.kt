package org.birkir.carplay.parser

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.core.content.ContextCompat
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSources
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.facebook.react.views.imagehelper.ImageSource
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri

object BitmapCache {
    private val maxMemory = Runtime.getRuntime().maxMemory().toInt()
    private val cacheSize = minOf(maxMemory / 8, 8388608) //limit cache to 8 megabyte

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(uri: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    private fun parseBitmap(uri: String, context: Context): Bitmap {
        if (uri.startsWith("res://")) {
            val name = uri.toUri().path?.substring(1)
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)

            val drawable = ContextCompat.getDrawable(context, id)
                ?: throw NotFoundException("drawable name: $name, id: $id not found")

            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }

            val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        }
        
        val source = ImageSource(context, uri)
        val imageRequest = ImageRequestBuilder.newBuilderWithSource(source.uri).build()
        val dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest, context)
        val result =
            DataSources.waitForFinalResult(dataSource) as CloseableReference<CloseableBitmap>
        val bitmap = result.get().underlyingBitmap.copy(Bitmap.Config.ARGB_8888, false)

        CloseableReference.closeSafely(result)
        dataSource.close()

        return bitmap
    }

    fun get(uri: String, context: Context): Bitmap {
        synchronized(bitmapCache) {
            bitmapCache.get(uri)?.let {
                return it
            }

            val bitmap = parseBitmap(uri, context)
            bitmapCache.put(uri, bitmap)
            return bitmap
        }
    }
}