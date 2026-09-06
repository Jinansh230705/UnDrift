package com.undrift.network

import com.undrift.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val model: String? = null,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val temperature: Double? = 0.7,
    val max_tokens: Int? = null,
    val top_p: Double? = null,
)

@Serializable
data class ChatChoice(
    val index: Int,
    val message: ChatMessage? = null,
    val delta: ChatMessage? = null,
    val finish_reason: String? = null
)

@Serializable
data class ChatUsage(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null
)

@Serializable
data class ChatResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: ChatUsage? = null
)

@Serializable
data class ChatStreamChunk(
    val id: String? = null,
    val choices: List<ChatChoice>,
)

object ConduitRateLimiter {
    private const val MAX_REQUESTS_PER_MINUTE = 5
    private const val WINDOW_MILLIS = 60_000L
    private val requestTimestamps = java.util.ArrayDeque<Long>()
    private val lock = Any()

    @Volatile
    private var backoffUntil = 0L

    fun tryAcquire(): Boolean {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            if (now < backoffUntil) {
                return false
            }

            // Prune timestamps outside the 60-second window
            while (requestTimestamps.isNotEmpty() && (now - (requestTimestamps.peekFirst() ?: now)) >= WINDOW_MILLIS) {
                requestTimestamps.pollFirst()
            }

            if (requestTimestamps.size < MAX_REQUESTS_PER_MINUTE) {
                requestTimestamps.addLast(now)
                return true
            }
            return false
        }
    }

    fun recordRateLimitPenalty(retryAfterSeconds: Long = 60L) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            backoffUntil = now + (retryAfterSeconds * 1000L)
            requestTimestamps.clear()
            for (i in 0 until MAX_REQUESTS_PER_MINUTE) {
                requestTimestamps.addLast(backoffUntil)
            }
        }
    }

    fun getRemainingRequests(): Int {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            if (now < backoffUntil) return 0
            while (requestTimestamps.isNotEmpty() && (now - (requestTimestamps.peekFirst() ?: now)) >= WINDOW_MILLIS) {
                requestTimestamps.pollFirst()
            }
            return (MAX_REQUESTS_PER_MINUTE - requestTimestamps.size).coerceAtLeast(0)
        }
    }
}

open class ConduitClient(
    private val baseUrl: String = BuildConfig.PROXY_BASE_URL,
    private val proxyApiKey: String? = null, // Can be null now
    private val timeoutSeconds: Long = 60
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    open suspend fun chatCompletion(
        messages: List<ChatMessage>,
        model: String? = null,
        temperature: Double = 0.7,
        maxTokens: Int? = null,
    ): String {
        // Enforce 5 RPM rate limit before initiating network call
        if (!ConduitRateLimiter.tryAcquire()) {
            throw ConduitRateLimitException("Conduit 5 RPM rate limit active. Falling back to local offline rules.")
        }

        val reqBody = ChatRequest(
            model = model,
            messages = messages,
            stream = false,
            temperature = temperature,
            max_tokens = maxTokens
        )
        val bodyStr = json.encodeToString(ChatRequest.serializer(), reqBody)
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/v1/chat/completions")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .apply { addAuthHeader(this) }
            .build()

        http.newCall(request).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (resp.code == 429) {
                ConduitRateLimiter.recordRateLimitPenalty(60)
                throw ConduitRateLimitException("Received HTTP 429 Too Many Requests from proxy: $raw")
            }
            if (!resp.isSuccessful) {
                throw ConduitException("Proxy error ${resp.code}: $raw")
            }
            val response = json.decodeFromString(ChatResponse.serializer(), raw)
            return response.choices.firstOrNull()?.message?.content ?: throw ConduitException("Proxy response did not contain assistant content")
        }
    }

    open fun chatCompletionStream(
        messages: List<ChatMessage>,
        model: String? = null,
        temperature: Double = 0.7,
        maxTokens: Int? = null,
    ): Flow<String> = flow {
        if (!ConduitRateLimiter.tryAcquire()) {
            throw ConduitRateLimitException("Conduit 5 RPM rate limit active. Please wait a moment.")
        }

        val reqBody = ChatRequest(
            model = model,
            messages = messages,
            stream = true,
            temperature = temperature,
            max_tokens = maxTokens
        )
        val bodyStr = json.encodeToString(ChatRequest.serializer(), reqBody)
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/v1/chat/completions")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .apply { addAuthHeader(this) }
            .header("Accept", "text/event-stream")
            .build()

        http.newCall(request).execute().use { resp ->
            if (resp.code == 429) {
                ConduitRateLimiter.recordRateLimitPenalty(60)
                throw ConduitRateLimitException("Received HTTP 429 Too Many Requests from proxy")
            }
            if (!resp.isSuccessful) {
                val err = resp.body?.string() ?: ""
                throw ConduitException("Proxy error ${resp.code}: $err")
            }
            val source = resp.body?.source() ?: return@use
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    if (data.isEmpty()) continue
                    try {
                        val chunk = json.decodeFromString(ChatStreamChunk.serializer(), data)
                        val delta = chunk.choices.firstOrNull()?.delta?.content
                        if (!delta.isNullOrEmpty()) emit(delta)
                    } catch (_: Exception) {
                        // skip malformed chunk
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun addAuthHeader(builder: Request.Builder) {
        if (!proxyApiKey.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $proxyApiKey")
        }
        builder.header("Content-Type", "application/json")
    }
}

open class ConduitException(message: String) : Exception(message)
class ConduitRateLimitException(message: String) : ConduitException(message)
