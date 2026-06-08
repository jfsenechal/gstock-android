package be.marche.gstock.data.mapper

import be.marche.gstock.data.local.entity.CheckoutEntity
import be.marche.gstock.data.local.entity.ToolEntity
import be.marche.gstock.data.local.entity.WorkerEntity
import be.marche.gstock.data.remote.dto.CheckoutDto
import be.marche.gstock.data.remote.dto.ToolDto
import be.marche.gstock.data.remote.dto.WorkerDto

fun WorkerDto.toEntity() = WorkerEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    phone = phone,
    status = status,
    isActive = isActive,
    activeCheckoutsCount = activeCheckoutsCount,
    createdAt = createdAt,
)

fun ToolDto.toEntity() = ToolEntity(
    id = id,
    name = name,
    description = description,
    manufacturer = manufacturer,
    model = model,
    status = status,
    isAvailable = isAvailable,
    isCheckedOut = isCheckedOut,
    categoryId = category?.id,
    categoryName = category?.name,
    createdAt = createdAt,
)

fun CheckoutDto.toEntity() = CheckoutEntity(
    id = id,
    workerId = workerId ?: worker?.id,
    workerName = worker?.let { "${it.firstName} ${it.lastName}" },
    toolId = toolId ?: tool?.id,
    toolName = tool?.name,
    checkedOutAt = checkedOutAt,
    dueAt = dueAt ?: expectedReturnAt,
    returnedAt = returnedAt,
    isReturned = isReturned,
    isActive = isActive,
    isOverdue = isOverdue,
    notes = notes,
)
