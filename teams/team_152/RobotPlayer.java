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
    static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
        Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

    public static void run(RobotController rc) {
        roc = rc;
        rand = new Random(roc.getID());

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

        }
    }

    static void execHQ() {
        while (true) {
            try {
                if (roc.isCoreReady() && roc.getTeamOre() >= 100) //thoretically we are going to change this so that it is more deterministic
                //as opposed to random
                {
                    trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
                }
            } catch (GameActionException e) {
                continue;
            }
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

    static void execBeav() {
        while (true) {
            try {

        //run a check to      
                if (roc.isWeaponReady()) {
                    RobotInfo[] enemies = roc.senseNearbyRobots(roc.getType().attackRadiusSquared);
                    if (enemies.length > 0) {
                        roc.attackLocation(enemies[0].location);
                    }
                }
                if (roc.isCoreReady()) {
                    MapLocation m = roc.senseEnemyHQLocation();
                    //MapLocation here = roc.getLocation();
                    tryMove(roc.senseHQLocation().directionTo(m));
                }
            } catch (GameActionException e) {
                continue;
            }

        }
    }
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

    static void execTower() {
        while (true) {
            try {

                if (roc.isWeaponReady()) {
                    RobotInfo[] enemies = roc.senseNearbyRobots(roc.getType().attackRadiusSquared);
                    if (enemies.length > 0) {
                        roc.attackLocation(enemies[0].location);
                    }
                }
            } catch (GameActionException e) {
                continue;
            }
        }
    }

} //end of class
