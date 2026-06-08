package be.marche.gstock.data.repository

import be.marche.gstock.core.ApiResult
import retrofit2.HttpException
import java.io.IOException

/** Wraps a suspending API call, converting common failures into readable messages. */
suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: HttpException) {
    val code = e.code()
    val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    ApiResult.Error(body?.takeIf { it.isNotBlank() } ?: "HTTP $code error")
} catch (e: IOException) {
    ApiResult.Error("Network error — check your connection")
} catch (e: Exception) {
    ApiResult.Error(e.message ?: "Unexpected error")
}
