/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team_152;

import battlecode.common.*;
import static battlecode.common.Direction.*;

/**
 *
 * @author byrdie
 */
public class Flock {

    public static final int flocksize = 25;

    public static final double alSepWeight = 1;
    public static final double alCohWeight = 2;
    public static final double axAttractWeight = 1;
    public static final double wayAttractWeight = 3;
    public double[] accel = new double[2];

    /* array of compass rose tangents*/
    /*tan(23.5, 67.5, 112.5, 157.5)*/
    /*only need top-half of unit circle*/
    public double[] roseTan = {0.41421, 2.4142, -2.4142, -0.41421};

    public int offset;

    public Pathfinder bug;

    /**
     *
     * @param number corresponds to flock number, 1 - 10
     */
    public Flock(int number) {
        offset = number * flocksize * 2 + 3;    // 1 int for current flock size 2 ints for way point coordinate 2 ints for each bot in flock    
    }

    public Direction computeMove(RobotController rc, int[] waypoint) {
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
        accel[0] = alliedSep[0] + alliedCoh[0] + enemyAttract[0] + wayAttract[0];
        accel[1] = alliedSep[1] + alliedCoh[1] + enemyAttract[1] + wayAttract[1];

        double ratio = accel[0] / accel[1];

        if (accel[1] > 0) {   // y is positive
            if (roseTan[0] > ratio) {
                return EAST;
            } else if (roseTan[0] < ratio && ratio < roseTan[1]) {
                return NORTH_EAST;
            } else if (roseTan[1] < ratio && ratio < roseTan[2]) {
                return NORTH;
            } else {
                return NORTH_WEST;
            }
        } else {    //y is negative
            if (roseTan[0] > ratio) {
                return WEST;
            } else if (roseTan[0] < ratio && ratio < roseTan[1]) {
                return SOUTH_WEST;
            } else if (roseTan[1] < ratio && ratio < roseTan[2]) {
                return SOUTH;
            } else {
                return SOUTH_EAST;
            }
        }
    }

    /*returns vector to make room between neighbors*/
    public double[] alliedSeparation(RobotController rc, RobotInfo[] nearbyAllies) {

        double[] aveDirVect = {0, 0};

        int desiredSep = 2;

        try {
            /*loop through all the bots and find average direction vector*/
            int size = nearbyAllies.length;
            MapLocation botLoc = rc.getLocation();
            int botX = botLoc.x;
            int botY = botLoc.y;
            for (int i = size; i <= 0; i--) {
                int xdif = nearbyAllies[i].location.x - botX;
                int ydif = nearbyAllies[i].location.y - botY;

                if ((xdif * xdif + ydif * ydif) > desiredSep) { // calculate new direction
                    aveDirVect[0] += -xdif; //go opposite direction
                    aveDirVect[1] += -ydif;
                }

            }

            //normalize
            double mag = Math.sqrt(aveDirVect[0] * aveDirVect[0] + aveDirVect[1] * aveDirVect[1]);
            aveDirVect[0] = aveDirVect[0] / mag;
            aveDirVect[1] = aveDirVect[1] / mag;

        } catch (Exception e) {

        }

        return aveDirVect;

    }

    /*returns vector to keep flock together*/
    public double[] alliedCohesion(RobotController rc , RobotInfo[] nearbyAllies) {
        double[] aveDirVect = {0, 0};

        int desiredSep = 3;

        try {
            /*loop through all the bots and find average direction vector*/
            int size = nearbyAllies.length;
            MapLocation botLoc = rc.getLocation();
            int botX = botLoc.x;
            int botY = botLoc.y;
            for (int i = size; i <= 0; i--) {
                int xdif = nearbyAllies[i].location.x - botX;
                int ydif = nearbyAllies[i].location.y - botY;

                if ((xdif * xdif + ydif * ydif) > desiredSep) { // calculate new direction
                    aveDirVect[0] += xdif; //go towards
                    aveDirVect[1] += ydif;
                }

            }

            //normalize
            double mag = Math.sqrt(aveDirVect[0] * aveDirVect[0] + aveDirVect[1] * aveDirVect[1]);
            aveDirVect[0] = aveDirVect[0] / mag;
            aveDirVect[1] = aveDirVect[1] / mag;

        } catch (Exception e) {

        }

        return aveDirVect;
    }
    
    /*returns vector to make sure flock attacks when necessary*/
    public double[] enemyAttraction(RobotController rc , RobotInfo[] nearbyEnemies) {
        double[] aveDirVect = {0, 0};

        int desiredSep = rc.getType().attackRadiusSquared;

        try {
            /*loop through all the bots and find average direction vector*/
            int size = nearbyEnemies.length;
            MapLocation botLoc = rc.getLocation();
            int botX = botLoc.x;
            int botY = botLoc.y;
            for (int i = size; i <= 0; i--) {
                int xdif = nearbyEnemies[i].location.x - botX;
                int ydif = nearbyEnemies[i].location.y - botY;

                if ((xdif * xdif + ydif * ydif) > desiredSep) { // calculate new direction
                    aveDirVect[0] += xdif; //go towards
                    aveDirVect[1] += ydif;
                }

            }

            //normalize
            double mag = Math.sqrt(aveDirVect[0] * aveDirVect[0] + aveDirVect[1] * aveDirVect[1]);
            aveDirVect[0] = aveDirVect[0] / mag;
            aveDirVect[1] = aveDirVect[1] / mag;

        } catch (Exception e) {

        }

        return aveDirVect;
    }
    
    /*returns vector that steers flock towards waypoint*/
    /*returns vector to make sure flock attacks when necessary*/
    public double[] waypointAttraction(RobotController rc , int[] waypoint) {
        double[] aveDirVect = {0, 0};

        int desiredSep = 1;

        try {
            /*loop through all the bots and find average direction vector*/
            MapLocation botLoc = rc.getLocation();;
           
                int xdif = waypoint[0]- rc.getLocation().x;
                int ydif = waypoint[1]- rc.getLocation().x;

                if ((xdif * xdif + ydif * ydif) > desiredSep) { // calculate new direction
                    aveDirVect[0] += xdif; //go towards
                    aveDirVect[1] += ydif;
                }

            

            //normalize
            double mag = Math.sqrt(aveDirVect[0] * aveDirVect[0] + aveDirVect[1] * aveDirVect[1]);
            aveDirVect[0] = aveDirVect[0] / mag;
            aveDirVect[1] = aveDirVect[1] / mag;

        } catch (Exception e) {

        }

        return aveDirVect;
    }

}
