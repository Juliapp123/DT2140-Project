package furhatos.app.furhatlab.flow.fruitseller
import furhatos.app.furhatlab.flow.chat.ChatState
import furhatos.app.furhatlab.flow.Idle
import furhatos.app.furhatlab.flow.chat.responseGenerator
import furhatos.flow.kotlin.*
import furhatos.nlu.EnumEntity
import furhatos.nlu.Intent
import furhatos.nlu.common.No
import furhatos.nlu.common.RequestRepeat
import furhatos.nlu.common.Yes
import furhatos.records.User
import furhatos.util.Language
import furhatos.gestures.Gestures
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

    onResponse {
        //furhat.say("I am not sure I understand that")
        //reentry()

        val furhatResponse = responseGenerator.generate(this)
        furhat.say(furhatResponse)
        reentry()
    }

    onNoResponse {
        furhat.say("I didn't hear anything")
        reentry()
    }
    onUserEnter(){
        furhat.attend(it)
        furhat.say(text = "I will be with you shortly")
        furhat.attend(users.other)
        reentry()

    }
}

/**
 * This is the entry state to the fruit seller
 */
val FruitSellerGreeting: State = state(Interaction) {

    onEntry {
        furhat.gesture(Gestures.Smile)
        furhat.say("Hi there!")
        goto(TakingOrder)
    }


}

val TakingOrder: State = state(Interaction) {

    onEntry {
        furhat.ask("Would you like to buy some fruit?")
    }

    onResponse<BuyFruit> {
        val fruit = it.intent.fruit!!.value!!
        furhat.say("Alright, $fruit it is.")
        users.current.order.fruits.add(fruit)
        furhat.say("Your current order is ${users.current.order.summarize()}")
        goto(AnythingElse)
    }

    onResponse<Yes> {
        furhat.ask("So, what fruit do you want?")
    }

    onResponse<No> {
        goto( state = Confirm)
        goto(Goodbye)
    }
    onResponse<askFruit>{
        furhat.say(text = "banana, orange, apple, pineapple and pear")
        furhat.ask("So, what fruit do you want?")

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

val Confirm: State = state(parent = TakingOrder){
    onEntry {
        furhat.ask(text = "Is this what you want?")

    }
    onResponse<Yes> {
        furhat.say(text = "Confirmed list")
        goto(state = Goodbye)
    }
    onResponse<No> {
        goto(state = TakingOrder)
    }
}

val Goodbye: State = state(Interaction) {

    onEntry {
        furhat.gesture(Gestures.ExpressSad)

        furhat.say("Alright, it was nice talking to you")
        goto(Idle)
    }

}


/*** NLU: INTENTS AND ENTITIES **/

class BuyFruit(val fruit : Fruit? = null) : Intent() {

    override fun getExamples(lang: Language) = listOf("I would like to buy an orange", "can I have some a banana", "banana")

}
class askFruit: Intent(){
    override fun getExamples(lang: Language) = listOf(
        "What fruits do you have",
        "What do you have"
    )
}
class Fruit : EnumEntity() {
    override fun getEnum(lang: Language) = listOf("banana", "orange", "apple", "pineapple", "pear")

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
