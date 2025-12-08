package furhatos.app.furhatlab.flow

import furhatos.app.furhatlab.flow.chat.ChatState
import furhatos.app.furhatlab.flow.chat.GestureState
import furhatos.app.furhatlab.flow.chat.CombinedState
import furhatos.app.furhatlab.flow.fruitseller.FruitSellerGreeting
import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.state

val StartInteraction: State = state {
    onEntry {
        //goto(FruitSellerGreeting)
        //goto(GestureState)
        goto(ChatState)
        //goto(CombinedState)
    }

}
