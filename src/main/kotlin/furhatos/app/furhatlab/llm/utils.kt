package furhatos.app.furhatlab.llm

import org.json.JSONObject

inline fun <reified T> JSONObject.getOrNull(key: String): T? {
    if (has(key)) {
        val obj = get(key)
        if (obj is T) {
            return obj
        } else {
            return null
        }
    } else {
        return null
    }
}