package org.mxr.shop.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.header.*
import org.mxr.shop.util.JwtUtil.*
import org.mxr.shop.model.User
import org.mxr.shop.util.JwtUtil

object AuthorizationMiddleware {
  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}

  import dsl._

  private val authUser
      : Kleisli[IO, Request[IO], Either[ValidationError, User]] =
    Kleisli { req =>
      val message = for {
        header <- req.headers
          .get[Authorization]
          .toRight(ValidationErrorTokenNotFound)
        token  <-
          if header.value.length < 7
          then Left(ValidationErrorTokenNotFound)
          else Right(header.value.substring(7))
        msg    <- JwtUtil.validateJwt(token)
      } yield msg
      IO(message)
    }

  private val onFailure: AuthedRoutes[ValidationError, IO] =
    Kleisli(req => OptionT.liftF(Forbidden(req.context.toString)))

  val middleware: AuthMiddleware[IO, User] =
    AuthMiddleware(authUser, onFailure)
}
