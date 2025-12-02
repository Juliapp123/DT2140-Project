package furhatos.app.furhatlab.llm

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

val httpClient = HttpClient(Apache) {
    install(HttpTimeout) {
        requestTimeoutMillis = 10000L
    }
    followRedirects = true
}


/**
 * Posts a request using OpenAI:s http api (common among vendors)
 */
suspend fun ChatCompletionModel.postOpenAIRequest(apiUrl: String, jsonRequest: JSONObject, prompt: Prompt, headers: Map<String,String>, requestParams: RequestParams?): ChatCompletion {

    val defRequestParams =  requestParams?:RequestParams()

    val startTime = System.currentTimeMillis()

    jsonRequest.put("messages", prompt.toJSON())
    if (defRequestParams.temperature != null) {
        jsonRequest.put("temperature", defRequestParams.temperature)
    }
    if (defRequestParams.tools != null) {
        jsonRequest.put("tools", defRequestParams.tools)
    }
    jsonRequest.put("max_tokens", defRequestParams.maxTokens)
    if (defRequestParams.stop != null) {
        jsonRequest.put("stop", JSONArray(defRequestParams.stop))
    }

    val postResultBody = try {
        val postResult = httpClient.post(apiUrl) {
            headers.forEach { (k, v) -> header(k, v) }
            setBody(jsonRequest.toString())
            contentType(ContentType.Application.Json)
        }
        postResult.bodyAsText()
    } catch (e: IOException) {
        throw LLMException("Error in LLM request: " + (e.message?:e.cause?.message))
    }

    val responseTime = System.currentTimeMillis() - startTime

    try {
        val responseJSONObject = JSONObject(postResultBody)

        val error: JSONObject? = responseJSONObject.getOrNull("error")
        if (error != null) {
            val errorCode: Int? = error.getOrNull("status")
            if (errorCode == 400) {
                throw LLMUserContentFilterException()
            } else {
                throw LLMException(error.getOrNull("message") ?: error.toString())
            }
        } else {
            val choices = responseJSONObject.getJSONArray("choices")[0] as JSONObject
            val finishReason: String? = choices.getOrNull("finish_reason")
            if (finishReason == "content_filter") {
                throw LLMAssistantContentFilterException()
            }
            val message: JSONObject? = choices.getOrNull("message")
            val content: String? = message?.getOrNull<String>("content")?.trim()
            return ChatCompletion(content, this, responseTime.toInt())
        }
    } catch (e: JSONException) {
        throw LLMException("Error parsing response: ${e.message}")
    }
}

class OpenAIChatCompletionModel(
    val model: String = "gpt-4.1-mini",
    serviceKey: String): ChatCompletionModel() {

    private val apiUrl = "https://api.openai.com/v1/chat/completions"
    private val headers = mapOf("Content-Type" to "application/json", "Authorization" to "Bearer $serviceKey")

    override suspend fun request(prompt: Prompt, requestParams: RequestParams?): ChatCompletion {
        val jsonRequest = JSONObject().apply {
            put("model", model)
        }
        return postOpenAIRequest(apiUrl, jsonRequest, prompt, headers, requestParams)
    }

    override fun toString(): String {
        return model
    }

}

