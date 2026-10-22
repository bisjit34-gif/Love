package com.riptika.app.agent

import android.content.Context
import android.content.Intent
import com.riptika.app.access.RiptikaAccessibilityService
import com.riptika.app.data.Config
import com.riptika.app.data.Net
import com.riptika.app.data.SecureMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

interface Tool {
    val name: String
    val description: String
    val needsConfirm: Boolean
    suspend fun run(args: JSONObject): String
}

class Tools(private val ctx: Context, private val cfg: Config, private val mem: SecureMemory) {
    val all: List<Tool> = listOf(
        object : Tool {
            override val name = "get_time"
            override val description = "get_time {} -> current date/time"
            override val needsConfirm = false
            override suspend fun run(args: JSONObject): String = DateFormat.getDateTimeInstance().format(Date())
        },
        object : Tool {
            override val name = "remember"
            override val description = "remember {\"note\":\"...\"} -> save a fact about the user"
            override val needsConfirm = false
            override suspend fun run(args: JSONObject): String { mem.add(args.getString("note")); return "saved" }
        },
        object : Tool {
            override val name = "recall"
            override val description = "recall {\"query\":\"...\"} -> search saved notes"
            override val needsConfirm = false
            override suspend fun run(args: JSONObject): String =
                mem.search(args.getString("query")).joinToString("\n").ifBlank { "nothing found" }
        },
        object : Tool {
            override val name = "web_search"
            override val description = "web_search {\"query\":\"...\"} -> web results"
            override val needsConfirm = false
            override suspend fun run(args: JSONObject): String = withContext(Dispatchers.IO) {
                Net.http.newCall(Net.post(cfg, "/search", JSONObject().put("query", args.getString("query")))).execute().use {
                    if (!it.isSuccessful) "search failed (${it.code})" else JSONObject(it.body!!.string()).getString("results")
                }
            }
        },
        object : Tool {
            override val name = "open_app"
            override val description = "open_app {\"name\":\"WhatsApp\"} -> launch an installed app"
            override val needsConfirm = true
            override suspend fun run(args: JSONObject): String {
                val pm = ctx.packageManager
                val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val hit = pm.queryIntentActivities(q, 0)
                    .firstOrNull { it.loadLabel(pm).toString().contains(args.getString("name"), true) }
                    ?: return "app not found"
                val i = pm.getLaunchIntentForPackage(hit.activityInfo.packageName) ?: return "cannot launch"
                ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return "opened ${hit.loadLabel(pm)}"
            }
        },
        object : Tool {
            override val name = "read_screen"
            override val description = "read_screen {} -> text currently on screen (needs accessibility service enabled)"
            override val needsConfirm = true
            override suspend fun run(args: JSONObject): String =
                RiptikaAccessibilityService.instance?.dumpScreen()
                    ?: "Accessibility service is off. Ask the user to enable RIPTIKA in Settings > Accessibility."
        }
    )
    fun find(name: String): Tool? = all.firstOrNull { it.name == name }
}
