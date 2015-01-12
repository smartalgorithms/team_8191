/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team_152;

import battlecode.common.*;
import java.util.*;

/**
 *
 * @author byrdie
 * @author albmin
 */

//TODO: ADD HASHCODE BUCKETS FOR ALL THE TOWERS LOCATIONS
public class RobotPlayer {

    static int spawnPos = 0;
    static RobotController roc;
    static Random rand;
    static int myRange;
    static Team enemyTeam;
    static int flockOffset = 50;
    /*memory map*/
    //(x,y) for flocks 1 - 10
    static final int[][] currWayBuckets = {{60050, 60051}, {60052, 60053}, {60054, 60055}, {60056, 60057}, {60058, 60059}, {60060, 60061}, {60062, 60063}, {60064, 60065}, {60066, 60067}, {60068, 60069}};
    // bucket containing flock number for various units
    static final int beavFlockNum = 60070;
    static final int minerFlockNum = 60071;
    static final int soldierFlockNum = 60072;
    static final int basherFlockNum = 60073;
    static final int droneFlockNum = 60074;
    static final int tankFlockNum = 60075;
    static final int commanderFlockNum = 60076;
    static final int launcerFlockNum = 60077;
    // building request fields
    static final int[] barracksReq = {60080, 60081, 60082}; //{x, y, requested?(1,0)}
    /*action booleans*/
    static boolean buildReq = false;    //set to true for contruct building request
    static int buildingType = 0;    // 0=none, 1=supply depot, 2=minerfactory, 3=techinstitute, 4=barracks, 5=helipad, 6=trainingfield, 7=tankfactory, 8=aerospacelab, 9=handwash
    static int[] waypoint = new int[2];    // current waypoint of the robot
    static int modSize;
    static int x;
    static int y;

    //probably clean, but i dont know the bytecode cost of instantiating after declaring
    int enemyHQDist = computeDistanceToEnemyHQ(roc.getLocation());
    static MapLocation enemyHQLoc;
    static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
        Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    static boolean firstMove = true;
    static int flockNumber;

    public static void run(RobotController rc) {
        roc = rc;
        //get the array information containing map data
        //should probably put this below HQ
        //end map data array info
        enemyHQLoc = roc.senseEnemyHQLocation();
        RobotType type = roc.getType();
        rand = new Random(roc.getID());
        myRange = type.attackRadiusSquared;
        enemyTeam = roc.getTeam().opponent();
        switch (type) {
            case HQ:
                execHQ();
                break;
            case BEAVER:
                execBeav();
                break;
            case TOWER:
                execTower();
                break;
            case MINER:
                execMiner();
            case SOLDIER:
                execSoldier();
            case BASHER:
                execBasher();
            case MINERFACTORY:
                execMinerFact();
            case BARRACKS:
                execBarracks();
            case COMMANDER:
                execCommander();
            case TRAININGFIELD:
                execTrainingField();
        }
    }

    static void execHQ() {
        //only occurs when hq initialized
        //determine map information and initizliaze said components into
        //team messaging system
        ArrayList<Integer> botList;
        botList = new ArrayList<Integer>();
        try {
            if (firstMove) {
                firstMove = false;
                roc.broadcast(58000, 1);
                //determine reduced size of board
                int x = 240 - (roc.getLocation().x - enemyHQLoc.x);
                int y = 240 - (roc.getLocation().y - enemyHQLoc.y);
                int modSize = x * y;
                roc.broadcast(1, modSize);
                roc.broadcast(65534, x);
                roc.broadcast(65535, y);
                //broadcast the first waypoint for flock 1
                int wayX = (enemyHQLoc.x - roc.getLocation().x) / 2 + roc.getLocation().x;
                int wayY = (enemyHQLoc.y - roc.getLocation().y) / 2 + roc.getLocation().y;
                System.out.println(wayX + ", " + wayY);
                roc.broadcast(flockOffset + 0, wayX);
                roc.broadcast(flockOffset + 1, wayY);
                roc.broadcast(71, 0);
                //request barracks
                roc.broadcast(barracksReq[0], ((enemyHQLoc.x - roc.getLocation().x) / 6 + roc.getLocation().x));
                roc.broadcast(barracksReq[1], ((enemyHQLoc.y - roc.getLocation().y) / 6 + roc.getLocation().y));
                roc.broadcast(barracksReq[2], 1);
            }

        } catch (GameActionException e) {
            System.out.println("exception in execHQ - pre infinite loop");
            e.printStackTrace();
        }

        while (true) {
            try {
                if (Clock.getRoundNum() == 13) {
                    ordertTowerPQ();
                }
                //sense nearby bots
                RobotInfo[] bots = roc.senseNearbyRobots(15, roc.getTeam());
                if (bots.length != 0) {
                    for (int i = 0; i < bots.length; i++) {
                        if (botList.contains(bots[i].ID)) {
                            continue;
                        }
                        roc.transferSupplies(900, bots[i].location);
                        botList.add(bots[i].ID);
                    }
                }
                if (needSpawn(roc.getType())) {
                    if (roc.isCoreReady() && roc.getTeamOre() >= 100) //thoretically we are going to change this so that it is more deterministic
                    //as opposed to random
                    {
                        trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
                    }
                }
                if (roc.isWeaponReady()) {
                    attackSomething();
                }
                if (Clock.getRoundNum() == 1000) {
                    int wayX = (enemyHQLoc.x - roc.getLocation().x) / 4 + roc.getLocation().x;
                    int wayY = (enemyHQLoc.y - roc.getLocation().y) / 4 + roc.getLocation().y;
                    System.out.println(wayX + ", " + wayY);
                    roc.broadcast(flockOffset + 0, wayX);
                    roc.broadcast(flockOffset + 1, wayY);
                }
            } catch (GameActionException e) {
                System.out.println("Unexpected exception in execHQ");
                e.printStackTrace();
                continue;
            }
        }
    }

    static void execBeav() {

        if (firstMove) {
            try {
                firstMove = false;
                flockNumber = roc.readBroadcast(beavFlockNum);

                /*loop here to see if there is anything requested for build*/
                if (roc.readBroadcast(barracksReq[2]) == 1) {

                    roc.broadcast(barracksReq[2], 0);   //acknowledge request to build

                    /*if so, go to specified location and build one at specified location*/
                    waypoint[0] = roc.readBroadcast(barracksReq[0]);
                    waypoint[1] = roc.readBroadcast(barracksReq[1]);

                    System.out.println("Build barracks at" + waypoint[0] + ", " + waypoint[1]);

                    buildReq = true;
                    buildingType = 4;

                }

            } catch (Exception e) {
                System.out.println("Unexpected exception in execBeav pre-init");
                e.printStackTrace();
            }

        }

        //TODO write a check that will see if the health has changed, if so, 'fight or flight'
        //^ really gauge your location and from there broadcast the info, or do something else
        try {
            modSize = roc.readBroadcast(1);
        } catch (GameActionException e) {
            System.out.println("Unexpected exception in execBeav");
            e.printStackTrace();
        }

        while (true) {
//            System.out.println("Beaver at ");
//            System.out.println(roc.getLocation().x);
//            System.out.println(roc.getLocation().y);
            try {

                //       if (roc.getSupplyLevel() == 0 )//&& roc.isCoreReady())
                //    {
                //        if (roc.isCoreReady())
                //               roc.mine();
                //        else {
                //        roc.yield();
                //         continue;
                //        }
                //         }
                if (roc.getSupplyLevel() == 0 && roc.isCoreReady()) {
                    if (roc.isCoreReady()) {
                        roc.mine();
                    } else {
                        roc.yield();
                        continue;
                    }
                }

                //run a check to
                if (roc.isWeaponReady()) {
                    attackSomething();
                }

                if (roc.isCoreReady()) { //if robot ready, make move towards enemy HQ
//                    MapLocation m = roc.senseEnemyHQLocation();
//                    MapLocation here = roc.getLocation();
//                    tryMove(here.directionTo(m));

                    /*find updated waypoint from shared array*/
                    if (!buildReq) {        // get next waypoint if we're not building
                        waypoint[0] = roc.readBroadcast(flockOffset + flockNumber + 0);
                        waypoint[1] = roc.readBroadcast(flockOffset + flockNumber + 1);
                    } else {    //otherwise see if we've reached current waypoint
                        if (waypoint[0] == roc.getLocation().x && waypoint[1] == roc.getLocation().y) {   //we've arrived at location of waypoint
                            tryBuild(Direction.NORTH, RobotType.BARRACKS);  //this should be a case statement for all building types
                            buildReq = false;   // ackowledge local building request
                        }

                    }

                    takeNextMove(waypoint);

                }
            } catch (GameActionException e) {
                System.out.println("Unexpected exception in execBeav");
                e.printStackTrace();
                continue;
            }

        }
    }

    static void execTower() {
        try {
            modSize = roc.readBroadcast(1);
        } catch (GameActionException e) {
            System.out.println("Unexpected exception in execBeav");
            e.printStackTrace();
        }
        boolean surroundingsNotSensed = true;
        boolean distNotPublished = true;
        while (true) {
            try {
                if (roc.isWeaponReady()) {
                    attackSomething();
                }
                //we want to be sure to execute this during the tower's downtime
                //  aka at the start of the game
                //this should execute at the start of the game regardless
                if (surroundingsNotSensed) {
                    // System.out.println("Code arrived here");
                    int xx = 0;
                    if (distNotPublished) {
                        int yy = computeDistanceToEnemyHQ(roc.getLocation());
                        xx = 65000;
                        while (roc.readBroadcast(xx) != 0) {
                            xx += 1;
                        }
                        roc.broadcast(xx + 12, roc.getLocation().hashCode());
                        roc.broadcast(xx, yy);
                        distNotPublished = false;
                    } else {
                        int avgCoreLevel = publishSurroundings();
                        roc.broadcast(xx + 6, avgCoreLevel);
                        surroundingsNotSensed = false;
                    }
                    if (roc.isWeaponReady()) {
                        attackSomething();
                    }
                }
            } catch (GameActionException e) {
                System.out.println("Unexpected exception in execTower");
                e.printStackTrace();           
            }
        }
    }

    private static int computeDistanceToEnemyHQ(MapLocation location) {
        return location.distanceSquaredTo(roc.senseEnemyHQLocation());
    }

    private static void initMemMap() throws GameActionException {
        for (int i = 0; i < x; i++) {
            roc.broadcast(i + 2, getRowHash(i));
        }
    }

    //TODO It would be cool to look at the two lengths (between these two
    //      methods) and determine which way is faster to do
    // ^ Don't do this, it really doesnt matter a whole lot
    private static int getRowHash(int row) throws GameActionException {
        int[] data = new int[y];
        for (int i = 0; i < y; y++) {
            //TODO TESTME --- although I believe this should work
            data[i] = roc.readBroadcast((i + 2 + x) * (row + 1));
            //we know the map array starts at 2
        }
        return Arrays.hashCode(data);

    }

    /**
     * Method that will publish the individual surroundings of each location
     * within the sensing radius of the RobotController calling this function
     * @return average core value of the surroundings (for use with towers during
     * start of game)
     * @throws GameActionException 
     */
    private static int publishSurroundings() throws GameActionException {
        MapLocation[] info = MapLocation.getAllMapLocationsWithinRadiusSq(roc.getLocation(),
                roc.getType().sensorRadiusSquared);
        int avg = 0;
        for (int i = 0; i < info.length; i++) {
            //add the maplocation to the map

            avg += updateLocationInfo(info[i]);
            //if we can attack, might as well do so
            if (roc.isWeaponReady()) {
                attackSomething();
            }
        }
        return avg / info.length;
    }

    private static int updateLocationInfo(MapLocation loc) {
        try {
            boolean isNegative = false;
            int abslochashCode = loc.hashCode();
            if (abslochashCode < 0) {
                abslochashCode = Math.abs(abslochashCode);
                isNegative = true;
            }

            int memLocation = abslochashCode % (modSize / 2);
            if (!isNegative) {
                memLocation = memLocation + (modSize / 2);
            }
            if (roc.senseOre(loc) > 10) {
                roc.broadcast(memLocation, 1);
                return 1;
            } else if (roc.senseOre(loc) > 20) {
                roc.broadcast(memLocation, 2);
                return 2;
            } else if (roc.senseTerrainTile(loc) != TerrainTile.NORMAL) {
                roc.broadcast(memLocation, 0);
                return 0;
            }
            return 0; // no significant ore to be found
        } catch (GameActionException g) {
            System.out.println("Exception caught in updatelocationinfo");
            g.printStackTrace();
            return 0;  //just so compiler stops griping
        }
        //else broadcast nothing, and everyone will just simply assume that there is nothing in the way at that location

    }

    private static void ordertTowerPQ() throws GameActionException {
        double[] queue = new double[6];
        int[] queue2 = new int[6];
        for (int i = 0; i < 6; i++) {
            int temp = roc.readBroadcast(65000 + i);
            if (temp == 0) {
                break; // we've gotten the data that we needed
            } else {
                int temp2 = roc.readBroadcast(65006 + i);
                int temp3 = roc.readBroadcast(65012 + i);
                double weight = temp - (1.4 * temp2);
                if (i == 0) {
                    queue[i] = weight;
                    queue2[i] = temp3;
                } else {
                    boolean weightAdded = false;
                    for (int g = 0; g < i; g++) {
                        if (weight < queue[g]) {
                            //make room for the array in its proper location
                            for (int j = 0; j < i; j++) {
                                queue[i - j] = queue[i - j - 1];
                                queue2[i - j] = queue2[i - j - 1];

                            }
                            queue[g] = weight; // finally, add the damn less than variable to the array
                            queue2[g] = temp3;
                            weightAdded = true;
                            break;
                        }

                    }
                    if (!weightAdded) {
                        queue[i] = weight;
                        queue2[i] = temp3;
                    }
                }
            }
        }
    }

    private static void execMiner() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void execSoldier() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void execBasher() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void execMinerFact() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void execBarracks() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void execTrainingField() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void execCommander() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    //Battlecode helper functions created by us
    /**
     * Method that allows for message interaction in order to determine the
     * spawning of a child robot from the parent caller
     *
     * @Param RobotType is the type of building determining the spawn
     *
     */
    static boolean needSpawn(RobotType type) throws GameActionException {
        switch (type) {
            case BARRACKS: {

                if (roc.readBroadcast(58001) == 1) {

                    return true;

                } else if (roc.readBroadcast(58002) == 1) {
                    return true;
                } else {
                    return false;
                }
            }

            case MINERFACTORY: {
                if (roc.readBroadcast(58004) == 1) {
                    return true;
                } else {
                    return false;
                }
            }

            case HQ: {
                if (roc.readBroadcast(58000) == 1) {
                    return true;
                } else {
                    return false;
                }
            }

            case HELIPAD: {
                if (roc.readBroadcast(58005) == 1) {
                    return true;
                } else {
                    return false;
                }
            }
            case TECHNOLOGYINSTITUTE: {
                if (roc.readBroadcast(58003) == 1) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        //TODO add an enum identifier most likely
        //TODO write the remaining child robot calls, right now i just need it for HQ
        return true; //DELETE ME once cases are finished
    }

    //BATTLECODE HELPER FUNCTIONS PULLED IN FROM EXAMPLE PLAYER
    //write your own damn method for this
    // This method will attempt to move in Direction d (or as close to it as possible)
    static void tryMove(Direction d) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = {0, 1, -1, 2, -2};
        int dirint = directionToInt(d);
        boolean blocked = false;
        while (offsetIndex < 5 && !roc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
            offsetIndex++;
        }
        if (offsetIndex < 5) {
            roc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
        }
    }

    static void attackSomething() throws GameActionException {
        RobotInfo[] enemies = roc.senseNearbyRobots(myRange, enemyTeam);
//        System.out.println(roc.getType().toString());
//        System.out.println(myRange);
//        System.out.println(enemyTeam);
        // System.out.println(enemies.toString());
        if (enemies.length > 0) {
            roc.attackLocation(enemies[0].location);
        }
    }

    //i'd really prefer to write my own code for the robots spawning TODO
    // This method will attempt to spawn in the given direction (or as close to it as possible)
    static void trySpawn(Direction d, RobotType type) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
        int dirint = directionToInt(d);
        boolean blocked = false;
        while (offsetIndex < 8 && !roc.canSpawn(directions[(dirint + offsets[offsetIndex] + 8) % 8], type)) {
            offsetIndex++;
        }
        if (offsetIndex < 8) {
            roc.spawn(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
//            if (type.equals(RobotType.BEAVER))
//                    roc.transferSupplies(900,
//                            directionOffset(roc.senseHQLocation(),
//                                    (directions[(dirint + offsets[offsetIndex] + 8) % 8])));
        }
    }

    // This method will attempt to build in the given direction (or as close to it as possible)
    static void tryBuild(Direction d, RobotType type) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
        int dirint = directionToInt(d);
        boolean blocked = false;
        while (offsetIndex < 8 && !roc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
            offsetIndex++;
        }
        if (offsetIndex < 8) {
            roc.build(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
        }
    }

//    static MapLocation directionOffset(MapLocation m, Direction d) throws GameActionException
//    {
    //convert the direction
    //      int x, y;
//        switch(d)
//        {
//            case NORTH:
//                x = 0;
//                y = -1;
//                break;
//            case NORTH_EAST:
//                x = -1;
//                y = -1;
//                break;
//            case EAST:
//                x = -1;
//                y = 0;
//                break;
//            case SOUTH_EAST:
//                x = -1;
//                y = 1;
//
//                break;
//            case SOUTH:
//                x = 0;
//                y = 1;
//                break;
//            case SOUTH_WEST:
//                x = 1;
//                y = 1;
//                break;
//            case WEST:
//                x = 1;
//                y = 0;
//                break;
//            case NORTH_WEST:
//                x = 1;
//                y = -1;
//                break;
//            default:
//                throw new GameActionException(null, "Fucked up map location");
//        }
//        System.out.println("new location");
//        System.out.println(m.x + d.dx);
//        System.out.println(m.y + d.dy);
//        return new MapLocation(m.x + d.dx, m.y + d.dy);
//
//    }
//
    static void takeNextMove(int[] waypoint) {

        Direction next = Flock.computeMove(roc, waypoint);
        boolean movePossible;
        try {
            movePossible = roc.canMove(next);
            if (!movePossible) {
                next = intToDirection(rand.nextInt(8));
            }

            roc.move(next);

        } catch (Exception e) {

        }

    }

    static int directionToInt(Direction d) {
        switch (d) {
            case NORTH:
                return 0;
            case NORTH_EAST:
                return 1;
            case EAST:
                return 2;
            case SOUTH_EAST:
                return 3;
            case SOUTH:
                return 4;
            case SOUTH_WEST:
                return 5;
            case WEST:
                return 6;
            case NORTH_WEST:
                return 7;
            default:
                return -1;
        }
    }

    static Direction intToDirection(int d) {
        switch (d) {
            case 0:
                return Direction.NORTH;
            case 1:
                return Direction.NORTH_EAST;
            case 2:
                return Direction.EAST;
            case 3:
                return Direction.SOUTH_EAST;
            case 4:
                return Direction.SOUTH;
            case 5:
                return Direction.SOUTH_WEST;
            case 6:
                return Direction.WEST;
            case 7:
                return Direction.NORTH_WEST;
            default:
                return Direction.NORTH;
        }
    }

} //end of class

