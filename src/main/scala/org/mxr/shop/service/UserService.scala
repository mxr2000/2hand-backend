package org.mxr.shop.service

import cats.effect.IO
import cats.effect.std.Random
import org.mxr.shop.db.Connection
import org.mxr.shop.exception.Exception.{
  LogInError,
  RequestAuthenticationError,
  SignUpError
}
import org.mxr.shop.model.*
import org.mxr.shop.util.{EmailUtil, RedisUtil}
import org.postgresql.util.PSQLException
import slick.jdbc.PostgresProfile

import javax.mail.MessagingException
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import cats.implicits.*

trait UserService {
  def logIn(email: String, passwordHash: String): IO[Either[LogInError, User]]
  def signUp(
      user: User,
      authCode: String
  ): IO[Either[SignUpError, User]]
  def requestAuthenticationCode(
      email: String
  ): IO[Either[RequestAuthenticationError, Unit]]
  def updateInformation(
      email: String,
      firstName: String,
      lastName: String,
      imageLink: String
  ): IO[Either[String, Int]]
  def addUserContact(email: String, content: String): IO[Either[String, Long]]
  def deleteUserContact(email: String, id: Long): IO[Either[String, Int]]
  def getUserDetail(email: String): IO[Option[UserDetail]]
}

object UserService {
  import slick.jdbc.PostgresProfile.api._
  import org.mxr.shop.util.DbUtil.*

  def userService: UserService = new UserService:
    override def logIn(
        email: String,
        passwordHash: String
    ): IO[Either[LogInError, User]] = {
      val query = UserTable.userTable.filter {
        _.email === email
      }
      Connection.db.run(query.result.headOption).toIO.map {
        case Some(user) if user.passwordHash != passwordHash =>
          Left(LogInError.PasswordError(email))
        case Some(user)                                      => Right(user)
        case None => Left(LogInError.EmailDoesNotExist(email))
      }
    }

    override def signUp(
        user: User,
        authCode: String
    ): IO[Either[SignUpError, User]] = {

      def doesEmailExist = Connection.db.run(
        UserTable.userTable.filter { _.email === user.email }.exists.result
      )

      val query  = UserTable.userTable += user
      val result = for {
        emailExists     <- doesEmailExist.toIO
        _               <-
          if emailExists then
            IO.raiseError(SignUpError.EmailAlreadyExists(user.email))
          else IO.unit
        authCodeInRedis <- RedisUtil.getAuthCode(user.email)
        _               <- authCodeInRedis match
          case Some(value) =>
            if value === authCode then IO.unit
            else IO.raiseError(SignUpError.AuthCodeIncorrect(authCode))
          case None => IO.raiseError(SignUpError.AuthCodeDoesNotExist(authCode))
        successful      <- Connection.db.run(query).toIO
      } yield successful

      result
        .map {
          case 1 => Right(user)
          case _ => Left(SignUpError.UnknownError)
        }
        .recover {
          case e @ SignUpError.EmailAlreadyExists(_)    => Left(e)
          case e @ SignUpError.UsernameAlreadyExists(_) => Left(e)
          case _ => Left(SignUpError.UnknownError)
        }
    }

    def checkIfUserDoesNotExist(email: String): IO[Unit] = {
      val query = UserTable.userTable.filter { _.email === email }.exists
      Connection.db.run(query.result).toIO.map {
        case true  =>
          IO.raiseError(RequestAuthenticationError.EmailAlreadyExists(email))
        case false => IO.unit
      }
    }

    def checkEmailPostfix(email: String): IO[Unit] = {
      if email.endsWith("@virginia.edu") then IO.unit
      else IO.raiseError(RequestAuthenticationError.WrongEmail(email))
    }

    def generateRandomDigitString(length: Int): IO[String] = {
      val randomDigitIO =
        Random.scalaUtilRandom[IO].flatMap { _.betweenInt(0, 10) }

      val randomDigitsIO = List.fill(length)(randomDigitIO).sequence
      randomDigitsIO.map(_.mkString)
    }

    override def requestAuthenticationCode(
        email: String
    ): IO[Either[RequestAuthenticationError, Unit]] = {
      val result = for {
        _   <- checkIfUserDoesNotExist(email)
        _   <- checkEmailPostfix(email)
        seq <- generateRandomDigitString(6)
        _   <- EmailUtil.sendEmail(
          "1099493928@qq.com",
          "syyvipxyiqcdhcaa",
          email,
          "Auth code",
          seq
        )
        _   <- RedisUtil.setAuthCode(email, seq)
      } yield ()
      result.attempt.map {
        case Left(exception) =>
          exception match {
            case e: MessagingException         =>
              Left(RequestAuthenticationError.SendEmailError(e.getMessage))
            case e: RequestAuthenticationError => Left(e)
            case e => Left(RequestAuthenticationError.UnknownError(e))
          }
        case Right(value)    => Right(value)
      }
    }

    def runInsertOrDeleteAndMapToEither[A](
        query: PostgresProfile.ProfileAction[A, NoStream, Effect.Write]
    ): IO[Either[String, A]] = {
      Connection.db.run(query.asTry).toIO.map {
        case Failure(exception) => {
          exception.printStackTrace()
          Left(exception.getMessage)
        }
        case Success(value)     => Right(value)
      }
    }

    override def addUserContact(
        email: String,
        content: String
    ): IO[Either[String, Long]] = {
      val query =
        (UserTable.userContactTable returning UserTable.userContactTable.map {
          _.id
        }) += UserContact(email, content)
      runInsertOrDeleteAndMapToEither(query)
    }

    override def deleteUserContact(
        email: String,
        id: Long
    ): IO[Either[String, Int]] = {
      val query = UserTable.userContactTable.filter { row =>
        row.id === id && row.email === email
      }.delete
      runInsertOrDeleteAndMapToEither(query)
    }

    override def getUserDetail(email: String): IO[Option[UserDetail]] = {
      val basicQuery   = UserTable.userTable.filter { _.email === email }
      val contactQuery = UserTable.userContactTable.filter { _.email === email }
      (
        Connection.db.run(basicQuery.result.headOption).toIO,
        Connection.db.run(contactQuery.result).toIO
      ).mapN { (basicInfoOption, contacts) =>
        basicInfoOption.map { basicInfo =>
          UserDetail(
            email,
            basicInfo.firstName,
            basicInfo.lastName,
            basicInfo.imageLink,
            contacts.toList
          )
        }
      }
    }

    override def updateInformation(
        email: String,
        firstName: String,
        lastName: String,
        imageLink: String
    ): IO[Either[String, Int]] = {
      val query = UserTable.userTable
        .filter { _.email === email }
        .map { user => (user.firstName, user.lastName, user.imageLink) }
        .update(firstName, lastName, imageLink)
      Connection.db.run(query.asTry).toIO.map {
        case Failure(exception) =>
          exception match {
            case e: PSQLException =>
              Left(s"Unknown error: ${e.getSQLState} ${e.getMessage}")
          }
        case Success(value)     =>
          value match
            case 1 => Right(1)
            case 0 => Left(s"Not found: ${email}")
      }
    }
}
