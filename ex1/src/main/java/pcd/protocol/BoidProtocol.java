package pcd.protocol;

import pcd.actor.BoidsParams;
import pcd.model.*;

import java.util.List;

public interface BoidProtocol {

    interface Command {}

    // UpdateRequest
    class UpdateRequest implements Command {
        private final long tick;
        private final List<BoidState> boids;

        public UpdateRequest(long tick, List<BoidState> boids) {
            this.tick = tick;
            this.boids = boids;
        }

        public long tick() { return tick; }
        public List<BoidState> boids() { return boids; }
    }

    // WaitUpdateRequest
    class WaitUpdateRequest implements Command {
        private final long tick;

        public WaitUpdateRequest(long tick) {
            this.tick = tick;
        }

        public long tick() { return tick; }
    }

    // UpdateParams
    class UpdateParams implements Command {
        private final BoidsParams params;

        public UpdateParams(BoidsParams params) {
            this.params = params;
        }

        public BoidsParams params() { return params; }
    }
}