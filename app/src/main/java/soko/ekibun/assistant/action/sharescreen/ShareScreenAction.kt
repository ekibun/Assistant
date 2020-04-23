package soko.ekibun.assistant.action.sharescreen

import android.content.Context
import android.graphics.Bitmap
import soko.ekibun.assistant.action.AssistAction
import soko.ekibun.assistant.action.ScreenActivity

class ShareScreenAction(private val screenshot: Bitmap) : AssistAction() {
    override val actionName: String = "分享屏幕"

    override fun run(context: Context): Boolean {
        ScreenActivity.startActivityWithScreenShot(context, screenshot, true)
        return true
    }
}