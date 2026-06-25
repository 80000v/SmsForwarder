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
        pushplusToken: String,
        entry: ForwardEntry,
        callback: (Boolean, String) -> Unit
    ) {
        when (method) {
            "webhook" -> sendWebhook(webhookUrl, entry, callback)
            "pushplus" -> sendPushplus(pushplusToken, entry, callback)
            "both" -> {
                sendWebhook(webhookUrl, entry) { ok1, msg1 ->
                    sendPushplus(pushplusToken, entry) { ok2, msg2 ->
                        callback(ok1 && ok2, if (ok1 && ok2) "全部成功" else "Webhook: $msg1; PushPlus: $msg2")
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

    private fun sendPushplus(
        token: String,
        entry: ForwardEntry,
        callback: (Boolean, String) -> Unit
    ) {
        if (token.isBlank()) {
            callback(false, "PushPlus Token 未配置")
            return
        }

        val json = JSONObject().apply {
            put("token", token)
            put("title", "短信来自 ${entry.sender}")
            put("content", entry.body)
            put("template", "txt")
        }

        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("http://www.pushplus.plus/send")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "PushPlus 错误: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful, "PushPlus ${response.code}")
                response.close()
            }
        })
    }
}