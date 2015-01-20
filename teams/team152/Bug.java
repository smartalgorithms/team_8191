/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team152;

import battlecode.common.*;
import static battlecode.common.Direction.*;
import static battlecode.common.RobotType.*;
import java.util.Random;

/**
 *
 * @author byrdie
 */
public class Bug {

    static boolean tracing = false; // true if we're currently tracing a wall
    static boolean right = true;    // true if we're currently tracing using right hand rule

    static Direction previousDirection = null;  // last direction we took

    static int minDistanceSquared = -1;  // minimum distance to waypoint found
    static MapLocation minLocation;

    static MapLocation possibleLocation;   // possible location for next move to be evaluated

    static Random rand;
    static int flockNum;

    static final int flockThreshold = 50;      // If flocking, we step out of bug after this many turns
    static int flockStep = 0;
    static int firstOptionNum = 0;     //number of times we have gone through the first statement in LH/RH rule cases
    /**
     * compute next move according to the right hand rule
     *
     * @param rc
     * @param waypoint
     * @param attemptDir
     * @return
     */
    public static Direction computeMove(RobotController rc, int[] waypoint, Direction attemptDir, MapLocation[] towers) {

        MapLocation waypointLocation = new MapLocation(waypoint[0], waypoint[1]);

        if (flockNum > -1) {
            int waypointFound = RobotPlayer.wayAck[flockNum];
            if (waypointFound < 0) { // follow left hand rule
                right = false;
            } else if (waypointFound > 0) { // follow right hand rule
                right = true;
            }
        }


        /*if this is the first turn in the trace, set the previous direction to last attempted direction*/
        if (previousDirection == null) {
            previousDirection = attemptDir;
            minLocation = rc.getLocation();
            minDistanceSquared = rc.getLocation().distanceSquaredTo(waypointLocation);
            rand = new Random(rc.getID());
            int leftOrRight = rand.nextInt(2);
            if (leftOrRight > 0) {
                right = false;
            }
            flockNum = RobotPlayer.flockNumber;
            System.out.println("Entered bug mode, squared distance to waypoint is " + minDistanceSquared);

        }
        Direction possibleMove;
        if (right) {
            System.out.println("Right!");
            possibleMove = rightHandRule(rc, towers);
        } else {
            System.out.println("Left!");
            possibleMove = leftHandRule(rc, towers);
        }

        previousDirection = possibleMove;

        possibleLocation = rc.getLocation().add(possibleMove);
        int newDistanceSquared = possibleLocation.distanceSquaredTo(waypointLocation);

        if (newDistanceSquared < minDistanceSquared || flockStep > flockThreshold) {
//        if (newDistanceSquared < minDistanceSquared) {
            System.out.println("Exited bug mode, distance to waypoint is " + newDistanceSquared);
            tracing = false;
            previousDirection = null;
            minDistanceSquared = -1;
            minLocation = null;
            possibleLocation = null;
            firstOptionNum = 0;
            flockStep = 0;
            right = true;
        } 
        else if (firstOptionNum > 4){
            possibleMove = RobotPlayer.intToDirection(RobotPlayer.rand.nextInt());
            System.out.println("Exited bug mode after 8, distance to waypoint is " + newDistanceSquared);
            tracing = false;
            previousDirection = null;
            minDistanceSquared = -1;
            minLocation = null;
            possibleLocation = null;
            firstOptionNum = 0;
            flockStep = 0;
            right = true;
        }

//        minDistanceSquared = minDistanceSquared + 1;  // slowly make the min distance larger to escape long wall hugs
        return possibleMove;
    }

    static Direction rightHandRule(RobotController rc, MapLocation[] towers) {

        Direction possibleMove = NONE;

        boolean canMove = false;

        /*if forward is empty and right is empty, move right*/
//        if (rc.canMove(previousDirection)) {
        if (terrainTileState(rc, previousDirection, towers) < 1) {
            
            switch (previousDirection) {
                case NORTH:
                    possibleMove = EAST;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case EAST:
                    possibleMove = SOUTH;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case SOUTH:
                    possibleMove = WEST;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case WEST:
                    possibleMove = NORTH;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
            }
        }

        /*else if forward is empty but right is blocked move forward*/
        if (!canMove) {
            possibleMove = previousDirection;
            if (terrainTileState(rc, possibleMove, towers) < 1) {
                firstOptionNum = 0;
                canMove = true;
            }
        } else {
            firstOptionNum++;
        }

        /* else if left is empty move left until we can move*/
        while (!canMove) {
            firstOptionNum = 0;
            switch (possibleMove) {
                case NORTH:
                case NORTH_WEST:
                    possibleMove = WEST;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case WEST:
                case SOUTH_WEST:
                    possibleMove = SOUTH;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case SOUTH:
                case SOUTH_EAST:
                    possibleMove = EAST;
//                    if (rc.canMove(possibleMove)) {                    if (terrainTileState(rc, possibleMove) < 1) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case EAST:
                case NORTH_EAST:
                    possibleMove = NORTH;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
            }
        }
        return possibleMove;
    }

    static Direction leftHandRule(RobotController rc, MapLocation[] towers) {

        Direction possibleMove = NONE;

        boolean canMove = false;

        /*if forward is empty and right is empty, move right*/
//        if (rc.canMove(previousDirection)) {
        if (terrainTileState(rc, previousDirection, towers) < 1) {

            switch (previousDirection) {
                case NORTH:
                    possibleMove = WEST;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case EAST:
                    possibleMove = NORTH;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case SOUTH:
                    possibleMove = EAST;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case WEST:
                    possibleMove = SOUTH;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
            }
        }

        /*else if forward is empty but right is blocked move forward*/
        if (!canMove) {
            possibleMove = previousDirection;
            if (terrainTileState(rc, possibleMove, towers) < 1) {
                firstOptionNum = 0;
                canMove = true;
            }
        }  else {
            firstOptionNum++;
        }

        /* else if left is empty move left until we can move*/
        while (!canMove) {
            firstOptionNum = 0;
            
            switch (possibleMove) {
                case NORTH:
                case NORTH_WEST:
                    possibleMove = EAST;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case WEST:
                case SOUTH_WEST:
                    possibleMove = NORTH;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case SOUTH:
                case SOUTH_EAST:
                    possibleMove = WEST;
//                    if (rc.canMove(possibleMove)) {                    if (terrainTileState(rc, possibleMove) < 1) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
                case EAST:
                case NORTH_EAST:
                    possibleMove = SOUTH;
//                    if (rc.canMove(possibleMove)) {
                    if (terrainTileState(rc, possibleMove, towers) < 1) {
                        canMove = true;
                    }
                    break;
            }
        }
        return possibleMove;
    }

    /**
     * Determines whether the map location in the given direction from the robot
     * is void, or contains a robot (only call if rc.canMove() returns false)
     *
     * @return 0 if movable, 1 if void, 2 if contains robot
     */
    public static int terrainTileState(RobotController roc, Direction dir, MapLocation[] towers) {

        possibleLocation = roc.getLocation().add(dir);   // possible location for testing 

        if (!roc.canMove(dir)) {
            RobotInfo[] nearRobots = roc.senseNearbyRobots(2);   //sense all robots within moving distance          

            /*see if the move we're trying to make intersects another robot or void square, if so, turn off tracing */
            int numNearRobots = nearRobots.length;
            for (int i = 0; i < numNearRobots; i++) {
                RobotType t = nearRobots[i].type;
                if (t == BASHER || t == BEAVER || t == COMMANDER || t == DRONE || t == LAUNCHER || t == MINER || t == MISSILE || t == SOLDIER || t == TANK) {
                    if (nearRobots[i].location.equals(possibleLocation)) {
                        return 2;
                    }
                }

            }

            return 1;
        } else {

            /*if we're avoiding towers, make sure we don't move into them*/
            if (towers != null) {
                if (inTowerRange(roc, towers, possibleLocation)) {
//                    System.out.println("Tower in Range!");
                    return 1;
                }
            }

            return 0;
        }

    }

    /**
     *
     * @param rc
     * @param towers
     * @param newLocation
     * @return true if in range of enemy towers, false if not
     */
    public static boolean inTowerRange(RobotController rc, MapLocation[] towers, MapLocation newLocation) {
        int attackRange = 24;

        int size = towers.length;
        for (int i = 0; i < size; i++) {
            if (newLocation.distanceSquaredTo(towers[i]) <= attackRange) {
//                System.out.println("Tower in Range!");
                return true;
            }
        }
        return false;

    }

}
