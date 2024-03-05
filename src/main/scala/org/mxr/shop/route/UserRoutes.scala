package org.mxr.shop.route

import cats.implicits.*
import org.http4s.{AuthedRoutes, EntityDecoder, EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import io.circe.*
import io.circe.syntax.*
import org.http4s.circe.*
import org.mxr.shop.service.UserService
import org.mxr.shop.exception.Exception.{LogInError, WrongUser}
import org.mxr.shop.middleware.AuthorizationMiddleware
import org.mxr.shop.model.*
import org.mxr.shop.util.JwtUtil

import java.time.Instant

object UserRoutes {

  case class LogInBody(email: String, passwordHash: String)
  case class SignUpBody(user: User, authCode: String)

  given Decoder[LogInBody]           = Decoder.derived[LogInBody]
  given EntityDecoder[IO, LogInBody] = jsonOf

  given Decoder[SignUpBody]           = Decoder.derived[SignUpBody]
  given EntityDecoder[IO, SignUpBody] = jsonOf

  given Encoder[LogInData]           = Encoder.AsObject.derived[LogInData]
  given EntityEncoder[IO, LogInData] = jsonEncoderOf

  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}

  import dsl._

  private def checkSameUser(body1: HasEmail, body2: HasEmail): IO[Unit] =
    if body1.email != body2.email then IO.raiseError(WrongUser) else IO.unit

  private def authedRoutes(userService: UserService): AuthedRoutes[User, IO] =
    AuthedRoutes.of {
      case req @ PUT -> Root as user =>
        for {
          body   <- req.req.as[User]
          _      <- checkSameUser(user, body)
          result <- userService.updateInformation(
            body.email,
            body.firstName,
            body.lastName,
            body.imageLink
          )
          resp   <- result.fold(err => BadRequest(err), _ => Ok())
        } yield resp

      case req @ POST -> Root / "contact" as user =>
        for {
          body   <- req.req.as[UserContact]
          _      <- checkSameUser(user, body)
          result <- userService.addUserContact(body.email, body.content)
          resp   <- result.fold(err => BadRequest(err), _ => Ok())
        } yield resp

      case DELETE -> Root / "contact" / LongVar(id) as user =>
        for {
          result <- userService.deleteUserContact(user.email, id)
          resp   <- result match {
            case Left(err)  => BadRequest(err)
            case Right(cnt) =>
              cnt match {
                case 1 => Ok()
                case _ => NotFound()
              }
          }
        } yield resp

    }

  private def publicRoutes(userService: UserService): HttpRoutes[IO] = {

    val expirationInSeconds = 60 * 60 * 24 * 7

    HttpRoutes.of[IO] {
      case req @ POST -> Root / "logIn" =>
        for {
          body   <- req.as[LogInBody]
          result <- userService.logIn(body.email, body.passwordHash)
          resp   <- result match
            case Left(err)    =>
              err match {
                case LogInError.EmailDoesNotExist(_) => NotFound(err.getMessage)
                case LogInError.PasswordError(_) => BadRequest(err.getMessage)
              }
            case Right(value) =>
              Ok(
                LogInData(
                  value,
                  JwtUtil.encodeJwt(
                    value.copy(passwordHash = "").asJson.toString
                  ),
                  Instant.now.getEpochSecond + expirationInSeconds
                )
              )
        } yield resp

      case req @ POST -> Root / "signUp" =>
        for {
          body   <- req.as[SignUpBody]
          result <- userService.signUp(
            body.user,
            body.authCode
          )
          resp   <- result match
            case Left(err)    => BadRequest(err.getMessage)
            case Right(value) => Ok(value)
        } yield resp

      case POST -> Root / "auth" / email =>
        for {
          result <- userService.requestAuthenticationCode(email)
          resp   <- result.fold(err => BadRequest(err.getMessage), _ => Ok())
        } yield resp
    }
  }

  def userRoutes(userService: UserService) =
    publicRoutes(userService) <+> AuthorizationMiddleware.middleware(
      authedRoutes(userService)
    )
}
