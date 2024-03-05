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
import org.mxr.shop.exception.Exception.WrongUser
import org.mxr.shop.middleware.AuthorizationMiddleware
import org.mxr.shop.model.*

object BatchRoutes {
  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}

  import dsl._
  import org.mxr.shop.service.ItemBatchService
  import org.mxr.shop.route.ItemRoutes.given
  import org.mxr.shop.model.UserDetail.given

  given Encoder[ItemBatchStatus] =
    Encoder.instance {
      case ItemBatchStatus.Draft    => Json.fromString("draft")
      case ItemBatchStatus.Active   => Json.fromString("active")
      case ItemBatchStatus.Inactive => Json.fromString("inactive")
    }

  given EntityEncoder[IO, ItemBatchStatus] = jsonEncoderOf

  given Encoder[ItemBatchSummary] =
    Encoder.AsObject.derived[ItemBatchSummary]

  given EntityEncoder[IO, ItemBatchSummary] = jsonEncoderOf

  given Encoder[ItemBatchListResponse] =
    Encoder.AsObject.derived[ItemBatchListResponse]

  given EntityEncoder[IO, ItemBatchListResponse] = jsonEncoderOf

  given Encoder[ItemBatchDetail] =
    Encoder.AsObject.derived[ItemBatchDetail]

  given EntityEncoder[IO, ItemBatchDetail] = jsonEncoderOf

  given Decoder[ItemBatchStatus] = Decoder.decodeString.emap {
    case "active" => Right(ItemBatchStatus.Active)
    case "inactive" => Right(ItemBatchStatus.Inactive)
    case "draft" => Right(ItemBatchStatus.Draft)
    case unknown => Left(s"Unknown value: $unknown")
  }

  given Decoder[CreateOrUpdateItemBatchRequest] =
    Decoder.derived[CreateOrUpdateItemBatchRequest]

  given EntityDecoder[IO, CreateOrUpdateItemBatchRequest] = jsonOf

  private def publicRoutes(itemBatchService: ItemBatchService): HttpRoutes[IO] =
    HttpRoutes.of {
      case GET -> Root / LongVar(id) =>
        for {
          detail <- itemBatchService.getBatchDetail(id)
          resp   <- detail.fold(NotFound())(Ok(_))
        } yield resp

      case GET -> Root / "latest" / IntVar(pageNumber) =>
        for {
          result <- itemBatchService.getLatestBatches(pageNumber - 1)
          resp   <- Ok(result)
        } yield resp

    }

  private def checkUser(
      user: User,
      req: CreateOrUpdateItemBatchRequest
  ): IO[Unit] =
    if req.email != user.email then IO.raiseError(WrongUser)
    else IO.unit

  private def authedRoutes(
      itemBatchService: ItemBatchService
  ): AuthedRoutes[User, IO] =
    AuthedRoutes.of {
      case req @ POST -> Root as user =>
        for {
          req    <- req.req.as[CreateOrUpdateItemBatchRequest]
          _      <- checkUser(user, req)
          result <- itemBatchService.createBatch(req)
          resp   <- result.fold(BadRequest(_), Ok(_))
        } yield resp

      case req @ PUT -> Root as user =>
        for {
          req    <- req.req.as[CreateOrUpdateItemBatchRequest]
          _      <- checkUser(user, req)
          result <- itemBatchService.updateBatch(req)
          resp   <- result.fold(BadRequest(_), Ok(_))
        } yield resp

      case PUT -> Root / "deactivate" / LongVar(id) as user =>
        for {
          result <- itemBatchService.deactivateBatch(id, user.email)
          resp   <- result.fold(BadRequest(_), Ok(_))
        } yield resp

      case DELETE -> Root / LongVar(itemId) as user =>
        for {
          result <- itemBatchService.removeItemFromBatch(itemId, user.email)
          resp <- result.fold(BadRequest(_), Ok(_))
        } yield resp
    }

  def itemBatchRoutes(itemBatchService: ItemBatchService) =
    publicRoutes(itemBatchService)
      <+> AuthorizationMiddleware.middleware(
        authedRoutes(itemBatchService)
      )
}
