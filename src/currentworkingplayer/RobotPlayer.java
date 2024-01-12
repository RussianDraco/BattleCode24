package currentworkingplayer;

import battlecode.common.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


//later, bit-shifting should be implemented in order to use shared array better
/*Shared array allocation
0 - next builder target
1,2,3,4,5,6 - enemy spawn locations
7 - number of flags collected

63 - temporary bit for scout creation
**/
public strictfp class RobotPlayer {
    static int turnCount = 0;

    static Random rng;

    static int profession; // 0 = soldier, 1 = builder, 2 = healer, 3 - scout(temp profession -> soldier)
    static int builderTarget; // which spawn point he builds at
    static boolean[] buildProgess = {false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false};

    static boolean goingAround = false;

    static Direction bugodir = null;
    static MapLocation bugoBad = null; //place where bugo has to go around

    static int scoutNum;
    static int[] scoutDest;

    static boolean isTeamA;

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        rng = new Random(rc.getID());

        isTeamA = (rc.getTeam() == Team.A);

        boolean isBuilder = (rng.nextFloat() > 0.85); // is a builder

        if (isBuilder) {
            if (rc.readSharedArray(0) == 0) {
                builderTarget = 0;
                if (rc.canWriteSharedArray(0, 1)) {
                    rc.writeSharedArray(0, 1);
                }
            } else {
                builderTarget = rc.readSharedArray(0) % 3;
                if (rc.canWriteSharedArray(0, builderTarget + 1)) {
                    rc.writeSharedArray(0, builderTarget + 1);
                }
            }
        }

        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation[] actualSpawns = {spawnLocs[4], spawnLocs[13], spawnLocs[22]};
        spawnLocs = null;

        if (isBuilder) {profession = 1;} else {if (rng.nextFloat() >= 0.5) {profession = 2;} else {
            if (rc.readSharedArray(63) < 7) { // 7 scouts
                profession = 3;
                scoutNum = rc.readSharedArray(63);

                if (rc.canWriteSharedArray(63, rc.readSharedArray(63) + 1)) {
                    rc.writeSharedArray(63, rc.readSharedArray(63) + 1);
                }
            } else {
                profession = 0;
            }
        }}

        if (profession == 3) {
            float spawnAvgX = (actualSpawns[0].x + actualSpawns[1].x + actualSpawns[2].x) / 3;
            float spawnAvgY = (actualSpawns[0].y + actualSpawns[1].y + actualSpawns[2].y) / 3;
            float scoutDIncr = (rc.getMapHeight() + rc.getMapWidth())/7; //uses scout number: 7
            float incredScoutNum = scoutNum * scoutDIncr;

            if (spawnAvgX > rc.getMapWidth() / 2) {
                if (spawnAvgY > rc.getMapHeight() / 2) {
                    //width, height
                    if (incredScoutNum < rc.getMapWidth()) {
                        scoutDest = new int[] {clamp0(rc.getMapWidth() - Math.round(incredScoutNum) - 1), 0};
                    } else if (incredScoutNum == rc.getMapWidth()) {
                        scoutDest = new int[] {0, 0};
                    } else {
                        scoutDest = new int[] {0, clamp0(Math.round(incredScoutNum) - rc.getMapWidth() - 1)};
                    }
                } else {
                    //width, 0
                    if (incredScoutNum < rc.getMapWidth()) {
                        scoutDest = new int[] {clamp0(rc.getMapWidth() - Math.round(incredScoutNum) - 1), rc.getMapHeight() - 1};
                    } else if (incredScoutNum == rc.getMapWidth()) {
                        scoutDest = new int[] {0, rc.getMapHeight() - 1};
                    } else {
                        scoutDest = new int[] {0, clamp0(rc.getMapHeight() - (Math.round(incredScoutNum) - rc.getMapWidth()) - 1)};
                    }
                }
            } else {
                if (spawnAvgY > rc.getMapHeight() / 2) {
                    //0, height
                    if (incredScoutNum < rc.getMapWidth()) {
                        scoutDest = new int[] {Math.round(incredScoutNum), 0};
                    } else if (incredScoutNum == rc.getMapWidth()) {
                        scoutDest = new int[] {rc.getMapWidth() - 1, 0};
                    } else {
                        scoutDest = new int[] {rc.getMapWidth() - 1, clamp0(Math.round(incredScoutNum) - rc.getMapWidth() - 1)};
                    }
                } else {
                    //0, 0
                    if (incredScoutNum < rc.getMapWidth()) {
                        scoutDest = new int[] {Math.round(incredScoutNum), rc.getMapHeight()};
                    } else if (incredScoutNum == rc.getMapWidth()) {
                        scoutDest = new int[] {rc.getMapWidth() - 1, rc.getMapHeight() - 1};
                    } else {
                        scoutDest = new int[] {rc.getMapWidth() - 1, clamp0(rc.getMapHeight() - (Math.round(incredScoutNum) - rc.getMapWidth()) - 1)};
                    }
                }
            }
        }

        while (true) {
            if (profession == 3) {
                boolean has0 = false;
                for (int i = 8; i < 12; i++) {
                    if (rc.readSharedArray(i) < 1) {
                        has0 = true;
                        break;
                    }
                }
                if (!has0) {profession = 0;}
            }

            turnCount += 1;

            if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                rc.buyGlobal(GlobalUpgrade.HEALING);
            } else if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
                rc.buyGlobal(GlobalUpgrade.ACTION);
            }

            try {
                if (!rc.isSpawned()){
                    if (profession == 1) {
                        MapLocation randomLoc = actualSpawns[builderTarget];
                        if (rc.canSpawn(randomLoc)){
                            rc.spawn(randomLoc);
                        }
                    } else {
                        spawnLocs = rc.getAllySpawnLocations();
                        MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                        if (rc.canSpawn(randomLoc)) {
                            rc.spawn(randomLoc);
                        }
                    }
                }
                else {
                    if (profession == 0) {
                        rc.setIndicatorString("I am a soldier!");
                    } else if (profession == 1) {
                        rc.setIndicatorString("I am a builder!");
                    } else if (profession == 2) {
                        rc.setIndicatorString("I am a healer!");
                    } else if (profession == 3) {
                        rc.setIndicatorString("I am a scout!");
                    }

                    if (rc.canPickupFlag(rc.getLocation())){
                        if (rc.senseNearbyFlags(1, rc.getTeam().opponent()).length != 0){
                            rc.pickupFlag(rc.getLocation());
                            rc.setIndicatorString("Holding a flag!");
                        }
                    }
                    if (rc.hasFlag() && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS){
                        spawnLocs = rc.getAllySpawnLocations();
                        
                        MapLocation closestLoc = null;
                        int closestDist = 999999;

                        for (MapLocation ml : spawnLocs) {
                            int d = rc.getLocation().distanceSquaredTo(ml);
                            if (d < closestDist) {
                                closestDist = d;
                                closestLoc = ml;
                            }
                        }

                        pathfind(rc, closestLoc);
                    }

                    MapLocation[] crumbLocations = rc.senseNearbyCrumbs(-1);
                    if (profession != 3 && crumbLocations.length != 0){
                        System.out.println("I found a crumb!");
                        Direction dir = rc.getLocation().directionTo(crumbLocations[0]);
                        if (rc.canMove(dir)) rc.move(dir);
                    }
                    
                    if (profession == 0 || profession == 2) {
                        militaryPathfinding(rc);
                    }

                    if (profession == 1 && rc.getCrumbs() >= 350) {
                        updateBuildProgress(rc, actualSpawns);

                        MapLocation ml = actualSpawns[builderTarget];
                        MapLocation[] trapLocs = {ml.translate(-2, 2), ml.translate(-1, 2), ml.translate(0, 2), ml.translate(1, 2), ml.translate(2, 2), ml.translate(2, 1), ml.translate(2, 0), ml.translate(2, -1), ml.translate(2, -2), ml.translate(1, -2), ml.translate(0, -2), ml.translate(-1, -2), ml.translate(-2, -2), ml.translate(-2, -1), ml.translate(-2, 0), ml.translate(-2, 1)};
                        
                        int n = 0;

                        for (MapLocation trapLoc : trapLocs) {
                            if (buildProgess[n]) {n += 1; continue;}

                            if (rc.getLocation().distanceSquaredTo(trapLoc) <= 2) {
                                if (rc.senseMapInfo(trapLoc).getTrapType() == TrapType.NONE) {
                                    if (rc.canBuild(TrapType.EXPLOSIVE, trapLoc)) {
                                        rc.build(TrapType.EXPLOSIVE, trapLoc);
                                        buildProgess[n] = true;
                                    } else {buildProgess[n] = true;}
                                } else {buildProgess[n] = true; n += 1; continue;}
                            } else {
                                Direction tdir = rc.getLocation().directionTo(trapLoc);
                                if (rc.canMove(tdir)) {
                                    rc.move(tdir);
                                }
                            }
                            n += 1;
                        }

                        boolean allDone = true;
                        for (boolean b : buildProgess) {if (!b) {allDone = false; break;}}

                        if (allDone) {
                            Direction dir = directions[rng.nextInt(directions.length)];
                            MapLocation nextLoc = rc.getLocation().add(dir);
                            if (rc.canMove(dir)){
                                rc.move(dir);
                            }
                            else if (rc.canAttack(nextLoc)){
                                rc.attack(nextLoc);
                                System.out.println("Builder had to take arms!");
                            }

                            MapLocation prevLoc = rc.getLocation().subtract(dir);
                            if (rc.canBuild(TrapType.EXPLOSIVE, prevLoc) && rng.nextInt() % 37 == 1) {
                                rc.build(TrapType.EXPLOSIVE, prevLoc);
                            }
                        }
                    } else if (profession == 2) {
                        for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
                            if (rc.canHeal(robot.location)) {
                                rc.heal(robot.location);
                                System.out.println("Healer healed a robot!");
                            }
                        }
                    } else if (profession == 3) {
                        if (rc.getLocation().x == scoutDest[0] && rc.getLocation().y == scoutDest[1]) {
                            profession = 0; //scout returns to being an ordinary soldier
                        }

                        pathfind(rc, new MapLocation(scoutDest[0], scoutDest[1]));
                        scoutEnemyDetect(rc);
                    }
                }

            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                Clock.yield();
            }
        }
    }

    public static void scoutEnemyDetect(RobotController rc) throws GameActionException{
        for (MapInfo mi : rc.senseNearbyMapInfos()) {
            if ((mi.getSpawnZoneTeam() == 2 && isTeamA) || (mi.getSpawnZoneTeam() == 1 && !isTeamA)) {
                if (rc.readSharedArray(1) < 1) {
                    if (rc.canWriteSharedArray(1, mi.getMapLocation().x)) {
                        rc.writeSharedArray(1, mi.getMapLocation().x);
                    }
                    if (rc.canWriteSharedArray(2, mi.getMapLocation().y)) {
                        rc.writeSharedArray(2, mi.getMapLocation().y);
                    }
                } else if (rc.readSharedArray(3) < 1) {
                    if ((Math.abs(rc.readSharedArray(1) - mi.getMapLocation().x) < 4) && (Math.abs(rc.readSharedArray(2) - mi.getMapLocation().y) < 4)) {continue;}

                    if (rc.canWriteSharedArray(3, mi.getMapLocation().x)) {
                        rc.writeSharedArray(3, mi.getMapLocation().x);
                    }
                    if (rc.canWriteSharedArray(4, mi.getMapLocation().y)) {
                        rc.writeSharedArray(4, mi.getMapLocation().y);
                    }
                } else if (rc.readSharedArray(5) < 1) {
                    if ((Math.abs(rc.readSharedArray(1) - mi.getMapLocation().x) < 4) && (Math.abs(rc.readSharedArray(2) - mi.getMapLocation().y) < 4)) {continue;}
                    if ((Math.abs(rc.readSharedArray(3) - mi.getMapLocation().x) < 4) && (Math.abs(rc.readSharedArray(4) - mi.getMapLocation().y) < 4)) {continue;}

                    if (rc.canWriteSharedArray(5, mi.getMapLocation().x)) {
                        rc.writeSharedArray(5, mi.getMapLocation().x);
                    }
                    if (rc.canWriteSharedArray(6, mi.getMapLocation().y)) {
                        rc.writeSharedArray(6, mi.getMapLocation().y);
                    }
                }
            }
        }
    }

    public static void updateBuildProgress(RobotController rc, MapLocation[] actualSpawns) throws GameActionException{
        MapLocation ml = actualSpawns[builderTarget];
        MapLocation[] trapLocs = {ml.translate(-2, 2), ml.translate(-1, 2), ml.translate(0, 2), ml.translate(1, 2), ml.translate(2, 2), ml.translate(2, 1), ml.translate(2, 0), ml.translate(2, -1), ml.translate(2, -2), ml.translate(1, -2), ml.translate(0, -2), ml.translate(-1, -2), ml.translate(-2, -2), ml.translate(-2, -1), ml.translate(-2, 0), ml.translate(-2, 1)};
                        
        int i = 0;
        for (MapLocation trapLoc : trapLocs) {
            if (!buildProgess[i]) {i += 1; continue;}
            if (rc.getLocation().distanceSquaredTo(trapLoc) <= GameConstants.VISION_RADIUS_SQUARED) {
                if (rc.senseMapInfo(trapLoc).getTrapType() == TrapType.NONE) {
                    buildProgess[i] = false;
                }
            }
            i += 1;
        }
    }

    public static void militaryPathfinding(RobotController rc) throws GameActionException{
        MapInfo mi = rc.senseMapInfo(rc.getLocation());
        if (((mi.getSpawnZoneTeam() == 2 && isTeamA) || (mi.getSpawnZoneTeam() == 1 && !isTeamA)) && rc.getCrumbs() > 700) {
            if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation())) {
                rc.build(TrapType.EXPLOSIVE, rc.getLocation());
            }
        }

        MapLocation moveTarget = null;

        MapLocation[] flagDetections = rc.senseBroadcastFlagLocations();
        if (flagDetections.length != 0) {
            moveTarget = flagDetections[0];
        } else if (!(rc.readSharedArray(1) < 1)) {
            moveTarget = new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2));
        }

        if (moveTarget != null) {
            pathfind(rc, moveTarget);
        } else {
            militaryMovement(rc);
        }

        for (RobotInfo ml : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
            MapLocation nextLoc = ml.location;
            if (rc.canAttack(nextLoc)){
                rc.attack(nextLoc);
                System.out.println("Healer/Soldier attacked!");
            }
        }
    }

    public static void militaryMovement(RobotController rc) throws GameActionException{
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        if (rc.canMove(dir)) {
            rc.move(dir);
        } else if (rc.canFill(nextLoc)) {
            rc.fill(nextLoc);
        } else if (rc.canAttack(nextLoc)){
            rc.attack(nextLoc);
            System.out.println("Healer/Soldier attacked!");
        }
    }

    public static int clamp0(int n) {if (n < 0) {return 0;} else {return n;}}


    public static void pathfind(RobotController rc, MapLocation destination) throws GameActionException{
        if (rc.getLocation().equals(destination)) {System.out.println("REQUESTING PATHFINDING TO CURRENT LOC. " + destination.x + " " + destination.y); return;} //debugging

        Direction dir = rc.getLocation().directionTo(destination);
        if (rc.canMove(dir) && !rc.getLocation().add(dir).equals(bugoBad)) {
            rc.move(dir);
            bugodir = null;
            bugoBad = null;
            return;
        } else if (rc.canFill(rc.getLocation().add(dir))) {
            rc.fill(rc.getLocation().add(dir));
            if (rc.canMove(dir) && !rc.getLocation().add(dir).equals(bugoBad)) {
                rc.move(dir);
                bugodir = null;
                bugoBad = null;
                return;
            }
        } else {
            if (bugodir == null) {
                bugodir = dir;
            }

            for (int i = 0; i < 8; i++) {
                if (rc.canMove(bugodir) && !rc.getLocation().add(bugodir).equals(bugoBad)) {
                    bugoBad = rc.getLocation();
                    rc.move(bugodir);
                    bugodir = bugodir.rotateRight();
                    return;
                } else if (rc.canFill(rc.getLocation().add(bugodir))) {
                    rc.fill(rc.getLocation().add(bugodir));
                    if (rc.canMove(bugodir)) {
                        bugoBad = rc.getLocation();
                        rc.move(bugodir);
                        bugodir = bugodir.rotateRight();
                        return;
                    }
                } else {
                    bugodir = bugodir.rotateLeft();
                }
            }
        }
    }
}
