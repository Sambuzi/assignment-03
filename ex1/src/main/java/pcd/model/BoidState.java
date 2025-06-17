package pcd.model;

import pcd.utils.*;

public class BoidState {
    private final P2d pos;
    private final V2d vel;
    private final String id;

    public BoidState(P2d pos, V2d vel, String id) {
        this.pos = pos;
        this.vel = vel;
        this.id = id;
    }

    public P2d pos() { return pos; }
    public V2d vel() { return vel; }
    public String id() { return id; }
}