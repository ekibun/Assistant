package soko.ekibun.assistant.card.translate

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import soko.ekibun.assistant.AssistInteractionSession
import soko.ekibun.assistant.R
import soko.ekibun.assistant.action.AssistAction
import soko.ekibun.assistant.card.AssistCard
import soko.ekibun.assistant.util.AppUtil
import java.util.concurrent.TimeUnit

class TranslateCard(private val session: AssistInteractionSession) : AssistCard(session.cardAdapter) {
    override val title: String = "翻译"
    override val icon: Int = R.drawable.ic_translate

    private val appid get() = session.sp.getString("translate_app_id", null)?:""
    private val key get() = session.sp.getString("translate_secret_key", null)?:""

    private val translateSubject = PublishSubject.create<String>().also {
        it.debounce(400, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .flatMap { text ->
                BaiduApi.translate(text, appid, key) }
            .retryWhen { throwableObservable ->
                throwableObservable.flatMap { Observable.just(0) }
            }.observeOn(AndroidSchedulers.mainThread())
            .subscribe {rsp ->
                val result = rsp.trans_result?.getOrNull(0)?.dst
                if (result != null) updateCard("${rsp.from}->${rsp.to}: $result", listOf(
                        AssistAction.build("复制") {context ->
                            AppUtil.copy(context, result)
                            false
                        }
                )) else removeCard()
            }
    }

    val action = AssistAction.build("翻译", session.context.getDrawable(R.drawable.ic_translate)) {
        translateSubject.onNext(text)
        false
    }

    var text = ""
    override fun processTextChange(text: String) {
        removeCard()
        this.text = text
        if(text.isEmpty() || appid.isEmpty() || key.isEmpty())
            session.actionAdapter.remove(action)
        else if(!session.actionAdapter.data.contains(action))
            session.actionAdapter.addData(action)
    }
}