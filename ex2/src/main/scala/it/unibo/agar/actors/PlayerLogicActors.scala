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