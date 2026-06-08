package be.marche.gstock.data.repository

import be.marche.gstock.core.ApiResult
import be.marche.gstock.data.local.dao.CheckoutDao
import be.marche.gstock.data.local.entity.CheckoutEntity
import be.marche.gstock.data.mapper.toEntity
import be.marche.gstock.data.remote.GstockApi
import be.marche.gstock.data.remote.dto.ActionResponse
import be.marche.gstock.data.remote.dto.CheckoutRequest
import be.marche.gstock.data.remote.dto.ReturnRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckoutRepository @Inject constructor(
    private val api: GstockApi,
    private val dao: CheckoutDao,
) {
    fun observeCheckouts(): Flow<List<CheckoutEntity>> = dao.observeAll()

    suspend fun refresh(
        active: Boolean? = null,
        returned: Boolean? = null,
        overdue: Boolean? = null,
    ): ApiResult<Int> = safeApiCall {
        val response = api.getCheckouts(active = active, returned = returned, overdue = overdue)
        val entities = response.data.map { it.toEntity() }
        dao.replaceAll(entities)
        entities.size
    }

    /** Check a tool out to a worker, then refresh the cached list. */
    suspend fun checkout(toolId: Long, workerId: Long): ApiResult<ActionResponse> {
        val result = safeApiCall { api.checkout(CheckoutRequest(toolId = toolId, workerId = workerId)) }
        if (result is ApiResult.Success) refresh()
        return result
    }

    /**
     * Check several tools out to the same worker. The API exposes a single-tool endpoint,
     * so this issues one request per tool and stops at the first failure, returning how many
     * tools were checked out before that point. The cached list is refreshed once at the end.
     */
    suspend fun checkout(toolIds: List<Long>, workerId: Long): ApiResult<Int> {
        var done = 0
        for (toolId in toolIds) {
            when (val result = safeApiCall { api.checkout(CheckoutRequest(toolId = toolId, workerId = workerId)) }) {
                is ApiResult.Success -> done++
                is ApiResult.Error -> {
                    refresh()
                    return ApiResult.Error(result.message)
                }
            }
        }
        refresh()
        return ApiResult.Success(done)
    }

    /** Return a checked-out tool, then refresh the cached list. */
    suspend fun returnTool(checkoutId: Long): ApiResult<ActionResponse> {
        val result = safeApiCall { api.returnTool(ReturnRequest(checkoutId = checkoutId)) }
        if (result is ApiResult.Success) refresh()
        return result
    }
}
