package pcd.utils;
/**
 * two dimensional vector
 * objects are completely state-less
 */
public record V2d(double x,double y) {

    public V2d sum(V2d v){
        return new V2d(x+v.x,y+v.y);
    }
    public V2d sub(V2d v){
        return new V2d(x-v.x,y-v.y);
    }

    public double abs(){
        return Math.sqrt(x*x+y*y);
    }

    public V2d getNormalized(){
        double module= Math.sqrt(x*x+y*y);
        return new V2d(x/module,y/module);
    }

    public V2d mul(double fact){
        return new V2d(x*fact,y*fact);
    }

    public String toString(){
        return "V2d("+x+","+y+")";
    }
    
}