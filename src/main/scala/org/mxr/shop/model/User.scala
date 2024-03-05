package org.mxr.shop.model

import io.circe.Decoder
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import io.circe.*
import cats.effect.IO
import org.http4s.circe.*
import slick.lifted.ForeignKeyQuery

trait HasEmail {
  def email: String
}

case class User(
    email: String,
    firstName: String,
    lastName: String,
    passwordHash: String,
    imageLink: String = ""
) extends HasEmail

object User {
  given Decoder[User] = Decoder.derived[User]

  given EntityDecoder[IO, User] = jsonOf

  given Encoder[User] = Encoder.AsObject.derived[User]

  given EntityEncoder[IO, User] = jsonEncoderOf
}

case class UserContact(
    email: String,
    content: String,
    id: Long = 0L
) extends HasEmail

object UserContact {
  given Decoder[UserContact] = Decoder.derived[UserContact]

  given EntityDecoder[IO, UserContact] = jsonOf

  given Encoder[UserContact] = Encoder.AsObject.derived[UserContact]

  given EntityEncoder[IO, UserContact] = jsonEncoderOf
}

object UserTable {
  import slick.jdbc.PostgresProfile.api._

  class UserTable(tag: Tag) extends Table[User](tag, None, "account") {
    def email        = column[String]("email", O.PrimaryKey)
    def firstName    = column[String]("first_name")
    def lastName     = column[String]("last_name")
    def imageLink = column[String]("image_link")
    def passwordHash = column[String]("password_hash")

    override def * =
      (email, firstName, lastName, passwordHash, imageLink).mapTo[User]
  }

  class UserContactTable(tag: Tag)
      extends Table[UserContact](tag, None, "user_contact") {
    def id      = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def email   = column[String]("email")
    def content = column[String]("content")

    def user: ForeignKeyQuery[UserTable, User] =
      foreignKey("user_fk", email, TableQuery[UserTable])(_.email)

    override def * = (email, content, id).mapTo[UserContact]
  }

  lazy val userTable = TableQuery[UserTable]

  lazy val userContactTable = TableQuery[UserContactTable]
}

case class UserDetail(
    email: String,
    firstName: String,
    lastName: String,
    imageLink: String,
    contacts: List[UserContact]
)

object UserDetail {
  import UserContact.given

  given Decoder[UserDetail] = Decoder.derived[UserDetail]

  given EntityDecoder[IO, UserDetail] = jsonOf

  given Encoder[UserDetail] = Encoder.AsObject.derived[UserDetail]

  given EntityEncoder[IO, UserDetail] = jsonEncoderOf
}
