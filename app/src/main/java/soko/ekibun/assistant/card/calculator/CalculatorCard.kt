package soko.ekibun.assistant.card.calculator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.objecthunter.exp4j.ExpressionBuilder
import soko.ekibun.assistant.CardAdapter
import soko.ekibun.assistant.R
import soko.ekibun.assistant.card.AssistCard
import soko.ekibun.assistant.util.CoroutineUtil

class CalculatorCard(cardAdapter: CardAdapter) : AssistCard(cardAdapter) {
    override val title: String = "表达式"
    override val icon: Int = R.drawable.ic_exposure

    override fun processTextChange(text: String) {
        if(text.isEmpty()) removeCard()
        CoroutineUtil.subscribe {
            try {
                val result = withContext(Dispatchers.IO) { ExpressionBuilder(text).build().evaluate() }
                updateCard("= $result", listOf())
            } catch (e: Throwable) {
                removeCard()
            }
        }
    }
}