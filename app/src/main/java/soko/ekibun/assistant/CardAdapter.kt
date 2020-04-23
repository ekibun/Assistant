package soko.ekibun.assistant

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_card.view.*
import soko.ekibun.assistant.action.AssistAction
import soko.ekibun.assistant.card.AssistCard

/**
 * Card Adapter
 * @constructor
 */
class CardAdapter(data: MutableList<AssistCard.CardData>? = null, val runAction: (AssistAction) -> Unit) :
    BaseQuickAdapter<AssistCard.CardData, BaseViewHolder>(R.layout.item_card, data) {

    private val handler = Handler()
    fun runOnUiThread (action: () -> Unit) {
        if(Looper.myLooper() != Looper.getMainLooper()) handler.post(action)
        else action()
    }

    override fun convert(holder: BaseViewHolder, item: AssistCard.CardData) {
        holder.itemView.item_icon.setImageResource(item.icon)
        holder.itemView.item_title.text = item.title
        holder.itemView.item_message.text = item.message
        holder.itemView.item_message.visibility = if(item.message.isEmpty()) View.GONE else View.VISIBLE
        val adapter = holder.itemView.item_actions.adapter as? ActionAdapter?: ActionAdapter().also {
            it.setOnItemClickListener { _, _, position ->
                runAction(it.data[position])
            }
        }
        holder.itemView.item_actions.adapter = adapter
        holder.itemView.item_actions.layoutManager = holder.itemView.item_actions.layoutManager?:
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        adapter.setNewInstance(item.actions.toMutableList())
    }
}