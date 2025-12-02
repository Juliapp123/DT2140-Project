package furhatos.app.furhatlab.llm

import furhatos.flow.kotlin.DialogHistory
import furhatos.flow.kotlin.FlowControlRunner
import furhatos.flow.kotlin.Furhat
import kotlinx.coroutines.runBlocking

class ResponseGenerator(val systemPrompt: String, val model: ChatCompletionModel) {

    fun generate(runner: FlowControlRunner): String {
        val response = runner.call {
            getChatCompletion()
        } as? String
        return response?:"An error has occurred"
    }

    fun getChatCompletion(): String? {
        val contextWindowSize = 10
        val messages = mutableListOf<Message>(SystemMessage(systemPrompt))
        Furhat.dialogHistory.all.takeLast(contextWindowSize).forEach {
            when (it) {
                is DialogHistory.ResponseItem -> {
                    messages.add(UserMessage(it.response.text))
                }
                is DialogHistory.UtteranceItem -> {
                    messages.add(AssistantMessage(it.toText()))
                }
            }
        }
        try {
            val completion = runBlocking {
                model.request(Prompt(messages))
            }
            return completion.text
        } catch (e: Exception) {
            return null
        }
    }

}
