package pcd;

import akka.actor.typed.ActorSystem;
import pcd.actor.ManagerActor;
import pcd.protocol.ManagerProtocol;

public class BoidsSimulation {
    public static void main(String[] args) {
        ActorSystem<ManagerProtocol.Command> system =
                ActorSystem.create(ManagerActor.create(), "boids-system");

        system.getWhenTerminated().toCompletableFuture().join();
    }
}