package furhatos.app.furhatlab.flow.chat

import furhatos.app.furhatlab.flow.Idle
import furhatos.app.furhatlab.llm.OpenAIChatCompletionModel
import furhatos.app.furhatlab.llm.ResponseGenerator
import furhatos.flow.kotlin.*
import furhatos.nlu.EnumEntity
import furhatos.nlu.Intent
import furhatos.util.Language
import furhatos.flow.kotlin.Audio

val model = OpenAIChatCompletionModel(serviceKey = "")
//glöm inte ta bort key innan vi pushar main!!

val listOfPeople = listOf(  "Rihanna",
                            "Drake",
                            "Ed Sheeran",
                            "Justin Bieber",
                            "Taylor Swift",
                            "Christiano Ronaldo",
                            "Donald Trump",
                            "Kim Kardashian",
                            "Barack Obama",
                            "Ariana Grande",
                            "Lionel Messi",
                            "Emma Watson",
                            "Gordon Ramsay",
                            "Angelina Jolie",
                            "Dolly Parton",
                            "Leonardo Dicaprio",
                            "Queen Elizabeth",
                            "Usain Bolt"
                        );

var chosenPerson = listOfPeople.random();

val responseGenerator = ResponseGenerator(
    systemPrompt = """
        You are a social robot who plays the game Guess Who.
        The person you have chosen is: $chosenPerson.
        
        The user will ask yes-or-no questions to guess who it is.
        
        Your job is to:
        1) Decide if the correct answer is YES, NO, or INVALID.
           - INVALID = it is not a yes/no question or you cannot answer it as yes/no.
        2) Judge how good the question is for the game by choosing a strength:
           - STRONG  = very good, highly informative question
           - GOOD    = helpful question
           - MINIMAL = only a little helpful
           - HESITANT = vague/unclear/almost good
           - MISC    = wrong type of question or other meta feedback
        
        IMPORTANT:
        - Do NOT write full sentences.
        - Always answer with exactly: ANSWER|STRENGTH
          Examples:
          YES|STRONG
          NO|GOOD
          INVALID|MISC
    """.trimIndent(),
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
        val raw = responseGenerator.generate(this)
        val parts = raw.split("|", limit = 2)

        val answerTag = parts.getOrNull(0)?.trim()
        val strengthTag = parts.getOrNull(1)?.trim()

        val answerType = when (answerTag) {
            "YES" -> AnswerType.YES
            "NO"  -> AnswerType.NO
            else  -> AnswerType.INVALID
        }

        val strength = try {
            strengthTag?.let { ResponseStrength.valueOf(it) } ?: ResponseStrength.MISC
        } catch (e: IllegalArgumentException) {
            ResponseStrength.MISC
        }

        val prosody = utterance {
            + chooseProsody(answerType, strength)
        }

        // speech-only feedback
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

class PeopleToChooseFrom : EnumEntity() {
    override fun getEnum(lang: Language) = listOfPeople
}

class GuessPerson(val person : PeopleToChooseFrom? = null) : Intent() {
    override fun getExamples(lang: Language) = listOf("Is it $chosenPerson?", "Is the person $chosenPerson", "$chosenPerson?",)
}

class EndGame() : Intent(){
    override fun getExamples(lang: Language) = listOf("I do not want to play anymore", "End the game", "I give up", "Goodbye")
}

enum class AnswerType {
    YES, NO, INVALID
}

enum class ResponseStrength {
    GOOD, STRONG, HESITANT, MINIMAL, MISC
}

fun chooseProsody(answer: AnswerType, strength: ResponseStrength): Audio {

    return when (answer) {
        AnswerType.YES -> when (strength) {
            ResponseStrength.STRONG ->  Audio("classpath:sound/yesyesyes.wav", "YES YES YES!!")
            ResponseStrength.GOOD -> Audio("classpath:sound/yes.wav", "Yes")
            ResponseStrength.HESITANT -> Audio("classpath:sound/hmmmyes.wav", "Hmmm... yes?")
            ResponseStrength.MINIMAL -> Audio("classpath:sound/mhm.wav", "Mhm.")
            ResponseStrength.MISC -> Audio("classpath:sound/yes.wav", "Yes")
        }

        AnswerType.NO -> when (strength) {
            ResponseStrength.STRONG -> Audio("classpath:sound/absolutelynot.wav", "Absolutely NOT!")
            ResponseStrength.GOOD -> Audio("classpath:sound/no.wav", "No")
            ResponseStrength.HESITANT -> Audio("classpath:sound/hmmmno.wav", "Hmmm... no?")
            ResponseStrength.MINIMAL -> Audio("classpath:sound/mm-mm(no).wav", "Mm-mm")
            ResponseStrength.MISC -> Audio("classpath:sound/no.wav", "No")
        }

        // wrong type of question etc.
        AnswerType.INVALID -> when (strength) {
            ResponseStrength.STRONG -> Audio("classpath:sound/cantanswer.wav", "I can't really answer that.")
            ResponseStrength.GOOD -> Audio("classpath:sound/cantanswer.wav", "I can't really answer that.")
            ResponseStrength.MISC -> Audio("classpath:sound/notyesno.wav", "That's not a yes or no question")
            ResponseStrength.HESITANT -> Audio("classpath:sound/dontknow.wav", "I don't know..")
            ResponseStrength.MINIMAL -> Audio("classpath:sound/hmmm.wav", "Hmmmm...")
        }
    }
}