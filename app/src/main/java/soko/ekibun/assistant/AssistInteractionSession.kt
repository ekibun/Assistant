package soko.ekibun.assistant

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.core.view.GestureDetectorCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.content_assistant.view.*
import soko.ekibun.assistant.action.AssistAction
import soko.ekibun.assistant.action.screenocr.ScreenOcrAction
import soko.ekibun.assistant.action.sharescreen.ShareScreenAction
import soko.ekibun.assistant.card.applaucher.AppLauncherCard
import soko.ekibun.assistant.card.calculator.CalculatorCard
import soko.ekibun.assistant.card.screenqr.ScreenQrCard
import soko.ekibun.assistant.card.translate.TranslateCard
import soko.ekibun.assistant.util.AppUtil


class AssistInteractionSession constructor(context: Context) : VoiceInteractionSession(context) {

    private val sp by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    private fun handleAction(action: AssistAction) {
        if(action.run(context)) hide()
    }

    private val actionAdapter = ActionAdapter().also {
        it.setOnItemClickListener { _, _, position -> handleAction(it.data[position]) }
    }

    private val cardAdapter = CardAdapter(null, ::handleAction)

    private val assistCards = listOf(
        ScreenQrCard(cardAdapter),
        TranslateCard(actionAdapter, cardAdapter, context),
        CalculatorCard(cardAdapter),
        AppLauncherCard(context, cardAdapter)
    )

    lateinit var mContentView: View
    override fun onCreateContentView(): View {
        context.setTheme(R.style.AppTheme_Dialog)
        mContentView = layoutInflater.inflate(R.layout.content_assistant, null)
        val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                hide()
                return super.onSingleTapConfirmed(e)
            }
        })
        mContentView.list_card.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
        mContentView.list_action.adapter = actionAdapter
        mContentView.list_action.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        mContentView.list_card.adapter = cardAdapter
        mContentView.list_card.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)

        mContentView.setOnApplyWindowInsetsListener { _, insets ->
            mContentView.item_content.setPadding(0, 0, 0, 0 + insets.systemWindowInsetBottom)
            insets.consumeSystemWindowInsets()
        }

        mContentView.input_message.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                assistCards.forEach {
                    it.processTextChange(s?.toString()?:"")
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        mContentView.input_message.setOnEditorActionListener { v, actionId, event ->
            if (actionId== EditorInfo.IME_ACTION_SEND || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                AppUtil.open(context, (sp.getString("search_template", null)
                    ?:"https://m.baidu.com/s?word=%s").replace("%s", v.text.toString()),
                    sp.getString("search_package", null))
                hide()
                true
            }else false
        }

        window.window?.attributes?.dimAmount = 0.6f
        window.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        return mContentView
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.v("onShow", showFlags.toString())
        actionAdapter.setNewInstance(null)
        cardAdapter.setNewInstance(null)
        mContentView.input_message.setText("")
        mContentView.input_message.requestFocus()
        window.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        window.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED)
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        Log.v("assist", "screen $screenshot")
        if(screenshot == null) return
        actionAdapter.addData(ShareScreenAction(screenshot))
        assistCards.forEach {
            it.processScreenshot(screenshot)
        }
        // ocr
        val appid = sp.getString("ocr_app_id", null)?:""
        val key = sp.getString("ocr_secret_key", null)?:""
        if(appid.isEmpty() || key.isEmpty()) return
        actionAdapter.addData(ScreenOcrAction(screenshot))
    }
}