package soko.ekibun.assistant.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import soko.ekibun.assistant.R

object AppUtil {

    fun copy(context: Context, result: String) {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("Assist", result))
        Toast.makeText(context, String.format(context.getString(R.string.toast_copied), result), Toast.LENGTH_SHORT).show()
    }

    fun share(context: Context, result: String) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, result)
        intent.type = "text/plain"
        context.startActivity(Intent.createChooser(intent, "分享")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun open(context: Context, result: String, pkg: String? = null) {
        try {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(result)
            if(!pkg.isNullOrEmpty()) intent.setPackage(pkg)
            context.startActivity(Intent.createChooser(intent, "打开").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }

    }
}