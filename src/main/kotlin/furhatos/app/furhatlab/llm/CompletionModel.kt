package furhatos.app.furhatlab.llm

import furhatos.util.CommonUtils
import org.json.JSONArray
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL


private val logger = CommonUtils.getLogger("LLM")

data class RequestParams(
    val temperature: Double? = null,
    val maxTokens: Int = 300,
    val stop: List<String>? = null,
    val tools: JSONArray? = null)


class ChatCompletion(val text: String?, val model: ChatCompletionModel, val requestTime: Int)

abstract class ChatCompletionModel() {

    suspend fun request(prompt: Prompt) = request(prompt, null)

    abstract suspend fun request(prompt: Prompt, requestParams: RequestParams?): ChatCompletion

}


open class LLMException(message: String): Exception(message)

class LLMAssistantContentFilterException(): LLMException("Content filter in system response")

class LLMUserContentFilterException(): LLMException("Content filter in user request")