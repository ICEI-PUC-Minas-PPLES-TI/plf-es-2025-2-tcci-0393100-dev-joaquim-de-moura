package br.com.seunome.mobulite.data.remote

import android.content.Context
import br.com.seunome.mobulite.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val BASE_URL: String =
        BuildConfig.MOBU_API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }

    @Volatile
    private var token: String? = null

    private lateinit var retrofit: Retrofit

    lateinit var api: ApiService
    lateinit var authApi: AuthApi

    val driverApi: DriverApi by lazy {
        retrofit.create(DriverApi::class.java)
    }

    fun init(context: Context) {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { token })
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ApiService::class.java)
        authApi = retrofit.create(AuthApi::class.java)
    }

    fun setToken(newToken: String?) {
        token = newToken
    }

    fun websocketUrl(): String = BASE_URL.replace("http://", "ws://").replace("https://", "wss://").trimEnd('/') + "/ws"

    fun photoUrl(relativePath: String?): String? {
        if (relativePath.isNullOrBlank()) return null
        return BASE_URL.trimEnd('/') + relativePath
    }
}
