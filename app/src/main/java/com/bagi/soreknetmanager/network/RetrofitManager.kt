package com.bagi.soreknetmanager.network

import com.bagi.soreknetmanager.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory



class RetrofitManager private constructor(){
    companion object {
        private const val BASE_URL = "https://api.imgur.com/3/"

        val instanceServiceApi : ServiceApi by lazy {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            val okHttpClientBuilder = OkHttpClient.Builder()
            okHttpClientBuilder.addInterceptor(object : Interceptor{
                override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
                    val request: Request =
                        chain.request().newBuilder()
                            .addHeader("Authorization", "${BuildConfig.TOKEN_TYPE} ${BuildConfig.ACCESS_TOKEN}").build()
                    return chain.proceed(request)
                }
            })
                .addInterceptor(interceptor)

            val retrofit = Retrofit.Builder()
                .client(okHttpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .build()
            retrofit.create(ServiceApi::class.java)
        }
    }
}