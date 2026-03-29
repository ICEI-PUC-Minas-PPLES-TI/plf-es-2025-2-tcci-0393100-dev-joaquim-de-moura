package br.com.seunome.mobulite.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:3000/"

    @Volatile
    private var token: String? = null

    private lateinit var retrofit: Retrofit

    lateinit var api: ApiService
    lateinit var authApi: AuthApi

    val driverApi: DriverApi by lazy {
        retrofit.create(DriverApi::class.java)
    }

    fun init(context: Context) {

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { token })
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
}