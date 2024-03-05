file://<WORKSPACE>/src/main/scala/org/mxr/shop/route/ItemRoutes.scala
### java.lang.AssertionError: assertion failed: denotation trait GenConcurrent invalid in run 1. ValidFor: Period(1..5, run = 5)

occurred in the presentation compiler.

action parameters:
uri: file://<WORKSPACE>/src/main/scala/org/mxr/shop/route/ItemRoutes.scala
text:

```scala
package org.mxr.shop.route

import cats.effect.{IO, Resource}
import cats.*
import cats.data.*
import cats.syntax.all.*
import io.circe.*
import org.http4s.circe.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.multipart.Multipart

import java.io.FileOutputStream
import java.nio.file.Paths
import scala.util.control.NoStackTrace
import dev.profunktor.redis4cats.algebra.Auth

import java.time.LocalDateTime

object ItemRoutes {

  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}

  import dsl._
  import org.mxr.shop.service.ItemService.*
  import org.mxr.shop.service.ItemService

  val singlePageItemCount = 10

  private def publicRoutes(itemService: ItemService): HttpRoutes[IO] =
    HttpRoutes.of {
      case GET -> Root => Ok(itemService.getAllItems)

      case GET -> Root / LongVar(id) =>
        itemService.getItem(id).flatMap {
          case Some(value) => Ok(value)
          case None => NotFound()
        }

      case GET -> Root / "latest" / IntVar(pageNumber) => {
        itemService
          .getLatestItems(pageNumber * singlePageItemCount, singlePageItemCount)
          .flatMap {
            Ok(_)
          }
      }

      case GET -> Root / ItemCategory(category) / IntVar(pageNumber) => {
        itemService
          .getCategoryItems(
            category,
            pageNumber * singlePageItemCount,
            singlePageItemCount
          )
          .flatMap {
            Ok(_)
          }
      }

      case GET -> Root / "user" / user =>
        Ok(itemService.getAllItemsByUser(user))
    }

  sealed trait AuthoiozationError extends NoStackTrace

  case object WrongUser extends AuthorizationError

  private def checkUser(user: User, item: Item): IO[Unit] = {
    import cats.implicits.*
    if user.email == item.user
    then IO
  .unit
    else WrongUser.raiseError[IO, Unit]
  }

  private def authedRoutes(itemService: ItemService): AuthedRoutes[User, IO] =
    AuthedRoutes.of {
      case req@POST -> Root as user =>
        for {
          body <- req.req.as[Item]
          _ <- checkUser(user, body)
          result <- itemService.postItem(body)
          resp <- result.fold(BadRequest(_), Ok(_))
        } yield resp

      case req@PUT -> Root / "status" as user =>
        for {
          body <- req.req.as[UpdateStatusBody]
          result <- itemService.updateItemStatus(body.id, body.status)
          resp <- result.fold(BadRequest(_), Ok(_))
        } yield resp

      case req@POST -> Root / "image" as user =>
        req.req.decode[IO, Multipart[IO]] { m =>
          m.parts.find(_.name.contains("file")) match {
            case None =>
              IO.pure(
                Response[IO](Status.BadRequest).withEntity(
                  "File not found in request"
                )
              )
            case Some(filePart) =>
              val path =
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

  <+> AuthorizationMiddleware
.middleware(
    authedRoutes(itemService)
  )

}

```



#### Error stacktrace:

```
scala.runtime.Scala3RunTime$.assertFailed(Scala3RunTime.scala:8)
	dotty.tools.dotc.core.Denotations$SingleDenotation.updateValidity(Denotations.scala:717)
	dotty.tools.dotc.core.Denotations$SingleDenotation.bringForward(Denotations.scala:742)
	dotty.tools.dotc.core.Denotations$SingleDenotation.toNewRun$1(Denotations.scala:799)
	dotty.tools.dotc.core.Denotations$SingleDenotation.current(Denotations.scala:870)
	dotty.tools.dotc.core.Symbols$Symbol.recomputeDenot(Symbols.scala:120)
	dotty.tools.dotc.core.Symbols$Symbol.computeDenot(Symbols.scala:114)
	dotty.tools.dotc.core.Symbols$Symbol.denot(Symbols.scala:107)
	dotty.tools.dotc.core.Definitions.scalaClassName(Definitions.scala:1471)
	dotty.tools.dotc.core.Definitions.isFunctionClass(Definitions.scala:1503)
	dotty.tools.dotc.core.Definitions.isNonRefinedFunction(Definitions.scala:1693)
	dotty.tools.dotc.core.Definitions.isFunctionType(Definitions.scala:1704)
	dotty.tools.dotc.typer.Implicits$ImplicitRefs.candidateKind$1(Implicits.scala:225)
	dotty.tools.dotc.typer.Implicits$ImplicitRefs.tryCandidate$1(Implicits.scala:254)
	dotty.tools.dotc.typer.Implicits$ImplicitRefs.filterMatching$$anonfun$2(Implicits.scala:263)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.typer.Implicits$ImplicitRefs.filterMatching(Implicits.scala:263)
	dotty.tools.dotc.typer.Implicits$ContextualImplicits.computeEligible(Implicits.scala:368)
	dotty.tools.dotc.typer.Implicits$ContextualImplicits.eligible(Implicits.scala:360)
	dotty.tools.dotc.typer.Implicits$ContextualImplicits.computeEligible(Implicits.scala:370)
	dotty.tools.dotc.typer.Implicits$ContextualImplicits.eligible(Implicits.scala:360)
	dotty.tools.dotc.typer.Implicits$ContextualImplicits.computeEligible(Implicits.scala:370)
	dotty.tools.dotc.typer.Implicits$ContextualImplicits.eligible(Implicits.scala:360)
	dotty.tools.dotc.typer.Implicits$ContextualImplicits.computeEligible(Implicits.scala:370)
	dotty.tools.dotc.typer.Implicits$ContextualImplicits.eligible(Implicits.scala:360)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1537)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1572)
	dotty.tools.dotc.typer.Implicits.inferImplicit(Implicits.scala:1060)
	dotty.tools.dotc.typer.Implicits.inferImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg(Implicits.scala:884)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicitArg(Typer.scala:116)
	dotty.tools.dotc.typer.Typer.implicitArgs$1(Typer.scala:3737)
	dotty.tools.dotc.typer.Typer.addImplicitArgs$1(Typer.scala:3773)
	dotty.tools.dotc.typer.Typer.adaptNoArgsImplicitMethod$1(Typer.scala:3849)
	dotty.tools.dotc.typer.Typer.adaptNoArgs$1(Typer.scala:4038)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4268)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Typer.readapt$1(Typer.scala:3598)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4255)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Implicits.typedImplicit(Implicits.scala:1117)
	dotty.tools.dotc.typer.Implicits.typedImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.typedImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.tryImplicit(Implicits.scala:1242)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.rank$1(Implicits.scala:1341)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1511)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1539)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1547)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1572)
	dotty.tools.dotc.typer.Implicits.inferImplicit(Implicits.scala:1060)
	dotty.tools.dotc.typer.Implicits.inferImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg(Implicits.scala:884)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicitArg(Typer.scala:116)
	dotty.tools.dotc.typer.Typer.implicitArgs$1(Typer.scala:3737)
	dotty.tools.dotc.typer.Typer.addImplicitArgs$1(Typer.scala:3773)
	dotty.tools.dotc.typer.Typer.adaptNoArgsImplicitMethod$1(Typer.scala:3849)
	dotty.tools.dotc.typer.Typer.adaptNoArgs$1(Typer.scala:4038)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4268)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Typer.readapt$1(Typer.scala:3598)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4255)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Implicits.typedImplicit(Implicits.scala:1117)
	dotty.tools.dotc.typer.Implicits.typedImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.typedImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.tryImplicit(Implicits.scala:1242)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.rank$1(Implicits.scala:1341)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1511)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1539)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1547)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1572)
	dotty.tools.dotc.typer.Implicits.inferImplicit(Implicits.scala:1060)
	dotty.tools.dotc.typer.Implicits.inferImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg(Implicits.scala:884)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicitArg(Typer.scala:116)
	dotty.tools.dotc.typer.Typer.implicitArgs$1(Typer.scala:3737)
	dotty.tools.dotc.typer.Typer.addImplicitArgs$1(Typer.scala:3773)
	dotty.tools.dotc.typer.Typer.adaptNoArgsImplicitMethod$1(Typer.scala:3849)
	dotty.tools.dotc.typer.Typer.adaptNoArgs$1(Typer.scala:4038)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4268)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Typer.readapt$1(Typer.scala:3598)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4255)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Implicits.typedImplicit(Implicits.scala:1117)
	dotty.tools.dotc.typer.Implicits.typedImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.typedImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.tryImplicit(Implicits.scala:1242)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.rank$1(Implicits.scala:1341)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1511)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1539)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1547)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1572)
	dotty.tools.dotc.typer.Implicits.inferImplicit(Implicits.scala:1060)
	dotty.tools.dotc.typer.Implicits.inferImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg(Implicits.scala:884)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicitArg(Typer.scala:116)
	dotty.tools.dotc.typer.Typer.implicitArgs$1(Typer.scala:3737)
	dotty.tools.dotc.typer.Typer.addImplicitArgs$1(Typer.scala:3773)
	dotty.tools.dotc.typer.Typer.adaptNoArgsImplicitMethod$1(Typer.scala:3849)
	dotty.tools.dotc.typer.Typer.adaptNoArgs$1(Typer.scala:4038)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4268)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Typer.readapt$1(Typer.scala:3598)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4255)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Implicits.typedImplicit(Implicits.scala:1117)
	dotty.tools.dotc.typer.Implicits.typedImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.typedImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.tryImplicit(Implicits.scala:1242)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.rank$1(Implicits.scala:1341)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1511)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1539)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1547)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1572)
	dotty.tools.dotc.typer.Implicits.inferImplicit(Implicits.scala:1060)
	dotty.tools.dotc.typer.Implicits.inferImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg(Implicits.scala:884)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicitArg(Typer.scala:116)
	dotty.tools.dotc.typer.Typer.implicitArgs$1(Typer.scala:3737)
	dotty.tools.dotc.typer.Typer.addImplicitArgs$1(Typer.scala:3773)
	dotty.tools.dotc.typer.Typer.adaptNoArgsImplicitMethod$1(Typer.scala:3849)
	dotty.tools.dotc.typer.Typer.adaptNoArgs$1(Typer.scala:4038)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4268)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Typer.readapt$1(Typer.scala:3598)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4255)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Implicits.typedImplicit(Implicits.scala:1117)
	dotty.tools.dotc.typer.Implicits.typedImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.typedImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.tryImplicit(Implicits.scala:1242)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.rank$1(Implicits.scala:1341)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1511)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1539)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1547)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1572)
	dotty.tools.dotc.typer.Implicits.inferImplicit(Implicits.scala:1060)
	dotty.tools.dotc.typer.Implicits.inferImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg(Implicits.scala:884)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicitArg(Typer.scala:116)
	dotty.tools.dotc.typer.Typer.implicitArgs$1(Typer.scala:3737)
	dotty.tools.dotc.typer.Typer.addImplicitArgs$1(Typer.scala:3773)
	dotty.tools.dotc.typer.Typer.adaptNoArgsImplicitMethod$1(Typer.scala:3849)
	dotty.tools.dotc.typer.Typer.adaptNoArgs$1(Typer.scala:4038)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4268)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Typer.readapt$1(Typer.scala:3598)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4255)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Implicits.typedImplicit(Implicits.scala:1117)
	dotty.tools.dotc.typer.Implicits.typedImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.typedImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.tryImplicit(Implicits.scala:1242)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.rank$1(Implicits.scala:1341)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1511)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1539)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1547)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1572)
	dotty.tools.dotc.typer.Implicits.inferImplicit(Implicits.scala:1060)
	dotty.tools.dotc.typer.Implicits.inferImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg(Implicits.scala:884)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicitArg(Typer.scala:116)
	dotty.tools.dotc.typer.Typer.implicitArgs$1(Typer.scala:3737)
	dotty.tools.dotc.typer.Typer.addImplicitArgs$1(Typer.scala:3773)
	dotty.tools.dotc.typer.Typer.adaptNoArgsImplicitMethod$1(Typer.scala:3849)
	dotty.tools.dotc.typer.Typer.adaptNoArgs$1(Typer.scala:4038)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4268)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Typer.readapt$1(Typer.scala:3598)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4255)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.ProtoTypes$FunProto.typedArg(ProtoTypes.scala:497)
	dotty.tools.dotc.typer.Applications$ApplyToUntyped.typedArg(Applications.scala:897)
	dotty.tools.dotc.typer.Applications$ApplyToUntyped.typedArg(Applications.scala:897)
	dotty.tools.dotc.typer.Applications$Application.addTyped$1(Applications.scala:589)
	dotty.tools.dotc.typer.Applications$Application.matchArgs(Applications.scala:653)
	dotty.tools.dotc.typer.Applications$Application.init(Applications.scala:492)
	dotty.tools.dotc.typer.Applications$TypedApply.<init>(Applications.scala:779)
	dotty.tools.dotc.typer.Applications$ApplyToUntyped.<init>(Applications.scala:896)
	dotty.tools.dotc.typer.Applications.ApplyTo(Applications.scala:1126)
	dotty.tools.dotc.typer.Applications.ApplyTo$(Applications.scala:352)
	dotty.tools.dotc.typer.Typer.ApplyTo(Typer.scala:116)
	dotty.tools.dotc.typer.Applications.simpleApply$1(Applications.scala:969)
	dotty.tools.dotc.typer.Applications.realApply$1$$anonfun$2(Applications.scala:1052)
	dotty.tools.dotc.typer.Typer.tryEither(Typer.scala:3324)
	dotty.tools.dotc.typer.Applications.realApply$1(Applications.scala:1063)
	dotty.tools.dotc.typer.Applications.typedApply(Applications.scala:1101)
	dotty.tools.dotc.typer.Applications.typedApply$(Applications.scala:352)
	dotty.tools.dotc.typer.Typer.typedApply(Typer.scala:116)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3048)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3112)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3184)
	dotty.tools.dotc.typer.Implicits.tryConversion$1(Implicits.scala:1136)
	dotty.tools.dotc.typer.Implicits.typedImplicit(Implicits.scala:1167)
	dotty.tools.dotc.typer.Implicits.typedImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.typedImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.tryImplicit(Implicits.scala:1242)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.rank$1(Implicits.scala:1341)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1511)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1539)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1572)
	dotty.tools.dotc.typer.Implicits.inferImplicit(Implicits.scala:1060)
	dotty.tools.dotc.typer.Implicits.inferImplicit$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferImplicit(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits.inferView(Implicits.scala:856)
	dotty.tools.dotc.typer.Implicits.inferView$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.inferView(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits.viewExists(Implicits.scala:831)
	dotty.tools.dotc.typer.Implicits.viewExists$(Implicits.scala:818)
	dotty.tools.dotc.typer.Typer.viewExists(Typer.scala:116)
	dotty.tools.dotc.typer.Implicits.ignoredConvertibleImplicits$1$$anonfun$3(Implicits.scala:960)
	scala.collection.Iterator$$anon$6.hasNext(Iterator.scala:479)
	scala.collection.Iterator.isEmpty(Iterator.scala:466)
	scala.collection.Iterator.isEmpty$(Iterator.scala:466)
	scala.collection.AbstractIterator.isEmpty(Iterator.scala:1300)
	scala.collection.View$Filter.isEmpty(View.scala:146)
	scala.collection.IterableOnceOps.nonEmpty(IterableOnce.scala:833)
	scala.collection.IterableOnceOps.nonEmpty$(IterableOnce.scala:833)
	scala.collection.AbstractIterable.nonEmpty(Iterable.scala:933)
	dotty.tools.dotc.reporting.MissingImplicitArgument.noChainConversionsNote$1(messages.scala:2838)
	dotty.tools.dotc.reporting.MissingImplicitArgument.msgPostscript$$anonfun$4(messages.scala:2846)
	scala.Option.orElse(Option.scala:477)
	dotty.tools.dotc.reporting.MissingImplicitArgument.msgPostscript(messages.scala:2846)
	dotty.tools.dotc.reporting.Message.message$$anonfun$1(Message.scala:345)
	dotty.tools.dotc.reporting.Message.inMessageContext(Message.scala:341)
	dotty.tools.dotc.reporting.Message.message(Message.scala:345)
	dotty.tools.dotc.reporting.Message.isNonSensical(Message.scala:322)
	dotty.tools.dotc.reporting.HideNonSensicalMessages.isHidden(HideNonSensicalMessages.scala:16)
	dotty.tools.dotc.reporting.HideNonSensicalMessages.isHidden$(HideNonSensicalMessages.scala:10)
	dotty.tools.dotc.interactive.InteractiveDriver$$anon$5.isHidden(InteractiveDriver.scala:156)
	dotty.tools.dotc.reporting.Reporter.issueUnconfigured(Reporter.scala:156)
	dotty.tools.dotc.reporting.Reporter.go$1(Reporter.scala:181)
	dotty.tools.dotc.reporting.Reporter.issueIfNotSuppressed(Reporter.scala:200)
	dotty.tools.dotc.reporting.Reporter.report(Reporter.scala:203)
	dotty.tools.dotc.reporting.StoreReporter.report(StoreReporter.scala:50)
	dotty.tools.dotc.reporting.Reporter.flush$$anonfun$1(Reporter.scala:261)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.reporting.Reporter.flush(Reporter.scala:261)
	dotty.tools.dotc.core.TyperState.commit(TyperState.scala:175)
	dotty.tools.dotc.typer.Applications.fail$1(Applications.scala:1055)
	dotty.tools.dotc.typer.Applications.realApply$1$$anonfun$3$$anonfun$1(Applications.scala:1061)
	scala.Option.getOrElse(Option.scala:201)
	dotty.tools.dotc.typer.Applications.realApply$1$$anonfun$3(Applications.scala:1062)
	dotty.tools.dotc.typer.Typer.tryEither(Typer.scala:3327)
	dotty.tools.dotc.typer.Applications.realApply$1(Applications.scala:1063)
	dotty.tools.dotc.typer.Applications.typedApply(Applications.scala:1101)
	dotty.tools.dotc.typer.Applications.typedApply$(Applications.scala:352)
	dotty.tools.dotc.typer.Typer.typedApply(Typer.scala:116)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3048)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3112)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3184)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3188)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3300)
	dotty.tools.dotc.typer.Typer.$anonfun$57(Typer.scala:2486)
	dotty.tools.dotc.inlines.PrepareInlineable$.dropInlineIfError(PrepareInlineable.scala:243)
	dotty.tools.dotc.typer.Typer.typedDefDef(Typer.scala:2486)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3024)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3111)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3184)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3188)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3210)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3256)
	dotty.tools.dotc.typer.Typer.typedClassDef(Typer.scala:2669)
	dotty.tools.dotc.typer.Typer.typedTypeOrClassDef$1(Typer.scala:3036)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3040)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3111)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3184)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3188)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3210)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3256)
	dotty.tools.dotc.typer.Typer.typedPackageDef(Typer.scala:2812)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3081)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3112)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3184)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3188)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3300)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$1(TyperPhase.scala:44)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$adapted$1(TyperPhase.scala:54)
	scala.Function0.apply$mcV$sp(Function0.scala:42)
	dotty.tools.dotc.core.Phases$Phase.monitor(Phases.scala:440)
	dotty.tools.dotc.typer.TyperPhase.typeCheck(TyperPhase.scala:54)
	dotty.tools.dotc.typer.TyperPhase.runOn$$anonfun$3(TyperPhase.scala:88)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.typer.TyperPhase.runOn(TyperPhase.scala:88)
	dotty.tools.dotc.Run.runPhases$1$$anonfun$1(Run.scala:246)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.ArrayOps$.foreach$extension(ArrayOps.scala:1321)
	dotty.tools.dotc.Run.runPhases$1(Run.scala:262)
	dotty.tools.dotc.Run.compileUnits$$anonfun$1(Run.scala:270)
	dotty.tools.dotc.Run.compileUnits$$anonfun$adapted$1(Run.scala:279)
	dotty.tools.dotc.util.Stats$.maybeMonitored(Stats.scala:67)
	dotty.tools.dotc.Run.compileUnits(Run.scala:279)
	dotty.tools.dotc.Run.compileSources(Run.scala:194)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:165)
	scala.meta.internal.pc.MetalsDriver.run(MetalsDriver.scala:45)
	scala.meta.internal.pc.PcCollector.<init>(PcCollector.scala:45)
	scala.meta.internal.pc.PcSemanticTokensProvider$Collector$.<init>(PcSemanticTokensProvider.scala:61)
	scala.meta.internal.pc.PcSemanticTokensProvider.Collector$lzyINIT1(PcSemanticTokensProvider.scala:61)
	scala.meta.internal.pc.PcSemanticTokensProvider.Collector(PcSemanticTokensProvider.scala:61)
	scala.meta.internal.pc.PcSemanticTokensProvider.provide(PcSemanticTokensProvider.scala:90)
	scala.meta.internal.pc.ScalaPresentationCompiler.semanticTokens$$anonfun$1(ScalaPresentationCompiler.scala:99)
```
#### Short summary: 

java.lang.AssertionError: assertion failed: denotation trait GenConcurrent invalid in run 1. ValidFor: Period(1..5, run = 5)