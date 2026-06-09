package be.marche.gstock.ui.checkout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.marche.gstock.R
import be.marche.gstock.core.ApiResult
import be.marche.gstock.core.GstockCode
import be.marche.gstock.core.GstockQr
import be.marche.gstock.data.remote.dto.ToolDto
import be.marche.gstock.data.remote.dto.WorkerDto
import be.marche.gstock.data.repository.CheckoutRepository
import be.marche.gstock.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CheckoutStep { SCAN_WORKER, SCAN_TOOLS, DONE }

data class CheckoutUiState(
    val step: CheckoutStep = CheckoutStep.SCAN_WORKER,
    val worker: WorkerDto? = null,
    /** Tools scanned so far, in scan order. */
    val tools: List<ToolDto> = emptyList(),
    val isProcessing: Boolean = false,
    val error: String? = null,
    /** Set when a scanned tool is already reserved; drives a blocking alert dialog. */
    val reservedTool: ToolDto? = null,
    val resultMessage: String? = null,
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    private val checkoutRepository: CheckoutRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    fun onWorkerScanned(qrData: String) {
        if (_uiState.value.isProcessing) return
        val code = GstockQr.parse(qrData)
        if (code !is GstockCode.Worker) {
            _uiState.update { it.copy(error = context.getString(R.string.checkout_error_not_worker)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            when (val result = scanRepository.scanWorker(qrData)) {
                is ApiResult.Success -> {
                    val worker = result.data.worker
                    if (worker == null) {
                        _uiState.update { it.copy(isProcessing = false, error = context.getString(R.string.checkout_error_worker_not_recognised)) }
                    } else {
                        _uiState.update {
                            it.copy(isProcessing = false, worker = worker, step = CheckoutStep.SCAN_TOOLS)
                        }
                    }
                }
                is ApiResult.Error ->
                    _uiState.update { it.copy(isProcessing = false, error = result.message) }
            }
        }
    }

    fun onToolScanned(qrData: String) {
        if (_uiState.value.isProcessing || _uiState.value.reservedTool != null) return
        val code = GstockQr.parse(qrData)
        if (code !is GstockCode.Tool) {
            _uiState.update { it.copy(error = context.getString(R.string.checkout_error_not_tool)) }
            return
        }
        if (_uiState.value.tools.any { it.id == code.id }) {
            _uiState.update { it.copy(error = context.getString(R.string.checkout_error_tool_in_list)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            when (val result = scanRepository.scanTool(qrData)) {
                is ApiResult.Success -> {
                    val tool = result.data.tool
                    when {
                        tool == null ->
                            _uiState.update { it.copy(isProcessing = false, error = context.getString(R.string.checkout_error_tool_not_recognised)) }
                        tool.isCheckedOut || !tool.isAvailable ->
                            // Already reserved: surface a blocking alert and do not add it.
                            _uiState.update { it.copy(isProcessing = false, reservedTool = tool) }
                        else ->
                            _uiState.update {
                                it.copy(isProcessing = false, tools = it.tools + tool)
                            }
                    }
                }
                is ApiResult.Error ->
                    _uiState.update { it.copy(isProcessing = false, error = result.message) }
            }
        }
    }

    /** Dismiss the "already reserved" alert. */
    fun dismissReservedAlert() {
        _uiState.update { it.copy(reservedTool = null) }
    }

    fun removeTool(toolId: Long) {
        _uiState.update { it.copy(tools = it.tools.filterNot { tool -> tool.id == toolId }) }
    }

    /** "Finished" — check every scanned tool out to the worker. */
    fun finish() {
        val state = _uiState.value
        val worker = state.worker ?: return
        if (state.tools.isEmpty() || state.isProcessing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            when (val result = checkoutRepository.checkout(state.tools.map { it.id }, worker.id)) {
                is ApiResult.Success -> _uiState.update {
                    val count = result.data
                    it.copy(
                        isProcessing = false,
                        step = CheckoutStep.DONE,
                        resultMessage = "$count tool${if (count > 1) "s" else ""} checked out to " +
                            "${worker.firstName} ${worker.lastName}",
                    )
                }
                is ApiResult.Error ->
                    _uiState.update { it.copy(isProcessing = false, error = result.message) }
            }
        }
    }

    fun reset() {
        _uiState.value = CheckoutUiState()
    }
}
