package furhatos.app.furhatlab

import furhatos.app.furhatlab.flow.Init
import furhatos.flow.kotlin.Flow
import furhatos.skills.Skill

class FurhatlabSkill : Skill() {
    override fun start() {
        Flow().run(Init)
    }
}

fun main(args: Array<String>) {
    Skill.main(args)
}
