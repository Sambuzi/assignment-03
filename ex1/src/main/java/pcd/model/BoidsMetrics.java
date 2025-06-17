package pcd.model;

public class BoidsMetrics {
    private final int totalBoids;
    private final int fps;
    private final long tickNumber;

    public BoidsMetrics(int totalBoids, int fps, long tickNumber) {
        this.totalBoids = totalBoids;
        this.fps = fps;
        this.tickNumber = tickNumber;
    }

    public int totalBoids() { return totalBoids; }
    public int fps() { return fps; }
    public long tickNumber() { return tickNumber; }
}