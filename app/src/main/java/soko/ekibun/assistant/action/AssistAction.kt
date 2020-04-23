package soko.ekibun.assistant.action

import android.content.Context
import android.graphics.drawable.Drawable

abstract class AssistAction {
    abstract val actionName: String
    abstract fun run(context: Context): Boolean

    open val icon: Drawable? = null

    companion object {
        fun build(actionName: String, icon: Drawable? = null, run: (Context) -> Boolean): AssistAction {
            return object: AssistAction() {
                override val icon: Drawable? = icon
                override val actionName: String = actionName
                override fun run(context: Context): Boolean {
                    return run(context)
                }
            }
        }
    }
}