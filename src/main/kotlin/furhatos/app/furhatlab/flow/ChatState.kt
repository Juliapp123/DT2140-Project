package furhatos.app.furhatlab.flow.chat

import furhatos.app.furhatlab.flow.Idle
import furhatos.app.furhatlab.llm.OpenAIChatCompletionModel
import furhatos.app.furhatlab.llm.ResponseGenerator
import furhatos.flow.kotlin.*
import furhatos.nlu.common.Goodbye

val model = OpenAIChatCompletionModel(serviceKey = "")
//glöm inte ta bort key innan vi pushar main!!


val responseGenerator = ResponseGenerator(
    systemPrompt = "You are a social robot who plays the game Guess Who. You are the one thinking of a person. The user will ask yes-or-no-questions about which person it is that you have selected. You only answer yes or no, unless it isn't a yes-or-no-quetion, in which case you explain that you can only answer yes-or-no-questions. Do not end the sentence with a question.",
    model = model
)

val ChatState = state {
    numberOfGuesses = 0;
    numberOfQuestions = 0;
    hasGuessedCorrectly = false;
    chosenPerson = "";

    onEntry {
        chosenPerson = listOfPeople.random();

        val greeting = utterance {
            +"Hi there!"
            +"We are going to play Guess Who?"
            +"Lets' start the game."
        }
        furhat.say(greeting)
        furhat.gesture(Gestures.BigSmile(duration=4.0))
        goto(StartGame)
        reentry()
    }

    onReentry {
        furhat.listen()
    }

    onResponse<Goodbye> {
        furhat.say("Thank you for playing, that was fun!")
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
                +"It took you" +  ${numberOfGuesses} + "guesses and"
                + ${numberOfQuestions} + "questions to find out I was thinking of" + chosenPerson + "!"
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

listOfPeople = listOf("Rihanna", "Drake", "Ed Sheeran", "Justin Bieber", "Taylor Swift",
    "Christiano Ronaldo", "Donald Trump");

class PeopleToChooseFrom : EnumEntity() {
    override fun getEnum(lang: Language) = listOfPeople
}

class GuessPerson(val person : PeopleToChooseFrom? = null) : Intent() {
    override fun getExamples(lang: Language) = listOf("Is it Rihanna?", "Is the person Rihanna?", "Rihanna?")
}

enum class YesNo  {
    YES,
    NO
}
class ResponseStrength{
    GOOD, STRONG, HESITANT, MINIMAl, MISC
}

fun GestureChooser(responseStrength: ResponseStrength) : Gesture {
    return when (responseStrength) {
        ResponseStrength.GOOD -> Gestures.BigSmile(duration=2.0)
        ResponseStrength.STRONG -> Gestures.Smile(duration=2.0)
        ResponseStrength.HESITANT -> Gestures.BrowFurrow(duration=2.0)
        ResponseStrength.MINIMAl -> Gestures.Nod(duration=1.0)
        ResponseStrength.MISC -> Gestures.ShakeHead(duration=2.0)
    }
}