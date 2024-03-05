package org.mxr.shop.route

import cats.effect.IO
import cats.*
import cats.data.*
import cats.syntax.all.*
import io.circe.*
import org.http4s.circe.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.*
import org.mxr.shop.middleware.AuthorizationMiddleware
import org.mxr.shop.model.*
import org.mxr.shop.model.ItemCategory.Other
import org.mxr.shop.service.UserService

import java.time.LocalDateTime

object ItemRoutes {

  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}
  import dsl._
  import org.mxr.shop.service.ItemService

  given Encoder[ItemStatus] = Encoder.instance {
    case ItemStatus.Sold    => Json.fromString("sold")
    case ItemStatus.Inactive => Json.fromString("inactive")
    case ItemStatus.Active  => Json.fromString("active")
  }

  given Encoder[ItemCategory] = Encoder.instance {
    case ItemCategory.Digital => Json.fromString("digital")
    case ItemCategory.Food    => Json.fromString("food")
    case ItemCategory.Other   => Json.fromString("other")
  }

  given Decoder[ItemStatus] = Decoder.decodeString.emap {
    case "sold"    => Right(ItemStatus.Sold)
    case "inactive" => Right(ItemStatus.Inactive)
    case "active"  => Right(ItemStatus.Active)
    case unknown   => Left(s"Unknown value: $unknown")
  }

  given Decoder[ItemCategory] = Decoder.decodeString.emap {
    case "digital" => Right(ItemCategory.Digital)
    case "food"    => Right(ItemCategory.Food)
    case "other"   => Right(ItemCategory.Other)
    case unknown   => Left(s"Unknown value: $unknown")
  }

  given Encoder[CreateOrUpdateItemRequest] =
    Encoder.AsObject.derived[CreateOrUpdateItemRequest]

  given EntityEncoder[IO, CreateOrUpdateItemRequest] = jsonEncoderOf

  given Decoder[CreateOrUpdateItemRequest] =
    Decoder.derived[CreateOrUpdateItemRequest]

  given EntityDecoder[IO, CreateOrUpdateItemRequest] = jsonOf

  given Decoder[Item] = Decoder.derived[Item]

  given EntityDecoder[IO, Item] = jsonOf

  given Encoder[Item] = Encoder.AsObject.derived[Item]

  given EntityEncoder[IO, Item] = jsonEncoderOf

  given Encoder[ItemDetail] = Encoder.AsObject.derived[ItemDetail]

  given EntityEncoder[IO, ItemDetail] = jsonEncoderOf

  given Encoder[ItemSummary] = Encoder.AsObject.derived[ItemSummary]

  given EntityEncoder[IO, ItemSummary] = jsonEncoderOf

  given Encoder[ItemListResponse] = Encoder.AsObject.derived[ItemListResponse]

  given EntityEncoder[IO, ItemListResponse] = jsonEncoderOf



  val singlePageItemCount = 10

  private def publicRoutes(
      itemService: ItemService
  ): HttpRoutes[IO] =
    HttpRoutes.of {
      case GET -> Root => Ok(itemService.getAllItems)

      case GET -> Root / LongVar(id) =>
        itemService.getItem(id).flatMap { _.fold(NotFound())(Ok(_)) }

      case GET -> Root / "latest" / IntVar(pageNumber) => {
        itemService
          .getLatestItems(pageNumber * singlePageItemCount, singlePageItemCount)
          .flatMap(Ok(_))
      }

      case GET -> Root / ItemCategory(category) / IntVar(pageNumber) =>
        itemService
          .getCategoryItems(
            category,
            pageNumber * singlePageItemCount,
            singlePageItemCount
          )
          .flatMap(Ok(_))


    }

  private def checkUser(
      req: CreateOrUpdateItemRequest,
      email: String
  ): IO[Unit] =
    if req.user != email then IO.raiseError(new IllegalArgumentException())
    else IO.unit

  private def authedRoutes(itemService: ItemService): AuthedRoutes[User, IO] =
    AuthedRoutes.of {
      case req @ POST -> Root as user =>
        for {
          body   <- req.req.as[CreateOrUpdateItemRequest]
          result <- itemService.postItem(user.email, body)
          resp   <- result.fold(
            err => {
              println(s"Error is $err")
              BadRequest(err)
            },
            Ok(_)
          )
        } yield resp

      case req @ PUT -> Root as user =>
        for {
          body   <- req.req.as[CreateOrUpdateItemRequest]
          _      <- checkUser(body, user.email)
          result <- itemService.updateItem(body)
          resp   <- result.fold(BadRequest(_), Ok(_))
        } yield resp

    }

  def itemRoutes(itemService: ItemService) =
    publicRoutes(itemService)
      <+> AuthorizationMiddleware.middleware(
        authedRoutes(itemService)
      )

}
