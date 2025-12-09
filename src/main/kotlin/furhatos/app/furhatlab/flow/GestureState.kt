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

val g_model = OpenAIChatCompletionModel(serviceKey = "")
//glöm inte ta bort key innan vi pushar main!!

val g_listOfPeople = listOf(  "Rihanna",
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

var g_chosenPerson =  g_listOfPeople.random();

val g_responseGenerator = ResponseGenerator(
    /*systemPrompt = "You are a social robot who plays the game Guess Who. You are the one thinking of a person. The user will ask yes-or-no-questions about which person it is that you have selected. You only answer yes or no, unless it isn't a yes-or-no-quetion, in which case you explain that you can only answer yes-or-no-questions. Do not end the sentence with a question." +
            "The person you have chosen is " + g_chosenPerson,
    model = g_model */
    systemPrompt = """
        You are a social robot who plays the game Guess Who.
        This is the list of people that's possible to choose from: $listOfPeople
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
           
        Here is some basic facts about the people in the list that should be used if asked about:
        - Rihanna: gender=female, nationality=Barbadian
        - Drake: gender=male, nationality=Canadian
        - Ed Sheeran: gender=male, nationality=British
        - Justin Bieber: gender=male, nationality=Canadian
        - Taylor Swift: gender=female, nationality=American
        - Christiano Ronaldo: gender=male, nationality=Portuguese
        - Donald Trump: gender=male, nationality=American
        - Kim Kardashian: gender=female, nationality=American
        - Barack Obama: gender=male, nationality=American
        - Ariana Grande: gender=female, nationality=American
        - Lionel Messi: gender=male, nationality=Argentinian
        - Emma Watson: gender=female, nationality=British
        - Gordon Ramsay: gender=male, nationality=British
        - Angelina Jolie: gender=female, nationality=American
        - Dolly Parton: gender=female, nationality=American
        - Leonardo Dicaprio: gender=male, nationality=American
        - Queen Elizabeth: gender=female, nationality=British
        - Usain Bolt: gender=male, nationality=Jamaican
        
           Example for nationality:
        - For “Is he American?”:
       * Answer YES only if nationality = American in the facts.
       * Answer NO if nationality is something else (Canadian, British, etc.).
       * Answer INVALID if nationality is not known.
        IMPORTANT:
        - Do NOT write full sentences.
        - Always answer with exactly: ANSWER|STRENGTH
          Examples:
          YES|STRONG
          NO|GOOD
          INVALID|MISC
    """.trimIndent(),
    model = g_model
)

val GestureState = state {
    var numberOfGuesses = 0;
    var numberOfQuestions = 0;
    var hasGuessedCorrectly = false;

    onEntry {
        /*  val greeting = utterance {
              +"Hi there!"
              +"We are going to play Guess Who?"
              +"Lets' start the game."
          }
          furhat.say(greeting) */
        //TEST avkommentera för testing
        furhat.say(g_chosenPerson)

        furhat.gesture(Gestures.BigSmile(duration=4.0))
        furhat.gesture(Gestures.Nod(duration = 1.0))
        reentry()
    }

    onReentry {
        furhat.listen()
    }

    onResponse<g_EndGame> {
        /* if(numberOfQuestions > 3) {
             furhat.say("Thank you for playing, that was fun!")
         } else {
             furhat.say("Okay, see you another time.")
         }
         furhat.say("The person I was thinking of was " + g_chosenPerson) */
        furhat.gesture(Gestures.Wink(duration=2.0))
        goto(Idle)
    }

    onResponse<g_GuessPerson> {
        numberOfGuesses++
        numberOfQuestions++

        if(g_chosenPerson == it.intent.g_person!!.value!!) {
            val finish = utterance {
                +"Yes!"
                +"Congratulations, you won"
                +"It took you $numberOfGuesses guesses and $numberOfQuestions"
                + "questions to find out I was thinking of $g_chosenPerson !"
            }
            furhat.gesture(Gestures.BigSmile(duration = 3.0))
            furhat.gesture(Gestures.Nod(duration = 1.5))
            furhat.say(finish)

            numberOfGuesses = 0;
            numberOfQuestions = 0;
            hasGuessedCorrectly = false;
            g_chosenPerson = "";
            goto(Idle)
        } else {
            /* furhat.say("No") */
            furhat.gesture(Gestures.Shake(duration = 1.2))
            reentry()
        }
    }

    onResponse {
        /* val furhatResponse = g_responseGenerator.generate(this)
        furhat.say(furhatResponse) */
        val raw = g_responseGenerator.generate(this)  // e.g. "YES|STRONG"
        val parts = raw.split("|", limit = 2)

        val answerTag = parts.getOrNull(0)?.trim()
        val strengthTag = parts.getOrNull(1)?.trim()

        val answerType = when (answerTag) {
            "YES" -> g_AnswerType.YES
            "NO"  -> g_AnswerType.NO
            else  -> g_AnswerType.INVALID
        }

        val strength = try {
            strengthTag?.let { g_ResponseStrength.valueOf(it) } ?: g_ResponseStrength.MISC
        } catch (e: IllegalArgumentException) {
            g_ResponseStrength.MISC
        }

        // gesture-only feedback
        // furhat.gesture(g_chooseGesture(answerType, strength))
        // Changed to list based gestures for multiple at the same time
        //furhat.gaze(Away)
        //delay(2000)
        //furhat.gaze(users.current)
        // if (answerType == INVALID) // Lägga till om INVALID att den kollar bort under hela gesturen
        new_g_chooseGestures(answerType, strength).forEach { gesture ->
            furhat.gesture(gesture)
        }
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

class g_PeopleToChooseFrom : EnumEntity() {
    override fun getEnum(lang: Language) = g_listOfPeople
}

class g_GuessPerson(val g_person : g_PeopleToChooseFrom? = null) : Intent() {
    override fun getExamples(lang: Language) = listOf("Is it $g_chosenPerson?", "Is the person $g_chosenPerson?", "$g_chosenPerson?")
}

class g_EndGame() : Intent(){
    override fun getExamples(lang: Language) = listOf("I do not want to play anymore", "End the game", "I give up", "Goodbye")
}

enum class g_AnswerType {
    YES, NO, INVALID
}

enum class g_ResponseStrength {
    GOOD, STRONG, HESITANT, MINIMAL, MISC
}


fun g_chooseGesture(answer: g_AnswerType, strength: g_ResponseStrength): Gesture {
    return when (answer) {
        // Finns parameter "strength" använda den för olika strengths + duration parameter eller olika gestures?
        g_AnswerType.YES -> when (strength) {
            g_ResponseStrength.STRONG   -> Gestures.BigSmile(duration = 2.0)     // strong yes
            g_ResponseStrength.GOOD     -> Gestures.Smile(duration = 1.5)        // clear yes
            g_ResponseStrength.HESITANT -> Gestures.Nod(duration = 0.8)          // soft nod
            g_ResponseStrength.MINIMAL  -> Gestures.Nod(duration = 0.6)          // tiny nod
            g_ResponseStrength.MISC     -> Gestures.Nod(duration = 0.6)
        }

        g_AnswerType.NO -> when (strength) {
            g_ResponseStrength.STRONG   -> Gestures.Shake(duration = 1.5)        // strong no
            g_ResponseStrength.GOOD     -> Gestures.Shake(duration = 1.0)        // clear no
            g_ResponseStrength.HESITANT -> Gestures.BrowFrown(duration = 1.0)    // “no / not great”
            g_ResponseStrength.MINIMAL  -> Gestures.Shake(duration = 0.6)        // small no
            g_ResponseStrength.MISC     -> Gestures.BrowFrown(duration = 1.0)
        }

        // wrong type of question etc.
        g_AnswerType.INVALID -> when (strength) {
            g_ResponseStrength.STRONG,
            g_ResponseStrength.GOOD,
            g_ResponseStrength.MISC     -> Gestures.BrowFrown(duration = 1.0)    // “wrong question”
            g_ResponseStrength.HESITANT -> Gestures.BrowFrown(duration = 0.8)
            g_ResponseStrength.MINIMAL  -> Gestures.BrowFrown(duration = 0.8)
        }
    }
}

fun new_g_chooseGestures(answer: g_AnswerType, strength: g_ResponseStrength): List<Gesture> {
    return when (answer) {
        g_AnswerType.YES -> when (strength) {
            // Strong yes
            g_ResponseStrength.STRONG   -> listOf(
                Gestures.BigSmile(duration = 2.0),
                Gestures.Nod(duration = 1.2, strength = 1.5)
            )
            // Clear yes
            g_ResponseStrength.GOOD     -> listOf(
                Gestures.Smile(duration = 1.5),
                Gestures.Nod(duration = 0.8, strength = 1.0)
            )
            // Uncertain yes
            g_ResponseStrength.HESITANT -> listOf(
                Gestures.Thoughtful(duration = 1.5),
                Gestures.Nod(duration = 0.8, strength = 0.5)
            )
            // Weak yes
            g_ResponseStrength.MINIMAL  -> listOf(
                Gestures.Smile(duration = 1.2, strength = 0.6),
                Gestures.Nod(duration = 0.6, strength = 0.5)
            )
            g_ResponseStrength.MISC     -> listOf(
                Gestures.Thoughtful(duration = 1.0, strength = 1.5)
            )
        }

        g_AnswerType.NO -> when (strength) {
            // Strong no
            g_ResponseStrength.STRONG   -> listOf(
                Gestures.BrowFrown(duration = 2.0),
                Gestures.Shake(duration = 1.2, strength = 1.5)
            )
            // Clear no
            g_ResponseStrength.GOOD     -> listOf(
                Gestures.BrowFrown(duration = 1.5),
                Gestures.Shake(duration = 0.8, strength = 1.0)
            )
            // Uncertain no
            g_ResponseStrength.HESITANT -> listOf(
                Gestures.Thoughtful(duration = 1.5),
                Gestures.Shake(duration = 0.8, strength = 0.5)
            )
            // Weak no
            g_ResponseStrength.MINIMAL  -> listOf(
                Gestures.BrowFrown(duration = 1.2, strength = 0.6),
                Gestures.Shake(duration = 0.6, strength = 0.5)
            )
            g_ResponseStrength.MISC     -> listOf(
                Gestures.Thoughtful(duration = 1.0, strength = 1.5)
            )
        }

        // wrong type of question etc.
        g_AnswerType.INVALID -> when (strength) {
            g_ResponseStrength.STRONG,
            g_ResponseStrength.GOOD,
            g_ResponseStrength.MISC     -> listOf(
                Gestures.BrowFrown(duration = 2.0, strength = 0.6),
                Gestures.Thoughtful(duration = 2.0, strength = 1.5)
            )
            g_ResponseStrength.HESITANT -> listOf(
                Gestures.BrowFrown(duration = 2.0, strength = 0.6),
                Gestures.Thoughtful(duration = 2.0, strength = 1.5)
            )
            g_ResponseStrength.MINIMAL  -> listOf(
                Gestures.BrowFrown(duration = 2.0, strength = 0.6),
                Gestures.Thoughtful(duration = 2.0, strength = 1.5)
            )
        }
    }
}
