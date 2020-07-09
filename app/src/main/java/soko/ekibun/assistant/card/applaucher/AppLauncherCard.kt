package soko.ekibun.assistant.card.applaucher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import soko.ekibun.assistant.CardAdapter
import soko.ekibun.assistant.R
import soko.ekibun.assistant.action.AssistAction
import soko.ekibun.assistant.card.AssistCard
import soko.ekibun.assistant.util.CoroutineUtil
import java.util.*
import kotlin.collections.ArrayList


class AppLauncherCard(val context: Context, cardAdapter: CardAdapter) : AssistCard(cardAdapter) {
    override val title: String = "应用程序"
    override val icon: Int = R.drawable.ic_widgets

    data class AppInfo(
        val label: String,
        val matcher: (String) -> Boolean,
        val icon: Drawable?,
        val action: AssistAction
    )

    var appList: List<AppInfo> = listOf()

    private val pinyinFormatter = HanyuPinyinOutputFormat().also {
        it.toneType = HanyuPinyinToneType.WITHOUT_TONE
    }
    private fun matcher(str: String): (String) -> Boolean  {
        val groupIndex = arrayListOf(1)
        val pinyin = Regex(str.map { c ->
            var groupCount = 1
            val matchString = (PinyinHelper.toHanyuPinyinStringArray(c, pinyinFormatter)?.distinct()?.map {
                it.map { it.toString() }.reduceRight { s, acc ->
                    groupCount++
                    "$s($acc)?"
                }
            }?:ArrayList()).plus( if (c in "$()*+.[\\^{|") "\\$c" else c).joinToString("|").let { "($it)?" }
            groupIndex.add(groupIndex.last() + groupCount)
            matchString
        }.joinToString(""), RegexOption.IGNORE_CASE)
        return ret@{ test ->
            str.contains((pinyin.matchEntire(test)?: return@ret false).groups.filterIndexed { i, _ ->
                i in groupIndex
            }.mapIndexed { i, g -> if(g == null) null else str.getOrNull(i)
            }.filterNotNull().joinToString(""))
        }
    }

    override fun processScreenshot(bitmap: Bitmap) {
        appList = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.MATCH_ALL).map {info ->
            val label = info.loadLabel(context.packageManager).toString()
            val icon = info.loadIcon(context.packageManager)
            val matcher = matcher(label)
            AppInfo(label, matcher, info.loadIcon(context.packageManager),
                AssistAction.build(label, icon) {context ->
                    context.startActivity(Intent(Intent.ACTION_MAIN)
                        .setComponent(ComponentName(info.activityInfo.packageName, info.activityInfo.name))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    true
                })
        }
    }

    override fun processTextChange(text: String) {
        if(text.isEmpty()) removeCard()
        CoroutineUtil.subscribe {
            val txt = text.toLowerCase(Locale.ROOT)
            if(txt.isEmpty()) return@subscribe removeCard()
            val result = withContext(Dispatchers.IO) {
                appList.filter { it.matcher(txt) }.map { it.action } }
            if(result.isEmpty()) removeCard()
            else updateCard("", result)
        }
    }
}