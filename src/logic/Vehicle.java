package logic;

import javafx.scene.image.ImageView;

import java.awt.*;

public  abstract class Vehicle implements Runnable {
    private Direction currentDir;
    private int speed = 0;
    private float transparency = 0;
    private boolean intersectionFlag = false;
    private final int minBufferDistance = 10;
    private Point location = new Point();
    private Lane lane;
    private boolean running = false;
    private ImageView imageView;
    private double imageRotation;
    private int maxSpeed;
    private double tileSize;
    private int id;


    public int getId(){
        return this.id;
    }

    public void setId(int id){
        this.id = id;
    }


    public double getTileSize(){
        return this.tileSize;
    }

    public void setTileSize(double tileSize){
        this.tileSize = tileSize;
    }

    public Direction getDirection(){
        return currentDir;
    }

    public void setDirection(Direction dir){
        this.currentDir = dir;
    }

    public int getSpeed(){
        return this.speed;
    }

    public void setSpeed(int speed){
        this.speed = speed;
    }

    public float getTransparency(){
        return this.transparency;
    }

    public void setTransparency(float transparency){
        this.transparency = transparency;
    }

    public boolean getIntersectionFlag(){
        return this.intersectionFlag;
    }

    public void flipIntersectionFlag(){
        this.intersectionFlag = !this.intersectionFlag;
    }

    public Point getLocation(){
        return this.location;
    }

    public void setLocation(Point p){
        this.location = p;
    }

    public void update(){
        this.imageView.setX(this.location.x*tileSize/200);
        this.imageView.setY(this.location.y*tileSize/200);

    }

    public void setLane(Lane lane){
        this.lane = lane;
    }

    public Lane getLane(){
        return this.lane;
    }

    public ImageView getImageView(){
        return this.imageView;
    }

    public void setImageView(ImageView image){
        this.imageView = image;
    }

    public void setImageRotation(Double rotation){
        this.imageRotation = rotation;
    }

    public double getImageRotation(Double rotation){
        return this.imageRotation;
    }

    public int getMaxSpeed(){
        return this.maxSpeed;
    }

    public void setMaxSpeed(int speed){
        this.maxSpeed = speed;
    }


    public abstract boolean move();


    public abstract void run();

    public abstract void stop();

}
