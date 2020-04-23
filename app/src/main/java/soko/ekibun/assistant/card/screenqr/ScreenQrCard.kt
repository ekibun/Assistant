package soko.ekibun.assistant.card.screenqr

import android.graphics.Bitmap
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import soko.ekibun.assistant.CardAdapter
import soko.ekibun.assistant.R
import soko.ekibun.assistant.action.AssistAction
import soko.ekibun.assistant.card.AssistCard
import soko.ekibun.assistant.util.AppUtil
import soko.ekibun.assistant.util.ImageUtil
import java.util.*


class ScreenQrCard(cardAdapter: CardAdapter) : AssistCard(cardAdapter) {
    override val title: String = "二维码"
    override val icon: Int = R.drawable.qrcode

    private fun decode(bitmap: Bitmap): Result? {
        var result: Result? = null
        try {
            val data = ImageUtil.getYUV420sp(bitmap)
            val source = PlanarYUVLuminanceSource(data, bitmap.width, bitmap.height, 0, 0, bitmap.width, bitmap.height, false)
            val hints = Hashtable<DecodeHintType, Any>()
            hints[DecodeHintType.TRY_HARDER] = java.lang.Boolean.TRUE
            hints[DecodeHintType.POSSIBLE_FORMATS] = BarcodeFormat.QR_CODE
            val bitmap1 = BinaryBitmap(HybridBinarizer(source))
            result = QRCodeReader().decode(bitmap1, hints)
        } catch (e: Exception) { }
        return result
    }

    private val decodeSubject = PublishSubject.create<Bitmap>().also { subject ->
        subject.subscribeOn(Schedulers.io())
            .map {bitmap ->
                decode(bitmap)?.text?:""
            }.filter { it.isNotEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{ result ->
                updateCard(result, listOf(
                    AssistAction.build("打开") { context ->
                        AppUtil.open(context, result)
                        true
                    },
                    AssistAction.build("复制") { context ->
                        AppUtil.copy(context, result)
                        false
                    }))
            }
    }

    override fun processScreenshot(bitmap: Bitmap) {
        decodeSubject.onNext(bitmap)
    }
}