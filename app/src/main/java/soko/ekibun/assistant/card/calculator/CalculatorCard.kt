package soko.ekibun.assistant.card.calculator

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import net.objecthunter.exp4j.ExpressionBuilder
import soko.ekibun.assistant.CardAdapter
import soko.ekibun.assistant.R
import soko.ekibun.assistant.card.AssistCard

class CalculatorCard(cardAdapter: CardAdapter) : AssistCard(cardAdapter) {
    override val title: String = "表达式"
    override val icon: Int = R.drawable.ic_exposure

    private val calculateSubject = PublishSubject.create<String>().also {
        it.subscribeOn(Schedulers.io())
            .filter { text -> text.isNotEmpty() }
            .map {text ->
                ExpressionBuilder(text).build().evaluate()
            }.retryWhen { throwableObservable ->
                throwableObservable.observeOn(AndroidSchedulers.mainThread())
                    .flatMap {
                        removeCard()
                        Observable.just(0)
                    }
            }.observeOn(AndroidSchedulers.mainThread())
            .subscribe{ result ->
                updateCard("= $result", listOf())
            }
    }

    override fun processTextChange(text: String) {
        if(text.isEmpty()) removeCard()
        calculateSubject.onNext(text)
    }
}