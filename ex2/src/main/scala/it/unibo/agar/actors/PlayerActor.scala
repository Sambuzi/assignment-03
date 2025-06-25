package it.unibo.agar.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.receptionist._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import scala.util.{Success, Failure}
import it.unibo.agar.distributed._
import it.unibo.agar.model._
import it.unibo.agar.view.DistributedLocalView
import it.unibo.agar.GameConfig._

object PlayerActor:
  val PlayerServiceKey: ServiceKey[PlayerActorMessage] = ServiceKey[PlayerActorMessage]("PlayerActor")

  private case class InitializeComplete(player: Player) extends PlayerActorMessage
  private case object RegistrationFailed extends PlayerActorMessage
  private case class ViewUpdateFailed(error: Throwable) extends PlayerActorMessage

  def apply(playerId: String, worldManager: ActorRef[WorldManagerMessage], isAI: Boolean = false): Behavior[PlayerActorMessage] =
    Behaviors.setup { context =>
      Behaviors.withStash(100) { stash =>
        def initializing(retryCount: Int = 0): Behavior[PlayerActorMessage] = {
          context.system.receptionist ! Receptionist.Register(PlayerServiceKey, context.self)
          implicit val timeout: Timeout = ThreeSeconds
          context.ask(worldManager, RegisterPlayer(playerId, _)) {
            case Success(PlayerRegistered(player)) => InitializeComplete(player)
            case _ => RegistrationFailed
          }
          Behaviors.receiveMessage {
            case InitializeComplete(player) =>
              stash.unstashAll(PlayerLogicActors.active(context, player, None, worldManager, isAI))
            case RegistrationFailed if isAI && retryCount < 3 =>
              Behaviors.withTimers { timers =>
                timers.startSingleTimer("retry-registration", RegistrationFailed, TwoSeconds)
                initializing(retryCount + 1)
              }
            case RegistrationFailed =>
              context.system.receptionist ! Receptionist.Deregister(PlayerServiceKey, context.self)
              Behaviors.stopped
            case msg =>
              stash.stash(msg)
              Behaviors.same
          }
        }
        initializing()
      }
    }