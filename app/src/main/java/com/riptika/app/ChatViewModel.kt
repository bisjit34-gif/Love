package com.riptika.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.riptika.app.agent.Agent
import com.riptika.app.data.Config
import com.riptika.app.data.SecureMemory
import com.riptika.app.voice.Speaker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

data class Msg(val role: String, val text: String)

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    val cfg = Config(app)
    private val agent = Agent(app, cfg, SecureMemory(app))
    private val speaker = Speaker(app, cfg)

    val messages = mutableStateListOf<Msg>()
    var busy by mutableStateOf(false)
    var confirmText by mutableStateOf<String?>(null)
    private var pending: CompletableDeferred<Boolean>? = null

    fun send(text: String) {
        if (text.isBlank() || busy) return
        messages.add(Msg("user", text))
        busy = true
        viewModelScope.launch {
            val reply = try {
                agent.respond(messages.map { it.role to it.text }) { ask(it) }
            } catch (e: Exception) { "Error: ${e.message}" }
            messages.add(Msg("assistant", reply))
            busy = false
            speaker.speak(reply)
        }
    }

    private suspend fun ask(t: String): Boolean {
        val d = CompletableDeferred<Boolean>()
        pending = d
        confirmText = t
        return d.await()
    }

    fun answer(ok: Boolean) { pending?.complete(ok); pending = null; confirmText = null }
}
