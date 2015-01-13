/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team152;

import battlecode.common.*;
import static battlecode.common.Direction.*;

/**
 *
 * @author byrdie
 */
public class Flock {

    public static final int flocksize = 25;

    public static final double alSepWeight = 1.7;
    public static final double alCohWeight = 2.1;
    public static final double axAttractWeight = 2.0;
    public static final double wayAttractWeight = 1.0;

    /* array of compass rose tangents*/
    /*tan(23.5, 67.5, 112.5, 157.5)*/
    /*only need top-half of unit circle*/
    public static double[] roseTan = {0.41421, 2.4142, -2.4142, -0.41421};

    public static Direction computeMove(RobotController rc, int[] waypoint) {
        double[] accel = new double[2];
        Team allies = rc.getTeam();
        Team enemies = allies.opponent();
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(24, allies);
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(24, enemies);

        //accumalate acceleration based on these rules
        double[] alliedSep = alliedSeparation(rc, nearbyAllies);   //Separation from allies
        double[] alliedCoh = alliedCohesion(rc, nearbyAllies);     // Cohesion to allies
        double[] enemyAttract = enemyAttraction(rc, nearbyEnemies);      // Attraction to enemies
        double[] wayAttract = waypointAttraction(rc, waypoint);    //Waypoint attraction

        // apply weights to each acceleration
        alliedSep[0] = alSepWeight * alliedSep[0];
        alliedSep[1] = alSepWeight * alliedSep[1];

        alliedCoh[0] = alCohWeight * alliedCoh[0];
        alliedCoh[1] = alCohWeight * alliedCoh[1];

        enemyAttract[0] = axAttractWeight * enemyAttract[0];
        enemyAttract[1] = axAttractWeight * enemyAttract[1];

        wayAttract[0] = wayAttractWeight * wayAttract[0];
        wayAttract[1] = wayAttractWeight * wayAttract[1];

        /* calculate total acceleration from all terms */
//        accel[0] = alliedCoh[0] + enemyAttract[0] + wayAttract[0];
//        accel[1] = alliedCoh[1] + enemyAttract[1] + wayAttract[1];
        accel[0] = alliedSep[0] + alliedCoh[0] + enemyAttract[0] + wayAttract[0];
        accel[1] = alliedSep[1] + alliedCoh[1] + enemyAttract[1] + wayAttract[1];
//        accel[0] = wayAttract[0];
//        accel[1] = wayAttract[1];
//        System.out.println(accel[0] + ", " + accel[1]);

        return vectorToDirection(accel);

    }

    public static Direction computeWaypointMove(RobotController rc, int[] waypoint) {
//        double[] accel = new double[2];

        double[] wayAttract = waypointAttraction(rc, waypoint);    //Waypoint attraction

        return vectorToDirection(wayAttract);
    }

    public static Direction vectorToDirection(double[] accel) {
        /*Check if we should move at all in any cardinal direction*/
        if (accel[0] == 0.0) {  // x component is zero
            if (accel[1] > 0.0) { //y component is positive
                return SOUTH;
            } else if (accel[1] < 0.0) {    // y component is negative
                return NORTH;
            } else {            // y component is zero
                System.out.println("No acceleration!");
                return NONE;
            }
        }

        if (accel[1] == 0.0) {    // y component is zero
            if (accel[0] > 0.0) { // x component is positive
                return EAST;
            } else if (accel[0] < 0.0) { // x component is negative
                return WEST;
            } else {    // y component is also zero (may be redundant)
                System.out.println("No acceleration!");
                return NONE;
            }
        }

        /* otherwise find approximate angle of travel*/
        double ratio = accel[1] / accel[0]; //tanx

        if (accel[1] < 0.0) {   // y is negative
            if (accel[0] < 0.0) {   // x is negative             
                if (ratio < roseTan[0]) {
                    return WEST;
                } else if (ratio > roseTan[0] && ratio < roseTan[1]) {
                    return NORTH_WEST;
                } else {
                    return NORTH;
                }
            } else {    // x is positive
                if (ratio < roseTan[2]) {
                    return NORTH;
                } else if (ratio > roseTan[2] && ratio < roseTan[3]) {
                    return NORTH_EAST;
                } else {
                    return EAST;
                }
            }
        } else {    //y is positive
            if (accel[0] > 0.0) {   // x is positive
                if (ratio < roseTan[0]) {
                    return EAST;
                } else if (ratio > roseTan[0] && ratio < roseTan[1]) {
                    return SOUTH_EAST;
                } else {
                    return SOUTH;
                }
            } else {    // x is negative
                if (ratio < roseTan[2]) {
                    return SOUTH;
                } else if (ratio > roseTan[2] && ratio < roseTan[3]) {
                    return SOUTH_WEST;
                } else {
                    return WEST;
                }

            }
        }
    }

    /*returns vector to make room between neighbors*/
    public static double[] alliedSeparation(RobotController rc, RobotInfo[] nearbyAllies) {

        double[] aveDirVect = {0.0, 0.0};

        int desiredSep = 1;

        try {
            /*loop through all the bots and find average direction vector*/
            int size = nearbyAllies.length;
            MapLocation botLoc = rc.getLocation();
            int botX = botLoc.x;
            int botY = botLoc.y;
            for (int i = size - 1; i >= 0; i--) {
                int xdif = nearbyAllies[i].location.x - botX;
                int ydif = nearbyAllies[i].location.y - botY;

                if ((xdif * xdif + ydif * ydif) > desiredSep) { // calculate new direction
                    aveDirVect[0] += -xdif; //go opposite direction
                    aveDirVect[1] += -ydif;
                }

//                System.out.println(xdif + ", " + ydif);
            }

            //normalize
            double mag = Math.sqrt(aveDirVect[0] * aveDirVect[0] + aveDirVect[1] * aveDirVect[1]);
            if (mag != 0.0) {
                aveDirVect[0] = aveDirVect[0] / mag;
                aveDirVect[1] = aveDirVect[1] / mag;
            }

        } catch (Exception e) {
            System.out.println("There was an exeption in allied separation calculation");
            e.printStackTrace();
        }

        return aveDirVect;

    }

    /*returns vector to keep flock together*/
    public static double[] alliedCohesion(RobotController rc, RobotInfo[] nearbyAllies) {
        double[] aveDirVect = {0.0, 0.0};

        int desiredSep = 2;

        try {
            /*loop through all the bots and find average direction vector*/
            int size = nearbyAllies.length;
            MapLocation botLoc = rc.getLocation();
            int botX = botLoc.x;
            int botY = botLoc.y;
            for (int i = size - 1; i >= 0; i--) {
                int xdif = nearbyAllies[i].location.x - botX;
                int ydif = nearbyAllies[i].location.y - botY;

                if ((xdif * xdif + ydif * ydif) > desiredSep) { // calculate new direction
                    aveDirVect[0] += xdif; //go towards
                    aveDirVect[1] += ydif;
                }

            }

            //normalize
            double mag = Math.sqrt(aveDirVect[0] * aveDirVect[0] + aveDirVect[1] * aveDirVect[1]);
            if (mag != 0.0) {
                aveDirVect[0] = aveDirVect[0] / mag;
                aveDirVect[1] = aveDirVect[1] / mag;
            }

//                        System.out.println(aveDirVect[0] + ", " + aveDirVect[1]);
        } catch (Exception e) {
            System.out.println("There was an exeption in allied cohesion calculation");
            e.printStackTrace();
        }

        return aveDirVect;
    }

    /*returns vector to make sure flock attacks when necessary*/
    public static double[] enemyAttraction(RobotController rc, RobotInfo[] nearbyEnemies) {
        double[] aveDirVect = {0.0, 0.0};

        int desiredSep = rc.getType().attackRadiusSquared;

        try {
            /*loop through all the bots and find average direction vector*/
            int size = nearbyEnemies.length;
            MapLocation botLoc = rc.getLocation();
            int botX = botLoc.x;
            int botY = botLoc.y;
            if (size != 0) {
                for (int i = size - 1; i >= 0; i--) {
                    int xdif = nearbyEnemies[i].location.x - botX;
                    int ydif = nearbyEnemies[i].location.y - botY;

                    if ((xdif * xdif + ydif * ydif) > desiredSep) { // calculate new direction
                        aveDirVect[0] += xdif; //go towards
                        aveDirVect[1] += ydif;
                    }

                }
            }

            //normalize
            double mag = Math.sqrt(aveDirVect[0] * aveDirVect[0] + aveDirVect[1] * aveDirVect[1]);

            if (mag != 0.0) {
                aveDirVect[0] = aveDirVect[0] / mag;
                aveDirVect[1] = aveDirVect[1] / mag;
            }

//            System.out.println(aveDirVect[0] + ", " + aveDirVect[1]);
        } catch (Exception e) {
            System.out.println("There was an exeption in enemy attraction calculation");
            e.printStackTrace();
        }

        return aveDirVect;
    }

    /*returns vector that steers flock towards waypoint*/
    /*returns vector to make sure flock attacks when necessary*/
    public static double[] waypointAttraction(RobotController rc, int[] waypoint) {
        double[] aveDirVect = {0.0, 0.0};

        int desiredSep = 0;

        try {
            /*loop through all the bots and find average direction vector*/
            MapLocation botLoc = rc.getLocation();;

            int xdif = waypoint[0] - rc.getLocation().x;
            int ydif = waypoint[1] - rc.getLocation().y;

            if ((xdif * xdif + ydif * ydif) > desiredSep) { // calculate new direction
                aveDirVect[0] += xdif; //go towards
                aveDirVect[1] += ydif;
            }

            //normalize
            double mag = Math.sqrt(aveDirVect[0] * aveDirVect[0] + aveDirVect[1] * aveDirVect[1]);
            if (mag != 0.0) {
                aveDirVect[0] = aveDirVect[0] / mag;
                aveDirVect[1] = aveDirVect[1] / mag;
            }
//            System.out.println(aveDirVect[0] + ", " + aveDirVect[1]);

        } catch (Exception e) {
            System.out.println("There was an exeption in waypoint attraction calculation");
            e.printStackTrace();
        }

        return aveDirVect;
    }

}
