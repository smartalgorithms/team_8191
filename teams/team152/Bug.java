/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team152;

import battlecode.common.*;
import static battlecode.common.Direction.*;
import static battlecode.common.RobotType.*;

/**
 *
 * @author byrdie
 */
public class Bug {

    static boolean tracing = false; // true if we're currently tracing a wall

    static Direction previousDirection = null;  // last direction we took

    static int minDistanceSquared = -1;  // minimum distance to waypoint found

    static MapLocation possibleLocation;   // possible location for next move to be evaluated

    /**
     * compute next move according to the right hand rule
     *
     * @param rc
     * @param waypoint
     * @param attemptDir
     * @return
     */
    public static Direction computeMove(RobotController rc, int[] waypoint, Direction attemptDir) {

        

        MapLocation waypointLocation = new MapLocation(waypoint[0], waypoint[1]);

        Direction possibleMove = NONE;

        /*if this is the first turn in the trace, set the previous direction to last attempted direction*/
        if (previousDirection == null) {
            previousDirection = attemptDir;

            minDistanceSquared = rc.getLocation().distanceSquaredTo(waypointLocation);
            System.out.println("Entered bug mode, squared distance to waypoint is " + minDistanceSquared);
        }
        
        

        boolean canMove = false;

        /*if forward is empty and right is empty, move right*/
        if (rc.canMove(previousDirection)) {
            
            switch (previousDirection) {
                case NORTH:
                    possibleMove = EAST;
                    if (rc.canMove(possibleMove)) {
                        canMove = true;
                    }
                    break;
                case EAST:
                    possibleMove = SOUTH;
                    if (rc.canMove(possibleMove)) {
                        canMove = true;
                    }
                    break;
                case SOUTH:
                    possibleMove = WEST;
                    if (rc.canMove(possibleMove)) {
                        canMove = true;
                    }
                    break;
                case WEST:
                    possibleMove = NORTH;
                    if (rc.canMove(possibleMove)) {
                        canMove = true;
                    }
                    break;
            }
        }

                 
        /*else if forward is empty but right is blocked move forward*/          
        if (!canMove) {    
            possibleMove = previousDirection;
            if (rc.canMove(previousDirection)) {  
                
                canMove = true;
            }
        }

        /* else if left is empty move left until we can move*/
        while (!canMove) {
            switch (possibleMove) {
                case NORTH:
                case NORTH_WEST:
                    possibleMove = WEST;
                    if (rc.canMove(possibleMove)) {
                        canMove = true;
                    }
                    break;
                case WEST:
                case SOUTH_WEST:
                    possibleMove = SOUTH;
                    if (rc.canMove(possibleMove)) {
                        canMove = true;
                    }
                    break;
                case SOUTH:
                case SOUTH_EAST:
                    possibleMove = EAST;
                    if (rc.canMove(possibleMove)) {
                        canMove = true;
                    }
                    break;
                case EAST:
                case NORTH_EAST:
                    possibleMove = NORTH;
                    if (rc.canMove(possibleMove)) {
                        canMove = true;
                    }
                    break;
            }
        }

        previousDirection = possibleMove;
        
        
        possibleLocation = rc.getLocation().add(possibleMove);
        int newDistanceSquared = possibleLocation.distanceSquaredTo(waypointLocation);
        
        if (newDistanceSquared < minDistanceSquared) {          
            System.out.println("Exited bug mode, distance to waypoint is " + newDistanceSquared);
            tracing = false;
            previousDirection = null;
            minDistanceSquared = -1;
            possibleLocation = null;
        }

        return possibleMove;
    }

    /**
     * Determines whether the map location in the given direction from the robot
     * is void, or contains a robot (only call if rc.canMove() returns false)
     *
     * @param rc
     * @param loc
     * @return 0 if movable, 1 if void, 2 if contains robot
     */
    public static int terrainTileState(RobotController roc, Direction dir) {

        if (!roc.canMove(dir)) {
            RobotInfo[] nearRobots = roc.senseNearbyRobots(2);   //sense all robots within moving distance

            possibleLocation = roc.getLocation().add(dir);   // possible location for testing 

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
            return 0;
        }

    }

}
