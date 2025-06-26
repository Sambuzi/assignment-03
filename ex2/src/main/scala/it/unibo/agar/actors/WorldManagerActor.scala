package it.unibo.agar.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.typed._
import com.typesafe.config.Config

import scala.util.{Success, Failure, Random}

import it.unibo.agar.protocol._
import it.unibo.agar.model._
import it.unibo.agar.GameConfig._

object WorldManagerActor:

  def apply(config: Config): Behavior[WorldManagerMessage] =
    Behaviors.supervise(worldManagerBehavior(config))
      .onFailure(SupervisorStrategy.restart)

  private def worldManagerBehavior(config: Config): Behavior[WorldManagerMessage] =
    Behaviors.setup[WorldManagerMessage]: context =>
      import akka.actor.typed.scaladsl.adapter._
      import akka.actor.CoordinatedShutdown

      CoordinatedShutdown(context.system.toClassic).addTask(
        CoordinatedShutdown.PhaseServiceStop,
        "stop-world-manager-timers"
      ) { () =>
        context.self ! ShuttingDown
        scala.concurrent.Future.successful(akka.Done)
      }

      Behaviors.withTimers: timers =>
        var isShuttingDown = false
        val width = WorldWidth
        val height = WorldHeight
        var world = World(width, height, Seq.empty, Seq.empty)
        var registeredPlayers = Set.empty[ActorRef[PlayerActorMessage]]

        timers.startSingleTimer(InitializeFoodManager, TwoSeconds)

        val foodManagerProxy = ClusterSingleton(context.system).init(
          SingletonActor(FoodManagerActor(config), "FoodManager")
        )

        def checkCollisions(player: Player, context: ActorContext[WorldManagerMessage]): Unit =
          world.foods.filter(food => EatingManager.canEatFood(player, food)).foreach: food =>
            context.self ! AteFood(player.id, food.id)
          world.playersExcludingSelf(player)
            .filter(other => EatingManager.canEatPlayer(player, other))
            .foreach: other =>
              context.self ! AtePlayer(player.id, other.id)

        Behaviors.receiveMessage {
          case ShuttingDown =>
            isShuttingDown = true
            timers.cancelAll()
            registeredPlayers.foreach(_ ! PlayerDied("SHUTDOWN"))
            Behaviors.same

          case InitializeFoodManager =>
            timers.startTimerAtFixedRate(Tick, FiftyMillis)
            Behaviors.same

          case RegisterPlayer(playerId, replyTo) =>
            val player = Player(playerId, Random.nextInt(width), Random.nextInt(height), PlayerMass)
            world = world.copy(players = world.players :+ player)
            replyTo ! PlayerRegistered(player)
            Behaviors.same

          case UnregisterPlayer(playerId) =>
            world = world.copy(players = world.players.filterNot(_.id == playerId))
            Behaviors.same

          case GetWorldState(replyTo) =>
            replyTo ! WorldState(world)
            Behaviors.same

          case UpdatePlayerPosition(playerId, x, y) =>
            world.playerById(playerId).foreach { player =>
              val updatedPlayer = player.copy(x = x, y = y)
              world = world.updatePlayer(updatedPlayer)
              checkCollisions(updatedPlayer, context)
            }
            Behaviors.same

          case AteFood(playerId, foodId) =>
            world.playerById(playerId).foreach { player =>
              world.foods.find(_.id == foodId).foreach { food =>
                val grownPlayer = player.grow(food)
                world = world.updatePlayer(grownPlayer).copy(
                  foods = world.foods.filterNot(_.id == foodId)
                )
                foodManagerProxy ! RemoveFood(foodId)
              }
            }
            Behaviors.same

          case AtePlayer(killerId, victimId) =>
            (world.playerById(killerId), world.playerById(victimId)) match {
              case (Some(killer), Some(victim)) =>
                val grownKiller = killer.grow(victim)
                world = world.updatePlayer(grownKiller).removePlayers(Seq(victim))

                implicit val Timeout: akka.util.Timeout = HundredMillis
                context.ask(context.system.receptionist, Receptionist.Find(PlayerActor.PlayerServiceKey, _)) {
                  case Success(listing: Receptionist.Listing) =>
                    listing.serviceInstances(PlayerActor.PlayerServiceKey).foreach { playerRef =>
                      val pathname = playerRef.path.name
                      val isTargetPlayer = if (victimId.startsWith("AI-")) {
                        val aiNumber = victimId.substring(3)
                        pathname == s"AI-Player-$aiNumber"
                      } else {
                        pathname == s"Player-$victimId"
                      }
                      if (isTargetPlayer) playerRef ! PlayerDied(victimId)
                    }
                    NoOp
                  case _ => NoOp
                }
                Behaviors.same
              case _ => Behaviors.same
            }

          case Tick if !isShuttingDown =>
            implicit val timeout: akka.util.Timeout = HundredMillis
            context.ask(foodManagerProxy, GetAllFood.apply) {
              case Success(FoodList(foods)) => UpdateFood(foods)
              case Failure(_)               => NoOp
            }

            context.ask(context.system.receptionist, Receptionist.Find(PlayerActor.PlayerServiceKey, _)) {
              case Success(listing: Receptionist.Listing) =>
                val activePlayers = listing.serviceInstances(PlayerActor.PlayerServiceKey)
                registeredPlayers = activePlayers
                activePlayers.foreach { actorRef =>
                  val pathName = actorRef.path.name
                  val playerStillExist = world.players.exists { p =>
                    if (p.id.startsWith("AI-")) {
                      val aiNumber = p.id.substring(3)
                      pathName == s"AI-Player-$aiNumber"
                    } else {
                      pathName == s"Player-${p.id}"
                    }
                  }
                  if (playerStillExist) actorRef ! UpdateView(world)
                }
                PlayersUpdate(activePlayers)
              case _ => NoOp
            }
            Behaviors.same

          case UpdateFood(foods) =>
            world = world.copy(foods = foods)
            Behaviors.same

          case PlayersUpdate(players) =>
            registeredPlayers = players
            Behaviors.same

          case NoOp =>
            Behaviors.same

          case _ =>
            // Caso di default per evitare warning di match non esaustivo
            Behaviors.same
        }
