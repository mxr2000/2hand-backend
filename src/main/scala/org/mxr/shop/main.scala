package org.mxr.shop

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Router
import org.mxr.shop.middleware.JsonErrorMiddleware
import org.mxr.shop.route.{
  BatchRoutes,
  ImageRoutes,
  ItemRoutes,
  ProfileRoutes,
  UserRoutes
}
import org.mxr.shop.service.{
  ImageService,
  ItemBatchService,
  ItemService,
  UserService
}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.http4s.server.middleware.*

import scala.util.Properties.envOrElse

object SimpleWebServer {
  implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]
  import cats.effect.unsafe.implicits.global

  private val port = envOrElse("PORT", "8080").toInt

  def run(): IO[Nothing] = {
    for {
      _ <- EmberClientBuilder.default[IO].build
      userService  = UserService.userService
      itemService  = ItemService.itemService(userService)
      imageService = ImageService.imageService
      batchService = ItemBatchService.itemBatchService(userService, itemService)
      router       = Router(
        "user"    -> UserRoutes.userRoutes(userService),
        "item"    -> ItemRoutes.itemRoutes(itemService),
        "image"   -> ImageRoutes.imageRoutes(imageService),
        "batch"   -> BatchRoutes.itemBatchRoutes(batchService),
        "profile" -> ProfileRoutes.profileRoutes(
          itemService,
          userService,
          batchService
        )
      )
      app          = JsonErrorMiddleware
        .errorHandlingMiddleware(
          router
        )
        .orNotFound
      corsApp      = CORS.policy.withAllowOriginAll.httpApp(app).unsafeRunSync()

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(Port.fromInt(port).getOrElse(port"8080"))
        .withHttpApp(corsApp)
        .build
    } yield ()
  }.useForever

}

object Main extends IOApp.Simple {
  val run: IO[Nothing] = SimpleWebServer.run()
}
