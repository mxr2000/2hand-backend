package org.mxr.shop.service

import cats.effect.IO
import org.mxr.shop.db.Connection
import org.mxr.shop.model.*
import org.postgresql.util.PSQLException

import scala.util.{Failure, Success}

trait ImageService {
  def deleteImage(id: Long): IO[Either[String, Int]]
  def addImage(image: ItemImage): IO[Either[String, Long]]
}

object ImageService {

  import slick.jdbc.PostgresProfile.api._
  import org.mxr.shop.util.DbUtil.*

  def imageService: ImageService = new ImageService:
    override def deleteImage(id: Long): IO[Either[String, Int]] = {
      val query = ItemTable.itemImageTable.filter { _.id === id }.delete
      Connection.db.run(query.asTry).toIO.map {
        case Success(value)     => Right(value)
        case Failure(exception) => Left(exception.getMessage)
      }
    }

    override def addImage(image: ItemImage): IO[Either[String, Long]] = {
      val query = (ItemTable.itemImageTable returning {
        ItemTable.itemImageTable.map { _.id }
      }) += image
      Connection.db.run(query.asTry).toIO.map {
        case Failure(exception) =>
          exception match {
            case e: PSQLException if e.getSQLState.equals("23503") =>
              Left(s"Item with id ${image.itemId} does not exists")
            case e: PSQLException                                  =>
              Left(s"Unknown error: ${e.getSQLState} ${e.getMessage}")
          }
        case Success(id)        => Right(id)
      }
    }
}
