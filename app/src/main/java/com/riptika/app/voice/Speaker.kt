package com.riptika.app.voice

import android.content.Context
import android.media.MediaPlayer
import com.riptika.app.data.Config
import com.riptika.app.data.Net
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** Calls backend /tts (holds the ElevenLabs key + Voice ID), then plays the mp3. */
class Speaker(private val ctx: Context, private val cfg: Config) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var player: MediaPlayer? = null

    fun speak(text: String) {
        scope.launch {
            try {
                val f = File(ctx.cacheDir, "tts.mp3")
                val ok = Net.http.newCall(Net.post(cfg, "/tts", JSONObject().put("text", text))).execute().use { r ->
                    if (!r.isSuccessful) false else {
                        r.body!!.byteStream().use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                        true
                    }
                }
                if (!ok) return@launch
                withContext(Dispatchers.Main) {
                    player?.release()
                    player = MediaPlayer().apply {
                        setDataSource(f.path)
                        setOnCompletionListener { it.release() }
                        prepare()
                        start()
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
