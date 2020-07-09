package soko.ekibun.assistant.action

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

interface OcrApi {

    @POST("/oauth/2.0/token")
    @FormUrlEncoded
    fun oauthToken(
        @Field("client_id") client_id: String,
        @Field("client_secret") client_secret: String,
        @Field("grant_type") grant_type: String = "client_credentials"
    ): Call<OauthToken>

    @POST("/rest/2.0/ocr/v1/general")
    @FormUrlEncoded
    fun ocrImage(
        @Query("access_token") access_token: String,
        @Field("image") image: String,
        @Field("language_type") language_type: String,
        @Field("detect_language") detect_language: Boolean
    ): Call<OcrResult>

    companion object {
        const val SERVER_API = "https://aip.baidubce.com"
        /**
         * 创建retrofit实例
         */
        fun createInstance(): OcrApi {
            return Retrofit.Builder().baseUrl(SERVER_API)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(OcrApi::class.java)
        }
    }

    data class OauthToken(
        val access_token: String,
        val expires_in: Int
    )

    data class OcrResult(
        val words_result: List<WordResult>
    ) {
        data class WordResult(
            val location: Location,
            val words: String
        ){
            data class Location(
                val width: Int,
                val top: Int,
                val left: Int,
                val height: Int
            )
        }
    }
}