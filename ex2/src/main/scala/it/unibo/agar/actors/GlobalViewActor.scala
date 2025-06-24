package it.unibo.agar.actors

import akka.actor.typed.*
import akka.actor.typed.scaladsl.*
import akka.util.Timeout
import it.unibo.agar.distributed.*
import it.unibo.agar.model.World
import it.unibo.agar.view.GlobalViewFrame
import it.unibo.agar.GameConfig._

import scala.util.{Failure, Success}

object GlobalViewActor:
  trait Command

  private case class UpdateWorld(world: World) extends Command

  private case object RequestUpdate extends Command

  def apply(worldManager: ActorRef[WorldManagerMessage]): Behavior[Command] =
    Behaviors.setup: context =>
      val view = new GlobalViewFrame()

      Behaviors.withTimers: timers =>
        timers.startTimerAtFixedRate(RequestUpdate, ThirtyMillis)

        implicit val timeout: Timeout = Timeout(ThreeSeconds)

        Behaviors.receiveMessage:
          case UpdateWorld(world) =>
            view.updateWorld(world)
            Behaviors.same

          case RequestUpdate =>
            context.ask(worldManager, GetWorldState.apply):
              case Success(WorldState(world)) =>
                UpdateWorld(world)
              case Failure(_) =>
                UpdateWorld(World(WorldWidth, WorldHeight, Seq.empty, Seq.empty))
            Behaviors.same