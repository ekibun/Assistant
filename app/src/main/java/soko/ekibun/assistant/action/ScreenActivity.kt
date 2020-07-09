package soko.ekibun.assistant.action

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.content_ocr.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import soko.ekibun.assistant.ActionAdapter
import soko.ekibun.assistant.CardAdapter
import soko.ekibun.assistant.R
import soko.ekibun.assistant.card.translate.TranslateCard
import soko.ekibun.assistant.util.AppUtil
import soko.ekibun.assistant.util.CoroutineUtil
import soko.ekibun.assistant.util.ImageUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import kotlin.math.roundToInt

class ScreenActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    val share by lazy { intent.getBooleanExtra(EXTRA_SHARE, false) }

    val actionAdapter by lazy {
        ActionAdapter(mutableListOf(
            AssistAction.build("复制") {
                AppUtil.copy(it, selectString)
                false
            },
            AssistAction.build("分享") {
                AppUtil.share(it, selectString)
                false
            }
        )).also {
            it.setOnItemClickListener { _, _, position ->
                it.data[position].run(this)
            }
        }
    }

    private val cardAdapter = CardAdapter(null) { action ->
        if (action.run(this)) finish()
    }

    private val assistCards by lazy {
        listOf(
            TranslateCard(actionAdapter, cardAdapter, this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_ocr)
        val screenshotFile = intent.getStringExtra(EXTRA_SCREENSHOT)!!
        val screenshot = BitmapFactory.decodeFile(screenshotFile)
        item_image.setImageBitmap(screenshot)
        list_action.adapter = actionAdapter
        list_action.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        list_card.adapter = cardAdapter
        list_card.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)

        window.attributes.dimAmount = 0.6f
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        if (share) {
            val contentUri =
                FileProvider.getUriForFile(this, "soko.ekibun.assistant.fileprovider", File(screenshotFile))
            if (contentUri != null) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
                shareIntent.setDataAndType(contentUri, "image/*")
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                startActivity(Intent.createChooser(shareIntent, "分享图片"))
            }
        } else {
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val appid = sp.getString("ocr_app_id", null) ?: ""
            val key = sp.getString("ocr_secret_key", null) ?: ""
            val language_type = sp.getString("ocr_language", null) ?: "CHN_ENG"
            val detect_language = sp.getBoolean("ocr_detect_language", true)
            if (appid.isEmpty() || key.isEmpty()) return finish()
            val ocrApi = OcrApi.createInstance()

            CoroutineUtil.subscribe({
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                finish()
            }, key = "screen_ocr") {
                val rsp = withContext(Dispatchers.IO) {
                    val token = ocrApi.oauthToken(appid, key).execute().body()!!.access_token
                    val outStream = ByteArrayOutputStream()
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                    ocrApi.ocrImage(
                        token,
                        Base64.encodeToString(outStream.toByteArray(), Base64.DEFAULT),
                        language_type,
                        detect_language
                    ).execute()
                }.body()!!
                val scaleRatio = item_container.height.toFloat() / screenshot.height
                rsp.words_result.forEach { result ->
                    val view = TextView(this@ScreenActivity)
                    view.setBackgroundColor(0x33555555)
                    item_container.addView(
                        view, (result.location.width * scaleRatio).roundToInt(),
                        (result.location.height * scaleRatio).roundToInt()
                    )
                    view.translationX = result.location.left * scaleRatio
                    view.translationY = result.location.top * scaleRatio
                    view.setOnClickListener {
                        it.isSelected = !it.isSelected
                        updateWord(result, it.isSelected)
                        view.setBackgroundColor(if (it.isSelected) 0x555677fc else 0x33555555)
                    }
                }
            }
            item_container.setOnClickListener {
                cardAdapter.setNewInstance(null)
            }
        }
    }

    private val selectResult = ArrayList<OcrApi.OcrResult.WordResult>()
    private val selectString get() = selectResult.joinToString(" ") { it.words }
    private fun updateWord(result: OcrApi.OcrResult.WordResult, select: Boolean) {
        if (select) selectResult.add(result)
        else selectResult.remove(result)

        assistCards.forEach {
            it.processTextChange(selectString)
        }

        if (selectResult.isEmpty()) {
            item_info.visibility = View.INVISIBLE
            return
        }
        item_info.visibility = View.VISIBLE
        item_message.text = selectString
    }

    var flag = false
    override fun onPause() {
        super.onPause()
        flag = true
    }

    override fun onResume() {
        super.onResume()
        if (share && flag) finish()
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    companion object {
        const val EXTRA_SCREENSHOT = "extraScreenShot"
        const val EXTRA_SHARE = "extraShare"
        fun startActivityWithScreenShot(context: Context, screenshot: Bitmap, share: Boolean = false) {
            context.startActivity(
                Intent(context, ScreenActivity::class.java)
                    .putExtra(EXTRA_SCREENSHOT, ImageUtil.saveToCache(context, screenshot).absolutePath)
                    .putExtra(EXTRA_SHARE, share)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
