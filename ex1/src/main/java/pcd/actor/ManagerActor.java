package pcd.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import pcd.model.*;
import pcd.protocol.*;
import pcd.utils.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ManagerActor {
    private static final int FPS = 60;
    private static final int TICK_MS = 40;
    private static final AtomicInteger VERSION = new AtomicInteger(0);

    private final ActorContext<ManagerProtocol.Command> ctx;
    private final TimerScheduler<ManagerProtocol.Command> timers;
    private ActorRef<GUIProtocol.Command> guiActor;
    private final Map<String, BoidState> boidStates = new HashMap<>();
    private final Map<String, ActorRef<BoidProtocol.Command>> boidRefs = new HashMap<>();
    private int completedBoids = 0;
    private final BoidsParams params = new BoidsParams(800, 800);
    private long tickCount = 0;
    private long lastFrameTime = 0;

    private ManagerActor(ActorContext<ManagerProtocol.Command> ctx, TimerScheduler<ManagerProtocol.Command> timers) {
        this.ctx = ctx;
        this.timers = timers;
    }

    public static Behavior<ManagerProtocol.Command> create() {
        return Behaviors.setup(ctx ->
            Behaviors.withTimers(timers ->
                Behaviors.supervise(new ManagerActor(ctx, timers).idle())
                        .onFailure(SupervisorStrategy.restart())
            )
        );
    }

    // Stato iniziale: attende l'avvio della simulazione
    private Behavior<ManagerProtocol.Command> idle() {
        guiActor = ctx.spawn(
            Behaviors.supervise(SimulationUIActor.create(params, ctx.getSelf()))
                    .onFailure(SupervisorStrategy.restart()),
            "gui-actor" + VERSION.getAndIncrement()
        );
        return Behaviors.receive(ManagerProtocol.Command.class)
            .onMessage(ManagerProtocol.StartSimulation.class, this::onStart)
            .build();
    }

    // Avvia la simulazione: crea boid, aggiorna GUI, avvia timer
    private Behavior<ManagerProtocol.Command> onStart(ManagerProtocol.StartSimulation cmd) {
        boidStates.clear();
        boidRefs.clear();
        completedBoids = 0;
        tickCount = 0;
        lastFrameTime = 0;

        for (int i = 0; i < cmd.nBoids(); i++) {
            String id = "boid-" + i;
            P2d pos = new P2d((Math.random() - 0.5) * cmd.width(), (Math.random() - 0.5) * cmd.height());
            V2d vel = new V2d((Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2);
            ActorRef<BoidProtocol.Command> ref = ctx.spawn(
                Behaviors.supervise(BoidActor.create(id, pos, vel, params, ctx.getSelf()))
                        .onFailure(SupervisorStrategy.restartWithBackoff(
                                Duration.ofMillis(100), Duration.ofSeconds(1), 0.2f)
                                .withResetBackoffAfter(Duration.ofSeconds(10))),
                id + "-" + System.currentTimeMillis()
            );
            boidRefs.put(id, ref);
            boidStates.put(id, new BoidState(pos, vel, id));
        }

        guiActor.tell(new GUIProtocol.UpdateWeights(
            params.getSeparationWeight(), params.getAlignmentWeight(), params.getCohesionWeight()));
        guiActor.tell(new GUIProtocol.RenderFrame(
            List.copyOf(boidStates.values()), new BoidsMetrics(cmd.nBoids(), FPS, TICK_MS)));
        guiActor.tell(new GUIProtocol.UpdateStatus(GUIProtocol.SimulationStatus.RUNNING));

        timers.startTimerAtFixedRate(new ManagerProtocol.Tick(), Duration.ofMillis(TICK_MS));
        return running();
    }

    // Stato running: gestisce tick, update, pause, stop, parametri
    private Behavior<ManagerProtocol.Command> running() {
        return Behaviors.receive(ManagerProtocol.Command.class)
            .onMessage(ManagerProtocol.Tick.class, this::onTick)
            .onMessage(ManagerProtocol.BoidUpdated.class, this::onBoidUpdated)
            .onMessage(ManagerProtocol.UpdateCompleted.class, this::onUpdateCompleted)
            .onMessage(ManagerProtocol.UpdateParams.class, this::onUpdateParams)
            .onMessage(ManagerProtocol.PauseSimulation.class, this::onPause)
            .onMessage(ManagerProtocol.ResumeSimulation.class, this::onResume)
            .onMessage(ManagerProtocol.StopSimulation.class, this::onStop)
            .build();
    }

    private Behavior<ManagerProtocol.Command> onTick(ManagerProtocol.Tick tick) {
        tickCount++;
        if (completedBoids == 0) {
            List<BoidState> snapshot = List.copyOf(boidStates.values());
            boidRefs.values().forEach(ref -> ref.tell(new BoidProtocol.UpdateRequest(tickCount, snapshot)));
        }
        return Behaviors.same();
    }

    private Behavior<ManagerProtocol.Command> onBoidUpdated(ManagerProtocol.BoidUpdated msg) {
        boidStates.put(msg.boidId(), new BoidState(msg.position(), msg.velocity(), msg.boidId()));
        completedBoids++;
        if (completedBoids >= boidRefs.size()) {
            ctx.getSelf().tell(new ManagerProtocol.UpdateCompleted(tickCount));
        }
        return Behaviors.same();
    }

    private Behavior<ManagerProtocol.Command> onUpdateCompleted(ManagerProtocol.UpdateCompleted cmd) {
        long now = System.currentTimeMillis();
        int fps = (lastFrameTime != 0) ? (int) Math.min(1000.0 / (now - lastFrameTime), FPS) : 0;
        lastFrameTime = now;
        if (guiActor != null) {
            guiActor.tell(new GUIProtocol.RenderFrame(
                List.copyOf(boidStates.values()),
                new BoidsMetrics(boidRefs.size(), fps, now - cmd.tick())
            ));
        }
        completedBoids = 0;
        return Behaviors.same();
    }

    private Behavior<ManagerProtocol.Command> onUpdateParams(ManagerProtocol.UpdateParams msg) {
        params.setAlignmentWeight(msg.alignment());
        params.setCohesionWeight(msg.cohesion());
        params.setSeparationWeight(msg.separation());
        boidRefs.values().forEach(ref -> ref.tell(new BoidProtocol.UpdateParams(params)));
        if (guiActor != null) {
            guiActor.tell(new GUIProtocol.UpdateWeights(
                params.getSeparationWeight(), params.getAlignmentWeight(), params.getCohesionWeight()));
            guiActor.tell(new GUIProtocol.ConfirmParamsUpdate());
        }
        return Behaviors.same();
    }

    private Behavior<ManagerProtocol.Command> onPause(ManagerProtocol.PauseSimulation msg) {
        timers.cancel(new ManagerProtocol.Tick());
        if (guiActor != null) {
            guiActor.tell(new GUIProtocol.ConfirmPause());
            guiActor.tell(new GUIProtocol.UpdateStatus(GUIProtocol.SimulationStatus.PAUSED)); // <--- AGGIUNGI QUESTA RIGA
        }
        return paused();
    }
    
    private Behavior<ManagerProtocol.Command> paused() {
        return Behaviors.receive(ManagerProtocol.Command.class)
            .onMessage(ManagerProtocol.ResumeSimulation.class, this::onResume)
            .onMessage(ManagerProtocol.StopSimulation.class, this::onStop)
            .onMessage(ManagerProtocol.UpdateParams.class, this::onUpdateParams)
            .onMessage(ManagerProtocol.BoidUpdated.class, this::onBoidUpdated)
            .build();
    }

    private Behavior<ManagerProtocol.Command> onResume(ManagerProtocol.ResumeSimulation msg) {
        completedBoids = 0;
        timers.startTimerAtFixedRate(new ManagerProtocol.Tick(), Duration.ofMillis(TICK_MS));
        if (guiActor != null) {
            guiActor.tell(new GUIProtocol.ConfirmResume());
            guiActor.tell(new GUIProtocol.UpdateStatus(GUIProtocol.SimulationStatus.RUNNING)); // <--- AGGIUNGI QUESTA RIGA
        }
        return running();
    }

    private Behavior<ManagerProtocol.Command> onStop(ManagerProtocol.StopSimulation msg) {
        timers.cancelAll();
        if (guiActor != null) {
            guiActor.tell(new GUIProtocol.UpdateStatus(GUIProtocol.SimulationStatus.STOPPED));
            // Invia la conferma di stop DOPO 2 secondi
            ctx.scheduleOnce(
                java.time.Duration.ofSeconds(2),
                guiActor,
                new GUIProtocol.ConfirmStop()
            );
        }
        boidRefs.values().forEach(ctx::stop);
        return idle();
    }
}