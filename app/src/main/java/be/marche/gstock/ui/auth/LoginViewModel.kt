package be.marche.gstock.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.marche.gstock.core.ApiResult
import be.marche.gstock.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    /** On success, [AuthRepository] flips the global auth state and the app swaps to the main UI. */
    fun login(deviceName: String) {
        val state = _uiState.value
        if (state.isLoading) return
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Enter your username and password") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.login(state.username.trim(), state.password, deviceName)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}
