package com.riptika.app.data

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class Config(ctx: Context) {
    private val p = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    var baseUrl: String
        get() = p.getString("url", "http://10.0.2.2:8787")!!
        set(v) { p.edit().putString("url", v.trim().trimEnd('/')).apply() }
    var token: String
        get() = p.getString("token", "")!!
        set(v) { p.edit().putString("token", v.trim()).apply() }
}

object Net {
    val http: OkHttpClient = OkHttpClient.Builder().readTimeout(90, TimeUnit.SECONDS).build()
    fun post(cfg: Config, path: String, body: JSONObject): Request =
        Request.Builder().url(cfg.baseUrl + path)
            .header("X-App-Token", cfg.token)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
}
