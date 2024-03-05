package org.mxr.shop.util

import cats.*
import cats.implicits.*
import io.circe.parser.decode
import org.mxr.shop.model.User
import pdi.jwt.*
import pdi.jwt.algorithms.JwtHmacAlgorithm

import java.time.Instant
import scala.util.{Failure, Success}

object JwtUtil {

  private val secretKey                   = "secretKey"
  private val algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS256

  def encodeJwt(content: String): String = {
    val claim = JwtClaim(
      content = content,
      expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
      issuedAt = Some(Instant.now.getEpochSecond)
    )
    JwtCirce.encode(claim, secretKey, algorithm)
  }

  sealed trait ValidationError
  case object ValidationErrorTokenOutdated extends ValidationError
  case object ValidationErrorTokenNotFound extends ValidationError
  case object ValidationErrorParsingFailure extends ValidationError


  def validateJwt(token: String): Either[ValidationError, User] =
    println(s"token is $token")
    JwtCirce
      .decode(token, secretKey, Seq(algorithm))
      .transform(s => Success(s), f =>
        println(s"Decode failed: $f" )
        Failure(f))
      .toOption match
      case Some(claim) =>
        claim.expiration match
          case Some(expr) if expr < Instant.now.getEpochSecond =>
            println(s"token is expired ")
            Left(ValidationErrorTokenOutdated)
          case _ => decode[User](claim.content).leftMap(_ => {
            println(s"Decode to user error")
            ValidationErrorParsingFailure
          })
      case None        =>
        Left(ValidationErrorParsingFailure)
}
