package pixel.cando.di

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import org.json.JSONObject
import pixel.cando.BuildConfig
import pixel.cando.R
import pixel.cando.data.local.AccessTokenProvider
import pixel.cando.data.remote.AuthApi
import pixel.cando.data.remote.NotAuthorizedException
import pixel.cando.data.remote.RestApi
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.logError
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

fun assembleRestApi(
    resourceProvider: ResourceProvider,
    accessTokenProvider: AccessTokenProvider,
    moshi: Moshi,
): RestApi {

    return Retrofit.Builder()
        .client(
            assembleOkHttpClient(
                NotAuthorizedHandlerInterceptor(),
                ContentTypeHeaderInterceptor(),
                BodyInterceptor(
                    accessTokenProvider = accessTokenProvider
                )
            )
        )
        .baseUrl(resourceProvider.getString(R.string.base_url))
        .addConverterFactory(
            MoshiConverterFactory.create(moshi)
        )
        .build()
        .create(RestApi::class.java)
}

fun assembleAuthApi(
    resourceProvider: ResourceProvider,
    moshi: Moshi,
): AuthApi {
    return Retrofit.Builder()
        .client(
            assembleOkHttpClient(
                ContentTypeHeaderInterceptor(),
                BodyInterceptor()
            )
        )
        .baseUrl(resourceProvider.getString(R.string.base_url))
        .addConverterFactory(
            MoshiConverterFactory.create(moshi)
        )
        .build()
        .create(AuthApi::class.java)
}

private fun assembleOkHttpClient(
    vararg interceptors: Interceptor
): OkHttpClient {
    val timeout = 60L
    return OkHttpClient.Builder()
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
        .apply {
            interceptors.forEach {
                addInterceptor(it)
            }
        }
        .build()
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

private class BodyInterceptor(
    private val accessTokenProvider: AccessTokenProvider? = null
) : Interceptor {

    override fun intercept(
        chain: Interceptor.Chain
    ): Response {
        val request = chain.request()
        val requestBody = request.body
        if (requestBody?.contentType()?.subtype == "json") {
            val newBody = processJsonRequestBody(
                requestBody = requestBody,
                accessToken = accessTokenProvider?.accessToken
            )
            if (newBody != null) {
                val newRequest = request.newBuilder()
                    .post(newBody)
                    .build()
                return chain.proceed(newRequest)
            }
        }
        return chain.proceed(request)
    }

    private fun processJsonRequestBody(
        requestBody: RequestBody,
        accessToken: String?
    ): RequestBody? {
        try {
            if (requestBody.contentLength() > 0) {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                val jsonBodyAsString = buffer.readUtf8()
                val obj = JSONObject(jsonBodyAsString)
                obj.put("accessToken", accessToken)
                obj.put("meta", getMeta())
                return obj.toString().toRequestBody(
                    requestBody.contentType()
                )
            }
        } catch (t: Throwable) {
            logError(t)
        }
        return null
    }

    private fun getMeta(): JSONObject {
        val locale = Locale.getDefault().let {
            "${it.language}-${it.country}"
        }
        val timeZone = TimeZone.getDefault()
            .getDisplayName(
                true,
                TimeZone.SHORT,
                Locale.ENGLISH
            )
            .split("GMT")
            .lastOrNull()
        return JSONObject().apply {
            put("locale", locale)
            put("timezone", timeZone)
            put("platform", "android")
        }
    }

}

private class ContentTypeHeaderInterceptor : Interceptor {

    private val contentTypeHeader = "Content-Type"

    override fun intercept(
        chain: Interceptor.Chain
    ): Response {

        val request = chain.request()

        return if (request.body != null
            && request.method == "POST"
            && request.header(contentTypeHeader) == null
        ) {
            val newRequest = request.newBuilder()
                .addHeader(contentTypeHeader, "application/json; charset=UTF-8")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }
    }
}