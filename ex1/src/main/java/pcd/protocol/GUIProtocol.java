package pcd.protocol;

import pcd.model.BoidState;
import pcd.model.BoidsMetrics;

import java.util.List;

public interface GUIProtocol {

    enum SimulationStatus {
        STARTING("Starting..."),
        RUNNING("Running"),
        PAUSED("Paused"),
        STOPPED("Stopped"),
        RESUMED("Running - Resumed");

        private final String displayText;

        SimulationStatus(String displayText) {
            this.displayText = displayText;
        }

        public String getDisplayText() {
            return displayText;
        }
    }

    interface Command {}

    // RenderFrame
    class RenderFrame implements Command {
        private final List<BoidState> boids;
        private final BoidsMetrics metrics;

        public RenderFrame(List<BoidState> boids, BoidsMetrics metrics) {
            this.boids = boids;
            this.metrics = metrics;
        }

        public List<BoidState> boids() { return boids; }
        public BoidsMetrics metrics() { return metrics; }
    }

    // UpdateWeights
    class UpdateWeights implements Command {
        private final double separationWeight;
        private final double alignmentWeight;
        private final double cohesionWeight;

        public UpdateWeights(double separationWeight, double alignmentWeight, double cohesionWeight) {
            this.separationWeight = separationWeight;
            this.alignmentWeight = alignmentWeight;
            this.cohesionWeight = cohesionWeight;
        }

        public double separationWeight() { return separationWeight; }
        public double alignmentWeight() { return alignmentWeight; }
        public double cohesionWeight() { return cohesionWeight; }
    }

    // ConfirmPause
    class ConfirmPause implements Command {}

    // ConfirmResume
    class ConfirmResume implements Command {}

    // ConfirmStop
    class ConfirmStop implements Command {}

    // ConfirmParamsUpdate
    class ConfirmParamsUpdate implements Command {}

    // UpdateStatus
    class UpdateStatus implements Command {
        private final SimulationStatus status;

        public UpdateStatus(SimulationStatus status) {
            this.status = status;
        }

        public SimulationStatus status() { return status; }
    }
}