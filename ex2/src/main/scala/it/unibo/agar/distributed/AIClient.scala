package it.unibo.agar.distributed

import akka.cluster.typed._
import it.unibo.agar.actors.{GameClusterSupervisorActor, PlayerActor, WorldManagerActor}
import it.unibo.agar.startupWithRole

object AIClient:
  def main(args: Array[String]): Unit =
    val numAI = args.headOption.map(_.toInt).getOrElse(4)

    val system = startupWithRole("ai-client", 0)(GameClusterSupervisorActor())

    val worldManagerProxy = ClusterSingleton(system).init(
      SingletonActor(WorldManagerActor(system.settings.config), "WorldManager")
    )
    
    Thread.sleep(2000) // Wait for the WorldManager to be ready

    (1 to numAI).foreach: i =>
      system.systemActorOf(PlayerActor(s"AI-$i", worldManagerProxy, isAI = true), s"AI-Player-$i")

    // Shutdown hook to gracefully terminate the system
    sys.addShutdownHook {
      system.terminate()
    }

    Thread.currentThread().join()