package soko.ekibun.assistant.card

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import soko.ekibun.assistant.CardAdapter
import soko.ekibun.assistant.action.AssistAction

abstract class AssistCard(private val cardAdapter: CardAdapter) {
    abstract val title: String
    abstract val icon: Int

    fun updateCard(message: String, actions: List<AssistAction>) {
        val data = cardAdapter.data
        data.removeAll { it.title == title }
        data.add(0, CardData(title, icon, message, actions))
        cardAdapter.setNewInstance(data.toMutableList())
    }

    fun removeCard() {
        val data = cardAdapter.data
        data.removeAll { it.title == title }
        cardAdapter.setNewInstance(data.toMutableList())
    }

    open fun processScreenshot(bitmap: Bitmap) {

    }

    open fun processTextChange(text: String) {

    }

    data class CardData(
        val title: String,
        @DrawableRes val icon: Int,
        val message: String,
        val actions: List<AssistAction>
    )
}