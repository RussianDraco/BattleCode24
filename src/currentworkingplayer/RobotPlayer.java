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
1,2,3,4,5,6 - actual spawn locations

**/
public strictfp class RobotPlayer {
    static int turnCount = 0;

    static final Random rng = new Random(6147);

    static int profession; // 0 = soldier, 1 = builder, 2 = healer
    static int builderTarget; // which spawn point he builds at
    static boolean[] buildProgess = {false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false};

    static boolean enemyEast;
    static boolean enemySouth;

    static boolean goingAround = false;

    static Direction bugodir = null;

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
        if (!(rc.readSharedArray(1) > 0)) {
            findSpawnCenters(rc);
        }

        boolean isBuilder = (rc.getID() % 9) == 0; // is a builder

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

        MapLocation[] actualSpawns = {new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2)), new MapLocation(rc.readSharedArray(3), rc.readSharedArray(4)), new MapLocation(rc.readSharedArray(5), rc.readSharedArray(6))};

        if (isBuilder) {profession = 1;} else {if (rc.getID() % 2 == 0) {profession = 2;} else {profession = 0;}}

        while (true) {
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
                            enemyEast = rc.getLocation().x > rc.getMapWidth() / 2;
                            enemySouth = rc.getLocation().y > rc.getMapHeight() / 2;
                        }
                    } else {
                        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                        MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                        if (rc.canSpawn(randomLoc)) {
                            rc.spawn(randomLoc);
                            enemyEast = rc.getLocation().x > rc.getMapWidth() / 2;
                            enemySouth = rc.getLocation().y > rc.getMapHeight() / 2;
                        }
                    }
                }
                else {
                    if (rc.canPickupFlag(rc.getLocation())){
                        if (rc.senseNearbyFlags(1, rc.getTeam().opponent()).length != 0){
                            rc.pickupFlag(rc.getLocation());
                            rc.setIndicatorString("Holding a flag!");
                        }
                    }
                    if (rc.hasFlag() && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS){
                        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                        
                        MapLocation closestLoc = null;
                        int closestDist = 999999;

                        for (MapLocation ml : spawnLocs) {
                            int d = rc.getLocation().distanceSquaredTo(ml);
                            if (d < closestDist) {
                                closestDist = d;
                                closestLoc = ml;
                            }
                        }

                        bugO(rc, closestLoc);
                    }

                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS){
                        MapLocation[] crumbLocations = rc.senseNearbyCrumbs(-1);
                        if (crumbLocations.length != 0){
                            rc.setIndicatorString("There are nearby crumbs! Yum!");
                            Direction dir = rc.getLocation().directionTo(crumbLocations[0]);
                            if (rc.canMove(dir)) rc.move(dir);
                        }
                    }
                    
                    if (profession == 0 || profession == 2) {
                        if (profession == 0) {
                            rc.setIndicatorString("I am a soldier!");
                        } else if (profession == 2) {
                            rc.setIndicatorString("I am a healer!");
                        }

                        militaryPathfinding(rc, false);
                    }

                    if (profession == 1 && rc.getCrumbs() >= 350) {
                        rc.setIndicatorString("I am a builder!");

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

    public static void findSpawnCenters(RobotController rc) throws GameActionException{
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();

        if (rc.canWriteSharedArray(1, spawnLocs[4].x)) {
            rc.writeSharedArray(1, spawnLocs[4].x);
        }
        if (rc.canWriteSharedArray(2, spawnLocs[4].y)) {
            rc.writeSharedArray(2, spawnLocs[4].y);
        }

        if (rc.canWriteSharedArray(3, spawnLocs[13].x)) {
            rc.writeSharedArray(3, spawnLocs[13].x);
        }
        if (rc.canWriteSharedArray(4, spawnLocs[13].y)) {
            rc.writeSharedArray(4, spawnLocs[13].y);
        }

        if (rc.canWriteSharedArray(5, spawnLocs[22].x)) {
            rc.writeSharedArray(5, spawnLocs[22].x);
        }
        if (rc.canWriteSharedArray(6, spawnLocs[22].y)) {
            rc.writeSharedArray(6, spawnLocs[22].y);
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

    public static void militaryPathfinding(RobotController rc, boolean inverted) throws GameActionException{
        militaryMovement(rc, inverted);

        for (RobotInfo ml : rc.senseNearbyRobots(-1, rc.getTeam().opponent())) {
            MapLocation nextLoc = ml.location;
            if (rc.canAttack(nextLoc)){
                rc.attack(nextLoc);
                System.out.println("Healer/Soldier attacked!");
            }
        }
    }

    public static void militaryMovement(RobotController rc, boolean inverted) throws GameActionException{
        if (!enemySouth) {
            if (militaryMove(rc, Direction.NORTH)) {goingAround = false; return;}
        } else {
            if (militaryMove(rc, Direction.SOUTH)) {goingAround = false; return;}
        }

        if (enemyEast) {
            if (militaryMove(rc, Direction.WEST)) {goingAround = false; return;}
        } else {
            if (militaryMove(rc, Direction.EAST)) {goingAround = false; return;}
        }

        if (!enemySouth) {
            if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {if (militaryMove(rc, Direction.SOUTH)) {goingAround = true; return;}}
        } else {
            if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {if (militaryMove(rc, Direction.NORTH)) {goingAround = true; return;}}
        }

        if (enemyEast) {
            if (militaryMove(rc, Direction.EAST)) {goingAround = true; return;}
        } else {
            if (militaryMove(rc, Direction.WEST)) {goingAround = true; return;}
        }

        System.out.println("I am stuck!");
    }

    public static boolean militaryMove(RobotController rc, Direction dir) throws GameActionException{
        MapLocation nextLoc = rc.getLocation().add(dir);
        if (rc.canMove(dir)){
            rc.move(dir);
            return true;
        }
        else if (rc.canAttack(nextLoc)){
            rc.attack(nextLoc);
            System.out.println("Healer/Soldier attacked!");
        }
        return false;
    }


    public static void bugO(RobotController rc, MapLocation destination) throws GameActionException{
        Direction dir = rc.getLocation().directionTo(destination);
        if (rc.canMove(dir)) {
            rc.move(dir);
            bugodir = null;
            return;
        } else {
            if (bugodir == null) {
                bugodir = dir;
            }

            for (int i = 0; i < 8; i++) {
                if (rc.canMove(bugodir)) {
                    rc.move(bugodir);
                    bugodir.rotateRight();
                    return;
                } else {
                    bugodir = bugodir.rotateLeft();
                }
            }
        }
    }
}
