package be.marche.gstock.data.repository

import be.marche.gstock.core.ApiResult
import be.marche.gstock.data.local.dao.WorkerDao
import be.marche.gstock.data.local.entity.WorkerEntity
import be.marche.gstock.data.mapper.toEntity
import be.marche.gstock.data.remote.GstockApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerRepository @Inject constructor(
    private val api: GstockApi,
    private val dao: WorkerDao,
) {
    /** Room is the single source of truth the UI observes. */
    fun observeWorkers(): Flow<List<WorkerEntity>> = dao.observeAll()

    /** Fetches from the API and caches into Room. Returns the count fetched. */
    suspend fun refresh(search: String? = null): ApiResult<Int> = safeApiCall {
        val response = api.getWorkers(active = true, search = search?.ifBlank { null })
        val entities = response.data.map { it.toEntity() }
        dao.replaceAll(entities)
        entities.size
    }
}
