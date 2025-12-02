package furhatos.app.furhatlab.flow.fruitseller

import furhatos.app.furhatlab.flow.Idle
import furhatos.app.furhatlab.flow.StartInteraction
import furhatos.app.furhatlab.flow.chat.ChatState
import furhatos.flow.kotlin.*
import furhatos.gestures.Gestures
import furhatos.nlu.EnumEntity
import furhatos.nlu.Intent
import furhatos.nlu.common.No
import furhatos.nlu.common.RequestRepeat
import furhatos.nlu.common.Yes
import furhatos.records.Location
import furhatos.records.User
import furhatos.util.Language

/**
 * This is the top parent state which all other states inherit
 */
val Interaction: State = state(ChatState) {

    onUserLeave(instant = true) {
        goto(Idle)
    }

    onResponse<RequestRepeat> {
        reentry()
    }

}

/**
 * This is the entry state to the fruit seller
 */
val FruitSellerGreeting: State = state(Interaction) {

    onEntry {
        val greeting = utterance {
            +"Hi there!"
            +"Welcome to the furhat fruit shop"
        }
        furhat.say(greeting)
        furhat.gesture(Gestures.BigSmile(duration=4.0))
        goto(TakingOrder)
    }

    onUserEnter() {
        val newUser = it
        val currentUser = users.current
        furhat.attend(newUser)
        furhat.say("I will help you soon.")
        furhat.attend(currentUser)
        reentry()
    }

}

val TakingOrder: State = state(Interaction) {

    onEntry {
        furhat.ask("Would you like to buy some fruit?")
    }

    onResponse<BuyFruit> {
        val response = listOf("Your current order is ", "You currently have ")
        val fruit = it.intent.fruit!!.value!!
        furhat.say("Alright, $fruit it is.")
        users.current.order.fruits.add(fruit)
        furhat.say( response.random() + users.current.order.summarize())
        goto(AnythingElse)
    }

    onResponse<Question> {
        furhat.say("Ah, we have " + Fruit().getEnum(Language.ENGLISH_US).joinToString(", "))
    }

    onResponse<Yes> {
        furhat.ask("So, what fruit do you want?")
    }

    onResponse<No> {
        when {
            users.current.order.fruits.isEmpty() -> {
                goto(Goodbye)
            }
            else -> goto(Confirmation)
        }
    }

}

/**
 * Note: we are inheriting all triggers from TakingOrder
 */
val AnythingElse: State = state(TakingOrder) {

    onEntry {
        furhat.ask("Anything else?")
    }

}


val Confirmation: State = state(TakingOrder) {

    onEntry {
        furhat.ask("Okay, so you want to order ${users.current.order.summarize()}?")
    }

    onResponse<Yes> {
        goto(Goodbye)
    }

    onResponse<No> {
        users.current.order.fruits.clear()
        goto(TakingOrder)
    }

}

val Goodbye: State = state(Interaction) {

    onEntry {
        furhat.say("Alright, it was nice talking to you")
        furhat.gesture(Gestures.Wink(duration=2.0))
        goto(Idle)
    }

}


/*** NLU: INTENTS AND ENTITIES **/

class BuyFruit(val fruit : Fruit? = null) : Intent() {

    override fun getExamples(lang: Language) = listOf("I would like to buy an orange", "can I have some a banana", "banana")

}

class Fruit : EnumEntity() {
    override fun getEnum(lang: Language) = listOf("banana", "orange", "apple", "pineapple", "pear")

}

class Question() : Intent() {
    override fun getExamples(lang: Language) = listOf("What fruits do you have?", "What fruits are you selling?")
}

/** KEEPING TRACK OF CURRENT ORDER **/

class Order {

    /**
     * Summarize the fruits into a string like "banana, apple and orange"
     */
    fun summarize() = fruits.toList().let { items ->
        when (items.size) {
            0 -> ""
            1 -> items[0]
            2 -> items.joinToString(" and ")
            else -> items.dropLast(1).joinToString(", ") + ", and " + items.last()
        }
    }

    val fruits = mutableSetOf<String>()
}

var User.order by NullSafeUserDataDelegate { Order() }
