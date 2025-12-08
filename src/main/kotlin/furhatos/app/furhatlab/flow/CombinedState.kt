package furhatos.app.furhatlab.flow.chat

import furhatos.app.furhatlab.flow.Idle
import furhatos.app.furhatlab.llm.OpenAIChatCompletionModel
import furhatos.app.furhatlab.llm.ResponseGenerator
import furhatos.flow.kotlin.*
import furhatos.nlu.common.Goodbye
import furhatos.gestures.Gestures
import furhatos.gestures.Gesture
import furhatos.nlu.EnumEntity
import furhatos.nlu.Intent
import furhatos.nlu.common.No
import furhatos.nlu.common.RequestRepeat
import furhatos.nlu.common.Yes
import furhatos.records.Location
import furhatos.records.User
import furhatos.util.Language
import furhatos.flow.kotlin.Audio

val c_model = OpenAIChatCompletionModel(serviceKey = "")
//glöm inte ta bort key innan vi pushar main!!

val c_listOfPeople = listOf("Rihanna", "Drake", "Ed Sheeran", "Justin Bieber", "Taylor Swift",
    "Christiano Ronaldo", "Donald Trump");
var c_chosenPerson = "Rihanna"// g_listOfPeople.random();

val c_responseGenerator = ResponseGenerator(
    /*systemPrompt = "You are a social robot who plays the game Guess Who. You are the one thinking of a person. The user will ask yes-or-no-questions about which person it is that you have selected. You only answer yes or no, unless it isn't a yes-or-no-quetion, in which case you explain that you can only answer yes-or-no-questions. Do not end the sentence with a question." +
            "The person you have chosen is " + g_chosenPerson,
    model = g_model */
    systemPrompt = """
        You are a social robot who plays the game Guess Who.
        You are thinking of one person from this list: $c_listOfPeople.
        The person you have chosen is: $c_chosenPerson.
        
        The user will ask yes-or-no questions to guess who it is.
        
        Your job is to:
        1) Decide if the correct answer is YES, NO, or INVALID.
           - INVALID = it is not a yes/no question or you cannot answer it as yes/no.
        2) Judge how good the question is for the game by choosing a strength:
           - STRONG  = very good, highly informative question
           - GOOD    = helpful question
           - MINIMAL = only a little helpful
           - HESITANT = vague/unclear / almost good
           - MISC    = wrong type of question or other meta feedback
        
        IMPORTANT:
        - Do NOT write full sentences.
        - Always answer with exactly: ANSWER|STRENGTH
          Examples:
          YES|STRONG
          NO|GOOD
          INVALID|MISC
    """.trimIndent(),
    model = c_model
)

val CombinedState = state {
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
        furhat.say(c_chosenPerson)

        furhat.gesture(Gestures.BigSmile(duration=4.0))
        furhat.gesture(Gestures.Nod(duration = 1.0))
        reentry()
    }

    onReentry {
        furhat.listen()
    }

    onResponse<c_EndGame> {
        if(numberOfQuestions > 3) {
            furhat.say("Thank you for playing, that was fun!")
        } else {
            furhat.say("Okay, see you another time.")
        }
        furhat.say("The person I was thinking of was " + c_chosenPerson)
        furhat.gesture(Gestures.Wink(duration=2.0))
        goto(Idle)
    }

    onResponse<c_GuessPerson> {
        numberOfGuesses++
        numberOfQuestions++

        if(c_chosenPerson == it.intent.c_person!!.value!!) {
            val finish = utterance {
                +"Yes!"
                +"Congratulations, you won"
                +"It took you $numberOfGuesses guesses and $numberOfQuestions"
                + "questions to find out I was thinking of $c_chosenPerson !"
            }
            furhat.gesture(Gestures.BigSmile(duration = 3.0))
            furhat.gesture(Gestures.Nod(duration = 1.5))
            furhat.say(finish)

            numberOfGuesses = 0;
            numberOfQuestions = 0;
            hasGuessedCorrectly = false;
            c_chosenPerson = "";
            goto(Idle)
        } else {
            furhat.gesture(Gestures.Shake(duration = 1.2))
            furhat.say("No")
            reentry()
        }
    }

    onResponse {
        /* val furhatResponse = c_responseGenerator.generate(this)
        furhat.say(furhatResponse) */
        val raw = c_responseGenerator.generate(this)  // e.g. "YES|STRONG"
        val parts = raw.split("|", limit = 2)

        val answerTag = parts.getOrNull(0)?.trim()
        val strengthTag = parts.getOrNull(1)?.trim()

        val answerType = when (answerTag) {
            "YES" -> c_AnswerType.YES
            "NO"  -> c_AnswerType.NO
            else  -> c_AnswerType.INVALID
        }

        val strength = try {
            strengthTag?.let { c_ResponseStrength.valueOf(it) } ?: c_ResponseStrength.MISC
        } catch (e: IllegalArgumentException) {
            c_ResponseStrength.MISC
        }

        // speech and gesture feedback
        val prosody = utterance {
            + c_chooseProsody(answerType, strength)
        }

        c_chooseGestures(answerType, strength).forEach { gesture ->
            furhat.gesture(gesture)
        }
        furhat.say(prosody)
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

class c_PeopleToChooseFrom : EnumEntity() {
    override fun getEnum(lang: Language) = c_listOfPeople
}

class c_GuessPerson(val c_person : g_PeopleToChooseFrom? = null) : Intent() {
    override fun getExamples(lang: Language) = listOf("Is it $c_chosenPerson?", "Is the person $c_chosenPerson?", "$c_chosenPerson?")
}

class c_EndGame() : Intent(){
    override fun getExamples(lang: Language) = listOf("I do not want to play anymore", "End the game", "I give up", "Goodbye")
}

enum class c_AnswerType {
    YES, NO, INVALID
}

enum class c_ResponseStrength {
    GOOD, STRONG, HESITANT, MINIMAL, MISC
}

fun c_chooseProsody(answer: c_AnswerType, strength: c_ResponseStrength): Audio {

    return when (answer) {
        c_AnswerType.YES -> when (strength) {
            c_ResponseStrength.STRONG ->  Audio("classpath:sound/yesyesyes.wav", "YES YES YES!!")
            c_ResponseStrength.GOOD -> Audio("classpath:sound/yes.wav", "Yes")
            c_ResponseStrength.HESITANT -> Audio("classpath:sound/hmmmyes.wav", "Hmmm... yes?")
            c_ResponseStrength.MINIMAL -> Audio("classpath:sound/mhm.wav", "Mhm.")
            c_ResponseStrength.MISC -> Audio("classpath:sound/yes.wav", "Yes")
        }

        c_AnswerType.NO -> when (strength) {
            c_ResponseStrength.STRONG -> Audio("classpath:sound/absolutelynot.wav", "Absolutely NOT!")
            c_ResponseStrength.GOOD -> Audio("classpath:sound/no.wav", "No")
            c_ResponseStrength.HESITANT -> Audio("classpath:sound/hmmmno.wav", "Hmmm... no?")
            c_ResponseStrength.MINIMAL -> Audio("classpath:sound/mm-mm(no).wav", "Mm-mm")
            c_ResponseStrength.MISC -> Audio("classpath:sound/no.wav", "No")
        }

        // wrong type of question etc.
        c_AnswerType.INVALID -> when (strength) {
            c_ResponseStrength.STRONG -> Audio("classpath:sound/cantanswer.wav", "I can't really answer that.")
            c_ResponseStrength.GOOD -> Audio("classpath:sound/cantanswer.wav", "I can't really answer that.")
            c_ResponseStrength.MISC -> Audio("classpath:sound/notyesno.wav", "That's not a yes or no question")
            c_ResponseStrength.HESITANT -> Audio("classpath:sound/dontknow.wav", "I don't know..")
            c_ResponseStrength.MINIMAL -> Audio("classpath:sound/hmmm.wav", "Hmmmm...")
        }
    }
}

fun c_chooseGestures(answer: c_AnswerType, strength: c_ResponseStrength): List<Gesture> {
    return when (answer) {
        c_AnswerType.YES -> when (strength) {
            // Strong yes
            c_ResponseStrength.STRONG   -> listOf(
                Gestures.BigSmile(duration = 2.0),
                Gestures.Nod(duration = 1.2, strength = 1.5)
            )
            // Clear yes
            c_ResponseStrength.GOOD     -> listOf(
                Gestures.Smile(duration = 1.5),
                Gestures.Nod(duration = 0.8, strength = 1.0)
            )
            // Uncertain yes
            c_ResponseStrength.HESITANT -> listOf(
                Gestures.Thoughtful(duration = 1.5),
                Gestures.Nod(duration = 0.8, strength = 0.5)
            )
            // Weak yes
            c_ResponseStrength.MINIMAL  -> listOf(
                Gestures.Smile(duration = 1.2, strength = 0.6),
                Gestures.Nod(duration = 0.6, strength = 0.5)
            )
            c_ResponseStrength.MISC     -> listOf(
                Gestures.Thoughtful(duration = 1.0, strength = 1.5)
            )
        }

        c_AnswerType.NO -> when (strength) {
            // Strong no
            c_ResponseStrength.STRONG   -> listOf(
                Gestures.BrowFrown(duration = 2.0),
                Gestures.Shake(duration = 1.2, strength = 1.5)
            )
            // Clear no
            c_ResponseStrength.GOOD     -> listOf(
                Gestures.BrowFrown(duration = 1.5),
                Gestures.Shake(duration = 0.8, strength = 1.0)
            )
            // Uncertain no
            c_ResponseStrength.HESITANT -> listOf(
                Gestures.Thoughtful(duration = 1.5),
                Gestures.Shake(duration = 0.8, strength = 0.5)
            )
            // Weak no
            c_ResponseStrength.MINIMAL  -> listOf(
                Gestures.BrowFrown(duration = 1.2, strength = 0.6),
                Gestures.Shake(duration = 0.6, strength = 0.5)
            )
            c_ResponseStrength.MISC     -> listOf(
                Gestures.Thoughtful(duration = 1.0, strength = 1.5)
            )
        }

        // wrong type of question etc.
        c_AnswerType.INVALID -> when (strength) {
            c_ResponseStrength.STRONG,
            c_ResponseStrength.GOOD,
            c_ResponseStrength.MISC     -> listOf(
                Gestures.BrowFrown(duration = 2.0, strength = 0.6),
                Gestures.Thoughtful(duration = 2.0, strength = 1.5)
            )
            c_ResponseStrength.HESITANT -> listOf(
                Gestures.BrowFrown(duration = 2.0, strength = 0.6),
                Gestures.Thoughtful(duration = 2.0, strength = 1.5)
            )
            c_ResponseStrength.MINIMAL  -> listOf(
                Gestures.BrowFrown(duration = 2.0, strength = 0.6),
                Gestures.Thoughtful(duration = 2.0, strength = 1.5)
            )
        }
    }
}
