file://<WORKSPACE>/src/main/scala/org/mxr/shop/route/ItemRoutes.scala
### java.lang.IndexOutOfBoundsException: 0

occurred in the presentation compiler.

action parameters:
offset: 2031
uri: file://<WORKSPACE>/src/main/scala/org/mxr/shop/route/ItemRoutes.scala
text:
```scala
package org.mxr.shop.route

import cats.effect.{IO, Resource}
import cats._
import cats.data._
import cats.syntax.all._
import io.circe.*
import org.http4s.circe.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.multipart.Multipart
import org.mxr.shop.middleware.AuthorizationMiddleware
import org.mxr.shop.model.{Item, ItemStatus, User}
import org.mxr.shop.service.ItemService

import java.io.FileOutputStream
import java.nio.file.Paths

object ItemRoutes {

  case class UpdateStatusBody(id: Long, status: ItemStatus)

  given Encoder[ItemStatus] = Encoder.instance {
    case ItemStatus.Sold    => Json.fromString("sold")
    case ItemStatus.Deleted => Json.fromString("deleted")
    case ItemStatus.Active  => Json.fromString("active")
  }

  given Decoder[ItemStatus] = Decoder.decodeString.emap {
    case "sold"    => Right(ItemStatus.Sold)
    case "deleted" => Right(ItemStatus.Deleted)
    case "active"  => Right(ItemStatus.Active)
    case unknown   => Left(s"Unknown value: $unknown")
  }

  given Decoder[Item]           = Decoder.derived[Item]
  given EntityDecoder[IO, Item] = jsonOf

  given Decoder[UpdateStatusBody]           = Decoder.derived[UpdateStatusBody]
  given EntityDecoder[IO, UpdateStatusBody] = jsonOf

  given Encoder[Item]           = Encoder.AsObject.derived[Item]
  given EntityEncoder[IO, Item] = jsonEncoderOf

  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}
  import dsl._

  private def publicRoutes(itemService: ItemService): HttpRoutes[IO] =
    HttpRoutes.of {
      case GET -> Root => Ok(itemService.getAllItems)

      case GET -> Root / LongVar(id) =>
        itemService.getItem(id).flatMap {
          case Some(value) => Ok(value)
          case None        => NotFound()
        }

      case GET -> Root / "user" / user =>
        Ok(itemService.getAllItemsByUser(user))
    }

  private def checkUser(user: User, item: Item): {
    import cats.implicits.*
    MonadError[IllegalArgumentException].ensure(IO.unit)(@@)

  }

  private def authedRoutes(itemService: ItemService): AuthedRoutes[User, IO] =
    AuthedRoutes.of {
      case req @ POST -> Root as user =>
        for {
          body   <- req.req.as[Item]
          _      <-
            if body.user != user.email then
              IO.raiseError(IllegalArgumentException("Wrong user"))
            else IO.unit
          result <- itemService.postItem(body)
          resp   <- result.fold(BadRequest(_), Ok(_))
        } yield resp

      case req @ PUT -> Root / "status" as user =>
        for {
          body   <- req.req.as[UpdateStatusBody]
          result <- itemService.updateItemStatus(body.id, body.status)
          resp   <- result.fold(BadRequest(_), Ok(_))
        } yield resp

      case req @ POST -> Root / "image" as user =>
        req.req.decode[IO, Multipart[IO]] { m =>
          m.parts.find(_.name.contains("file")) match {
            case None           =>
              IO.pure(
                Response[IO](Status.BadRequest).withEntity(
                  "File not found in request"
                )
              )
            case Some(filePart) =>
              val path   =
                Paths.get("./" + filePart.filename.getOrElse("uploadedFile"))
              val stream = Resource.make {
                IO {
                  new FileOutputStream(path.toFile)
                }
              } { stream =>
                IO {
                  stream.close()
                }.handleErrorWith { _ => IO.unit }
              }
              stream.use { output =>
                filePart.body.compile.toVector
                  .flatMap { bytes =>
                    IO(output.write(bytes.toArray))
                  }
                  .map { _ =>
                    Response[IO](Status.Ok).withEntity(
                      "File uploaded successfully"
                    )
                  }
              }
          }
        }
    }

  def itemRoutes(itemService: ItemService) =
    publicRoutes(itemService)
      <+> AuthorizationMiddleware.middleware(
        authedRoutes(itemService)
      )

}

```



#### Error stacktrace:

```
scala.collection.LinearSeqOps.apply(LinearSeq.scala:131)
	scala.collection.LinearSeqOps.apply$(LinearSeq.scala:128)
	scala.collection.immutable.List.apply(List.scala:79)
	dotty.tools.dotc.util.Signatures$.countParams(Signatures.scala:501)
	dotty.tools.dotc.util.Signatures$.applyCallInfo(Signatures.scala:186)
	dotty.tools.dotc.util.Signatures$.computeSignatureHelp(Signatures.scala:94)
	dotty.tools.dotc.util.Signatures$.signatureHelp(Signatures.scala:63)
	scala.meta.internal.pc.MetalsSignatures$.signatures(MetalsSignatures.scala:17)
	scala.meta.internal.pc.SignatureHelpProvider$.signatureHelp(SignatureHelpProvider.scala:51)
	scala.meta.internal.pc.ScalaPresentationCompiler.signatureHelp$$anonfun$1(ScalaPresentationCompiler.scala:388)
```
#### Short summary: 

java.lang.IndexOutOfBoundsException: 0