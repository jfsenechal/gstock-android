package be.marche.gstock.data.repository

import be.marche.gstock.core.ApiResult
import be.marche.gstock.data.auth.AuthState
import be.marche.gstock.data.auth.SessionManager
import be.marche.gstock.data.local.dao.AuthDao
import be.marche.gstock.data.local.entity.AuthEntity
import be.marche.gstock.data.remote.GstockApi
import be.marche.gstock.data.remote.dto.LoginRequest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: GstockApi,
    private val authDao: AuthDao,
    private val session: SessionManager,
    private val json: Json,
) {
    val authState: StateFlow<AuthState> = session.state

    /** Load any persisted token into memory at startup, resolving [AuthState.Loading]. */
    suspend fun restoreSession() {
        val saved = runCatching { authDao.get() }.getOrNull()
        if (saved != null) {
            session.setSession(saved.token, saved.username, saved.name)
        } else {
            session.markUnauthenticated()
        }
    }

    suspend fun login(username: String, password: String, deviceName: String): ApiResult<Unit> = try {
        val response = api.login(LoginRequest(username, password, deviceName))
        val user = response.user
        val entity = AuthEntity(
            token = response.token,
            userId = user.id,
            username = user.username ?: username,
            name = user.name ?: listOfNotNull(user.firstName, user.lastName)
                .joinToString(" ").ifBlank { null },
            email = user.email,
        )
        authDao.upsert(entity)
        session.setSession(entity.token, entity.username, entity.name)
        ApiResult.Success(Unit)
    } catch (e: HttpException) {
        ApiResult.Error(loginErrorMessage(e))
    } catch (e: IOException) {
        ApiResult.Error("Network error — check your connection")
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Unexpected error")
    }

    suspend fun logout() {
        runCatching { authDao.clear() }
        session.clear()
    }

    /** Turn a Laravel error body into a readable message, falling back to a friendly default. */
    private fun loginErrorMessage(e: HttpException): String {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val parsed = body?.let {
            runCatching { json.parseToJsonElement(it).jsonObject["message"]?.jsonPrimitive?.content }
                .getOrNull()
        }
        return parsed ?: when (e.code()) {
            401, 422 -> "Invalid username or password"
            else -> "Login failed (HTTP ${e.code()})"
        }
    }
}
