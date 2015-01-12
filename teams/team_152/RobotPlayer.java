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
public class RobotPlayer {

    static int spawnPos = 0;
    static RobotController roc;
    static Random rand;
    static int myRange;
    static Team enemyTeam;
    static int modSize;
    static int x;
    static int y;
    //probably clean, but i dont know the bytecode cost of instantiating after declaring
    int enemyHQDist = computeDistanceToEnemyHQ(roc.getLocation());
    static MapLocation enemyHQLoc;
    static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
        Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

    public static void run(RobotController rc) {
        roc = rc;
        //get the array information containing map data
        //should probably put this below HQ
        //end map data array info
        enemyHQLoc = roc.senseEnemyHQLocation();
        rand = new Random(roc.getID());
        myRange = roc.getType().attackRadiusSquared;
        enemyTeam = roc.getTeam().opponent();
        switch (roc.getType()) {
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
            roc.broadcast(58000, 1);
            //determine reduced size of board
            x = 240 - Math.abs(roc.getLocation().x - enemyHQLoc.x);
            y = 240 - Math.abs(roc.getLocation().y - enemyHQLoc.y);
            modSize = x * y;
            roc.broadcast(1, modSize);
            roc.broadcast(65534, x);
            roc.broadcast(65535, y);
            
            //move this to downtime area
           // initMemMap();
        } catch (GameActionException e) {
            System.out.println("exception in execHQ - pre infinite loop");
            e.printStackTrace();
        }

        while (true) {

            try {
                //sense nearby bots
                RobotInfo[] bots = roc.senseNearbyRobots(15, roc.getTeam());
               // System.out.println(bots.length);
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

            } catch (GameActionException e) {
                System.out.println("Unexpected exception in execHQ");
                e.printStackTrace();
                continue;
            }
        }
    }

    static void execBeav() {
        //TODO write a check that will see if the health has changed, if so, 'fight or flight'
        //^ really gauge your location and from there broadcast the info, or do something else
        try {
        modSize = roc.readBroadcast(1);
        }
        catch (GameActionException e) {
                System.out.println("Unexpected exception in execBeav");
                e.printStackTrace();
            }
        while (true) {
//            System.out.println("Beaver at ");
//            System.out.println(roc.getLocation().x);
//            System.out.println(roc.getLocation().y);
            try {
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
                    MapLocation m = roc.senseEnemyHQLocation();
                    MapLocation here = roc.getLocation();
                    tryMove(here.directionTo(m));
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
        }
        catch (GameActionException e) {
                System.out.println("Unexpected exception in execBeav");
                e.printStackTrace();
            }
        boolean surroundingsNotSensed = true;
        boolean distNotPublished = true;
        int count = 0;
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
                    if (distNotPublished) {
                        computeDistanceToEnemyHQ(roc.getLocation());
                        roc.broadcast(65000, roc.getLocation().hashCode());
                        distNotPublished = false;
                    } else {
                        publishSurroundings(count);
                        surroundingsNotSensed = false;
                    }
                }

            } catch (GameActionException e) {
                System.out.println("Unexpected exception in execTower");
                e.printStackTrace();
                continue;
            }
        }
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

    private static int computeDistanceToEnemyHQ(MapLocation location) {
        return location.distanceSquaredTo(roc.senseEnemyHQLocation());

    }

    
    private static void publishSurroundings(int count) throws GameActionException {
        MapLocation[] info = MapLocation.getAllMapLocationsWithinRadiusSq(roc.getLocation(),
                roc.getType().sensorRadiusSquared);
        for (int i = 0; i < info.length; i++) {
            //add the maplocation to the map
            updateLocationInfo(info[i]);
        }
    }

    private static void updateLocationInfo(MapLocation loc) {

        try{
       // System.out.println("detecting location info");
       // System.out.println(modSize);
            boolean isNegative = false;
            int abslochashCode = loc.hashCode();
            if (abslochashCode < 0)
            {
                abslochashCode = Math.abs(abslochashCode);
                isNegative = true;
            }
            
            int memLocation = abslochashCode % (modSize/2);
            if (!isNegative)
              memLocation = memLocation + (modSize/2);
        if (roc.senseOre(loc) > 10) {
           // System.out.println( loc.hashCode());
            if (isNegative)
            roc.broadcast(memLocation, 1);
            
        } else if (roc.senseOre(loc) > 20) {
           // System.out.println( loc.hashCode());
            roc.broadcast(memLocation, 2);
            
        } else if (roc.senseTerrainTile(loc) != TerrainTile.NORMAL) {
          //  System.out.println( loc.hashCode());
            roc.broadcast(memLocation, 0);
            
        }
        }
        
        catch (GameActionException g)
        {
            System.out.println("Exception caught in updatelocationinfo");
            g.printStackTrace();
        }
        //else broadcast nothing, and everyone will just simply assume that there is nothing in the way at that location
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

} //end of class



//depracated methods that will eventually get deleted

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