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

/** A selectable worker or tool, derived from the loaded checkouts. */
data class FilterOption(val id: Long, val name: String)

data class CheckoutsUiState(
    val checkouts: List<CheckoutEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val filter: CheckoutFilter = CheckoutFilter.ALL,
    val workerOptions: List<FilterOption> = emptyList(),
    val toolOptions: List<FilterOption> = emptyList(),
    val workerFilter: Long? = null,
    val toolFilter: Long? = null,
    val returningId: Long? = null,
)

@HiltViewModel
class CheckoutsViewModel @Inject constructor(
    private val repository: CheckoutRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val filter = MutableStateFlow(CheckoutFilter.ALL)
    private val workerFilter = MutableStateFlow<Long?>(null)
    private val toolFilter = MutableStateFlow<Long?>(null)
    private val status = MutableStateFlow(StatusState())

    private data class StatusState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val message: String? = null,
        val returningId: Long? = null,
    )

    private data class Filters(
        val status: CheckoutFilter,
        val workerId: Long?,
        val toolId: Long?,
    )

    val uiState: StateFlow<CheckoutsUiState> =
        combine(
            repository.observeCheckouts(),
            combine(filter, workerFilter, toolFilter, ::Filters),
            status,
        ) { checkouts, filters, st ->
            val workerOptions = checkouts
                .mapNotNull { c -> c.workerId?.let { FilterOption(it, c.workerName ?: it.toString()) } }
                .distinctBy { it.id }
                .sortedBy { it.name.lowercase() }
            val toolOptions = checkouts
                .mapNotNull { c -> c.toolId?.let { FilterOption(it, c.toolName ?: it.toString()) } }
                .distinctBy { it.id }
                .sortedBy { it.name.lowercase() }

            val filtered = checkouts
                .filter { c ->
                    when (filters.status) {
                        CheckoutFilter.ALL -> true
                        CheckoutFilter.ACTIVE -> c.isActive && !c.isReturned
                        CheckoutFilter.RETURNED -> c.isReturned
                        CheckoutFilter.OVERDUE -> c.isOverdue
                    }
                }
                .filter { c -> filters.workerId == null || c.workerId == filters.workerId }
                .filter { c -> filters.toolId == null || c.toolId == filters.toolId }

            CheckoutsUiState(
                checkouts = filtered,
                isLoading = st.isLoading,
                error = st.error,
                message = st.message,
                filter = filters.status,
                workerOptions = workerOptions,
                toolOptions = toolOptions,
                workerFilter = filters.workerId,
                toolFilter = filters.toolId,
                returningId = st.returningId,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckoutsUiState(isLoading = true))

    init {
        refresh()
    }

    fun onFilterChange(value: CheckoutFilter) {
        filter.value = value
    }

    fun onWorkerFilterChange(workerId: Long?) {
        workerFilter.value = workerId
    }

    fun onToolFilterChange(toolId: Long?) {
        toolFilter.value = toolId
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
