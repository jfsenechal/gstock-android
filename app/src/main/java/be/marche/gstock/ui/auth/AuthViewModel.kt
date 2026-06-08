package be.marche.gstock.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.marche.gstock.data.auth.AuthState
import be.marche.gstock.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Root-level auth state holder: restores the persisted session and exposes login/logout. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> =
        repository.authState.stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    init {
        viewModelScope.launch { repository.restoreSession() }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}
