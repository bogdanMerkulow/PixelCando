package pixel.cando.di

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import pixel.cando.BuildConfig
import pixel.cando.R
import pixel.cando.data.remote.NotAuthorizedException
import pixel.cando.data.remote.RestApi
import pixel.cando.utils.ResourceProvider
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

fun assembleWebApi(
    resourceProvider: ResourceProvider
): RestApi {
    val timeout = 60L
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(
            timeout,
            TimeUnit.SECONDS
        )
        .readTimeout(
            timeout,
            TimeUnit.SECONDS
        )
        .writeTimeout(
            timeout,
            TimeUnit.SECONDS
        )
        .addNetworkInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            }
        )
        .addInterceptor(NotAuthorizedHandlerInterceptor())
        .build()

    return Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(resourceProvider.getString(R.string.base_url))
        .addConverterFactory(
            MoshiConverterFactory.create(
                Moshi.Builder()
                    .build()
            )
        )
        .build()
        .create(RestApi::class.java)
}

private class NotAuthorizedHandlerInterceptor : Interceptor {

    override fun intercept(
        chain: Interceptor.Chain
    ): Response {
        val response = chain.proceed(
            chain.request()
        )
        if (response.code == 401) {
            throw NotAuthorizedException()
        }
        return response
    }
}