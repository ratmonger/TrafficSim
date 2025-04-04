package logic;

import GUI.TrafficGUI;
import javafx.application.Platform;
import javafx.scene.effect.Light;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static GUI.TrafficGUI.updateIntersectionGUI;

public class Intersection implements Runnable {
    private long greenRedDuration = 8000;
    // green and red light have minimum of 5 second duration
    private final long yellowDuration = 3500;
    private final long leftDuration = 4000;
    // yellow light minimum of 2 second duration
    private final long accLimit = 10;// number of cars required to change light
    private final long minLength = 4000;
    //minimum time spent during red or green light
    private long lightChangeTime; // time of previous light change
    private long lightChangeTimeCopy; // backup when EMS arrives
    private final int intersectionNumber; //
    private LightDirection northSouthDir; // direction of north, south lights
    private LightDirection eastWestDir; // direction fo east, west lights
    private LightColor northSouthColor; // color of the north and south lights
    private LightColor eastWestColor; // color of east and west lights
    private boolean construction = true;

    private LightColor EMSCopyNorthSouth;
    private LightColor EMSCopyEastWest;

    private Point center;
    private int northStop, southStop, eastStop, westStop;
    // northStop = boundary incoming cars from NORTH may not cross
    // when RED
    private int northBarrier, southBarrier, eastBarrier, westBarrier;
    // north/south refer to a Y coordinate, east/west X coordinate
    private ArrayList<Point> exits = new ArrayList<>();
    private ArrayList<Point> spawns = new ArrayList<>();

    private ImageView[] images;
    private int eastWestAcc;
    private int northSouthAcc;
    private boolean EMSinbound = false;
    private boolean EMSprior = false;
    private boolean pedestrians = false;
    private boolean EMShappened = false;

    // assuming that there will be some sort of number assigned to an
    // intersection so that we can differentiate btwn them
    public Intersection(int id, Point center) {
        Random rand = new Random();
        this.greenRedDuration = 8000 + rand.nextInt(-1000, 2000);
        this.lightChangeTime = System.currentTimeMillis();
        this.intersectionNumber = id;
        this.northSouthDir = LightDirection.NORTHSOUTH;
        this.eastWestDir = LightDirection.EASTWEST;

        if (rand.nextDouble() < 0.5) {
            this.northSouthColor = LightColor.GREEN;
            this.eastWestColor = LightColor.RED;

        } else {
            this.northSouthColor = LightColor.RED;
            this.eastWestColor = LightColor.GREEN;
        }

        this.eastWestAcc = 0; //traffic accumulator
        this.northSouthAcc = 0;
        this.images = TrafficGUI.intersectionImages;

        this.center = center;

        this.northStop = (int) center.getY() - 60;
        this.southStop = (int) center.getY() + 59;
        this.westStop = (int) center.getX() - 60;
        this.eastStop = (int) center.getX() + 59;

        this.northBarrier = (int) center.getY() - 40;
        this.southBarrier = (int) center.getY() + 39;
        this.eastBarrier = (int) center.getX() + 39;
        this.westBarrier = (int) center.getX() - 40;

        this.spawns.add(new Point(new Point(
                (int) center.getX() - 30,
                (int) center.getY() - 150)));
        this.spawns.add(new Point(new Point(
                (int) center.getX() - 10,
                (int) center.getY() - 150)));

        this.spawns.add(new Point(new Point(
                (int) center.getX() + 150,
                (int) center.getY() - 30)));
        this.spawns.add(new Point(new Point(
                (int) center.getX() + 150,
                (int) center.getY() - 10)));

        this.spawns.add(new Point(new Point(
                (int) center.getX() + 28,
                (int) center.getY() + 150)));
        this.spawns.add(new Point(new Point(
                (int) center.getX() + 9,
                (int) center.getY() + 150)));

        this.spawns.add(new Point(new Point(
                (int) center.getX() - 150,
                (int) center.getY() + 28)));
        this.spawns.add(new Point(new Point(
                (int) center.getX() - 150,
                (int) center.getY() + 9)));

        // adding EXITS in clockwise order, important for future indexed get(i)
        // NORTH, EAST, SOUTH, WEST
        this.exits.add(new Point(
                (int) center.getX() + 9,
                this.northBarrier));//northLeft
        this.exits.add(new Point(
                (int) center.getX() + 28,
                this.northBarrier)); //northRight

        this.exits.add(new Point(
                this.eastBarrier,
                (int) center.getY() + 9));//eastLeft
        this.exits.add(new Point(
                this.eastBarrier,
                (int) center.getY() + 28));//eastRight

        this.exits.add(new Point(
                (int) center.getX() - 10,
                this.southBarrier)); //southLeft
        this.exits.add(new Point(
                (int) center.getX() - 30,
                this.southBarrier)); // southRight

        this.exits.add(new Point(
                this.westBarrier,
                (int) center.getY() - 10));//westLeft
        this.exits.add(new Point(
                this.westBarrier,
                (int) center.getY() - 30));//westRight

        //setImages();
        construction = false;

    }


    public LightColor getEWState() {
        return eastWestColor;
    }

    public LightColor getNSState() {
        return northSouthColor;
    }

    public int getID() {
        return intersectionNumber;
    }


    public LightColor queryLight(Direction dir) {
        if (EMSinbound) {
            return LightColor.EMS;
        }
        if (dir == Direction.NORTH || dir == Direction.SOUTH) {
            return northSouthColor;
        } else {
            return eastWestColor;
        }
    }

    public void setEMSinbound(boolean flag) {
        this.EMSinbound = flag;
    }

    public boolean getEMSinbound() {
        return this.EMSinbound;
    }


    public int getStop(Direction dir) {
        return switch (dir) {
            case NORTH -> this.southStop;
            case SOUTH -> this.northStop;
            case EAST -> this.westStop;
            case WEST -> this.eastStop;
        };
    }

    public int getBarrier(Direction dir) {
        return switch (dir) {
            case NORTH -> this.southBarrier;
            case SOUTH -> this.northBarrier;
            case EAST -> this.westBarrier;
            case WEST -> this.eastBarrier;
        };
    }

    public int getExitBarrier(Direction dir) {
        return switch (dir) {
            case NORTH -> this.northBarrier;
            case SOUTH -> this.southBarrier;
            case EAST -> this.eastBarrier;
            case WEST -> this.westBarrier;
        };
    }


    private enum LightDirection {
        NORTHSOUTH,
        EASTWEST
    }

    public Point getCenter() {
        return this.center;
    }

    @Override
    public void run() {
        setImages();
        while (true) {
            updateIntersection();

            try {
                Thread.sleep(100);//ms
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //Changes the light images on gui
    private void setImages() {
        long currentTime = System.currentTimeMillis();
        if (images != null) {
            if (eastWestColor == LightColor.GREEN &&
                    northSouthColor == LightColor.RED && !construction) {
                String[] greenred = {"greenredppl1.png", "greenredppl2.png",
                        "greenredppl3.png",
                        "greenredppl4.png", "greenredppl5.png",
                        "greenredppl6.png", "greenRed.png"};
                this.pedestrians = true;
                long sleep = 500;
                for (int i = 0; i < 7; i++) {
                    if (EMShappened){
                        i = 6;
                        EMShappened = false;
                    }
                    if(EMSinbound){
                        sleep = 300;
                    }
                    int finalI = i;
                    //Platform.runLater(() -> {
                    updateIntersectionGUI(this.intersectionNumber, greenred[i]);
                    //});
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                this.pedestrians = false;


            }
            if (eastWestColor == LightColor.RED &&
                    northSouthColor == LightColor.GREEN && !construction) {
                String[] redgreenppl = {"redgreenppl1.png",
                        "redgreenppl2.png", "redgreenppl3.png",
                        "redgreenppl1.png",
                        "redgreenppl4.png", "redgreenppl5.png", "redgreen.png"};
                this.pedestrians = true;
                long sleep = 500;
                for (int i = 0; i < 7; i++) {
                    if (EMShappened){
                        i = 6;
                        EMShappened = false;
                    }
                    if(EMSinbound){
                        sleep = 300;
                    }
                    updateIntersectionGUI(this.intersectionNumber, redgreenppl[i]);
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                this.pedestrians = false;


            }
            if (eastWestColor == LightColor.RED &&
                    northSouthColor == LightColor.YELLOW) {

                images[intersectionNumber].setImage(new Image("redyellow.png"));

            }
            if (eastWestColor == LightColor.YELLOW &&
                    northSouthColor == LightColor.RED) {

                images[intersectionNumber].setImage(new Image("yellowred.png"));

            }
            if (eastWestColor == LightColor.LEFTYELLOW) {
                images[intersectionNumber].setImage(
                        new Image("leftYellowEast.png"));
            } else if (northSouthColor == LightColor.LEFTYELLOW) {
                images[intersectionNumber].setImage(
                        new Image("leftYellowNorth.png"));
            } else if (northSouthColor == LightColor.RED
                    && eastWestColor == LightColor.LEFTGREEN) {
                images[intersectionNumber].setImage(
                        new Image("leftTurnEastWest.png"));
            } else if (eastWestColor == LightColor.RED
                    && northSouthColor == LightColor.LEFTGREEN) {
                images[intersectionNumber].setImage(
                        new Image("leftTurnNorthSouth.png"));
            }
            else if (eastWestColor == LightColor.RED
                    && northSouthColor == LightColor.RED) {
                images[intersectionNumber].setImage(
                        new Image("redred.png"));
            }
        }
    }

    //Gets what opposite light should be
    private LightColor oppositeLight(LightColor color1, LightColor color2) {
        if (color1 == LightColor.GREEN) {
            return LightColor.RED;
        } else if (color1 ==
                LightColor.YELLOW) {//keeps the other light red for duration
            // of yellow
            return LightColor.RED;
        } else if (color1 ==
                LightColor.RED
                && color2 == LightColor.LEFTGREEN) {
            return LightColor.LEFTYELLOW;
        } else if (color1 ==
                LightColor.RED
                && color2 == LightColor.LEFTYELLOW) {
            return LightColor.GREEN;

        } else if (color1 ==
                LightColor.RED &&
                color2 == LightColor.RED) {
            return LightColor.LEFTGREEN;
        } else if (color1 ==
                LightColor.LEFTGREEN) {
            return LightColor.RED;
        } else if (color1 ==
                LightColor.LEFTYELLOW) {
            return LightColor.RED;
        } else {
            return LightColor.RED;
        }
    }

    private LightColor nextLight(LightColor color) {
        if (color ==
                LightColor.GREEN) {
            return LightColor.YELLOW;
        } else if (color ==
                LightColor.YELLOW) {//keeps the other light red for duration
            // of yellow
            return LightColor.RED;
        } else if (color ==
                LightColor.RED) {
            return LightColor.LEFTGREEN;
        } else if (color ==
                LightColor.LEFTGREEN) {
            return LightColor.LEFTYELLOW;
        } else {
            return LightColor.GREEN;
        }

    }

    // changes color of the lights
    private void changeLight(LightDirection direction, LightColor newColor) {

        if (direction == LightDirection.NORTHSOUTH) {
            if (northSouthColor == LightColor.LEFTYELLOW) {
                pedestrians = true;
                northSouthColor = LightColor.GREEN;
                eastWestColor = LightColor.RED;
            } else if (northSouthColor == LightColor.LEFTGREEN) {
                northSouthColor = LightColor.LEFTYELLOW;
                eastWestColor = LightColor.RED;
            } else if (northSouthColor == LightColor.GREEN) {
                northSouthColor = LightColor.YELLOW;
                eastWestColor = LightColor.RED;
            } else {
                northSouthColor = newColor;
                eastWestColor = oppositeLight(newColor, eastWestColor);
            }

        } else if (direction == LightDirection.EASTWEST
                && (northSouthColor == LightColor.GREEN
                && newColor ==
                LightColor.LEFTGREEN)) {//this is the one that was skipping
            // yellow
            northSouthColor = LightColor.YELLOW;
            eastWestColor =
                    LightColor.RED;//keep red for duration of yellow light
        } else if (eastWestColor == LightColor.LEFTYELLOW){
                pedestrians = true;
                eastWestColor = LightColor.GREEN;
                northSouthColor = LightColor.RED;
        } else if (direction == LightDirection.EASTWEST) {
            northSouthColor = oppositeLight(newColor, northSouthColor);
            eastWestColor = newColor;
        }
        setImages();
        lightChangeTime = System.currentTimeMillis();
    }

    // changes state of intersection based on time, will call changeLight method
    private void updateIntersection() {
        if (!this.EMSprior && this.EMSinbound){
            //this.lightChangeTimeCopy = currentTime - lightChangeTime;
            toggleEMSRedLights();
            this.EMSprior = true;
            this.setImages();
            return;
        }
        if (this.EMSprior && !this.EMSinbound){
            this.EMShappened = true;
            this.pedestrians = true;
            toggleEMSRedLights();
            this.EMSprior = false;
            this.setImages();
            this.lightChangeTime = System.currentTimeMillis();
        }

        long currentTime = System.currentTimeMillis();



        //red/green light timer check
        if ((((eastWestColor == LightColor.GREEN ||
                eastWestColor == LightColor.RED) ||
                (northSouthColor == LightColor.GREEN ||
                        northSouthColor == LightColor.RED))
                && (currentTime - lightChangeTime >= greenRedDuration))
                || (eastWestAcc >= accLimit &&
                currentTime - lightChangeTime >= minLength)) {

            changeLight(eastWestDir, nextLight(eastWestColor));//k

        }
        //acc check
        if (northSouthAcc >= accLimit &&
                currentTime - lightChangeTime >= minLength) {
            changeLight(northSouthDir, nextLight(northSouthColor));//k
        }

        //handles yellow light timing (unaffected by volume)
        if (eastWestColor == LightColor.YELLOW &&
                currentTime - lightChangeTime >= yellowDuration) {
            changeLight(eastWestDir, nextLight(eastWestColor));//k
            lightChangeTime = System.currentTimeMillis();
        } else if (northSouthColor == LightColor.YELLOW &&
                currentTime - lightChangeTime >= yellowDuration) {
            changeLight(northSouthDir, nextLight(northSouthColor));//k
            lightChangeTime = System.currentTimeMillis();
        }
        //handles left turns
        else if (northSouthColor == LightColor.LEFTGREEN
                && currentTime - lightChangeTime >= leftDuration) {
            changeLight(northSouthDir, nextLight(northSouthColor));//k
        } else if (eastWestColor == LightColor.LEFTGREEN
                && currentTime - lightChangeTime >= leftDuration) {
            changeLight(eastWestDir, nextLight(eastWestColor));//k
        } else if (eastWestColor == LightColor.LEFTYELLOW
                && currentTime - lightChangeTime >= yellowDuration) {
            changeLight(eastWestDir, nextLight(eastWestColor));
        } else if (northSouthColor == LightColor.LEFTYELLOW
                && currentTime - lightChangeTime >= yellowDuration) {
            changeLight(northSouthDir, nextLight(eastWestColor));
        }
    }

    // Parameter: dir = current direction of incoming car
    // Returns destination point for Car
    public Pair<Point, Lane> getSpawn(Direction dir) {
        ArrayList<Point> temp = new ArrayList<>();
        Lane tempLane;
        Random random = new Random();
        int choice = random.nextInt(2);
        if (choice == 0) {
            tempLane = Lane.RIGHT;
        } else {
            tempLane = Lane.LEFT;
        }
        switch (dir) {
            case NORTH -> {
                temp.add(spawns.get(4));
                temp.add(spawns.get(5));
                //System.out.println(temp);
                // code block
                return new Pair<>(temp.get(choice), tempLane);
            }
            case SOUTH -> {
                temp.add(spawns.get(0));
                temp.add(spawns.get(1));
                //System.out.println(temp);
                // code block
                return new Pair<>(temp.get(choice), tempLane);
            }
            case EAST -> {
                temp.add(spawns.get(6));
                temp.add(spawns.get(7));
                //System.out.println(temp);
                // code block
                return new Pair<>(temp.get(choice), tempLane);
            }
            case WEST -> {
                temp.add(spawns.get(2));
                temp.add(spawns.get(3));
                // code block
                return new Pair<>(temp.get(choice), tempLane);
            }
        }
        return null;

    }

    // Parameter: dir = current direction of incoming car
    // Returns destination point for Car
    public Point getLeftTurn(Direction dir) {
        return switch (dir) {
            case NORTH ->
                    // code block
                    exits.get(6);
            case SOUTH ->
                    // code block
                    exits.get(2);
            case EAST ->
                    // code block
                    exits.get(0);
            case WEST ->
                    // code block
                    exits.get(4);
            // add more cases as needed
        };
    }

    // Parameter: dir = current direction of incoming car
    public Point getRightTurn(Direction dir) {
        return switch (dir) {
            case NORTH ->
                    // code block
                    exits.get(3);
            case SOUTH ->
                    // code block
                    exits.get(7);
            case EAST ->
                    // code block
                    exits.get(5);
            case WEST ->
                    // code block
                    exits.get(1);
            // add more cases as needed
        };
    }

    public void toggleEMSRedLights() {
        if (this.EMSinbound) {
            this.EMSCopyNorthSouth = this.northSouthColor;
            this.EMSCopyEastWest = this.eastWestColor;
            this.northSouthColor = LightColor.RED;
            this.eastWestColor = LightColor.RED;

        } else {
            if(this.EMSCopyNorthSouth != LightColor.RED && this.EMSCopyNorthSouth != LightColor.YELLOW){
                this.northSouthColor = LightColor.GREEN;
                this.eastWestColor = LightColor.RED;
            } else {
                this.northSouthColor = LightColor.RED;
                this.eastWestColor = LightColor.GREEN;
            }
        }

    }

    public boolean getPedestrians(){
        return this.pedestrians;
    }


}
