package onlymash.flexbooru.ap.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import onlymash.flexbooru.ap.common.USER_AGENT_KEY
import onlymash.flexbooru.ap.data.model.*
import onlymash.flexbooru.ap.extension.getUserAgent
import onlymash.flexbooru.ap.util.Logger
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface Api {

    companion object {
        operator fun invoke(): Api {
            val logger = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    Logger.d("Api", message)
                }
            }).apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val interceptor = Interceptor { chain ->
                val builder =  chain.request().newBuilder()
                    .removeHeader(USER_AGENT_KEY)
                    .addHeader(USER_AGENT_KEY, getUserAgent())
                chain.proceed(builder.build())
            }
            val client = OkHttpClient.Builder().apply {
                connectTimeout(10, TimeUnit.SECONDS)
                readTimeout(10, TimeUnit.SECONDS)
                writeTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(interceptor)
                    .addInterceptor(logger)
            }
                .build()
            val contentType = "application/json".toMediaType()
            return Retrofit.Builder()
                .baseUrl("https://fiepi.me")
                .client(client)
                .addConverterFactory(Json(JsonConfiguration.Stable.copy(
                    ignoreUnknownKeys = true))
                    .asConverterFactory(contentType))
                .build()
                .create(Api::class.java)
        }
    }

    @GET
    suspend fun getPosts(@Url url: HttpUrl): Response<PostResponse>

    @GET
    suspend fun getDetail(@Url url: HttpUrl): Response<Detail>

    @GET
    fun getDetailNoSuspend(@Url url: HttpUrl): Call<Detail>

    @POST
    @FormUrlEncoded
    suspend fun login(@Url url: HttpUrl,
              @Field("login") username: String,
              @Field("password") password: String,
              @Field("time_zone") timeZone: String): Response<User>

    @GET
    suspend fun logout(@Url url: HttpUrl): Response<ResponseBody>

    @POST
    @FormUrlEncoded
    suspend fun vote(@Url url: HttpUrl,
             @Field("post") postId: Int,
             @Field("vote") vote: Int = 9, // 9: vote 0: remove vote
             @Field("token") token: String): Response<VoteResponse>

    @POST
    @FormUrlEncoded
    suspend fun getSuggestion(@Url url: HttpUrl,
                              @Field("tag") tag: String,
                              @Field("token") token: String): Response<Suggestion>

    @GET
    suspend fun getComments(@Url url: HttpUrl): Response<CommentResponse>

    @POST
    @FormUrlEncoded
    suspend fun createComment(@Url url: HttpUrl,
                              @Field("text") text: String,
                              @Field("token") token: String): Response<CreateCommentResponse>


    @GET
    suspend fun getAllComments(@Url url: HttpUrl): Response<CommentAllResponse>
}