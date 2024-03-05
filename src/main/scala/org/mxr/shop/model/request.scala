package org.mxr.shop.model

import java.time.LocalDateTime

case class LogInData(
    user: User,
    token: String,
    expiration: Long
)

case class ItemSummary(
    id: Long,
    user: User,
    name: String,
    image: String,
    price: Float,
    category: ItemCategory,
    status: ItemStatus
)

case class ItemDetail(
    id: Long,
    user: UserDetail,
    name: String,
    status: ItemStatus,
    category: ItemCategory,
    description: String,
    createTime: LocalDateTime,
    updateTime: LocalDateTime,
    imageLinks: List[String],
    price: Float
)

case class ItemListResponse(
    from: Int,
    count: Int,
    total: Int,
    items: List[ItemSummary]
)

case class CreateOrUpdateItemRequest(
    name: String,
    price: Float,
    category: ItemCategory,
    description: String,
    imageLinks: List[String],
    status: ItemStatus = ItemStatus.Active,
    user: String = "",
    batchId: Option[Long] = None,
    id: Long = 0L
)

case class UserProfilePageResponse(
    user: UserDetail,
    items: List[ItemSummary],
    batches: List[ItemBatchSummary]
)

case class ItemBatchSummary(
    id: Long,
    title: String,
    user: User,
    totalPrice: Float,
    status: ItemBatchStatus,
    count: Int
)

case class ItemBatchDetail(
    id: Long,
    title: String,
    description: String,
    user: UserDetail,
    status: ItemBatchStatus,
    items: List[ItemSummary],
    createTime: LocalDateTime,
    updateTime: LocalDateTime,
    totalPrice: Float
)

case class ItemBatchListResponse(
    from: Int,
    total: Int,
    batches: List[ItemBatchSummary]
)

case class CreateOrUpdateItemBatchRequest(
    title: String,
    description: String,
    status: ItemBatchStatus,
    email: String,
    id: Long = 0L
)
