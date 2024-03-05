package org.mxr.shop.route

import cats.implicits.*
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import io.circe.*
import org.http4s.circe.*
import org.mxr.shop.service.{ItemBatchService, ItemService, UserService}
import org.mxr.shop.model.*

object ProfileRoutes {

  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}

  import dsl._

  import org.mxr.shop.route.ItemRoutes.given
  import org.mxr.shop.route.BatchRoutes.given

  given Encoder[UserProfilePageResponse] =
    Encoder.AsObject.derived[UserProfilePageResponse]

  given EntityEncoder[IO, UserProfilePageResponse] = jsonEncoderOf

  def profileRoutes(
      itemService: ItemService,
      userService: UserService,
      batchService: ItemBatchService
  ): HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / email =>
      (
        itemService.getAllItemsByUser(email),
        userService.getUserDetail(email),
        batchService.getBatchesByUser(email)
      ).mapN { (items, user, batches) =>
        user.map {
          UserProfilePageResponse(_, items, batches)
        }
      }.flatMap {
        _.fold(NotFound())(Ok(_))
      }
  }

}
