package be.marche.gstock.data.repository

import be.marche.gstock.core.ApiResult
import be.marche.gstock.data.local.dao.ToolDao
import be.marche.gstock.data.local.entity.ToolEntity
import be.marche.gstock.data.mapper.toEntity
import be.marche.gstock.data.remote.GstockApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRepository @Inject constructor(
    private val api: GstockApi,
    private val dao: ToolDao,
) {
    fun observeTools(): Flow<List<ToolEntity>> = dao.observeAll()

    suspend fun refresh(search: String? = null, available: Boolean? = null): ApiResult<Int> = safeApiCall {
        val response = api.getTools(search = search?.ifBlank { null }, available = available)
        val entities = response.data.map { it.toEntity() }
        dao.replaceAll(entities)
        entities.size
    }
}
