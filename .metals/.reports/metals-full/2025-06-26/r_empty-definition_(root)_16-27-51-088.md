error id: file:///C:/assignment-03/ex2/src/main/scala/it/unibo/agar/actors/WorldManagerActor.scala:`<none>`.
file:///C:/assignment-03/ex2/src/main/scala/it/unibo/agar/actors/WorldManagerActor.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 274
uri: file:///C:/assignment-03/ex2/src/main/scala/it/unibo/agar/actors/WorldManagerActor.scala
text:
```scala
package it.unibo.agar.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.typed._
import com.typesafe.config.Config
import it.unibo.agar.model._
import it.unibo.agar.distributed._@@
import it.unibo.agar.GameConfig._
import it.unibo.agar.actors.WorldManagerInternal._
import scala.util.{Success, Failure, Random}

object WorldManagerActor:

  def apply(config: Config): Behavior[WorldManagerMessage] =
    Behaviors.supervise(behavior(config)).onFailure(SupervisorStrategy.restart)

  private def behavior(config: Config): Behavior[WorldManagerMessage]: Behavior[WorldManagerMessage] =
    Behaviors.setup { ctx =>
      import akka.actor.typed.scaladsl.adapter._
      import akka.actor.CoordinatedShutdown

      val system = ctx.system
      val timers = ctx.timers

      var isShuttingDown = false
      var world = World(WorldWidth, WorldHeight, Nil, Nil)
      var playerRefs = Set.empty[ActorRef[PlayerActorMessage]]
      val foodManager = ClusterSingleton(system).init(SingletonActor(FoodManagerActor(config), "FoodManager"))

      CoordinatedShutdown(system.toClassic).addTask(
        CoordinatedShutdown.PhaseServiceStop, "stop-world-manager"
      ) { () => ctx.self ! ShuttingDown; scala.concurrent.Future.successful(akka.Done) }

      timers.startSingleTimer(InitFoodManager, TwoSeconds)

      def updateView(): Unit =
        ctx.ask(system.receptionist, Receptionist.Find(PlayerActor.PlayerServiceKey, _)) {
          case Success(listing) =>
            val refs = listing.serviceInstances(PlayerActor.PlayerServiceKey)
            playerRefs = refs
            refs.foreach { ref =>
              if world.players.exists(p => ref.path.name.endsWith(p.id)) then
                ref ! UpdateView(world)
            }
            ctx.self ! PlayerRefsUpdate(refs)
          case _ => ctx.self ! NoOp
        }

      def handleCollision(p: Player): Unit =
        world.foods.filter(EatingManager.canEatFood(p, _)).foreach(f => ctx.self ! AteFood(p.id, f.id))
        world.playersExcludingSelf(p).filter(EatingManager.canEatPlayer(p, _)).foreach(v => ctx.self ! AtePlayer(p.id, v.id))

      Behaviors.receiveMessage {
        case ShuttingDown =>
          isShuttingDown = true
          timers.cancelAll()
          playerRefs.foreach(_ ! PlayerDied("SHUTDOWN"))
          Behaviors.same

        case InitFoodManager =>
          timers.startTimerAtFixedRate(Tick, FiftyMillis)
          Behaviors.same

        case RegisterPlayer(id, replyTo) =>
          val player = Player(id, Random.nextInt(WorldWidth), Random.nextInt(WorldHeight), PlayerMass)
          world = world.copy(players = player +: world.players)
          replyTo ! PlayerRegistered(player)
          Behaviors.same

        case UnregisterPlayer(id) =>
          world = world.removePlayersById(Seq(id))
          Behaviors.same

        case GetWorldState(replyTo) =>
          replyTo ! WorldState(world)
          Behaviors.same

        case UpdatePlayerPosition(id, x, y) =>
          world.playerById(id).foreach { p =>
            val moved = p.copy(x = x, y = y)
            world = world.updatePlayer(moved)
            handleCollision(moved)
          }
          Behaviors.same

        case AteFood(pid, fid) =>
          for
            p <- world.playerById(pid)
            f <- world.foods.find(_.id == fid)
          do
            world = world.updatePlayer(p.grow(f)).removeFoodById(fid)
            foodManager ! RemoveFood(fid)
          Behaviors.same

        case AtePlayer(kid, vid) =>
          for
            k <- world.playerById(kid)
            v <- world.playerById(vid)
          do
            world = world.updatePlayer(k.grow(v)).removePlayersById(Seq(vid))
            playerRefs.collect {
              case ref if ref.path.name.endsWith(vid) => ref
            }.foreach(_ ! PlayerDied(vid))
          Behaviors.same

        case Tick if !isShuttingDown =>
          ctx.ask(foodManager, GetAllFood.apply) {
            case Success(FoodList(fs)) => UpdateFoodList(fs)
            case _ => NoOp
          }
          updateView()
          Behaviors.same

        case UpdateFoodList(fs) =>
          world = world.copy(foods = fs)
          Behaviors.same

        case PlayerRefsUpdate(refs) =>
          playerRefs = refs
          Behaviors.same

        case NoOp => Behaviors.same

        case _ => Behaviors.same
      }
    }
```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.