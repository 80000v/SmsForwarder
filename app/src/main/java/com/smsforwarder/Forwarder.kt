package com.smsforwarder

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

object Forwarder {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun send(
        method: String,
        webhookUrl: String,
        ntfyTopic: String,
        ntfyServer: String,
        entry: ForwardEntry,
        callback: (Boolean, String) -> Unit
    ) {
        when (method) {
            "webhook" -> sendWebhook(webhookUrl, entry, callback)
            "ntfy" -> sendNtfy(ntfyTopic, ntfyServer, entry, callback)
            "both" -> {
                sendWebhook(webhookUrl, entry) { ok1, msg1 ->
                    sendNtfy(ntfyTopic, ntfyServer, entry) { ok2, msg2 ->
                        callback(ok1 && ok2, if (ok1 && ok2) "全部成功" else "Webhook: $msg1; ntfy: $msg2")
                    }
                }
            }
            else -> callback(false, "未知转发方式")
        }
    }

    private fun sendWebhook(url: String, entry: ForwardEntry, callback: (Boolean, String) -> Unit) {
        if (url.isBlank()) {
            callback(false, "Webhook URL 未配置")
            return
        }

        val json = JSONObject().apply {
            put("sender", entry.sender)
            put("body", entry.body)
            put("timestamp", entry.timestamp)
        }

        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "网络错误: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful, "HTTP ${response.code}")
                response.close()
            }
        })
    }

    private fun sendNtfy(
        topic: String,
        server: String,
        entry: ForwardEntry,
        callback: (Boolean, String) -> Unit
    ) {
        if (topic.isBlank()) {
            callback(false, "ntfy Topic 未配置")
            return
        }

        val baseUrl = server.trimEnd('/')
        val request = Request.Builder()
            .url("$baseUrl/$topic")
            .header("Title", "短信来自 ${entry.sender}")
            .header("Priority", "default")
            .post(RequestBody.create("text/plain".toMediaType(), entry.body))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "ntfy 错误: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful, "ntfy ${response.code}")
                response.close()
            }
        })
    }
}