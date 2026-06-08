package be.marche.gstock.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/** Adds the static bearer token (from BuildConfig) and JSON accept header to every request. */
class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .apply {
                if (token.isNotBlank()) header("Authorization", "Bearer $token")
            }
            .build()
        return chain.proceed(request)
    }
}
