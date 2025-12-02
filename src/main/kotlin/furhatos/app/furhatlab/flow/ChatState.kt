package furhatos.app.furhatlab.flow.chat

import furhatos.app.furhatlab.flow.Idle
import furhatos.app.furhatlab.llm.OpenAIChatCompletionModel
import furhatos.app.furhatlab.llm.ResponseGenerator
import furhatos.flow.kotlin.*
import furhatos.nlu.common.Goodbye
import furhatos.gestures.Gestures
import furhatos.nlu.EnumEntity
import furhatos.nlu.Intent
import furhatos.nlu.common.No
import furhatos.nlu.common.RequestRepeat
import furhatos.nlu.common.Yes
import furhatos.records.Location
import furhatos.records.User
import furhatos.util.Language
import io.github.cdimascio.dotenv.Dotenv

val dotenv = Dotenv.load()

val apiKey = dotenv["OPENAI_API_KEY"]
    ?: throw IllegalStateException("OPENAI_API_KEY missing in .env file")

val model = OpenAIChatCompletionModel(serviceKey = apiKey)

val listOfPeople = listOf("Rihanna", "Drake", "Ed Sheeran", "Justin Bieber", "Taylor Swift",
    "Christiano Ronaldo", "Donald Trump");
var chosenPerson = listOfPeople.random();

val responseGenerator = ResponseGenerator(
    systemPrompt = "You are a social robot who plays the game Guess Who. You are the one thinking of a person. The user will ask yes-or-no-questions about which person it is that you have selected. You only answer yes or no, unless it isn't a yes-or-no-quetion, in which case you explain that you can only answer yes-or-no-questions. Do not end the sentence with a question." +
            "The person you have chosen is " + chosenPerson,
    model = model
)

val ChatState = state {
    var numberOfGuesses = 0;
    var numberOfQuestions = 0;
    var hasGuessedCorrectly = false;

    onEntry {
        val greeting = utterance {
            +"Hi there!"
            +"We are going to play Guess Who?"
            +"Lets' start the game."
        }
        furhat.say(greeting)
        //TEST avkommentera för testing
        //furhat.say(chosenPerson)
        furhat.gesture(Gestures.BigSmile(duration=4.0))
        reentry()
    }

    onReentry {
        furhat.listen()
    }

    onResponse<EndGame> {
        if(numberOfQuestions > 3) {
            furhat.say("Thank you for playing, that was fun!")
        } else {
            furhat.say("Okay, see you another time.")
        }
        furhat.say("The person I was thinking of was " + chosenPerson)
        furhat.gesture(Gestures.Wink(duration=2.0))
        goto(Idle)
    }

    onResponse<GuessPerson> {
        numberOfGuesses++
        numberOfQuestions++

        if(chosenPerson == it.intent.person!!.value!!) {
            val finish = utterance {
                +"Yes!"
                +"Congratulations, you won"
                +"It took you $numberOfGuesses guesses and $numberOfQuestions"
                  + "questions to find out I was thinking of $chosenPerson !"
            }
            furhat.say(finish)
            numberOfGuesses = 0;
            numberOfQuestions = 0;
            hasGuessedCorrectly = false;
            chosenPerson = "";
            goto(Idle)
        } else {
            furhat.say("No")
            reentry()
        }
    }

    onResponse {
        val furhatResponse = responseGenerator.generate(this)
        furhat.say(furhatResponse)
        numberOfQuestions++
        reentry()
    }

    onNoResponse {
        reentry()
    }

    onUserLeave {
        furhat.say("Thank you for playing!")
        goto(Idle)
    }

}

class PeopleToChooseFrom : EnumEntity() {
    override fun getEnum(lang: Language) = listOfPeople
}

class GuessPerson(val person : PeopleToChooseFrom? = null) : Intent() {
    override fun getExamples(lang: Language) = listOf("Is it Rihanna?", "Is the person Rihanna?", "Rihanna?")
}

class EndGame() : Intent(){
    override fun getExamples(lang: Language) = listOf("I do not want to play anymore", "End the game", "I give up", "Goodbye")
}

enum class ResponseStrength{
    GOOD, STRONG, HESITANT, MINIMAl, MISC
}
/*
fun GestureChooser(responseStrength: ResponseStrength) : Gesture {
    return when (responseStrength) {
        ResponseStrength.GOOD -> Gestures.BigSmile(duration=2.0)
        ResponseStrength.STRONG -> Gestures.Smile(duration=2.0)
        ResponseStrength.HESITANT -> Gestures.BrowFrown(duration=2.0)
        ResponseStrength.MINIMAl -> Gestures.Nod(duration=1.0)
        ResponseStrength.MISC -> Gestures.Shake(duration = 2.0)
    }
} */