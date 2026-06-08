package be.marche.gstock.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Wrapper for Laravel paginated/collection responses: `{ "data": [...] }`. */
@Serializable
data class ListResponse<T>(
    val data: List<T> = emptyList(),
)

@Serializable
data class UserDto(
    val id: Long,
    val username: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String? = null,
)

@Serializable
data class WorkerDto(
    val id: Long,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String? = null,
    val phone: String? = null,
    val status: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("active_checkouts_count") val activeCheckoutsCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable(with = CategorySerializer::class)
data class CategoryDto(
    val id: Long,
    val name: String,
)

/**
 * The API is inconsistent about categories: the tools list returns an object
 * `{"id":1,"name":"…"}`, while the scan endpoint returns a bare string `"…"`.
 * This serializer accepts either form (a string becomes a nameless-id category).
 */
object CategorySerializer : KSerializer<CategoryDto> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CategoryDto")

    override fun deserialize(decoder: Decoder): CategoryDto {
        val input = decoder as? JsonDecoder
            ?: error("CategorySerializer only supports JSON")
        return when (val element = input.decodeJsonElement()) {
            is JsonPrimitive -> CategoryDto(id = 0, name = element.content)
            is JsonObject -> CategoryDto(
                id = element["id"]?.jsonPrimitive?.longOrNull ?: 0,
                name = element["name"]?.jsonPrimitive?.content ?: "",
            )
            else -> CategoryDto(id = 0, name = "")
        }
    }

    override fun serialize(encoder: Encoder, value: CategoryDto) =
        throw UnsupportedOperationException("CategoryDto is read-only")
}

@Serializable
data class ToolDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val status: String? = null,
    @SerialName("is_available") val isAvailable: Boolean = true,
    @SerialName("is_checked_out") val isCheckedOut: Boolean = false,
    val category: CategoryDto? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/**
 * The checkouts endpoint returned no rows during development, so this is modelled
 * defensively against the common Laravel shape. Unknown keys are ignored by the
 * Json configuration, and every field is nullable so parsing never fails.
 */
@Serializable
data class CheckoutDto(
    val id: Long,
    @SerialName("worker_id") val workerId: Long? = null,
    @SerialName("tool_id") val toolId: Long? = null,
    val worker: WorkerDto? = null,
    val tool: ToolDto? = null,
    @SerialName("checked_out_at") val checkedOutAt: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("expected_return_at") val expectedReturnAt: String? = null,
    @SerialName("returned_at") val returnedAt: String? = null,
    @SerialName("is_returned") val isReturned: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_overdue") val isOverdue: Boolean = false,
    val notes: String? = null,
)

@Serializable
data class ScanRequest(
    @SerialName("qr_data") val qrData: String,
)

@Serializable
data class CheckoutRequest(
    @SerialName("tool_id") val toolId: Long,
    @SerialName("worker_id") val workerId: Long,
)

@Serializable
data class ReturnRequest(
    @SerialName("checkout_id") val checkoutId: Long,
)

/**
 * Envelope for the scan endpoints: `{ "success": true, "data": { "worker"|"tool": … } }`.
 * The `worker`/`tool` accessors flatten the nested payload for callers.
 */
@Serializable
data class ScanResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: ScanData? = null,
) {
    val worker: WorkerDto? get() = data?.worker
    val tool: ToolDto? get() = data?.tool
}

@Serializable
data class ScanData(
    val worker: WorkerDto? = null,
    val tool: ToolDto? = null,
)

@Serializable
data class ActionResponse(
    val success: Boolean = false,
    val message: String? = null,
    val checkout: CheckoutDto? = null,
)
