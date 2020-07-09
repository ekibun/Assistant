package soko.ekibun.assistant.card.translate

import android.content.Context
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import soko.ekibun.assistant.ActionAdapter
import soko.ekibun.assistant.CardAdapter
import soko.ekibun.assistant.R
import soko.ekibun.assistant.action.AssistAction
import soko.ekibun.assistant.card.AssistCard
import soko.ekibun.assistant.util.AppUtil
import soko.ekibun.assistant.util.CoroutineUtil

class TranslateCard(private val actionAdapter: ActionAdapter, cardAdapter: CardAdapter, context: Context) : AssistCard(cardAdapter) {
    override val title: String = "翻译"
    override val icon: Int = R.drawable.ic_translate

    private val sp = PreferenceManager.getDefaultSharedPreferences(context)
    private val appid get() = sp.getString("translate_app_id", null)?:""
    private val key get() = sp.getString("translate_secret_key", null)?:""

    val action = AssistAction.build("翻译", context.getDrawable(R.drawable.ic_translate)) {
        CoroutineUtil.subscribe(key = "translate") {
            val rsp = withContext(Dispatchers.IO) { BaiduApi.translate(text, appid, key).execute() }.body()
            val result = rsp?.trans_result?.getOrNull(0)?.dst
            if (result != null) updateCard("${rsp.from}->${rsp.to}: $result", listOf(
                AssistAction.build("复制") {context ->
                    AppUtil.copy(context, result)
                    false
                }
            )) else removeCard()
        }
        false
    }

    var text = ""
    override fun processTextChange(text: String) {
        removeCard()
        this.text = text
        if(text.isEmpty() || appid.isEmpty() || key.isEmpty())
            actionAdapter.remove(action)
        else if(!actionAdapter.data.contains(action))
            actionAdapter.addData(action)
    }
}