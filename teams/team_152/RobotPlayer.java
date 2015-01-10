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
    static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
        Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

    public static void run(RobotController rc) {
        
        roc = rc;
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
        try{
        roc.broadcast(42, 1);
        }
        catch (GameActionException e)
        {
            
        }
        while (true) {
            try {

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
        while (true) {
            try {

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
        while (true) {
            try {

                if (roc.isWeaponReady()) {
                    attackSomething();
                
                }
            } catch (GameActionException e) {
                System.out.println("Unexpected exception in execTower");
				e.printStackTrace();
                continue;
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
     */
    static boolean needSpawn(RobotType type) throws GameActionException {
        switch (type) {
            case HQ: {
                if (roc.readBroadcast(42) == 1) {
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
        System.out.println(roc.getType().toString());
        System.out.println(myRange);
        System.out.println(enemyTeam);
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
