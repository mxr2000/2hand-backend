package org.mxr.shop.model

import UserTable.UserTable
import org.mxr.shop.model.ItemStatus.Active
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.lifted.{ForeignKeyQuery, ProvenShape}

import java.time.LocalDateTime
import scala.language.postfixOps

enum ItemStatus {
  case Active  extends ItemStatus
  case Inactive extends ItemStatus
  case Sold    extends ItemStatus
}

enum ItemCategory {
  case Food    extends ItemCategory
  case Digital extends ItemCategory
  case Other   extends ItemCategory
}

object ItemCategory {
  def unapply(str: String): Option[ItemCategory] = str.toLowerCase match {
    case "food"    => Some(Food)
    case "digital" => Some(Digital)
    case _         => Some(Other)
  }
}

case class Item(
    user: String,
    title: String,
    description: String,
    createTime: LocalDateTime,
    updateTime: LocalDateTime,
    status: ItemStatus,
    category: ItemCategory,
    price: Float,
    batchId: Option[Long] = None,
    id: Long = 0L
)

object Item {
  def fromCreateRequest(user: String, req: CreateOrUpdateItemRequest): Item = {
    Item(
      user = user,
      title = req.name,
      description = req.description,
      createTime = LocalDateTime.now(),
      updateTime = LocalDateTime.now(),
      status = Active,
      category = req.category,
      price = req.price,
      batchId = req.batchId
    )
  }
}

case class ItemImage(
    itemId: Long,
    link: String,
    id: Long = 0L
)

object ItemTable {
  import slick.jdbc.PostgresProfile.api._

  implicit val itemStatusType
      : JdbcType[ItemStatus] with BaseTypedType[ItemStatus] =
    MappedColumnType.base[ItemStatus, String](
      {
        case ItemStatus.Active  => "active"
        case ItemStatus.Inactive => "inactive"
        case ItemStatus.Sold    => "sold"
      },
      {
        case "active"  => ItemStatus.Active
        case "inactive" => ItemStatus.Inactive
        case "sold"    => ItemStatus.Sold
      }
    )

  implicit val itemCategoryType
      : JdbcType[ItemCategory] with BaseTypedType[ItemCategory] =
    MappedColumnType.base[ItemCategory, String](
      { category => category.toString },
      {
        case "food"    => ItemCategory.Food
        case "digital" => ItemCategory.Digital
        case _         => ItemCategory.Other
      }
    )

  class ItemTable(tag: Tag) extends Table[Item](tag, None, "item") {
    def id          = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def user        = column[String]("email")
    def title       = column[String]("title")
    def description = column[String]("description")
    def createTime  = column[LocalDateTime]("create_time")
    def updateTime  = column[LocalDateTime]("update_time")
    def status      = column[ItemStatus]("status")
    def category    = column[ItemCategory]("category")
    def price       = column[Float]("price")
    def batchId     = column[Option[Long]]("batch_id")

    def creator = foreignKey("creator_fk", user, TableQuery[UserTable])(_.email)

    override def * =
      (
        user,
        title,
        description,
        createTime,
        updateTime,
        status,
        category,
        price,
        batchId,
        id
      ) <> ((Item.apply _).tupled, Item.unapply)
  }

  class ItemImageTable(tag: Tag)
      extends Table[ItemImage](tag, None, "item_image") {
    def id     = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def itemId = column[Long]("item_id")
    def link   = column[String]("link")

    def item: ForeignKeyQuery[ItemTable, Item] =
      foreignKey("item_fk", itemId, TableQuery[ItemTable])(_.id)

    override def * : ProvenShape[ItemImage] =
      (itemId, link, id) <> ((ItemImage.apply _).tupled, ItemImage.unapply)
  }

  lazy val itemTable      = TableQuery[ItemTable]
  lazy val itemImageTable = TableQuery[ItemImageTable]
}
