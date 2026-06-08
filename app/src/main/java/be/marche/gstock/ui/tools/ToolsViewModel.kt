package be.marche.gstock.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.marche.gstock.core.ApiResult
import be.marche.gstock.data.local.entity.ToolEntity
import be.marche.gstock.data.repository.ToolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolsUiState(
    val tools: List<ToolEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val onlyAvailable: Boolean = false,
)

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val repository: ToolRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val onlyAvailable = MutableStateFlow(false)
    private val status = MutableStateFlow(StatusState())

    private data class StatusState(val isLoading: Boolean = false, val error: String? = null)

    val uiState: StateFlow<ToolsUiState> =
        combine(repository.observeTools(), query, onlyAvailable, status) { tools, q, avail, st ->
            val filtered = tools
                .filter { q.isBlank() || it.name.contains(q, ignoreCase = true) }
                .filter { !avail || it.isAvailable }
            ToolsUiState(
                tools = filtered,
                isLoading = st.isLoading,
                error = st.error,
                query = q,
                onlyAvailable = avail,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ToolsUiState(isLoading = true))

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun toggleOnlyAvailable() {
        onlyAvailable.value = !onlyAvailable.value
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
