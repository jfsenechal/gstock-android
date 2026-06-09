package be.marche.gstock.ui.checkouts

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.marche.gstock.R
import be.marche.gstock.core.ApiResult
import be.marche.gstock.data.local.entity.CheckoutEntity
import be.marche.gstock.data.repository.CheckoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CheckoutFilter(@param:StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    ACTIVE(R.string.filter_active),
    RETURNED(R.string.filter_returned),
    OVERDUE(R.string.filter_overdue),
}

data class CheckoutsUiState(
    val checkouts: List<CheckoutEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val filter: CheckoutFilter = CheckoutFilter.ALL,
    val returningId: Long? = null,
)

@HiltViewModel
class CheckoutsViewModel @Inject constructor(
    private val repository: CheckoutRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val filter = MutableStateFlow(CheckoutFilter.ALL)
    private val status = MutableStateFlow(StatusState())

    private data class StatusState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val message: String? = null,
        val returningId: Long? = null,
    )

    val uiState: StateFlow<CheckoutsUiState> =
        combine(repository.observeCheckouts(), filter, status) { checkouts, f, st ->
            val filtered = when (f) {
                CheckoutFilter.ALL -> checkouts
                CheckoutFilter.ACTIVE -> checkouts.filter { it.isActive && !it.isReturned }
                CheckoutFilter.RETURNED -> checkouts.filter { it.isReturned }
                CheckoutFilter.OVERDUE -> checkouts.filter { it.isOverdue }
            }
            CheckoutsUiState(
                checkouts = filtered,
                isLoading = st.isLoading,
                error = st.error,
                message = st.message,
                filter = f,
                returningId = st.returningId,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckoutsUiState(isLoading = true))

    init {
        refresh()
    }

    fun onFilterChange(value: CheckoutFilter) {
        filter.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            status.update { it.copy(isLoading = true, error = null) }
            val result = repository.refresh()
            status.update {
                when (result) {
                    is ApiResult.Success -> it.copy(isLoading = false, error = null)
                    is ApiResult.Error -> it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun returnTool(checkoutId: Long) {
        viewModelScope.launch {
            status.update { it.copy(returningId = checkoutId, error = null, message = null) }
            val result = repository.returnTool(checkoutId)
            status.update {
                when (result) {
                    is ApiResult.Success ->
                        it.copy(returningId = null, message = result.data.message ?: context.getString(R.string.checkouts_returned_message))
                    is ApiResult.Error ->
                        it.copy(returningId = null, error = result.message)
                }
            }
        }
    }

    fun consumeMessage() {
        status.update { it.copy(message = null, error = null) }
    }
}
