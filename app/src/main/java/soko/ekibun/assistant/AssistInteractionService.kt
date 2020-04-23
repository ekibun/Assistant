package soko.ekibun.assistant

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.widget.Toast

class AssistInteractionService : VoiceInteractionService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.getBooleanExtra(EXTRA_SHOW_SESSION, false) == true) try{
            showSession(null, VoiceInteractionSession.SHOW_WITH_ASSIST or VoiceInteractionSession.SHOW_WITH_SCREENSHOT)
        }catch (e: IllegalStateException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        const val EXTRA_SHOW_SESSION = "soko.ekibun.assist.extra.SHOW_SESSION"
        fun showSession(context: Context) {
            val assistant = Settings.Secure.getString(
                context.contentResolver,
                "voice_interaction_service"
            )
            if(assistant != null && ComponentName.unflattenFromString(assistant)?.packageName == context.packageName){
                context.startService(Intent(context, AssistInteractionService::class.java)
                    .setAction(Intent.ACTION_ASSIST)
                    .putExtra(EXTRA_SHOW_SESSION, true))
            } else {
                Toast.makeText(context, "助手未激活", Toast.LENGTH_LONG).show()
                context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
            }
        }
    }
}