/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team152;

import battlecode.common.*;
import java.util.*;



/**
 *
 * @author byrdie
 * @author albmin
 */
//TODO : in HQ code, clear the arraylist of nearby bots every 20-50 rounds
public class RobotPlayer {

    static int spawnPos = 0;
    static RobotController roc;
    static Random rand;
    static int myRange;
    static Team enemyTeam;
    static int numBeavers = 0;  //for HQ to keep track of beavers
    static int mineFarmNumX = -10;    //for keeping track of mining squares
    static int mineFarmNumY = -10;

    /*memory map*/
    static final int numBeaversBucket = 58000;
    static final int numSoldiersBucket = 58001;

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
    static final int launcherFlockNum = 60077;

    static final int[] wayAck = {60090, 60091, 60092, 60093, 60094, 60095, 60096, 60097, 60098, 60099};

    static int unitIteration = 0;
    static double health = 1000.0;
    static boolean healthFlag = true;

    /**
     * This enum class will specify the location of the request flag for the
     * building request, with an INVARIANT specifying that the x coordinate will
     * be reqLoc + 1, and the y-coordinate will be reqLoc+2
     */
    static enum buildingReq {

        Barracks(60080, RobotType.BARRACKS), MinerFactory(60086, RobotType.MINERFACTORY),
        SupplyDepot(60089, RobotType.SUPPLYDEPOT), TankFactory(60083, RobotType.TANKFACTORY),
        Helipad(60092, RobotType.HELIPAD),
        TrainingField(60095, RobotType.TRAININGFIELD), HandwashStation(60098, RobotType.HANDWASHSTATION),
        AeroSpaceLab(60101, RobotType.AEROSPACELAB), TechInstitute(60104, RobotType.TECHNOLOGYINSTITUTE);
        private final int reqLoc;
        private final RobotType type;

        private buildingReq(int value, RobotType typ) {
            this.type = typ;
            this.reqLoc = value;
        }
    }

    /*action booleans*/
    static boolean buildReq = false;    //set to true for contruct building request
    static boolean ferryReq = false; //set to true to tell robot to ferry supplies to the building it makes
    static int buildingType = 0;    // 0=none, 1=supply depot, 2=minerfactory, 3=techinstitute, 4=barracks, 5=helipad, 6=trainingfield, 7=tankfactory, 8=aerospacelab, 9=handwash
    static int[] waypoint = new int[2];    // current waypoint of the robot
    static MapLocation waypointLoc;
    static int modSize;
    static int x;
    static int y;
    static boolean surroundingsNotSensed = true;

    //probably clean, but i dont know the bytecode cost of instantiating after declaring
    int enemyHQDist = computeDistanceToEnemyHQ(roc.getLocation());
    static MapLocation enemyHQLoc;
    static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
        Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    static boolean firstMove = true;
    static int flockNumber = -1;
    static Direction facing = null;
    static MapLocation[] friendlyTowers;

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
                break;
            case HELIPAD:
                execHelipad();
                break;
            case DRONE:
                execDrone();
                break;
            case COMMANDER:
                execCommander();
            case TRAININGFIELD:
                execTrainingField();
            case TANKFACTORY:
                execTankFact();
                break;
            case SUPPLYDEPOT:
                execSupplyDepot();
                break;
            case TANK:
                execTank();
                break;

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

                friendlyTowers = roc.senseTowerLocations();

                roc.broadcast(58000, 1);
                //determine reduced size of board
                int x = 240 - (roc.getLocation().x - enemyHQLoc.x);
                int y = 240 - (roc.getLocation().y - enemyHQLoc.y);
                int modSize = x * y;
                roc.broadcast(1, modSize);
                roc.broadcast(65534, x);
                roc.broadcast(65535, y);

                //broadcast the first waypoint for flock 0
                int wayX = (enemyHQLoc.x - roc.getLocation().x) / 5 + roc.getLocation().x;
                int wayY = (enemyHQLoc.y - roc.getLocation().y) / 5 + roc.getLocation().y;
                System.out.println(wayX + ", " + wayY);
                roc.broadcast(currWayBuckets[0][0], wayX);
                roc.broadcast(currWayBuckets[0][1], wayY);
                roc.broadcast(beavFlockNum, 0);

                //broadcast first waypoint for flock 1
                wayX = (enemyHQLoc.x - roc.getLocation().x) / 3 + roc.getLocation().x;
                wayY = (enemyHQLoc.y - roc.getLocation().y) / 3 + roc.getLocation().y;
                System.out.println(wayX + ", " + wayY);
                roc.broadcast(currWayBuckets[1][0], wayX);
                roc.broadcast(currWayBuckets[1][1], wayY);

                //broadcast first waypoint for flock 2
                MapLocation buildLocation;
                buildLocation = getTowerBuildLocation();

                wayX = buildLocation.x;
                wayY = (enemyHQLoc.y - buildLocation.y) / 6 + buildLocation.y;
                System.out.println(wayX + ", " + wayY);
                roc.broadcast(currWayBuckets[2][0], wayX);
                roc.broadcast(currWayBuckets[2][1], wayY);

                //request barracks
//                requestBuilding(buildingReq.Barracks, friendlyTowers[0].x, friendlyTowers[0].y, true);
                //TODO need a better way to determine where I can put this, I think error checking is just needed overall for placing a building
                requestBuilding(buildingReq.MinerFactory, roc.senseHQLocation().x + 2, roc.senseHQLocation().y, false);
                 
                requestBuilding(buildingReq.SupplyDepot, roc.senseHQLocation().x + 4, roc.senseHQLocation().y, true);
//
//                requestBuilding(buildingReq.TankFactory, ((enemyHQLoc.x - roc.getLocation().x) / 5 + roc.getLocation().x),
//                        ((enemyHQLoc.y - roc.getLocation().y) / 5 + roc.getLocation().y), true);
            }

        } catch (GameActionException e) {
            System.out.println("exception in execHQ - pre infinite loop");
            e.printStackTrace();
        }

        while (true) {
            try {
                transferSupplies();
                 

                //this is a little hacky, but i think it could be the start of 
                //pre-game data
                RobotInfo[] myRobots = roc.senseNearbyRobots(999999, roc.getTeam());
                int numSoldiers = 0;
                int numBashers = 0;
                int numBeavers2 = 0;
                int numBarracks = 0;
                int numMiners = 0;
                for (RobotInfo r : myRobots) {
                    RobotType type = r.type;
                    if (type == RobotType.SOLDIER) {
                        numSoldiers++;
                    } else if (type == RobotType.BASHER) {
                        numBashers++;
                    } else if (type == RobotType.BEAVER) {
                        numBeavers2++;
                    } else if (type == RobotType.BARRACKS) {
                        numBarracks++;
                    } else if (type == RobotType.MINER) {
                        numMiners++;
                    }
                }

                MapLocation buildLocation;
                buildLocation = getTowerBuildLocation();

                if (Clock.getRoundNum() == 75) {
//                    requestBuilding(buildingReq.Barracks, ((enemyHQLoc.x - roc.getLocation().x) / 6 + roc.getLocation().x), ((enemyHQLoc.y - roc.getLocation().y) / 6 + roc.getLocation().y), false);
                    requestBuilding(buildingReq.Barracks, buildLocation.x, buildLocation.y, false);
                    numBeavers--;
                    
                }
                if (Clock.getRoundNum() == 300) {
                    requestBuilding(buildingReq.SupplyDepot, roc.senseHQLocation().x + 6, roc.senseHQLocation().y + 2, false);
                    numBeavers--;
                     requestBuilding(buildingReq.Helipad,((enemyHQLoc.x - roc.getLocation().x) / 10 + roc.getLocation().x),
                            ((enemyHQLoc.y - roc.getLocation().y) / 10+ roc.getLocation().y) , false);
                     numBeavers--;
                }
                if (Clock.getRoundNum() == 200) {
//                    requestBuilding(buildingReq.TankFactory, ((enemyHQLoc.x - roc.getLocation().x) / 5 + roc.getLocation().x),
//                            ((enemyHQLoc.y - roc.getLocation().y) / 5 + roc.getLocation().y), false);
                    requestBuilding(buildingReq.TankFactory, buildLocation.x, buildLocation.y, false);
                    numBeavers--;
                }
                if (Clock.getRoundNum() == 220) {
//                    requestBuilding(buildingReq.TankFactory, ((enemyHQLoc.x - roc.getLocation().x) / 5 + roc.getLocation().x + 2),
//                            ((enemyHQLoc.y - roc.getLocation().y) / 5 + roc.getLocation().y), false);
                    requestBuilding(buildingReq.TankFactory, buildLocation.x, buildLocation.y, false);
                    numBeavers--;
                }
                if (Clock.getRoundNum() == 240) {
//                    requestBuilding(buildingReq.TankFactory, ((enemyHQLoc.x - roc.getLocation().x) / 5 + roc.getLocation().x + 4),
//                            ((enemyHQLoc.y - roc.getLocation().y) / 5 + roc.getLocation().y), false);
                    requestBuilding(buildingReq.TankFactory, buildLocation.x, buildLocation.y, false);
                    numBeavers--;
                }
//                if (Clock.getRoundNum() == 260) {
////                    requestBuilding(buildingReq.TankFactory, ((enemyHQLoc.x - roc.getLocation().x) / 5 + roc.getLocation().x + 6),
////                            ((enemyHQLoc.y - roc.getLocation().y) / 5 + roc.getLocation().y), false);
//                    requestBuilding(buildingReq.TankFactory, buildLocation.x, buildLocation.y, false);
//                    numBeavers--;
//                }
                if (Clock.getRoundNum() == 13) {
                    ordertTowerPQ();
                }
                if (Clock.getRoundNum() % 15 == 0) {
                    botList = new ArrayList<Integer>();
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

                roc.broadcast(numBeaversBucket, numBeavers);
                if (needSpawn(roc.getType())) {
                    if (roc.isCoreReady() && roc.getTeamOre() >= 100) //thoretically we are going to change this so that it is more deterministic
                    //as opposed to random
                    {
                        trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
                        numBeavers++;
                        roc.broadcast(numBeaversBucket, numBeavers);
                    }
                }
                if (roc.isWeaponReady()) {
                    attackSomething();
                }
                if (Clock.getRoundNum() == 750) {
//                    int wayX = (enemyHQLoc.x - roc.getLocation().x) * 3 / 4 + roc.getLocation().x;
//                    int wayY = (enemyHQLoc.y - roc.getLocation().y) * 3 / 4 + roc.getLocation().y;
//                    System.out.println(wayX + ", " + wayY);
//                    roc.broadcast(currWayBuckets[1][0], wayX);
//                    roc.broadcast(currWayBuckets[1][1], wayY);
                    MapLocation way = getClosestEnemyTower();
                    System.out.println(way.x + ", " + way.y);
                    roc.broadcast(currWayBuckets[1][0], way.x);
                    roc.broadcast(currWayBuckets[1][1], way.y);
                }

                if (Clock.getRoundNum() == 875) {
                    int wayX = (enemyHQLoc.x - roc.getLocation().x) / 3 + roc.getLocation().x;
                    int wayY = (enemyHQLoc.y - roc.getLocation().y) / 3 + roc.getLocation().y;
                    System.out.println(wayX + ", " + wayY);
                    roc.broadcast(currWayBuckets[1][0], wayX);
                    roc.broadcast(currWayBuckets[1][1], wayY);
                }

                if (Clock.getRoundNum() == 1000) {

//                    int wayX = (enemyHQLoc.x - roc.getLocation().x) / 3 + roc.getLocation().x;
//                    int wayY = (enemyHQLoc.y - roc.getLocation().y) / 3 + roc.getLocation().y;
//                    roc.broadcast(currWayBuckets[1][0], wayX);
//                    roc.broadcast(currWayBuckets[1][1], wayY);
                    MapLocation way = getClosestEnemyTower();
                    System.out.println(way.x + ", " + way.y);
                    roc.broadcast(currWayBuckets[1][0], way.x);
                    roc.broadcast(currWayBuckets[1][1], way.y);
                }

                if (Clock.getRoundNum() == 1125) {
                    int wayX = (enemyHQLoc.x - roc.getLocation().x) / 3 + roc.getLocation().x;
                    int wayY = (enemyHQLoc.y - roc.getLocation().y) / 3 + roc.getLocation().y;
                    System.out.println(wayX + ", " + wayY);
                    roc.broadcast(currWayBuckets[1][0], wayX);
                    roc.broadcast(currWayBuckets[1][1], wayY);
                }

                if (Clock.getRoundNum() == 1250) {
//                    int wayX = (enemyHQLoc.x - roc.getLocation().x) * 7 / 8 + roc.getLocation().x;
//                    int wayY = (enemyHQLoc.y - roc.getLocation().y) * 7 / 8 + roc.getLocation().y;
//                    System.out.println(wayX + ", " + wayY);
//                    roc.broadcast(currWayBuckets[1][0], wayX);
//                    roc.broadcast(currWayBuckets[1][1], wayY);
                    MapLocation way = getClosestEnemyTower();
                    System.out.println(way.x + ", " + way.y);
                    roc.broadcast(currWayBuckets[1][0], way.x);
                    roc.broadcast(currWayBuckets[1][1], way.y);
                }
                if (Clock.getRoundNum() == 1500) {
                    int wayX = (enemyHQLoc.x - roc.getLocation().x) / 3 + roc.getLocation().x;
                    int wayY = (enemyHQLoc.y - roc.getLocation().y) / 3 + roc.getLocation().y;
                    System.out.println(wayX + ", " + wayY);
                    roc.broadcast(currWayBuckets[1][0], wayX);
                    roc.broadcast(currWayBuckets[1][1], wayY);
                }
                if (Clock.getRoundNum() == 1750) {
//                    int wayX = enemyHQLoc.x;
//                    int wayY = enemyHQLoc.y;
//                    System.out.println(wayX + ", " + wayY);
//                    roc.broadcast(currWayBuckets[1][0], wayX);
//                    roc.broadcast(currWayBuckets[1][1], wayY);
                    MapLocation way = getClosestEnemyTower();
                    System.out.println(way.x + ", " + way.y);
                    roc.broadcast(currWayBuckets[1][0], way.x);
                    roc.broadcast(currWayBuckets[1][1], way.y);

                }
                if (Clock.getRoundNum() == 2000) {
                    int wayX = (enemyHQLoc.x - roc.getLocation().x) / 3 + roc.getLocation().x;
                    int wayY = (enemyHQLoc.y - roc.getLocation().y) / 3 + roc.getLocation().y;
                    System.out.println(wayX + ", " + wayY);
                    roc.broadcast(currWayBuckets[1][0], wayX);
                    roc.broadcast(currWayBuckets[1][1], wayY);
                }
                if (Clock.getRoundNum() == 2100) {
                    int wayX = enemyHQLoc.x;
                    int wayY = enemyHQLoc.y;
                    System.out.println(wayX + ", " + wayY);
                    roc.broadcast(currWayBuckets[1][0], wayX);
                    roc.broadcast(currWayBuckets[1][1], wayY);
                }
                if (roc.getRoundLimit() - Clock.getRoundNum() == 400) {
                    int wayX = enemyHQLoc.x;
                    int wayY = enemyHQLoc.y;
                    System.out.println(wayX + ", " + wayY);
                    roc.broadcast(currWayBuckets[1][0], wayX);
                    roc.broadcast(currWayBuckets[1][1], wayY);
                    roc.broadcast(currWayBuckets[2][0], wayX);
                    roc.broadcast(currWayBuckets[2][1], wayY);
                }
                transferSupplies();
                roc.yield();

            } catch (GameActionException e) {
                System.out.println("Unexpected exception in execHQ");
                e.printStackTrace();
                continue;
            }

        }
    }

    static void execBeav() {
        MapLocation partner;  //once a robot is built by the beaver, it will be referenced here
        int[] origWayPoint = new int[2];
        RobotType toBuild = null;
        int delayBucket = 0;  //if we encounter a buildDelay, this will store the message channel to get the build alert from
        boolean buildDelay = false;
        try {

            if (firstMove) {

                // we don't have to worry about pathfinding due to the building target is right next to us
//                    tryBuild(oppositeDirec(roc.getLocation().directionTo(roc.senseHQLocation())), RobotType.MINERFACTORY); 
                firstMove = false;
                flockNumber = roc.readBroadcast(beavFlockNum);

                waypoint[0] = roc.readBroadcast(currWayBuckets[flockNumber][0]);
                waypoint[1] = roc.readBroadcast(currWayBuckets[flockNumber][1]);

                /*loop here to see if there is anything requested for build*/
                for (buildingReq br : buildingReq.values()) {
                    int flag = roc.readBroadcast(br.reqLoc);
                    if ((flag == 1 || flag == 2)) {
                        System.out.println("I'm building a " + br.toString());
                        roc.broadcast(br.reqLoc, 0);
                        buildReq = true;
                        toBuild = br.type;
                        if (flag == 2) {
                            System.out.println("Builddelay true on " + br.type.toString());
                            buildDelay = true;
                            delayBucket = br.reqLoc;
                        }
                        MapLocation locDirective = getBuildingRequestLoc(br.reqLoc);
                        waypoint[0] = locDirective.x;
                        waypoint[1] = locDirective.y;
                        break; //let another robot get the next request
                    }

                }

            }

            modSize = roc.readBroadcast(1);
        } catch (GameActionException e) {
            System.out.println("Exception caught in pre-loop of execBeav");
            e.printStackTrace();
        }

        while (true) {
            try {
                  
                System.out.println("Waypoint: " + waypoint[0] + ", " + waypoint[1]);
                if (roc.isCoreReady()) {
                    if (!buildReq && !ferryReq) {
                        //we've arrived at location of waypoint, aka the beaver's building, so lets drop off
                        // a reasonable amount of supplies and go pick up more
                        //TODO: add a check here to determine the building type, along with a calculation
                        //for the journey back to get more supplies (we wouldn't want to waste any!)
                        if ((roc.getLocation().distanceSquaredTo(new MapLocation(waypoint[0], waypoint[1])) < 15)) {
                            //    System.out.println("transfer proximity succeeded");
                            boolean successfulTransfer = false;
                            //determine if there exists a friendly building in range to pass supplies to
                            //this method call below could also be used to get more info, but im trying to keep computation
                            //somewhat down at the moment...
                            RobotInfo[] surroundingData = roc.senseNearbyRobots(15, roc.getTeam());
                            //  System.out.println(Arrays.toString(surroundingData));
                            for (int i = 0; i < surroundingData.length; i++) {
                                if (surroundingData[i].type == toBuild) //if the type is the same as the one we built
                                //should add an ID check too               // not the best way, but if we're consistent, will be fine.. (or we need to be more clever)
                                {
                                    roc.transferSupplies(9999999, surroundingData[i].location);  //TODO add exception handling on this line
                                    successfulTransfer = true;
                                }
                            }
                            if (successfulTransfer) {

                                MapLocation temp = roc.senseHQLocation();
                                origWayPoint[0] = waypoint[0];
                                origWayPoint[1] = waypoint[1];
                                waypoint[0] = temp.x;
                                waypoint[1] = temp.y;
                                ferryReq = true;
                            }
                        }

                        takeWaypointMove(waypoint);

                    } else if (buildReq) {
                        if (waypoint[0] == roc.getLocation().x && waypoint[1] == roc.getLocation().y) {
                            System.out.println("At build location with buildDelay equal to " + buildDelay);
                            while (buildDelay == true) {
                                if (roc.isCoreReady()) {
                                    roc.mine();
                                }
                                if (roc.readBroadcast(delayBucket) == 1) {
                                    System.out.println("Im breaking out");
                                    break;
                                }
                                roc.yield();
                            }
                            buildDelay = false;
                            tryBuild(Direction.NORTH, toBuild);
                            buildReq = false;
                            continue;
                        }

                        takeWaypointMove(waypoint);

                    } else if (ferryReq) {

                        //check to see if at HQ, if so, get more supplies and reset waypoint
                        if ((roc.getLocation().distanceSquaredTo(new MapLocation(waypoint[0], waypoint[1])) < 15)) {
                            //spin at this location until we're automatically replenished
                            while (roc.getSupplyLevel() <= 10) {
                                if (roc.isCoreReady()) {
                                    roc.mine();
                                }
                            }
                            ferryReq = false;

                            waypoint[0] = origWayPoint[0];
                            waypoint[1] = origWayPoint[1];
                            takeWaypointMove(waypoint);
                            continue;
                        } else {
                            takeWaypointMove(waypoint);
                        }

                        //else I'd assume we just want to keep moving, seeing as the check for the waypoint
                        //where we start ferrying is checked above (I think..)
                        //also once we get to HQ, we need to set ferryReq to false and reset the waypoints to the Building struct
                    }

                }

                //TODO need to add 'passive mode' to a robot, that way it doesn't attack unless
                //   it itself was attacked
                if (roc.isWeaponReady()) //do this after the main purpose of the robot duties
                {
                    attackSomething();
                } //TODO once the pathfinding gets resolved, implement this method, for now though just yield
                //                else if(surroundingsNotSensed) 
                //                    publishSurroundings();  //TODO fix this, currently it just attacks within this method
               transferSupplies();
                roc.yield();

            } catch (GameActionException e) {
                System.out.println("Exception caught in pre-loop of execBeav");
                e.printStackTrace();
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
                if (health > roc.getHealth()) {   //if tower is being attacked
                    int wayX = roc.getLocation().x;
                    int wayY = roc.getLocation().y;
                    System.out.println(wayX + ", " + wayY);
                    roc.broadcast(currWayBuckets[2][0], wayX);
                    roc.broadcast(currWayBuckets[2][1], wayY);
                    health = roc.getHealth();
                }
                //we want to be sure to execute this during the tower's downtime
                //  aka at the start of the game
                //this should execute at the start of the game regardless
                if (surroundingsNotSensed) {

                    //TODO: add a thread.sleep call here with a random number in order to get them all not to go at the same time
                    // ^apparently roy says that this should never happen, so if this method goes to shit because of that, it's
                    // ^ his fault, not mine
                    int xx = 0;
                    if (distNotPublished) {
                        int yy = computeDistanceToEnemyHQ(roc.getLocation());
                        xx = 65000;
                        while (roc.readBroadcast(xx) != 0) {
                            xx += 1;
                        }
                        roc.broadcast(xx, yy);
                        roc.broadcast(xx + 12, roc.getLocation().hashCode());

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
                transferSupplies();

            } catch (GameActionException e) {
                System.out.println("Unexpected exception in execTower");
                e.printStackTrace();
            }
            roc.yield();
        }
    }

    public static void execSupplyDepot()  {
        while (true) {
            try {
                transferSupplies();
               
                roc.yield();
            } catch (GameActionException e) {
               System.out.println("Unexpected exception in execTower");
                e.printStackTrace();
            }

        while (true) {
            try {
                transferSupplies();
            } catch (GameActionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method is a wrapper for the actual method, this just allows for a
     * location to be passed in as opposed to just a parameter
     *
     * Method which will request a building to be built, depending on the
     * parameters passed in
     *
     * @param buildingReq type : type of building to construct
     * @param location loc : location on the map to broadcast If this method is
     * being called, it is implicitly stated that the request bit will be
     * changed to 1, implying that the building should be built.
     */
    private static void requestBuilding(buildingReq type, MapLocation loc, boolean delay) throws GameActionException {
        requestBuilding(type, loc.x, loc.y, delay);
    }

    /**
     * Method which will request a building to be built, depending on the
     * parameters passed in
     *
     * @param buildingReq type : type of building to construct
     * @param int x : x location on the map to broadcast
     * @param int y: y location on the map to broadcast If this method is being
     * called, it is implicitly stated that the request bit will be changed to
     * 1, implying that the building should be built.
     * @param boolean isDelay: this will signify if the building should be 'held
     * off' until another one is complete, it will get signaled to one once the
     * rsrcs are available
     */
    private static void requestBuilding(buildingReq type, int x, int y, boolean isDelay) throws GameActionException {
        System.out.println("Building request " + type.name());
        if (isDelay) {
            roc.broadcast(type.reqLoc, 2);
        } else {
            roc.broadcast(type.reqLoc, 1);
        }
        roc.broadcast(type.reqLoc + 1, x);
        roc.broadcast(type.reqLoc + 2, y);
    }

    /**
     * simple method to just give you the location to travel to
     *
     * @param buildingReqFlag
     */
    private static MapLocation getBuildingRequestLoc(int buildingReqFlag) throws GameActionException {
        int x = roc.readBroadcast(buildingReqFlag + 1);
        int y = roc.readBroadcast(buildingReqFlag + 2);
        return new MapLocation(x, y);
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
     *
     * @return average core value of the surroundings (for use with towers
     * during start of game)
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
            //TODO: we could update this method to work for all methods, such as if 
            // the robot has no more cooldown, just go back to what it was doing

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

    private static int readLocationInfo(MapLocation loc) throws GameActionException {
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
        return roc.readBroadcast(memLocation);

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
                int temp3 = roc.readBroadcast(65012 + i);  //holds the location of each object
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
        for (int i = 0; i < queue2.length; i++) {
            roc.broadcast(65000 + i, queue2[i]);
        }
    }

    private static void execMiner() {//nevin
        ArrayList<Integer> botList;
        botList = new ArrayList<Integer>();
        facing = Direction.values()[(int) (rand.nextDouble() * 8)];

        try {
            while (true) {
                 if (roc.isWeaponReady()) {
                    attackSomething();
                }
                //   roc.getLocation();
                if (roc.senseOre(roc.getLocation()) > 2) {
                    if (roc.isCoreReady() && roc.canMine()) {
                        roc.mine();
                    }
//                  }else{
//                    if(rand.nextDouble()>.9){
//                       facing=facing.rotateLeft();
//                    }else if(rand.nextDouble()<.1){
//                        facing=facing.rotateRight();
//                    }
                } else {
                       
                    if (rand.nextDouble() > .9) {
                        facing = facing.rotateLeft();
                    } else if (rand.nextDouble() < .1) {
                        facing = facing.rotateRight();
                    }
                    if (roc.senseTerrainTile(roc.getLocation().add(facing)) != TerrainTile.NORMAL || Bug.inTowerRange(roc, roc.senseEnemyTowerLocations(), roc.getLocation().add(facing))) {
                        if (rand.nextDouble() > .5) {
                            facing = facing.rotateLeft();
                        } else {
                            facing = facing.rotateRight();
                        }
                    }
//                    if(roc.isCoreReady()&&roc.canMove(facing)){
//                        int counter=0;
//                        boolean done=true;
//                        while( done==true){
//                        if(roc.senseOre(roc.getLocation().add(facing))>2 || done==false){
//			done=false;
//                        }else{
//                            facing=facing.rotateRight();
//                            counter++;
//                            if(counter>=8){
//                                done=false;
//                            }
//                        }
//                    }
//		}
                    if (roc.isCoreReady() && roc.canMove(facing)) {
                        roc.move(facing);
                    }
                }
//                 RobotInfo[] bots = roc.senseNearbyRobots(15, roc.getTeam());
//                if (bots.length != 0) {
//                    for (int i = 0; i < bots.length; i++) {
//                        if (botList.contains(bots[i].ID)) {
//                            continue;
//                        }
//                        double supply=roc.getSupplyLevel();
//                        double transferAmount=0;
//                        if(bots[i].supplyLevel<supply){
//                            transferAmount=(roc.getSupplyLevel()-bots[i].supplyLevel);
//                        roc.transferSupplies((int)transferAmount, bots[i].location);
//                        botList.add(bots[i].ID);
//                    
//                        }}
//                }
                if (roc.getHealth() < roc.getType().maxHealth && healthFlag) {
                    int num = roc.readBroadcast(58004);
                    roc.broadcast(58004, num - 1);
                    healthFlag = false;
                }
                transferSupplies();
                roc.yield();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //To change body of generated methods, choose Tools | Templates.
    }

    private static void execSoldier() {
        try {
            if (firstMove) {

                firstMove = false;
                flockNumber = roc.readBroadcast(soldierFlockNum);
            }

            while (true) {
                if (roc.isWeaponReady()) {
                    attackSomething();
                }
                 transferSupplies();

                waypointLoc = new MapLocation(waypoint[0], waypoint[1]);

                if (roc.getLocation().equals(waypointLoc)) {

                    if (Bug.right) {
                        roc.broadcast(wayAck[flockNumber], 1);
                    } else {
                        roc.broadcast(wayAck[flockNumber], -1);
                    }

                }

                if (roc.isCoreReady()) {
                    waypoint[0] = roc.readBroadcast(currWayBuckets[flockNumber][0]);
                    waypoint[1] = roc.readBroadcast(currWayBuckets[flockNumber][1]);

                    MapLocation[] towers = roc.senseEnemyTowerLocations();
                    takeFlockMove(waypoint, towers);
                }
               
                roc.yield();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void execBasher() {
        try {
            if (firstMove) {

                firstMove = false;
                flockNumber = roc.readBroadcast(basherFlockNum);
            }

            while (true) {
                  
                if (roc.isCoreReady()) {
                    waypoint[0] = roc.readBroadcast(currWayBuckets[flockNumber][0]);
                    waypoint[1] = roc.readBroadcast(currWayBuckets[flockNumber][1]);

                    MapLocation[] towers = roc.senseEnemyTowerLocations();
                    takeFlockMove(waypoint, towers);
                }
                transferSupplies();
                roc.yield();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO: need to figure out how to handle how many to make (need allotment message bucket I believe)
    private static void execMinerFact() {

        ArrayList<Integer> botList;
        botList = new ArrayList<Integer>();
        if (firstMove) {

            try {
                //probably add some useful code here...
                firstMove = false;
                roc.broadcast(58004, 0);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        while (true) {

            try {
                if (needSpawn(roc.getType())) {
                    System.out.println("needSpawn() returned true");
                    if (Clock.getRoundNum() > 500) {   //this is just for testing, definitely need a better way to do this      
                        if (roc.isCoreReady() && roc.getTeamOre() > 500 && roc.getTeamOre() < 1000) //thoretically we are going to change this so that it is more deterministic
                        //as opposed to random
                        {
                            trySpawn(directions[rand.nextInt(8)], RobotType.MINER);
                            int numMiners = roc.readBroadcast(58004);
                            System.out.println("The old number of miners is" + numMiners);
                            roc.broadcast(58004, numMiners + 1);
                        }
                    } else {
                        if (roc.isCoreReady() && roc.getTeamOre() >= 60) //thoretically we are going to change this so that it is more deterministic
                        //as opposed to random
                        {
                            trySpawn(directions[rand.nextInt(8)], RobotType.MINER);
                            int numMiners = roc.readBroadcast(58004);
                            System.out.println("The old number of miners is" + numMiners);
                            roc.broadcast(58004, numMiners + 1);
                        }
                    }

                }
                transferSupplies();
                roc.yield();

            } catch (GameActionException e) {
                System.out.println("GameActionException in execMinerFact");
                e.printStackTrace();
            }
        }

    }

    private static void transferSupplies() throws GameActionException {
      
        RobotInfo[] nearbyAllies = roc.senseNearbyRobots(roc.getLocation(), 15, roc.getTeam());
        double lowestSupply = roc.getSupplyLevel();
        double transferAmount = 0;
        MapLocation suppliesToThisLocation = null;
        
       if(roc.getType()!=RobotType.DRONE){
        for (RobotInfo nAll : nearbyAllies) {
            if (nAll.supplyLevel < lowestSupply) {
                lowestSupply = nAll.supplyLevel;
                transferAmount = (roc.getSupplyLevel() - nAll.supplyLevel) / 2;
                suppliesToThisLocation = nAll.location;
               
              }
              if (suppliesToThisLocation != null&& roc.senseRobotAtLocation(nAll.location)!=null) {
            roc.transferSupplies((int) transferAmount, suppliesToThisLocation);
            break;
        
              }
           
            }}else{
           for (RobotInfo nAll : nearbyAllies) {
            if (nAll.supplyLevel < lowestSupply) {
                lowestSupply = nAll.supplyLevel;
                transferAmount = roc.getSupplyLevel()  / 2;
                suppliesToThisLocation = nAll.location;
               
              }
              if (suppliesToThisLocation != null&& roc.senseRobotAtLocation(nAll.location)!=null && (roc.senseRobotAtLocation(nAll.location).type==RobotType.SOLDIER || roc.senseRobotAtLocation(nAll.location).type==RobotType.TANK)) {
            roc.transferSupplies((int) transferAmount, suppliesToThisLocation);
            break;
        
              }
                 
       }
        
         
       }
      
    }

    private static void execBarracks() {
        ArrayList<Integer> botList;
        botList = new ArrayList<Integer>();
        if (firstMove) {

            try {

                firstMove = false;

                roc.broadcast(soldierFlockNum, 1);  //assign new soldiers to flock 1
                roc.broadcast(basherFlockNum, 1);  //assign new soldiers to flock 1

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        while (true) {

            try {
                if (Clock.getRoundNum() % 25 == 0) {
                    botList = new ArrayList<Integer>();
                }
                //sense nearby bots
                RobotInfo[] bots = roc.senseNearbyRobots(15, roc.getTeam());
                if (bots.length != 0) {
                    for (int i = 0; i < bots.length; i++) {
                        if (botList.contains(bots[i].ID) || bots[i].type == RobotType.BEAVER) {
                            continue;
                        }
                        roc.transferSupplies(900, bots[i].location);
                        botList.add(bots[i].ID);
                    }
                }

                if (needSpawn(roc.getType()) && Clock.getRoundNum() < 1500) {
//                    if (barracksUnitIteration % 2 == 0) {
                    if (roc.isCoreReady() && roc.getTeamOre() >= 510) //thoretically we are going to change this so that it is more deterministic
                    //as opposed to random
                    {
                        trySpawn(directions[rand.nextInt(8)], RobotType.SOLDIER);
                        int num = roc.readBroadcast(58002);
                        roc.broadcast(58002, num + 1);
                    }
//                    } else {
//                        if (roc.isCoreReady() && roc.getTeamOre() >= 80) //thoretically we are going to change this so that it is more deterministic
//                        //as opposed to random
//                        {
//                            trySpawn(directions[rand.nextInt(8)], RobotType.BASHER);
//                        }
//                    }
//                    barracksUnitIteration++;
                }
                transferSupplies();
            } catch (Exception e) {

            }
        }

    }

    private static void execTankFact() {
        ArrayList<Integer> botList;
        botList = new ArrayList<Integer>();
        if (firstMove) {

            try {

                firstMove = false;
                roc.broadcast(tankFlockNum, 1);  //assign new tanks to flock 1

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        while (true) {

            try {
                if (Clock.getRoundNum() % 25 == 0) {
                    botList = new ArrayList<Integer>();
                }
                //sense nearby bots
                RobotInfo[] bots = roc.senseNearbyRobots(15, roc.getTeam());
                if (bots.length != 0) {
                    for (int i = 0; i < bots.length; i++) {
                        if (botList.contains(bots[i].ID) || bots[i].type == RobotType.BEAVER) {
                            continue;
                        }
                       
                        botList.add(bots[i].ID);
                    }
                }

                if (needSpawn(roc.getType())) {

                    if (roc.isCoreReady() && roc.getTeamOre() >= 510) //thoretically we are going to change this so that it is more deterministic
                    {

                        if (unitIteration % 3 == 0) {
                            roc.broadcast(tankFlockNum, 2);
                        } else {
                            roc.broadcast(tankFlockNum, 1);
                        }
                        unitIteration++;
                        trySpawn(directions[rand.nextInt(8)], RobotType.TANK);
                    }

                }
                transferSupplies();
                roc.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static void execTank() {

        if (firstMove) {
            try {
                firstMove = false;
                flockNumber = roc.readBroadcast(tankFlockNum);
            } catch (GameActionException e) {
                e.printStackTrace();
            }

        }

        while (true) {
            try {
                System.out.println("Waypoint: " + waypoint[0] + ", " + waypoint[1]);
                if (roc.isWeaponReady()) {
                    attackSomething();
                }
              //   transferSupplies();

                waypointLoc = new MapLocation(waypoint[0], waypoint[1]);

                if (roc.getLocation().equals(waypointLoc)) {

                    if (Bug.right) {
                        roc.broadcast(wayAck[flockNumber], 1);
                    } else {
                        roc.broadcast(wayAck[flockNumber], -1);
                    }

                }

                if (roc.isCoreReady()) {
                    waypoint[0] = roc.readBroadcast(currWayBuckets[flockNumber][0]);
                    waypoint[1] = roc.readBroadcast(currWayBuckets[flockNumber][1]);

                    takeFlockMove(waypoint, null);
                }
                transferSupplies();
                roc.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private static void execHelipad(){
           ArrayList<Integer> botList;
        botList = new ArrayList<Integer>();
        if (firstMove) {

            try {
                //probably add some useful code here...
                firstMove = false;
              roc.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        while (true) {

            try {
//             if (needSpawn(roc.getType())) {
                if(Clock.getRoundNum()<1200){
                if (true) {   //this is just for testing, definitely need a better way to do this      
                    if (roc.isCoreReady() && roc.getTeamOre() <= 1000) //thoretically we are going to change this so that it is more deterministic
                    //as opposed to random
                    {
                        trySpawn(directions[rand.nextInt(8)], RobotType.DRONE);
                    }
                }}
                // transferSupplies();
               transferSupplies();
            roc.yield();
            } catch (GameActionException e) {
                System.out.println("GameActionException in execMinerFact");
                e.printStackTrace();
            }
        }

    }
    private static void execDrone(){
           
            if (firstMove) {
                try {
                    firstMove = false;
                    flockNumber = roc.readBroadcast(soldierFlockNum);
                } catch (Exception e) {
                     e.printStackTrace();
                }
            }

            while (true) {
                try{
                if (roc.isWeaponReady()) {
                    attackSomething();
                }
               

//                waypointLoc = new MapLocation(waypoint[0], waypoint[1]);
//
//                if (roc.getLocation().equals(waypointLoc)) {
//
//                    if (Bug.right) {
//                        roc.broadcast(wayAck[flockNumber], 1);
//                    } else {
//                        roc.broadcast(wayAck[flockNumber], -1);
//                    }
//
//                }
//
               
             if (roc.isCoreReady()) {
                 if(roc.getSupplyLevel()>3000|| Clock.getRoundNum() >1500 ){
                    
                    waypoint[0] = roc.readBroadcast(currWayBuckets[flockNumber][0]);
                    waypoint[1] = roc.readBroadcast(currWayBuckets[flockNumber][1]);

                   takeFlockMove(waypoint, null);
                    transferSupplies();
                   
                 }else if(roc.getSupplyLevel()<500){
                     waypoint[0]=roc.senseHQLocation().x;
                      waypoint[1]=roc.senseHQLocation().y;
                     takeFlockMove(waypoint, null);
                     
                 }else{
                      waypoint[0] = roc.readBroadcast(currWayBuckets[flockNumber][0]);
                    waypoint[1] = roc.readBroadcast(currWayBuckets[flockNumber][1]);

                   takeFlockMove(waypoint, null);
                     
                 }
               }
              
                
                
                roc.yield();
                 } catch (Exception e) {
            e.printStackTrace();
        }
                
            }
       
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

                if (roc.readBroadcast(58001) < 100) {

                    return true;

                } else if (roc.readBroadcast(58002) < 100) {
                    return true;
                } else {
                    return false;
                }
            }

            case MINERFACTORY: {
                if (roc.readBroadcast(58004) < 30) {
                    return true;
                } else {
                    return false;
                }
            }

            case HQ: {
                if (numBeavers < 2) {
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
        double lowestHP = 1000000000;
        int lowestEnemy = -1;
        if (enemies.length > 0) {
            for (int i = 0; i < enemies.length; i++) {
                if (enemies[i].type == RobotType.TOWER) {
                    lowestEnemy = i;
                    break;
                }
                if (enemies[i].health < lowestHP) {
                    lowestEnemy = i;
                    lowestHP = enemies[i].health;
                }
            }
            roc.attackLocation(enemies[lowestEnemy].location);
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

    // This method will attempt to build in the given direction (or as close to it as possible)
    static void tryBuild(Direction d, RobotType type) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
        int dirint = directionToInt(d);
        while (roc.getTeamOre() < type.oreCost) {
            if (roc.isCoreReady()) {
                roc.mine();
            }
            //  System.out.println("murdering turn");
            roc.yield();
        }
        // System.out.println("resurecting turn");
        while (offsetIndex < 8 && !roc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
            offsetIndex++;
        }
        if (offsetIndex < 8) {
            while (!roc.isCoreReady()) {
                //spin because I can't think of a better way to handle this
            }
            roc.build(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
        }

    }

    /**
     *
     * @param waypoint
     * @param towers null if you don't want tower avoidacne
     */
    static void takeFlockMove(int[] waypoint, MapLocation[] towers) {

        Direction next = Direction.OMNI;
        boolean movePossible = true;

        try {

            if (Bug.tracing) {  // if we're tracing, continue tracing
                next = Bug.computeMove(roc, waypoint, next, towers);
                Bug.flockStep++;
            } else {    // otherwise try to flock, if failure start tracing
                next = Flock.computeMove(roc, waypoint);
                if (Bug.terrainTileState(roc, next, towers) > 0) {
                    movePossible = false;
                }

                while (!movePossible) {  // if the next move can't be taken

                    if (Bug.terrainTileState(roc, next, towers) == 1) {   // the tile is void or contains buildings
                        Bug.tracing = true;
                        next = Bug.computeMove(roc, waypoint, next, towers);
                        Bug.flockStep++;
                        movePossible = true;
                    } else {    // the tile contains a robot
                        for (int i = 0; i < 10; i++) {
                            next = intToDirection(rand.nextInt(8));
                            if (Bug.terrainTileState(roc, next, towers) > 0) {
                                movePossible = true;
                                break;
                            }
                        }

                    }
                }

            }


            /*make sure no moves with none*/
            if (next != Direction.NONE && next != Direction.OMNI && roc.canMove(next)) {
                roc.move(next);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static void takeWaypointMove(int[] waypoint) {
        Direction next = Direction.OMNI;
        boolean movePossible;

        try {

            if (Bug.tracing) {  // if we're tracing, continue tracing
                next = Bug.computeMove(roc, waypoint, next, null);
            } else {    // otherwise try to flock, if failure start tracing
                next = Flock.computeWaypointMove(roc, waypoint);
                movePossible = roc.canMove(next);

                if (!movePossible) {  // if the next move can't be taken

                    if (Bug.terrainTileState(roc, next, null) == 1) {   // the tile is void or contains buildings
                        Bug.tracing = true;
                        next = Bug.computeMove(roc, waypoint, next, null);
                    } else {    // the tile contains a robot
                        next = intToDirection(rand.nextInt(8));
                    }
                }

            }


            /*make sure no moves with none*/
            if (next != Direction.NONE && next != Direction.OMNI && roc.canMove(next)&&roc.isCoreReady()) {
                roc.move(next);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static MapLocation getTowerBuildLocation() throws GameActionException {
        MapLocation[] towers = roc.senseTowerLocations();

        MapLocation buildTower = towers[0];
        double distance = 1000000000.0;

        int size = towers.length;
        for (int i = 0; i < size; i++) {
            double weight = Math.sqrt(10 * towers[i].distanceSquaredTo(roc.senseHQLocation()) +  towers[i].distanceSquaredTo(roc.senseEnemyHQLocation()));
            if (weight < distance) {
                buildTower = towers[i];
                distance = weight;
            }
        }

        MapLocation location;
        int[] buildLocation = new int[2];
        while (true) {
            int radius = (int) Math.sqrt(roc.getType().attackRadiusSquared);
//            buildLocation[0] = buildTower.x + rand.nextInt(2 * radius) - radius;
//            buildLocation[1] = buildTower.y + rand.nextInt(2 * radius) - radius;
            buildLocation[0] = (roc.senseHQLocation().x - buildTower.x) / 4 + buildTower.x + rand.nextInt(2 * radius) - radius;
            buildLocation[1] = (roc.senseHQLocation().y - buildTower.y) / 4 + buildTower.y + rand.nextInt(2 * radius) - radius;
            location = new MapLocation(buildLocation[0], buildLocation[1]);
            
            if (location.distanceSquaredTo(buildTower) < RobotType.TOWER.sensorRadiusSquared) {
                if (roc.senseTerrainTile(location) == TerrainTile.NORMAL && !roc.isLocationOccupied(location)) {
                    return location;
                }
            }

        }

    }

    public static MapLocation getClosestEnemyTower() {
        MapLocation[] tower = roc.senseEnemyTowerLocations();
        int distanceSquared = 1000000000;
        int closeTower = -1;

        int size = tower.length;

        for (int i = 0; i < size; i++) {
            int weight = 50*roc.getLocation().distanceSquaredTo(tower[i]) + roc.senseEnemyHQLocation().distanceSquaredTo(tower[i]);
            if (weight < distanceSquared) {
                closeTower = i;
                distanceSquared = roc.getLocation().distanceSquaredTo(tower[i]);
            }
        }

        return tower[closeTower];

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

    private static Direction oppositeDirec(Direction senseHQLocation) {
        switch (senseHQLocation) {
            case NORTH:
                return Direction.SOUTH;
            case NORTH_EAST:
                return Direction.SOUTH_WEST;
            case EAST:
                return Direction.WEST;
            case SOUTH_EAST:
                return Direction.NORTH_WEST;
            case SOUTH:
                return Direction.NORTH;
            case SOUTH_WEST:
                return Direction.NORTH_EAST;
            case WEST:
                return Direction.EAST;
            case NORTH_WEST:
                return Direction.SOUTH_EAST;
            default:
                return Direction.SOUTH;
        }
    }

} //end of class

