package be.marche.gstock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String?,
    val status: String?,
    val isActive: Boolean,
    val activeCheckoutsCount: Int,
    val createdAt: String?,
) {
    val fullName: String get() = "$firstName $lastName"
}

@Entity(tableName = "tools")
data class ToolEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String?,
    val manufacturer: String?,
    val model: String?,
    val status: String?,
    val isAvailable: Boolean,
    val isCheckedOut: Boolean,
    val categoryId: Long?,
    val categoryName: String?,
    val createdAt: String?,
)

/** Single-row table (id is always 0) holding the logged-in user's bearer token and identity. */
@Entity(tableName = "auth")
data class AuthEntity(
    @PrimaryKey val id: Int = 0,
    val token: String,
    val userId: Long,
    val username: String,
    val name: String?,
    val email: String?,
)

@Entity(tableName = "checkouts")
data class CheckoutEntity(
    @PrimaryKey val id: Long,
    val workerId: Long?,
    val workerName: String?,
    val toolId: Long?,
    val toolName: String?,
    val checkedOutAt: String?,
    val dueAt: String?,
    val returnedAt: String?,
    val isReturned: Boolean,
    val isActive: Boolean,
    val isOverdue: Boolean,
    val notes: String?,
)
