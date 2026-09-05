package com.undrift.network

import com.undrift.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** OpenAI-compatible client for the UnDrift Cloudflare AI proxy. */
open class ProxyAiClient(
    private val baseUrl: String = BuildConfig.PROXY_BASE_URL,
    private val proxyApiKey: String = BuildConfig.PROXY_API_KEY,
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    open suspend fun chatCompletion(
        messages: List<ChatMessage>,
        model: String? = null,
        temperature: Double? = null
    ): String = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            if (!model.isNullOrBlank()) put("model", model)
            if (temperature != null) put("temperature", temperature)
            put("messages", JSONArray().apply {
                messages.forEach { message ->
                    put(JSONObject().apply {
                        put("role", message.role)
                        put("content", message.content)
                    })
                }
            })
        }

        val requestBuilder = Request.Builder()
            .url(completionsUrl())
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")

        if (proxyApiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $proxyApiKey")
        }

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ProxyAiException("Proxy request failed (${response.code})")
            }
            parseContent(body)
                ?: throw ProxyAiException("Proxy response did not contain assistant content")
        }
    }

    open fun chatCompletionStream(
        messages: List<ChatMessage>,
        model: String? = null,
        temperature: Double? = null
    ): Flow<String> = flow {
        val payload = JSONObject().apply {
            if (!model.isNullOrBlank()) put("model", model)
            if (temperature != null) put("temperature", temperature)
            put("stream", true)
            put("messages", JSONArray().apply {
                messages.forEach { message ->
                    put(JSONObject().apply {
                        put("role", message.role)
                        put("content", message.content)
                    })
                }
            })
        }

        val requestBuilder = Request.Builder()
            .url(completionsUrl())
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "text/event-stream")

        if (proxyApiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $proxyApiKey")
        }

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                throw ProxyAiException("Proxy request failed (${response.code}): $err")
            }
            
            val source = response.body?.source() ?: return@use
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    if (data.isEmpty()) continue
                    try {
                        val chunk = JSONObject(data)
                        val choices = chunk.optJSONArray("choices")
                        val firstChoice = choices?.optJSONObject(0)
                        val delta = firstChoice?.optJSONObject("delta")
                        val content = delta?.optString("content")
                        if (!content.isNullOrEmpty()) {
                            emit(content)
                        }
                    } catch (_: Exception) {
                        // skip malformed chunk
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun completionsUrl(): String =
        "${baseUrl.trimEnd('/')}/v1/chat/completions"

    private fun parseContent(body: String): String? {
        val json = JSONObject(body)
        val choices = json.optJSONArray("choices") ?: return null
        val message = choices.optJSONObject(0)?.optJSONObject("message") ?: return null
        return when (val content = message.opt("content")) {
            is String -> content
            is JSONArray -> buildString {
                for (index in 0 until content.length()) {
                    val part = content.optJSONObject(index)
                    append(part?.optString("text").orEmpty())
                }
            }.ifBlank { null }
            else -> null
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        private fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}

data class ChatMessage(val role: String, val content: String)

class ProxyAiException(message: String) : Exception(message)
