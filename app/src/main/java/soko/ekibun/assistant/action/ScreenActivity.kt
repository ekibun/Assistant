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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.content_ocr.*
import soko.ekibun.assistant.ActionAdapter
import soko.ekibun.assistant.R
import soko.ekibun.assistant.util.AppUtil
import soko.ekibun.assistant.util.ImageUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import kotlin.math.roundToInt

class ScreenActivity : AppCompatActivity() {

    val share by lazy { intent.getBooleanExtra(EXTRA_SHARE, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_ocr)
        val screenshotFile = intent.getStringExtra(EXTRA_SCREENSHOT)!!
        val screenshot = BitmapFactory.decodeFile(screenshotFile)
        item_image.setImageBitmap(screenshot)
        list_action.adapter = ActionAdapter(mutableListOf(
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
        list_action.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        window.attributes.dimAmount = 0.6f
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        if (share) {
            val contentUri = FileProvider.getUriForFile(this, "soko.ekibun.assistant.fileprovider", File(screenshotFile))
            if (contentUri != null) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
                shareIntent.setDataAndType(contentUri, "image/*")
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                startActivity(Intent.createChooser(shareIntent, "分享图片"))
            }
        } else {
            val sp= PreferenceManager.getDefaultSharedPreferences(this)
            val appid = sp.getString("ocr_app_id", null)?:""
            val key = sp.getString("ocr_secret_key", null)?:""
            if (appid.isEmpty() || key.isEmpty()) return finish()
            val ocrApi = OcrApi.createInstance()
            ocrApi.oauthToken(appid, key)
                .subscribeOn(Schedulers.io())
                .flatMap {
                    val outStream = ByteArrayOutputStream()
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                    ocrApi.ocrImage(it.access_token, Base64.encodeToString(outStream.toByteArray(), Base64.DEFAULT))
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val scaleRatio = item_container.height.toFloat() / screenshot.height
                    it.words_result.forEach { result ->
                        val view = TextView(this)
                        view.setBackgroundColor(0x33555555)
                        item_container.addView(view, (result.location.width * scaleRatio).roundToInt(),
                            (result.location.height * scaleRatio).roundToInt())
                        view.translationX = result.location.left * scaleRatio
                        view.translationY = result.location.top * scaleRatio
                        view.setOnClickListener {
                            it.isSelected = !it.isSelected
                            updateWord(result, it.isSelected)
                            view.setBackgroundColor(if(it.isSelected) 0x555677fc else 0x33555555)
                        }
                    }
                }, {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    finish()
                })
        }
    }

    private val selectResult = ArrayList<OcrApi.OcrResult.WordResult>()
    private val selectString get() = selectResult.joinToString(" ") { it.words }
    private fun updateWord(result: OcrApi.OcrResult.WordResult, select: Boolean) {
        if(select) selectResult.add(result)
        else selectResult.remove(result)
        if(selectResult.isEmpty()) {
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
        if(share && flag) finish()
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    companion object {
        const val EXTRA_SCREENSHOT = "extraScreenShot"
        const val EXTRA_SHARE = "extraShare"
        fun startActivityWithScreenShot(context: Context, screenshot: Bitmap, share: Boolean = false) {
            context.startActivity(Intent(context, ScreenActivity::class.java)
                .putExtra(EXTRA_SCREENSHOT, ImageUtil.saveToCache(context, screenshot).absolutePath)
                .putExtra(EXTRA_SHARE, share)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
