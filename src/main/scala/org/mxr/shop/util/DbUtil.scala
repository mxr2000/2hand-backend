package org.mxr.shop.util

import cats.effect.IO
import org.mxr.shop.db.Connection
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.Future
import org.postgresql.util.PSQLException
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api.*

import scala.util.{Failure, Success}

object DbUtil {
  implicit class FutureToIO[T](val future: Future[T]) {
    def toIO: IO[T] = {
      IO.fromFuture(IO(future))
    }
  }

  def runInsertQuery(
      query: DBIOAction[Long, NoStream, Effect.Write],
      foreignKey: Option[String] = None
  ): IO[Either[String, Long]] =
    Connection.db.run(query.transactionally.asTry).toIO.map {
      case Failure(exception) =>
        exception match {
          case e: PSQLException if e.getSQLState.equals("23503") =>
            Left(
              s"Foreign key ( ${foreignKey.getOrElse("")} )restriction violated"
            )
          case e: PSQLException                                  =>
            Left(s"Unknown error: ${e.getSQLState} ${e.getMessage}")
        }
      case Success(id)        => Right(id)
    }

  def runUpdateQuery(
      query: PostgresProfile.ProfileAction[Int, NoStream, Effect.Write]
  ): IO[Either[String, Int]] =
    Connection.db.run(query.asTry).toIO.map {
      case Failure(exception) =>
        exception match {
          case e: PSQLException =>
            Left(s"Unknown error: ${e.getSQLState} ${e.getMessage}")
        }
      case Success(value)     => Right(value)
    }

}
