package be.marche.gstock.data.remote

import be.marche.gstock.data.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the JSON accept header and, when a session is active, the current bearer token (read live
 * from [SessionManager]) to every request. On a 401 it clears the session so the UI returns to the
 * login screen.
 */
class AuthInterceptor(private val session: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("Accept", "application/json")
        session.token?.takeIf { it.isNotBlank() }?.let {
            builder.header("Authorization", "Bearer $it")
        }
        val response = chain.proceed(builder.build())
        if (response.code == 401 && session.token != null) {
            session.clear()
        }
        return response
    }
}
