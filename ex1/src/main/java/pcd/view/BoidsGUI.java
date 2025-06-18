package pcd.view;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.util.function.Consumer;

public class BoidsGUI {
    private final JFrame frame;
    private final BoidsPanel boidsPanel;
    private final JSlider cohesionSlider, separationSlider, alignmentSlider;
    private final JButton resumeButton, pauseButton, stopButton, startButton;
    private final JLabel statusLabel;
    private final JPanel configPanel, controlsPanel;
    private final JTextField boidsField;

    public BoidsGUI(int width, int height, int nBoids, Consumer<Integer> onStart, Consumer<String> onPauseResume, Consumer<String> onStop, ChangeListener onSliderChange) {
        frame = new JFrame("Boids Simulation");
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Config panel (top)
        configPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        JLabel label = new JLabel("Number of Boids:");
        boidsField = new JTextField(String.valueOf(nBoids), 5);
        startButton = new JButton("Start Simulation");
        configPanel.add(label);
        configPanel.add(boidsField);
        configPanel.add(startButton);

        // Controls panel (top, hidden at start)
        controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        controlsPanel.setBorder(BorderFactory.createTitledBorder("Simulation Controls"));
        Dimension buttonSize = new Dimension(85, 25);
        resumeButton = new JButton("Resume");
        resumeButton.setPreferredSize(buttonSize);
        pauseButton = new JButton("Pause");
        pauseButton.setPreferredSize(buttonSize);
        stopButton = new JButton("Stop");
        stopButton.setPreferredSize(buttonSize);
        statusLabel = new JLabel("Status: Waiting...");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        controlsPanel.add(resumeButton);
        controlsPanel.add(pauseButton);
        controlsPanel.add(stopButton);
        controlsPanel.add(Box.createHorizontalStrut(10));
        controlsPanel.add(statusLabel);
        controlsPanel.setVisible(false);

        // Add both panels to NORTH
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(configPanel, BorderLayout.NORTH);
        topPanel.add(controlsPanel, BorderLayout.SOUTH);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Boids panel
        boidsPanel = new BoidsPanel(width, height, nBoids, new ArrayList<>());
        mainPanel.add(BorderLayout.CENTER, boidsPanel);

        // Sliders panel (bottom)
        JPanel slidersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        slidersPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        Dimension sliderSize = new Dimension(100, 30);
        cohesionSlider = makeSlider(onSliderChange, sliderSize);
        separationSlider = makeSlider(onSliderChange, sliderSize);
        alignmentSlider = makeSlider(onSliderChange, sliderSize);
        slidersPanel.add(new JLabel("  Cohesion:"));
        slidersPanel.add(cohesionSlider);
        slidersPanel.add(new JLabel("  Separation:"));
        slidersPanel.add(separationSlider);
        slidersPanel.add(new JLabel("  Alignment:"));
        slidersPanel.add(alignmentSlider);
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        bottomPanel.add(slidersPanel);
        mainPanel.add(BorderLayout.SOUTH, bottomPanel);

        frame.setContentPane(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Listeners
        startButton.addActionListener(e -> {
            try {
                int n = Integer.parseInt(boidsField.getText());
                onStart.accept(n);
                configPanel.setVisible(false);
                controlsPanel.setVisible(true);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter a valid number.");
            }
        });
        resumeButton.addActionListener(e -> onPauseResume.accept("resume"));
        pauseButton.addActionListener(e -> onPauseResume.accept("pause"));
        stopButton.addActionListener(e -> onStop.accept("stop"));
    }

    private JSlider makeSlider(ChangeListener listener, Dimension size) {
        var slider = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setPreferredSize(size);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel(""));
        labelTable.put(10, new JLabel(""));
        slider.setLabelTable(labelTable);
        slider.addChangeListener(listener);
        return slider;
    }

    // Getter methods for components
    public BoidsPanel getBoidsPanel() { return boidsPanel; }
    public JSlider getCohesionSlider() { return cohesionSlider; }
    public JSlider getSeparationSlider() { return separationSlider; }
    public JSlider getAlignmentSlider() { return alignmentSlider; }
    public JButton getResumeButton() { return resumeButton; }
    public JButton getPauseButton() { return pauseButton; }
    public JButton getStopButton() { return stopButton; }
    public JLabel getStatusLabel() { return statusLabel; }
    public JFrame getFrame() { return frame; }
    public void showConfigPanel() {
        configPanel.setVisible(true);
        controlsPanel.setVisible(false);
    }
}