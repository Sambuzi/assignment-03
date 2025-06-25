package it.unibo.agar.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.cluster.typed._
import akka.cluster.ClusterEvent._
import scala.concurrent.duration._

/**
 * Supervises the Akka Cluster, manages singleton actors for world and food,
 * and logs cluster membership and reachability events.
 */
object GameClusterSupervisorActor:

  // Internal protocol for cluster events
  private sealed trait ClusterCommand
  private case class MemberEvt(e: MemberEvent) extends ClusterCommand
  private case class ReachEvt(e: ReachabilityEvent) extends ClusterCommand

  /** Helper to start a singleton actor */
  private def startSingleton[T](ctx: ActorContext[_], behavior: Behavior[T], name: String): Unit =
    ClusterSingleton(ctx.system).init(
      SingletonActor(Behaviors.supervise(behavior).onFailure[Exception](SupervisorStrategy.restart), name)
    )

  /**
   * Entry point for the supervisor actor.
   * Restarts on failure, up to 10 times in 30 seconds.
   */
  def apply(): Behavior[Nothing] =
    Behaviors
      .supervise(clusterBehavior())
      .onFailure(SupervisorStrategy.restart.withLimit(10, 30.seconds))
      .narrow

  /**
   * Main cluster behavior: initializes singletons and subscribes to cluster events.
   */
  private def clusterBehavior(): Behavior[ClusterCommand] =
    Behaviors.setup { ctx =>
      val cluster = Cluster(ctx.system)

      // Start singleton actors if this node has the "server" role
      if cluster.selfMember.roles.contains("server") then
        startSingleton(ctx, WorldManagerActor(ctx.system.settings.config), "WorldManager")
        startSingleton(ctx, FoodManagerActor(ctx.system.settings.config), "FoodManager")

      // Subscribe to cluster events with adapters
      cluster.subscriptions ! Subscribe(ctx.messageAdapter[MemberEvent](MemberEvt.apply), classOf[MemberEvent])
      cluster.subscriptions ! Subscribe(ctx.messageAdapter[ReachabilityEvent](ReachEvt.apply), classOf[ReachabilityEvent])

      Behaviors.receiveMessage {
        case MemberEvt(MemberUp(m)) =>
          ctx.log.info(s"Member is Up: ${m.address}"); Behaviors.same
        case MemberEvt(MemberRemoved(m, prev)) =>
          ctx.log.info(s"Member Removed: ${m.address} after $prev"); Behaviors.same
        case MemberEvt(_) => Behaviors.same
        case ReachEvt(UnreachableMember(m)) =>
          ctx.log.info(s"Member unreachable: ${m.address}"); Behaviors.same
        case ReachEvt(ReachableMember(m)) =>
          ctx.log.info(s"Member reachable again: ${m.address}"); Behaviors.same
      }
    }