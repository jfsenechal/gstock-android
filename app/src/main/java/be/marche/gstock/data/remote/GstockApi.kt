package be.marche.gstock.data.remote

import be.marche.gstock.data.remote.dto.ActionResponse
import be.marche.gstock.data.remote.dto.CheckoutDto
import be.marche.gstock.data.remote.dto.CheckoutRequest
import be.marche.gstock.data.remote.dto.ListResponse
import be.marche.gstock.data.remote.dto.LoginRequest
import be.marche.gstock.data.remote.dto.LoginResponse
import be.marche.gstock.data.remote.dto.ReturnRequest
import be.marche.gstock.data.remote.dto.ScanRequest
import be.marche.gstock.data.remote.dto.ScanResponse
import be.marche.gstock.data.remote.dto.ToolDto
import be.marche.gstock.data.remote.dto.UserDto
import be.marche.gstock.data.remote.dto.WorkerDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GstockApi {

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/user")
    suspend fun getCurrentUser(): UserDto

    @GET("api/workers")
    suspend fun getWorkers(
        @Query("active") active: Boolean? = null,
        @Query("search") search: String? = null,
        @Query("per_page") perPage: Int? = null,
    ): ListResponse<WorkerDto>

    @GET("api/tools")
    suspend fun getTools(
        @Query("status") status: String? = null,
        @Query("available") available: Boolean? = null,
        @Query("search") search: String? = null,
    ): ListResponse<ToolDto>

    @GET("api/checkouts")
    suspend fun getCheckouts(
        @Query("active") active: Boolean? = null,
        @Query("returned") returned: Boolean? = null,
        @Query("overdue") overdue: Boolean? = null,
        @Query("worker_id") workerId: Long? = null,
        @Query("tool_id") toolId: Long? = null,
    ): ListResponse<CheckoutDto>

    @POST("api/scan-worker")
    suspend fun scanWorker(@Body request: ScanRequest): ScanResponse

    @POST("api/scan")
    suspend fun scanTool(@Body request: ScanRequest): ScanResponse

    @POST("api/checkout")
    suspend fun checkout(@Body request: CheckoutRequest): ActionResponse

    @POST("api/return")
    suspend fun returnTool(@Body request: ReturnRequest): ActionResponse
}
