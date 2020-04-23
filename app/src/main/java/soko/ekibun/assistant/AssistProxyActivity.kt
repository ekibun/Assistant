package soko.ekibun.assistant

import android.app.Activity
import android.os.Bundle

class AssistProxyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AssistInteractionService.showSession(this)
        finish()
    }
}
