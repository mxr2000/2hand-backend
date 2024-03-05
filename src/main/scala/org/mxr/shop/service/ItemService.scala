package org.mxr.shop.service

import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import org.mxr.shop.db.Connection
import org.mxr.shop.model.*
import org.postgresql.util.PSQLException

import scala.util.{Failure, Right, Success}

trait ItemService {
  def postItem(
      email: String,
      req: CreateOrUpdateItemRequest
  ): IO[Either[String, Long]]
  def getAllItems: IO[List[Item]]
  def getAllItemsByUser(email: String): IO[List[ItemSummary]]
  def getAllItemsByBatch(id: Long): IO[List[ItemSummary]]
  def getItem(id: Long): IO[Option[ItemDetail]]
  def getLatestItems(from: Int, count: Int): IO[ItemListResponse]
  def getCategoryItems(
      category: ItemCategory,
      from: Int,
      count: Int
  ): IO[ItemListResponse]
  def updateItem(req: CreateOrUpdateItemRequest): IO[Either[String, Int]]

}

object ItemService {
  import slick.jdbc.PostgresProfile.api._
  import org.mxr.shop.util.DbUtil.*

  private val countQuery = ItemTable.itemTable.length

  def itemService(userService: UserService): ItemService = new ItemService {
    override def getAllItems: IO[List[Item]] =
      Connection.db.run(ItemTable.itemTable.result).toIO.map(_.toList)

    override def getItem(id: Long): IO[Option[ItemDetail]] = {
      val itemQuery = ItemTable.itemTable.filter(_.id === id)

      val imageQuery = ItemTable.itemImageTable.filter(_.itemId === id)

      val result = for {
        item   <- OptionT(Connection.db.run(itemQuery.result.headOption).toIO)
        images <- OptionT.liftF(Connection.db.run(imageQuery.result).toIO)
        user   <- OptionT(userService.getUserDetail(item.user))
      } yield ItemDetail(
        item.id,
        user,
        item.title,
        item.status,
        item.category,
        item.description,
        item.createTime,
        item.updateTime,
        images.map {
          _.link
        }.toList,
        item.price
      )
      result.value
    }

    private def getItemSummaryList(
        itemQuery: Query[
          (ItemTable.ItemTable, UserTable.UserTable, Rep[Option[String]]),
          (Item, User, Option[String]),
          Seq
        ]
    ): IO[List[ItemSummary]] = {
      val defaultImageLink =
        "https://res.cloudinary.com/dfplwulxn/image/upload/v1706847861/kucrnvuogo1icnfjxs4v.jpg"
      Connection.db
        .run(itemQuery.result)
        .toIO
        .map(_.toList)
        .map {
          _.map { (item, user, linkOption) =>
            ItemSummary(
              item.id,
              user,
              item.title,
              linkOption.getOrElse(defaultImageLink),
              item.price,
              item.category,
              item.status
            )
          }
        }
    }

    override def getAllItemsByUser(email: String): IO[List[ItemSummary]] = {
      val itemQuery =
        (ItemTable.itemTable
          join UserTable.userTable.filter(_.email === email)
          joinLeft ItemTable.itemImageTable on { case ((item, user), image) =>
            item.id === image.itemId && item.user === user.email
          })
          .groupBy { case ((item, user), _) => (item, user) }
          .map { case ((item, user), group) =>
            (
              item,
              user,
              group.map(_._2.map(_.link)).min
            )
          }
      getItemSummaryList(itemQuery)
    }

    override def getAllItemsByBatch(id: Long): IO[List[ItemSummary]] = {
      val itemQuery =
        (ItemTable.itemTable.filter {
          _.batchId === id
        }
          join UserTable.userTable on { case (item, user) =>
            item.user === user.email
          }
          joinLeft ItemTable.itemImageTable on { case ((item, user), image) =>
            item.id === image.itemId && item.user === user.email
          })
          .groupBy { case ((item, user), _) => (item, user) }
          .map { case ((item, user), group) =>
            (
              item,
              user,
              group.map(_._2.map(_.link)).min
            )
          }
      getItemSummaryList(itemQuery)
    }

    override def postItem(
        email: String,
        request: CreateOrUpdateItemRequest
    ): IO[Either[String, Long]] = {
      import concurrent.ExecutionContext.Implicits.global
      val insertQuery = for {
        id <- (ItemTable.itemTable returning ItemTable.itemTable.map(
          _.id
        )) += Item.fromCreateRequest(email, request)
        _  <- ItemTable.itemImageTable ++= request.imageLinks.map {
          ItemImage(id, _)
        }
      } yield id
      runInsertQuery(insertQuery)
    }

    override def updateItem(
        req: CreateOrUpdateItemRequest
    ): IO[Either[String, Int]] = {
      import ItemTable.*
      val query = ItemTable.itemTable
        .filter(_.id === req.id)
        .map { row =>
          (row.status, row.category, row.title, row.description, row.price)
        }
        .update(req.status, req.category, req.name, req.description, req.price)
      runUpdateQuery(query)
    }

    override def getCategoryItems(
        category: ItemCategory,
        from: Int,
        count: Int
    ): IO[ItemListResponse] = {
      import ItemTable.*
      val itemQuery =
        (ItemTable.itemTable
          join UserTable.userTable
          joinLeft ItemTable.itemImageTable on { case ((item, user), image) =>
            item.id === image.itemId && item.user === user.email && item.category === category
          })
          .groupBy { case ((item, user), _) => (item, user) }
          .map { case ((item, user), group) =>
            (
              item,
              user,
              group.map(_._2.map(_.link)).min
            )
          }
          .drop(from)
          .take(count)

      (
        getItemSummaryList(itemQuery),
        Connection.db.run(countQuery.result).toIO
      ).mapN { (items, total) =>
        ItemListResponse(from, count, total, items)
      }
    }

    override def getLatestItems(from: Int, count: Int): IO[ItemListResponse] = {
      val itemQuery =
        (ItemTable.itemTable
          join UserTable.userTable
          joinLeft ItemTable.itemImageTable on { case ((item, user), image) =>
            item.id === image.itemId && item.user === user.email
          })
          .groupBy { case ((item, user), _) => (item, user) }
          .map { case ((item, user), group) =>
            (
              item,
              user,
              group.map(_._2.map(_.link)).min
            )
          }
          .drop(from)
          .take(count)
      (
        getItemSummaryList(itemQuery),
        Connection.db.run(countQuery.result).toIO
      ).mapN { (items, total) =>
        ItemListResponse(from, count, total, items)
      }
    }

    
  }
}
