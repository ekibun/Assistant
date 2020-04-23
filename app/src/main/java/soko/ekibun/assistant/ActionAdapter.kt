package soko.ekibun.assistant

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_action.view.*
import soko.ekibun.assistant.action.AssistAction

/**
 * Action Adapter
 * @constructor
 */
class ActionAdapter(data: MutableList<AssistAction>? = null) :
        BaseQuickAdapter<AssistAction, BaseViewHolder>(R.layout.item_action, data) {

    override fun convert(holder: BaseViewHolder, item: AssistAction) {
        holder.itemView.action_name.text = item.actionName
        holder.itemView.action_icon.setImageDrawable(item.icon)
    }
}