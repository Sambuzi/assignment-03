package pcd.protocol;

import pcd.utils.P2d;
import pcd.utils.V2d;

/**
 * Protocol for Manager actor that controls the simulation.
 * This protocol defines the commands that can be sent to the Manager actor.
 */
public interface ManagerProtocol {

    interface Command {}

    /**
     * Start the simulation.
     */
    class StartSimulation implements Command {
        private final int nBoids;
        private final double width;
        private final double height;

        public StartSimulation(int nBoids, double width, double height) {
            this.nBoids = nBoids;
            this.width = width;
            this.height = height;
        }

        public int nBoids() { return nBoids; }
        public double width() { return width; }
        public double height() { return height; }
    }

    /**
     * Stop the simulation.
     */
    class StopSimulation implements Command {}

    /**
     * Pause the simulation.
     */
    class PauseSimulation implements Command {}

    /**
     * Resume the simulation.
     */
    class ResumeSimulation implements Command {}

    /**
     * Command to update the parameters of the simulation, set by user from GUI.
     */
    class UpdateParams implements Command {
        private final double cohesion;
        private final double alignment;
        private final double separation;

        public UpdateParams(double cohesion, double alignment, double separation) {
            this.cohesion = cohesion;
            this.alignment = alignment;
            this.separation = separation;
        }

        public double cohesion() { return cohesion; }
        public double alignment() { return alignment; }
        public double separation() { return separation; }
    }

    /**
     * Tick command to update the simulation state.
     */
    class Tick implements Command {}

    /**
     * Command to notify that the update for the current tick is completed.
     */
    class UpdateCompleted implements Command {
        private final long tick;

        public UpdateCompleted(long tick) {
            this.tick = tick;
        }

        public long tick() { return tick; }
    }

    /**
     * Command to update the position and velocity of a boid.
     */
    class BoidUpdated implements Command {
        private final P2d position;
        private final V2d velocity;
        private final String boidId;

        public BoidUpdated(P2d position, V2d velocity, String boidId) {
            this.position = position;
            this.velocity = velocity;
            this.boidId = boidId;
        }

        public P2d position() { return position; }
        public V2d velocity() { return velocity; }
        public String boidId() { return boidId; }
    }
}