package pcd.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import pcd.protocol.*;
import pcd.view.*;

import javax.swing.*;
import javax.swing.event.*;

public class SimulationUIActor implements ChangeListener {
    private final ActorRef<ManagerProtocol.Command> managerActor;
    private final BoidsParams boidsParams;

    private BoidsGUI gui;
    private boolean isPaused = false;
    private boolean isRunning = false;
    private boolean waitingForConfirmation = false;

    private SimulationUIActor(BoidsParams boidsParams, ActorRef<ManagerProtocol.Command> managerActor) {
        this.boidsParams = boidsParams;
        this.managerActor = managerActor;
        SwingUtilities.invokeLater(this::createMainGUI);
    }

private void createMainGUI() {
    if (gui == null) {
        gui = new BoidsGUI(
            (int) boidsParams.getWidth(),
            (int) boidsParams.getHeight(),
            500, // valore di default
            this::onStartSimulation,
            action -> onPauseResume(),
            action -> onStop(),
            this
        );
    } else {
        SwingUtilities.invokeLater(() -> {
            gui.showConfigPanel();
            gui.getFrame().setVisible(true); // Riporta in primo piano la finestra
        });
    }
    updateButtonStates();
}


    private void onStartSimulation(int nBoids) {
        managerActor.tell(new ManagerProtocol.StartSimulation(
            nBoids,
            boidsParams.getWidth(),
            boidsParams.getHeight()
        ));
        isRunning = true;
        isPaused = false;
        updateButtonStates();
    }

    private void updateButtonStates() {
        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                gui.getPauseButton().setEnabled(isRunning && !isPaused && !waitingForConfirmation);
                gui.getResumeButton().setEnabled(isRunning && isPaused && !waitingForConfirmation);
                gui.getStopButton().setEnabled(isRunning && !waitingForConfirmation);
            }
        });
    }

    private void setWaitingState(boolean waiting) {
        this.waitingForConfirmation = waiting;
        updateButtonStates();
    }

    private void onStop() {
        if (isRunning && !waitingForConfirmation) {
            setWaitingState(true);
            managerActor.tell(new ManagerProtocol.StopSimulation());
        }
    }

    private void onPauseResume() {
        if (isRunning && !waitingForConfirmation) {
            setWaitingState(true);
            if (isPaused) {
                managerActor.tell(new ManagerProtocol.ResumeSimulation());
            } else {
                managerActor.tell(new ManagerProtocol.PauseSimulation());
            }
        }
    }

    public static Behavior<GUIProtocol.Command> create(BoidsParams boidsParams,
                                                       ActorRef<ManagerProtocol.Command> managerActor) {
        return Behaviors.setup(ctx ->
                Behaviors.supervise(new SimulationUIActor(boidsParams, managerActor).behavior())
                        .onFailure(SupervisorStrategy.restart()));
    }

    private Behavior<GUIProtocol.Command> behavior() {
        return Behaviors.receive(GUIProtocol.Command.class)
                .onMessage(GUIProtocol.RenderFrame.class, this::onRenderFrame)
                .onMessage(GUIProtocol.UpdateWeights.class, this::onUpdateWeights)
                .onMessage(GUIProtocol.UpdateStatus.class, this::onUpdateStatus)
                .onMessage(GUIProtocol.ConfirmPause.class, this::onConfirmPause)
                .onMessage(GUIProtocol.ConfirmResume.class, this::onConfirmResume)
                .onMessage(GUIProtocol.ConfirmStop.class, this::onConfirmStop)
                .onMessage(GUIProtocol.ConfirmParamsUpdate.class, this::onConfirmParamsUpdate)
                .build();
    }

    private Behavior<GUIProtocol.Command> onRenderFrame(GUIProtocol.RenderFrame msg) {
        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                gui.getBoidsPanel().updateBoids(msg.boids());
            }
        });
        return Behaviors.same();
    }

    private Behavior<GUIProtocol.Command> onUpdateWeights(GUIProtocol.UpdateWeights msg) {
        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                gui.getCohesionSlider().setValue((int) (msg.cohesionWeight() * 10));
                gui.getAlignmentSlider().setValue((int) (msg.alignmentWeight() * 10));
                gui.getSeparationSlider().setValue((int) (msg.separationWeight() * 10));
            }
        });
        return Behaviors.same();
    }

    private Behavior<GUIProtocol.Command> onUpdateStatus(GUIProtocol.UpdateStatus msg) {
        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                gui.getStatusLabel().setText("Status: " + msg.status().toString());
            }
        });
        return Behaviors.same();
    }

    private Behavior<GUIProtocol.Command> onConfirmPause(GUIProtocol.ConfirmPause msg) {
        isPaused = true;
        setWaitingState(false);
        updateButtonStates();
        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                gui.getCohesionSlider().setEnabled(false);
                gui.getSeparationSlider().setEnabled(false);
                gui.getAlignmentSlider().setEnabled(false);
            }
        });
        return Behaviors.same();
    }

    private Behavior<GUIProtocol.Command> onConfirmResume(GUIProtocol.ConfirmResume msg) {
        isPaused = false;
        setWaitingState(false);
        updateButtonStates();
        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                gui.getCohesionSlider().setEnabled(true);
                gui.getSeparationSlider().setEnabled(true);
                gui.getAlignmentSlider().setEnabled(true);
            }
        });
        return Behaviors.same();
    }

    private Behavior<GUIProtocol.Command> onConfirmStop(GUIProtocol.ConfirmStop msg) {
        isRunning = false;
        setWaitingState(false);
        updateButtonStates();

        if (gui != null && gui.getFrame() != null) {
            gui.getFrame().dispose();
            gui = null;
        }

        // NON ricreare subito la GUI qui!
        // Se vuoi far ripartire la simulazione, fallo solo su richiesta dell'utente (ad esempio, riavviando il programma o mostrando un pulsante "Nuova simulazione").

        return Behaviors.same();
    }


    private Behavior<GUIProtocol.Command> onConfirmParamsUpdate(GUIProtocol.ConfirmParamsUpdate msg) {
        setWaitingState(false);
        updateButtonStates();
        return Behaviors.same();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (!waitingForConfirmation && gui != null) {
            double sep = gui.getSeparationSlider().getValue() * 0.1;
            double coh = gui.getCohesionSlider().getValue() * 0.1;
            double ali = gui.getAlignmentSlider().getValue() * 0.1;         managerActor.tell(new ManagerProtocol.UpdateParams(coh, ali, sep));
        }
    }
}