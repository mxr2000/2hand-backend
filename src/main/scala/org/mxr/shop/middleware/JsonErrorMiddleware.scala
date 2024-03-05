package org.mxr.shop.middleware

import cats.data.*
import cats.effect.IO
import org.http4s.HttpRoutes
import cats.implicits.*
import org.http4s.dsl.Http4sDsl
import org.mxr.shop.exception.Exception.*

object JsonErrorMiddleware {
  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}
  import dsl._

  def errorHandlingMiddleware(service: HttpRoutes[IO]): HttpRoutes[IO] =
    Kleisli { req =>
      service(req).handleErrorWith {
        case failure: io.circe.DecodingFailure =>
          OptionT.liftF(BadRequest(s"Invalid JSON: ${failure.getMessage}"))
        case failure: IllegalArgumentException =>
          OptionT.liftF(Forbidden(failure.getMessage))
        case WrongUser =>
          OptionT.liftF(Forbidden("Wrong user"))    
        case other =>
          OptionT.liftF(
            InternalServerError(s"Internal server error: ${other.getMessage}")
          )
      }
    }

}
