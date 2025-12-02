package furhatos.app.furhatlab.flow

import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.state
import furhatos.flow.kotlin.users
import furhatos.skills.SimpleEngagementPolicy
import furhatos.skills.SingleUserEngagementPolicy

val Init: State = state {
    onEntry {
        /** Set our default interaction parameters */
        users.setSimpleEngagementPolicy(distance = 1.5, maxUsers = 2)
        // furhat.character = "Marty"
        // furhat.voice = ""
        when {
            users.hasAny() -> {
                furhat.attend(users.random)
                goto(StartInteraction)
            }
            else -> goto(Idle)
        }
    }

}
