file://<WORKSPACE>/src/main/scala/org/mxr/shop/middleware/AuthorizationMiddleware.scala
### java.lang.AssertionError: assertion failed: denotation trait Http4sDsl invalid in run 3. ValidFor: Period(1..5, run = 4)

occurred in the presentation compiler.

action parameters:
uri: file://<WORKSPACE>/src/main/scala/org/mxr/shop/middleware/AuthorizationMiddleware.scala
text:
```scala
package org.mxr.shop.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.header.*
import org.mxr.shop.util.JwtUtil.*
import org.mxr.shop.model.User
import org.mxr.shop.util.JwtUtil

object AuthorizationMiddleware {
  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}

  import dsl._

  private val authUser
      : Kleisli[IO, Request[IO], Either[ValidationError, User]] =
    Kleisli { req =>
      val message = for {
        header <- req.headers
          .get[Authorization]
          .toRight(ValidationErrorTokenNotFound)
        token  <-
          if header.value.length < 7
          then Left(ValidationErrorTokenNotFound)
          else Right(header.value)
        msg    <- JwtUtil.validateJwt(token)
      } yield msg
      IO(message)
    }

  private val onFailure: AuthedRoutes[ValidationError, IO] =
    Kleisli(req => OptionT.liftF(Forbidden(req.context.toString)))

  val middleware: AuthMiddleware[IO, User] =
    AuthMiddleware(authUser, onFailure)
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
	dotty.tools.dotc.core.Symbols$.toDenot(Symbols.scala:494)
	dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:183)
	dotty.tools.dotc.core.TypeComparer.compareAppliedType2$1(TypeComparer.scala:1152)
	dotty.tools.dotc.core.TypeComparer.thirdTry$1(TypeComparer.scala:623)
	dotty.tools.dotc.core.TypeComparer.secondTry$1(TypeComparer.scala:422)
	dotty.tools.dotc.core.TypeComparer.firstTry$1(TypeComparer.scala:410)
	dotty.tools.dotc.core.TypeComparer.recur(TypeComparer.scala:1469)
	dotty.tools.dotc.core.TypeComparer.isSubType(TypeComparer.scala:208)
	dotty.tools.dotc.core.TypeComparer.isSubType(TypeComparer.scala:218)
	dotty.tools.dotc.core.TypeComparer.topLevelSubType(TypeComparer.scala:128)
	dotty.tools.dotc.core.TypeComparer.testSubType(TypeComparer.scala:144)
	dotty.tools.dotc.core.TypeComparer$.testSubType(TypeComparer.scala:2955)
	dotty.tools.dotc.typer.Typer.adaptNoArgsOther$1(Typer.scala:3980)
	dotty.tools.dotc.typer.Typer.adaptNoArgs$1(Typer.scala:4062)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4268)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3587)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3184)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3188)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3300)
	dotty.tools.dotc.typer.Typer.typedBlock(Typer.scala:1166)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3056)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3112)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3184)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3188)
	dotty.tools.dotc.typer.Typer.typedNew(Typer.scala:910)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3052)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3112)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3184)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3188)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3300)
	dotty.tools.dotc.typer.Typer.typedValDef(Typer.scala:2424)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3021)
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

java.lang.AssertionError: assertion failed: denotation trait Http4sDsl invalid in run 3. ValidFor: Period(1..5, run = 4)