package com.riptika.app.agent

import android.content.Context
import com.riptika.app.data.Config
import com.riptika.app.data.Net
import com.riptika.app.data.SecureMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** The whole agent loop runs on-device. Only model calls go out, through your backend. */
class Agent(ctx: Context, private val cfg: Config, private val mem: SecureMemory) {
    private val tools = Tools(ctx, cfg, mem)

    private fun systemPrompt(): String {
        val toolList = tools.all.joinToString("\n") { "- " + it.description }
        val notes = mem.all().takeLast(20).joinToString("\n") { "- $it" }.ifBlank { "(none)" }
        return "You are RIPTIKA, a personal assistant on the user's phone. " +
            "Reply in the language the user writes in (Bengali, Hindi or English). Be natural, concise and helpful.\n" +
            "You can use tools. To use one, reply with ONLY a JSON object like {\"tool\":\"name\",\"args\":{...}} and nothing else. " +
            "You will then get TOOL_RESULT and continue. When you have the final answer, reply in plain text.\n" +
            "Tools:\n$toolList\nKnown notes about the user:\n$notes"
    }

    private suspend fun chat(msgs: List<Pair<String, String>>): String = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        msgs.forEach { arr.put(JSONObject().put("role", it.first).put("content", it.second)) }
        Net.http.newCall(Net.post(cfg, "/chat", JSONObject().put("messages", arr))).execute().use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) error("backend ${it.code}: ${body.take(200)}")
            JSONObject(body).getString("reply")
        }
    }

    private fun parseCall(out: String): Pair<String, JSONObject>? {
        val t = out.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        if (!t.startsWith("{")) return null
        return try {
            val j = JSONObject(t)
            if (j.has("tool")) Pair(j.getString("tool"), j.optJSONObject("args") ?: JSONObject()) else null
        } catch (e: Exception) { null }
    }

    suspend fun respond(history: List<Pair<String, String>>, confirm: suspend (String) -> Boolean): String {
        val msgs = mutableListOf("system" to systemPrompt())
        msgs.addAll(history)
        for (step in 0 until 5) {
            val out = chat(msgs).trim()
            val call = parseCall(out) ?: return out
            val name = call.first
            val args = call.second
            val tool = tools.find(name)
            val result = when {
                tool == null -> "unknown tool"
                tool.needsConfirm && !confirm("${tool.name} $args") -> "user denied"
                else -> try { tool.run(args) } catch (e: Exception) { "error: ${e.message}" }
            }
            msgs.add("assistant" to out)
            msgs.add("user" to "TOOL_RESULT($name): $result")
        }
        return "Kaj ta sompurno korte parlam na, abar bolun."
    }
}
