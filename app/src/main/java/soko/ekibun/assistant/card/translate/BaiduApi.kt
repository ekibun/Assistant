package soko.ekibun.assistant.card.translate

import android.util.Log
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.security.MessageDigest


interface BaiduApi {

    @POST("/api/trans/vip/translate")
    @FormUrlEncoded
    fun translate(
        @Field("q") q: String?,
        @Field("from") from: String?,
        @Field("to") to: String?,
        @Field("appid") appid: String?,
        @Field("salt") salt: String?,
        @Field("sign") sign: String?
    ): Call<RespondBean>

    companion object {
        private const val SERVER_API = "https://fanyi-api.baidu.com"
        /**
         * 创建retrofit实例
         */
        fun createInstance(): BaiduApi{
            return Retrofit.Builder().baseUrl(SERVER_API)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(BaiduApi::class.java)
        }

        private fun getMD5Code(info: String): String {
            return try {
                val md5: MessageDigest = MessageDigest.getInstance("MD5")
                md5.update(info.toByteArray(charset("utf-8"))) //设置编码格式
                val encryption: ByteArray = md5.digest()
                val stringBuffer = StringBuffer()
                for (i in encryption.indices) {
                    if (Integer.toHexString(0xff and encryption[i].toInt()).length == 1) {
                        stringBuffer.append("0").append(Integer.toHexString(0xff and encryption[i].toInt()))
                    } else {
                        stringBuffer.append(Integer.toHexString(0xff and encryption[i].toInt()))
                    }
                }
                stringBuffer.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                "err"
            }
        }

        fun translate(text: String, appid: String, key: String): Call<RespondBean>{
            val salt = (Math.random() * 100 + 1).toInt().toString() //随机数 这里范围是[0,100]整数 无强制要求
            val sign = getMD5Code(appid + text + salt + key)
            Log.v("sign", sign)
            return createInstance().translate(text,
                from = "auto",
                to = if(Regex("""[\u4e00-\u9fa5]+""").matches(text)) "en" else "zh",
                salt = salt,
                appid = appid,
                sign = sign)
        }
    }

    class RespondBean(
        var from: String? = null,
        var to: String? = null,
        var trans_result: List<TransResultBean>? = null
    ){
        data class TransResultBean(
            var src: String? = null,
            var dst: String? = null
        )
    }
}