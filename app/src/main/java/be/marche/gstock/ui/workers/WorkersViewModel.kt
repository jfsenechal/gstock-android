package be.marche.gstock.ui.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.marche.gstock.core.ApiResult
import be.marche.gstock.data.local.entity.WorkerEntity
import be.marche.gstock.data.repository.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkersUiState(
    val workers: List<WorkerEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = "",
)

@HiltViewModel
class WorkersViewModel @Inject constructor(
    private val repository: WorkerRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val status = MutableStateFlow(StatusState())

    private data class StatusState(val isLoading: Boolean = false, val error: String? = null)

    val uiState: StateFlow<WorkersUiState> =
        combine(repository.observeWorkers(), query, status) { workers, q, st ->
            val filtered = if (q.isBlank()) workers
            else workers.filter { it.fullName.contains(q, ignoreCase = true) }
            WorkersUiState(
                workers = filtered,
                isLoading = st.isLoading,
                error = st.error,
                query = q,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkersUiState(isLoading = true))

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            status.update { it.copy(isLoading = true, error = null) }
            val result = repository.refresh(search = query.value)
            status.update {
                when (result) {
                    is ApiResult.Success -> it.copy(isLoading = false, error = null)
                    is ApiResult.Error -> it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}
