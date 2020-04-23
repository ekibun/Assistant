package soko.ekibun.assistant.action.screenocr

import android.content.Context
import android.graphics.Bitmap
import soko.ekibun.assistant.action.ScreenActivity
import soko.ekibun.assistant.action.AssistAction

class ScreenOcrAction(private val screenshot: Bitmap): AssistAction() {
    override val actionName: String = "屏幕识别"

    override fun run(context: Context): Boolean {
        ScreenActivity.startActivityWithScreenShot(context, screenshot)
        return true
    }
}