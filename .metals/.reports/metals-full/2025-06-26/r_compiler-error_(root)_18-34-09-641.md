file:///C:/assignment-03/ex2/src/main/scala/it/unibo/agar/actors/PlayerLogicActors.scala
### java.lang.AssertionError: assertion failed

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
uri: file:///C:/assignment-03/ex2/src/main/scala/it/unibo/agar/actors/PlayerLogicActors.scala
text:
```scala
package it.unibo.agar.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.util.Timeout
import scala.util.{Success, Failure}
import it.unibo.agar.protocol._
import it.unibo.agar.model._
import it.unibo.agar.view.DistributedLocalView
import it.unibo.agar.GameConfig._

object PlayerLogicActors:
  private case class ViewUpdateFailed(error: Throwable) extends PlayerActorMessage
  private case class InitializeComplete(player: Player) extends PlayerActorMessage
  private case object RegistrationFailed extends PlayerActorMessage

  def active(
    context: ActorContext[PlayerActorMessage],
    currentPlayer: Player,
    localView: Option[DistributedLocalView],
    worldManager: ActorRef[WorldManagerMessage],
    isAI: Boolean
  ): Behavior[PlayerActorMessage] = {
    val view = localView.orElse {
      if !isAI then
        val v = new DistributedLocalView(currentPlayer.id)
        v.setPlayerActor(context.self)
        Some(v)
      else None
    }
    if isAI then
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate("ai-movement", StartAI, HundredMillis)
        handleMessages(context, currentPlayer, view, worldManager, Some(timers), isAI)
      }
    else
      handleMessages(context, currentPlayer, view, worldManager, None, isAI)
  }

  private def handleMessages(
    context: ActorContext[PlayerActorMessage],
    currentPlayer: Player,
    localView: Option[DistributedLocalView],
    worldManager: ActorRef[WorldManagerMessage],
    timers: Option[TimerScheduler[PlayerActorMessage]],
    isAI: Boolean
  ): Behavior[PlayerActorMessage] =
    Behaviors.receiveMessage {
      case MoveDirection(dx, dy) =>
        val speed = DefaultSpeed
        val newX = (currentPlayer.x + dx * speed).max(0).min(WorldWidth)
        val newY = (currentPlayer.y + dy * speed).max(0).min(WorldHeight)
        worldManager ! UpdatePlayerPosition(currentPlayer.id, newX, newY)
        handleMessages(context, currentPlayer.copy(x = newX, y = newY), localView, worldManager, timers, isAI)

      case UpdateView(world) =>
        localView.foreach(_.updateWorld(world))
        val updatedPlayer = world.playerById(currentPlayer.id).getOrElse(currentPlayer)
        handleMessages(context, updatedPlayer, localView, worldManager, timers, isAI)

      case PlayerDied(id) if id == currentPlayer.id =>
        if isAI then timers.foreach(_.cancel("ai-movement"))
        if isAI then
          Behaviors.withTimers { respawnTimers =>
            respawnTimers.startSingleTimer("respawn", Respawn, ThreeSeconds)
            waitingForRespawn(context, localView, worldManager, isAI)
          }
        else
          localView.foreach(_.showRespawnDialog())
          waitingForRespawn(context, localView, worldManager, isAI)

      case StartAI =>
        implicit val timeout: Timeout = Timeout(ThreeSeconds)
        context.ask(worldManager, GetWorldState.apply) {
          case Success(WorldState(world)) =>
            AIMovement.nearestFood(currentPlayer.id, world).foreach { food =>
              val dx = food.x - currentPlayer.x
              val dy = food.y - currentPlayer.y
              val dist = math.hypot(dx, dy)
              if dist > 0 then context.self ! MoveDirection(dx / dist, dy / dist)
            }
            UpdateView(world)
          case Failure(_) => ViewUpdateFailed(new Exception("AI update failed"))
        }
        Behaviors.same

      case ViewUpdateFailed(_) => Behaviors.same
      case InitializeComplete(_) | RegistrationFailed => Behaviors.same
      case PlayerDied(_) => Behaviors.same
      case Respawn => Behaviors.same
    }

  private def waitingForRespawn(
    context: ActorContext[PlayerActorMessage],
    localView: Option[DistributedLocalView],
    worldManager: ActorRef[WorldManagerMessage],
    isAI: Boolean
  ): Behavior[PlayerActorMessage] =
    Behaviors.withTimers { timers =>
      Behaviors.receiveMessage {
        case Respawn =>
          implicit val timeout: Timeout = Timeout(ThreeSeconds)
          context.ask(worldManager, RegisterPlayer(localView.map(_.getPlayerId).getOrElse(""), _)) {
            case Success(PlayerRegistered(player)) =>
              localView.foreach(_.setActive(true))
              InitializeComplete(player)
            case _ =>
              if isAI then timers.startSingleTimer("retry-registration", Respawn, ThreeSeconds)
              RegistrationFailed
          }
          Behaviors.same
        case InitializeComplete(player) =>
          active(context, player, localView, worldManager, isAI)
        case UpdateView(world) =>
          localView.foreach(_.updateWorld(world))
          Behaviors.same
        case PlayerDied(_) => Behaviors.same
        case StartAI => Behaviors.same
        case MoveDirection(_, _) => Behaviors.same
        case _ => Behaviors.same
      }
    }
```



#### Error stacktrace:

```
scala.runtime.Scala3RunTime$.assertFailed(Scala3RunTime.scala:11)
	dotty.tools.dotc.core.Annotations$LazyAnnotation.tree(Annotations.scala:139)
	dotty.tools.dotc.core.Annotations$Annotation$Child$.unapply(Annotations.scala:245)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:476)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.addChild(Namer.scala:487)
	dotty.tools.dotc.typer.Namer$Completer.register$1(Namer.scala:931)
	dotty.tools.dotc.typer.Namer$Completer.registerIfChildInCreationContext$$anonfun$1(Namer.scala:940)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:334)
	dotty.tools.dotc.typer.Namer$Completer.registerIfChildInCreationContext(Namer.scala:940)
	dotty.tools.dotc.typer.Namer$Completer.complete(Namer.scala:829)
	dotty.tools.dotc.core.SymDenotations$SymDenotation.completeFrom(SymDenotations.scala:174)
	dotty.tools.dotc.core.Denotations$Denotation.completeInfo$1(Denotations.scala:188)
	dotty.tools.dotc.core.Denotations$Denotation.info(Denotations.scala:190)
	dotty.tools.dotc.core.Types$NamedType.info(Types.scala:2363)
	dotty.tools.dotc.core.Types$TermLambda.dotty$tools$dotc$core$Types$TermLambda$$_$compute$1(Types.scala:3896)
	dotty.tools.dotc.core.Types$TermLambda.foldArgs$2(Types.scala:3903)
	dotty.tools.dotc.core.Types$TermLambda.dotty$tools$dotc$core$Types$TermLambda$$_$compute$1(Types.scala:4524)
	dotty.tools.dotc.core.Types$TermLambda.dotty$tools$dotc$core$Types$TermLambda$$depStatus(Types.scala:3923)
	dotty.tools.dotc.core.Types$TermLambda.dependencyStatus(Types.scala:3937)
	dotty.tools.dotc.core.Types$TermLambda.isResultDependent(Types.scala:3959)
	dotty.tools.dotc.core.Types$TermLambda.isResultDependent$(Types.scala:3853)
	dotty.tools.dotc.core.Types$MethodType.isResultDependent(Types.scala:3998)
	dotty.tools.dotc.typer.TypeAssigner.assignType(TypeAssigner.scala:295)
	dotty.tools.dotc.typer.TypeAssigner.assignType$(TypeAssigner.scala:16)
	dotty.tools.dotc.typer.Typer.assignType(Typer.scala:119)
	dotty.tools.dotc.ast.tpd$.Apply(tpd.scala:48)
	dotty.tools.dotc.ast.tpd$TreeOps$.appliedToTermArgs$extension(tpd.scala:966)
	dotty.tools.dotc.ast.tpd$.New(tpd.scala:544)
	dotty.tools.dotc.ast.tpd$.New(tpd.scala:535)
	dotty.tools.dotc.core.Annotations$Annotation$Child$.makeChildLater$1(Annotations.scala:234)
	dotty.tools.dotc.core.Annotations$Annotation$Child$.later$$anonfun$1(Annotations.scala:237)
	dotty.tools.dotc.core.Annotations$LazyAnnotation.tree(Annotations.scala:143)
	dotty.tools.dotc.core.Annotations$Annotation$Child$.unapply(Annotations.scala:245)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:476)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.addChild(Namer.scala:487)
	dotty.tools.dotc.typer.Namer$Completer.register$1(Namer.scala:931)
	dotty.tools.dotc.typer.Namer$Completer.registerIfChildInCreationContext$$anonfun$1(Namer.scala:940)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:334)
	dotty.tools.dotc.typer.Namer$Completer.registerIfChildInCreationContext(Namer.scala:940)
	dotty.tools.dotc.typer.Namer$Completer.complete(Namer.scala:829)
	dotty.tools.dotc.core.SymDenotations$SymDenotation.completeFrom(SymDenotations.scala:174)
	dotty.tools.dotc.core.Denotations$Denotation.completeInfo$1(Denotations.scala:188)
	dotty.tools.dotc.core.Denotations$Denotation.info(Denotations.scala:190)
	dotty.tools.dotc.core.SymDenotations$SymDenotation.ensureCompleted(SymDenotations.scala:392)
	dotty.tools.dotc.typer.Typer.retrieveSym(Typer.scala:3092)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3117)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3231)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3302)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3306)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3328)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3374)
	dotty.tools.dotc.typer.Typer.typedClassDef(Typer.scala:2771)
	dotty.tools.dotc.typer.Typer.typedTypeOrClassDef$1(Typer.scala:3139)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3143)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3231)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3302)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3306)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3328)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3374)
	dotty.tools.dotc.typer.Typer.typedPackageDef(Typer.scala:2914)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3184)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3232)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3302)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3306)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3417)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$1(TyperPhase.scala:45)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	dotty.tools.dotc.core.Phases$Phase.monitor(Phases.scala:467)
	dotty.tools.dotc.typer.TyperPhase.typeCheck(TyperPhase.scala:51)
	dotty.tools.dotc.typer.TyperPhase.$anonfun$4(TyperPhase.scala:97)
	scala.collection.Iterator$$anon$6.hasNext(Iterator.scala:479)
	scala.collection.Iterator$$anon$9.hasNext(Iterator.scala:583)
	scala.collection.immutable.List.prependedAll(List.scala:152)
	scala.collection.immutable.List$.from(List.scala:685)
	scala.collection.immutable.List$.from(List.scala:682)
	scala.collection.IterableOps$WithFilter.map(Iterable.scala:900)
	dotty.tools.dotc.typer.TyperPhase.runOn(TyperPhase.scala:96)
	dotty.tools.dotc.Run.runPhases$1$$anonfun$1(Run.scala:315)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.ArrayOps$.foreach$extension(ArrayOps.scala:1324)
	dotty.tools.dotc.Run.runPhases$1(Run.scala:308)
	dotty.tools.dotc.Run.compileUnits$$anonfun$1(Run.scala:348)
	dotty.tools.dotc.Run.compileUnits$$anonfun$adapted$1(Run.scala:357)
	dotty.tools.dotc.util.Stats$.maybeMonitored(Stats.scala:69)
	dotty.tools.dotc.Run.compileUnits(Run.scala:357)
	dotty.tools.dotc.Run.compileSources(Run.scala:261)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:161)
	dotty.tools.pc.CachingDriver.run(CachingDriver.scala:45)
	dotty.tools.pc.WithCompilationUnit.<init>(WithCompilationUnit.scala:31)
	dotty.tools.pc.SimpleCollector.<init>(PcCollector.scala:351)
	dotty.tools.pc.PcSemanticTokensProvider$Collector$.<init>(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector$lzyINIT1(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.provide(PcSemanticTokensProvider.scala:88)
	dotty.tools.pc.ScalaPresentationCompiler.semanticTokens$$anonfun$1(ScalaPresentationCompiler.scala:111)
```
#### Short summary: 

java.lang.AssertionError: assertion failed