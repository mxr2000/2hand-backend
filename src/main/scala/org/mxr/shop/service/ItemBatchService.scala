package org.mxr.shop.service

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import org.mxr.shop.db.Connection
import org.mxr.shop.model.*
import cats.implicits.*

import concurrent.ExecutionContext.Implicits.global

trait ItemBatchService {
  def getLatestBatches(pageNumber: Int): IO[ItemBatchListResponse]
  def getBatchesByUser(email: String): IO[List[ItemBatchSummary]]
  def createBatch(
      req: CreateOrUpdateItemBatchRequest
  ): IO[Either[String, Long]]
  def updateBatch(
      req: CreateOrUpdateItemBatchRequest
  ): IO[Either[String, Int]]
  def deactivateBatch(id: Long, email: String): IO[Either[String, Int]]
  def getBatchDetail(id: Long): IO[Option[ItemBatchDetail]]
  def removeItemFromBatch(id: Long, email: String): IO[Either[String, Int]]
}

object ItemBatchService {

  import slick.jdbc.PostgresProfile.api._
  import org.mxr.shop.util.DbUtil.*

  private def totalPriceQuery(id: Long) =
    ItemTable.itemTable.filter { _.batchId === id }.map { _.price }.sum.result

  private def priceTitlePairQuery(id: Long) =
    ItemTable.itemTable
      .filter {
        _.batchId === id
      }
      .map { row =>
        (row.price, row.title)
      }
      .result

  private val countQuery = ItemBatchTable.itemBatchTable.length

  private def runGetBatchSummaryListQuery(
      query: Query[
        (
            ItemBatchTable.ItemBatchTable,
            UserTable.UserTable,
            Rep[Option[Float]],
            Rep[Int]
        ),
        (ItemBatch, User, Option[Float], Int),
        Seq
      ]
  ) =
    Connection.db.run(query.result).toIO.map(_.toList).map {
      _.map { case (batch, user, sumOption, count) =>
        ItemBatchSummary(
          batch.id,
          batch.title,
          user,
          sumOption.getOrElse(0),
          batch.status,
          count
        )
      }
    }

  def itemBatchService(
      userService: UserService,
      itemService: ItemService
  ): ItemBatchService = new ItemBatchService:
    override def getLatestBatches(
        pageNumber: Int
    ): IO[ItemBatchListResponse] = {
      val from  = pageNumber * 10
      val query = ItemBatchTable.itemBatchTable
        .drop(from)
        .take(10)
        .join(UserTable.userTable)
        .on(_.user === _.email)
        .joinLeft(ItemTable.itemTable)
        .on { case ((batch, _), item) =>
          item.batchId === batch.id
        }
        .groupBy(_._1)
        .map { case ((batch, user), group) =>
          (
            batch,
            user,
            group.map { case (_, itemOption) =>
              itemOption.map(_.price)
            }.sum,
            group.countDistinct
          )
        }
      (
        runGetBatchSummaryListQuery(query),
        Connection.db.run(countQuery.result).toIO
      ).mapN { (batches, total) =>
        ItemBatchListResponse(from, total, batches)
      }
    }

    override def createBatch(
        req: CreateOrUpdateItemBatchRequest
    ): IO[Either[String, Long]] = {
      val insertQuery = for {
        id <-
          (ItemBatchTable.itemBatchTable returning ItemBatchTable.itemBatchTable
            .map(_.id)) += ItemBatch.fromCreateRequest(req)
      } yield id
      runInsertQuery(insertQuery)
    }

    override def updateBatch(
        req: CreateOrUpdateItemBatchRequest
    ): IO[Either[String, Int]] = {
      import ItemBatchTable.itemBatchStatusType
      val query = ItemBatchTable.itemBatchTable
        .filter(_.id === req.id)
        .map { row =>
          (row.status, row.title, row.description)
        }
        .update(req.status, req.title, req.description)
      runUpdateQuery(query)
    }

    override def getBatchDetail(id: Long): IO[Option[ItemBatchDetail]] = {
      val batchQuery = ItemBatchTable.itemBatchTable.filter(_.id === id)
      val result     =
        OptionT(Connection.db.run(batchQuery.result.headOption).toIO).flatMap {
          batch =>
            (
              OptionT(userService.getUserDetail(batch.user)),
              OptionT.liftF(
                Connection.db.run(totalPriceQuery(id)).toIO.map(_.getOrElse(0f))
              ),
              OptionT.liftF(itemService.getAllItemsByBatch(id))
            ).mapN { (user, sum, items) =>
              ItemBatchDetail(
                batch.id,
                batch.title,
                batch.description,
                user,
                batch.status,
                items,
                batch.createTime,
                batch.updateTime,
                sum
              )
            }
        }
      result.value
    }

    override def getBatchesByUser(email: String): IO[List[ItemBatchSummary]] = {
      val query = ItemBatchTable.itemBatchTable
        .join(UserTable.userTable.filter(_.email === email))
        .on(_.user === _.email)
        .joinLeft(ItemTable.itemTable)
        .on { case ((batch, _), item) =>
          item.batchId === batch.id
        }
        .groupBy(_._1)
        .map { case ((batch, user), group) =>
          (
            batch,
            user,
            group.map { case (_, itemOption) =>
              itemOption.map(_.price)
            }.sum,
            group.countDistinct
          )
        }
      runGetBatchSummaryListQuery(query)
    }

    override def removeItemFromBatch(
        id: Long,
        email: String
    ): IO[Either[String, Int]] = {
      val query =
        ItemTable.itemTable
          .filter(row => row.id === id && row.user === email)
          .map(_.batchId)
          .update(None)
      runUpdateQuery(query)
    }

    override def deactivateBatch(id: Long, email: String): IO[Either[String, Int]] = {
      import ItemBatchTable.given
      import ItemTable.given
      val batchQuery = ItemBatchTable.itemBatchTable.filter(row => row.user === email && row.id === id).map(_.status).update(ItemBatchStatus.Inactive)
      val itemQuery = ItemTable.itemTable.filter(row => row.user === email && row.batchId === Some(id)).map(_.status).update(ItemStatus.Inactive)
      val result = for {
        a <- EitherT(runUpdateQuery(batchQuery))
        b <- EitherT(runUpdateQuery(itemQuery))
      } yield a + b
      result.value
    }
}
