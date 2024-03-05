package org.mxr.shop.model

import org.mxr.shop.model.ItemBatchStatus.{Active, Inactive}
import org.mxr.shop.model.UserTable.UserTable
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import java.time.LocalDateTime

enum ItemBatchStatus {
  case Draft    extends ItemBatchStatus
  case Active   extends ItemBatchStatus
  case Inactive extends ItemBatchStatus
}

object ItemBatchStatus {
  def unapply(str: String): Option[ItemBatchStatus] = str.toLowerCase match {
    case "draft"    => Some(Draft)
    case "active"   => Some(Active)
    case "inactive" => Some(Inactive)
    case _          => None
  }
}

case class ItemBatch(
    user: String,
    title: String,
    description: String,
    createTime: LocalDateTime,
    updateTime: LocalDateTime,
    status: ItemBatchStatus,
    id: Long = 0L
)

object ItemBatch {
  def fromCreateRequest(req: CreateOrUpdateItemBatchRequest): ItemBatch = {
    ItemBatch(
      user = req.email,
      title = req.title,
      description = req.description,
      createTime = LocalDateTime.now(),
      updateTime = LocalDateTime.now(),
      status = req.status,
    )
  }
}

object ItemBatchTable {

  import slick.jdbc.PostgresProfile.api._

  implicit val itemBatchStatusType
      : JdbcType[ItemBatchStatus] with BaseTypedType[ItemBatchStatus] =
    MappedColumnType.base[ItemBatchStatus, String](
      {
        case ItemBatchStatus.Active   => "active"
        case ItemBatchStatus.Inactive => "inactive"
        case ItemBatchStatus.Draft    => "draft"
      },
      {
        case "active"   => ItemBatchStatus.Active
        case "inactive" => ItemBatchStatus.Inactive
        case "draft"    => ItemBatchStatus.Draft
      }
    )

  class ItemBatchTable(tag: Tag)
      extends Table[ItemBatch](tag, None, "item_batch") {
    def id          = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def user        = column[String]("email")
    def title       = column[String]("title")
    def description = column[String]("description")
    def createTime  = column[LocalDateTime]("create_time")
    def updateTime  = column[LocalDateTime]("update_time")
    def status      = column[ItemBatchStatus]("status")

    def creator = foreignKey("creator_fk", user, TableQuery[UserTable])(_.email)

    override def * =
      (
        user,
        title,
        description,
        createTime,
        updateTime,
        status,
        id
      ) <> ((ItemBatch.apply _).tupled, ItemBatch.unapply)
  }

  lazy val itemBatchTable = TableQuery[ItemBatchTable]
}
