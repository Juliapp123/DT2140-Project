package furhatos.app.furhatlab.flow.chat

import furhatos.app.furhatlab.flow.Idle
import furhatos.app.furhatlab.llm.OpenAIChatCompletionModel
import furhatos.app.furhatlab.llm.ResponseGenerator
import furhatos.flow.kotlin.*
import furhatos.nlu.common.Goodbye

val model = OpenAIChatCompletionModel(serviceKey = "")

val responseGenerator = ResponseGenerator(
    systemPrompt = "You are a flirty and social robot. Your name is Benny. You give very brief answers and a lot of compliments. No emojis. Do not end the sentence with a question.",
    model = model
)

val ChatState = state {

    onEntry {
        furhat.say("What do you want to chat about?")
        reentry()
    }

    onReentry {
        furhat.listen()
    }

    onResponse<Goodbye> {
        furhat.say("It was nice talking to you")
        goto(Idle)
    }

    onResponse {
        val furhatResponse = responseGenerator.generate(this)
        furhat.say(furhatResponse)
        reentry()
    }

    onNoResponse {
        reentry()
    }

    onUserLeave {
        furhat.say("It was nice talking to you")
        goto(Idle)
    }

}
