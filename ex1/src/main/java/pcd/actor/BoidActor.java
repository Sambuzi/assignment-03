package pcd.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import pcd.model.BoidState;
import pcd.protocol.*;
import pcd.utils.*;

import java.util.List;

public class BoidActor {
    private final String boidId;
    private final ActorContext<BoidProtocol.Command> context;
    private final ActorRef<ManagerProtocol.Command> managerActor;
    private P2d position;
    private V2d velocity;
    private BoidsParams params;

    public BoidActor(String boidId, P2d pos, V2d vel, BoidsParams params,
                     ActorRef<ManagerProtocol.Command> managerActor, ActorContext<BoidProtocol.Command> context) {
        this.boidId = boidId;
        this.position = pos;
        this.velocity = vel;
        this.params = params;
        this.context = context;
        this.managerActor = managerActor;
    }

    public static Behavior<BoidProtocol.Command> create(String boidId, P2d pos, V2d vel,
                                                        BoidsParams params,
                                                        ActorRef<ManagerProtocol.Command> managerActor) {
        return Behaviors.setup(ctx -> new BoidActor(boidId, pos, vel, params, managerActor, ctx).behavior());
    }

    private Behavior<BoidProtocol.Command> behavior() {
        return Behaviors.receive(BoidProtocol.Command.class)
            .onMessage(BoidProtocol.UpdateRequest.class, this::onUpdateRequest)
            .onMessage(BoidProtocol.UpdateParams.class, msg -> { this.params = msg.params(); return Behaviors.same(); })
            .onMessage(BoidProtocol.WaitUpdateRequest.class, msg -> Behaviors.same())
            .build();
    }

    private Behavior<BoidProtocol.Command> onUpdateRequest(BoidProtocol.UpdateRequest req) {
        var nearby = req.boids().stream()
            .filter(b -> !b.id().equals(boidId) && position.distance(b.pos()) <= params.getPerceptionRadius())
            .toList();

        V2d alignment = avgVector(nearby, BoidState::vel).sub(velocity).getNormalized();
        P2d avgPos = avgPosition(nearby);
        V2d cohesion = new V2d(avgPos.x() - position.x(), avgPos.y() - position.y()).getNormalized();
        V2d separation = avgSeparation(nearby);

        velocity = velocity
            .sum(alignment.mul(params.getAlignmentWeight()))
            .sum(separation.mul(params.getSeparationWeight()))
            .sum(cohesion.mul(params.getCohesionWeight()));

        if (velocity.abs() > params.getMaxSpeed())
            velocity = velocity.getNormalized().mul(params.getMaxSpeed());

        position = wrap(position.sum(velocity));

        managerActor.tell(new ManagerProtocol.BoidUpdated(position, velocity, boidId));
        context.getSelf().tell(new BoidProtocol.WaitUpdateRequest(req.tick()));
        return Behaviors.same();
    }

    private P2d avgPosition(List<BoidState> boids) {
        if (boids.isEmpty()) return position;
        double x = 0, y = 0;
        for (BoidState b : boids) {
            x += b.pos().x();
            y += b.pos().y();
        }
        return new P2d(x / boids.size(), y / boids.size());
    }

    private V2d avgVector(List<BoidState> boids, java.util.function.Function<BoidState, ? extends V2d> extractor) {
        if (boids.isEmpty()) return new V2d(0, 0);
        double x = 0, y = 0;
        for (BoidState b : boids) {
            V2d v = extractor.apply(b);
            x += v.x(); y += v.y();
        }
        return new V2d(x / boids.size(), y / boids.size());
    }

    private V2d avgSeparation(List<BoidState> boids) {
        double dx = 0, dy = 0; int count = 0;
        for (BoidState b : boids) {
            double dist = position.distance(b.pos());
            if (dist < params.getAvoidRadius()) {
                dx += position.x() - b.pos().x();
                dy += position.y() - b.pos().y();
                count++;
            }
        }
        return count > 0 ? new V2d(dx / count, dy / count).getNormalized() : new V2d(0, 0);
    }

    private P2d wrap(P2d pos) {
        double x = pos.x(), y = pos.y();
        if (x < params.getMinX()) x += params.getWidth();
        if (x >= params.getMaxX()) x -= params.getWidth();
        if (y < params.getMinY()) y += params.getHeight();
        if (y >= params.getMaxY()) y -= params.getHeight();
        return new P2d(x, y);
    }
}