package be.marche.gstock.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The current authentication state, observed by the UI to gate login vs. the main app. */
sealed interface AuthState {
    /** Still restoring a persisted session at startup. */
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val username: String, val name: String?) : AuthState
}

/**
 * Holds the bearer token in memory so [be.marche.gstock.data.remote.AuthInterceptor] can read it
 * synchronously on every request (no DB I/O on the network thread). Persistence lives in Room via
 * [be.marche.gstock.data.repository.AuthRepository], which keeps this in sync.
 */
@Singleton
class SessionManager @Inject constructor() {

    @Volatile
    var token: String? = null
        private set

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun setSession(token: String, username: String, name: String?) {
        this.token = token
        _state.value = AuthState.Authenticated(username, name)
    }

    /** No persisted session found at startup. */
    fun markUnauthenticated() {
        token = null
        _state.value = AuthState.Unauthenticated
    }

    /** Clear the in-memory session (explicit logout, or a 401 from the server). */
    fun clear() {
        token = null
        _state.value = AuthState.Unauthenticated
    }
}
