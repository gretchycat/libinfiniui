package com.infinifolder.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import com.infinifolder.preview.CanvasStreams
import com.infinifolder.viewer.ImageAnimationPlayback
import java.io.Closeable
import java.util.concurrent.Executors

/** Main-thread image ownership; one shared worker renders all palette/frame animations. */
internal class SkinImageCache(private val sources: (String) -> List<Pair<String, ByteArray>>, private val changed: () -> Unit) {
    private val entries = mutableMapOf<String, Image?>()
    private var pixels = 0L
    private var playing = false
    private val worker by lazy { Executors.newSingleThreadExecutor { Thread(it, "skin-animation") } }
    private class Image(val bitmap: Bitmap, val playbacks: List<ImageAnimationPlayback>, val natives: List<NativeLayer>) : Closeable {
        fun play() { playbacks.forEach { it.play() }; natives.forEach { it.play() } }
        fun pause() { playbacks.forEach { it.pause(true) }; natives.forEach { it.pause() } }
        override fun close() { playbacks.forEach { it.close() }; natives.forEach { it.close() } }
    }
    private class NativeLayer(val drawable: Drawable, val bitmap: Bitmap, val changed: () -> Unit) : Drawable.Callback, Closeable {
        private val main = Handler(Looper.getMainLooper())
        private var active = false
        private val draw = Runnable {
            if (active) { bitmap.eraseColor(0); drawable.draw(Canvas(bitmap)); changed() }
        }
        init { drawable.setBounds(0, 0, bitmap.width, bitmap.height); drawable.callback = this }
        fun play() { active = true; (drawable as? Animatable)?.start(); main.post(draw) }
        fun pause() { active = false; (drawable as? Animatable)?.stop(); main.removeCallbacksAndMessages(null) }
        override fun invalidateDrawable(who: Drawable) { if (active) { main.removeCallbacks(draw); main.post(draw) } }
        override fun scheduleDrawable(who: Drawable, what: Runnable, whenMillis: Long) { if (active) main.postAtTime(what, whenMillis) }
        override fun unscheduleDrawable(who: Drawable, what: Runnable) { main.removeCallbacks(what) }
        override fun close() { pause(); drawable.callback = null }
    }
    fun setPlaying(value: Boolean) {
        playing = value
        entries.values.filterNotNull().forEach { if (value) it.play() else it.pause() }
    }
    fun clear() {
        entries.values.filterNotNull().forEach { it.close() }
        // Published bitmaps can still be referenced by a View/Drawable during a theme change.
        entries.clear(); pixels = 0
    }
    fun bitmap(name: String): Bitmap? {
        val key = SkinArtwork.role(name)
        if (entries.containsKey(key)) return entries[key]?.bitmap
        val image = try { decode(sources(name)) } catch (error: Exception) { android.util.Log.w("SkinImage", "Cannot decode skin image $key", error); null }
        entries[key] = image
        if (playing) image?.play()
        return image?.bitmap
    }
    private fun decode(sources: List<Pair<String, ByteArray>>): Image? {
        if (sources.isEmpty()) return null
        val frames = sources.map { (name, bytes) -> CanvasStreams.decodeBytes(bytes, name, 2048) ?: throw IllegalArgumentException("Unsupported skin image: $name") }
        val width = frames.first().bitmap.width; val height = frames.first().bitmap.height
        require(width > 0 && height > 0 && frames.all { it.bitmap.width == width && it.bitmap.height == height }) { "Skin layers must share canvas dimensions" }
        val cost = width.toLong() * height * (frames.size + 1)
        require(width.toLong() * height <= 2_000_000 && pixels + cost <= 8_000_000) { "Skin exceeds decoded pixel budget" }
        val composite = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composite)
        fun redraw() {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)
            frames.forEach { canvas.drawBitmap(it.bitmap, 0f, 0f, null) }
        }
        redraw()
        val playbacks = frames.mapNotNull { frame ->
            (frame.animation ?: frame.colorCycling)?.let { animation ->
                ImageAnimationPlayback(animation, frame.bitmap, changed = { redraw(); changed() }, failed = { android.util.Log.w("SkinImage", "Animation stopped", it) }, executor = worker)
            }
        }
        val natives = frames.mapNotNull { frame -> frame.animationDrawable?.let { NativeLayer(it, frame.bitmap) { redraw(); changed() } } }
        pixels += cost
        return Image(composite, playbacks, natives)
    }
}
