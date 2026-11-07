package com.miniyou.app.ai

import android.util.Base64
import com.miniyou.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiException(message: String) : Exception(message)

/**
 * 智谱 GLM-5.3-Flash 视觉理解（Chat Completion API）。
 * 文档：https://docs.bigmodel.cn/cn/guide/models/vlm/glm-5.3-flash
 * - POST https://open.bigmodel.cn/api/paas/v4/chat/completions，Bearer 鉴权
 * - 图片放在 messages[].content[] 的 image_url 块里，支持 Base64 Data URL；单张 < 5MB、≤ 6000×6000，jpg/png
 * - thinking.type 只支持 enabled，用 reasoning_effort 控制思考强度
 * - response_format = json_object 让模型输出 JSON
 */
object ZhipuClient {
    private const val ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions"

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun visionJson(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        images: List<ByteArray>,
        reasoningEffort: String = "low",
    ): JSONObject = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw AiException("还没有配置 AI Key，请到「设置 - AI 识图」中填写智谱 API Key")
        val content = JSONArray()
        images.forEach { bytes ->
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            content.put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$b64")))
        }
        content.put(JSONObject().put("type", "text").put("text", userPrompt))
        val body = JSONObject()
            .put("model", BuildConfig.ZHIPU_MODEL)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt))
                .put(JSONObject().put("role", "user").put("content", content)))
            .put("thinking", JSONObject().put("type", "enabled"))
            .put("reasoning_effort", reasoningEffort)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("temperature", 0.3)
            .put("max_tokens", 8192)
            .put("stream", false)
        val text = call(apiKey, body)
        parseJsonLoose(text) ?: throw AiException("AI 返回的内容无法解析，请换一张更清晰的图片重试")
    }

    /** 用一句纯文本请求校验 Key 是否可用。 */
    suspend fun ping(apiKey: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw AiException("Key 为空")
        val body = JSONObject()
            .put("model", BuildConfig.ZHIPU_MODEL)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "用一句不超过15个字的话问候一位孕妈妈。")))
            .put("thinking", JSONObject().put("type", "enabled"))
            .put("reasoning_effort", "low")
            .put("max_tokens", 1024)
        call(apiKey, body).trim()
    }

    private fun call(apiKey: String, body: JSONObject): String {
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val response = try {
            http.newCall(request).execute()
        } catch (e: IOException) {
            throw AiException("网络连接失败，请检查网络后重试（${e.javaClass.simpleName}）")
        }
        response.use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull()
                throw AiException(
                    when (resp.code) {
                        401 -> "API Key 无效或已过期，请到设置中更换"
                        429 -> "请求太频繁或额度不足，请稍后再试" + (msg?.let { "（$it）" } ?: "")
                        else -> "AI 服务出错（${resp.code}）" + (msg?.let { "：$it" } ?: "")
                    }
                )
            }
            val choice = JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)
                ?: throw AiException("AI 没有返回结果")
            if (choice.optString("finish_reason") == "sensitive") throw AiException("内容被安全审核拦截，请换一张图片")
            return choice.optJSONObject("message")?.optString("content").orEmpty()
        }
    }

    fun parseJsonLoose(text: String): JSONObject? {
        val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        runCatching { return JSONObject(cleaned) }
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start >= 0 && end > start) runCatching { return JSONObject(cleaned.substring(start, end + 1)) }
        return null
    }
}
