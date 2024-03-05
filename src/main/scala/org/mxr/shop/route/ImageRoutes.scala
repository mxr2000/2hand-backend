package org.mxr.shop.route

import cats.effect.IO
import com.cloudinary.utils.ObjectUtils
import io.circe.Decoder
import org.http4s.circe.jsonOf
import org.http4s.{AuthedRoutes, EntityDecoder, Response, Status}
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart.Multipart
import org.mxr.shop.db.Cloudinary
import org.mxr.shop.middleware.AuthorizationMiddleware
import org.mxr.shop.model.{ItemImage, User}
import org.mxr.shop.service.ImageService
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder

object ImageRoutes {
  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}
  import dsl._

  private def routes(imageService: ImageService): AuthedRoutes[User, IO] = {

    given Decoder[ItemImage]           = Decoder.derived[ItemImage]
    given EntityDecoder[IO, ItemImage] = jsonOf

    AuthedRoutes.of {
      case req @ POST -> Root / "file" as _ =>
        req.req.decode[IO, Multipart[IO]] { m =>
          m.parts.find(_.name.contains("file")) match {
            case None           =>
              IO.pure(
                Response[IO](Status.BadRequest).withEntity(
                  "File not found in request"
                )
              )
            case Some(filePart) =>
              filePart.body.compile.toVector
                .flatMap { bytes =>
                  val uploadParams = ObjectUtils.asMap(
                    // "folder",
                    // "your-folder-name",
                    // "public_id",
                    // "desired-public-id"
                  )
                  IO.blocking {
                    Cloudinary.cloudinary
                      .uploader()
                      .upload(bytes.toArray, uploadParams)
                  }.handleErrorWith { e =>
                    IO.println(e) *> IO.raiseError(e)
                  }
                }
                .flatMap { response =>
                  response.get("secure_url") match
                    case link: String => IO { link }
                    case _ => IO.raiseError(new IllegalArgumentException())
                }
                .map { link =>
                  Response[IO](Status.Ok).withEntity(
                    link
                  )
                }
          }
        }

      case req @ POST -> Root as _ =>
        for {
          body   <- req.req.as[ItemImage]
          result <- imageService.addImage(body)
          resp   <- result.fold(BadRequest(_), Ok(_))
        } yield resp

      case DELETE -> Root / "item" / LongVar(id) as _ =>
        for {
          result <- imageService.deleteImage(id)
          resp   <- result.fold(BadRequest(_), Ok(_))
        } yield resp

    }
  }

  def imageRoutes(imageService: ImageService) =
    AuthorizationMiddleware.middleware(routes(imageService))

}
