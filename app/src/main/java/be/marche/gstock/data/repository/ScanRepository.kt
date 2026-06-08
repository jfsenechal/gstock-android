package be.marche.gstock.data.repository

import be.marche.gstock.core.ApiResult
import be.marche.gstock.data.remote.GstockApi
import be.marche.gstock.data.remote.dto.ScanRequest
import be.marche.gstock.data.remote.dto.ScanResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val api: GstockApi,
) {
    suspend fun scanWorker(qrData: String): ApiResult<ScanResponse> =
        safeApiCall { api.scanWorker(ScanRequest(qrData)) }
            .ensureSuccessFlag()

    suspend fun scanTool(qrData: String): ApiResult<ScanResponse> =
        safeApiCall { api.scanTool(ScanRequest(qrData)) }
            .ensureSuccessFlag()

    /** The API returns HTTP 200 with `success:false` for invalid QR codes; surface those as errors. */
    private fun ApiResult<ScanResponse>.ensureSuccessFlag(): ApiResult<ScanResponse> = when (this) {
        is ApiResult.Success ->
            if (data.success) this
            else ApiResult.Error(data.message ?: "Invalid QR code")
        is ApiResult.Error -> this
    }
}
