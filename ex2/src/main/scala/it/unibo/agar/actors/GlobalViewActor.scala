package it.unibo.agar.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.util.Timeout
import it.unibo.agar.distributed._
import it.unibo.agar.model.World
import it.unibo.agar.view.GlobalViewFrame
import it.unibo.agar.GameConfig._
import scala.util.{Failure, Success}

/**
 * Actor that periodically requests the world state and updates the global view.
 */
object GlobalViewActor:
  sealed trait Command
  private case class UpdateWorld(world: World) extends Command
  private case object RequestUpdate extends Command

  def apply(worldManager: ActorRef[WorldManagerMessage]): Behavior[Command] =
    Behaviors.setup { ctx =>
      val view = new GlobalViewFrame()
      implicit val timeout: Timeout = Timeout(ThreeSeconds)

      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(RequestUpdate, ThirtyMillis)

        Behaviors.receiveMessage {
          case UpdateWorld(world) =>
            view.updateWorld(world)
            Behaviors.same

          case RequestUpdate =>
            ctx.ask(worldManager, GetWorldState.apply) {
              case Success(WorldState(world)) => UpdateWorld(world)
              case _ => UpdateWorld(World(WorldWidth, WorldHeight, Seq.empty, Seq.empty))
            }
            Behaviors.same
        }
      }
    }