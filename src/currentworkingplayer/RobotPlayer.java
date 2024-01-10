package currentworkingplayer;

import battlecode.common.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/*Shared array allocation
0 - next builder target
1,2,3,4,5,6 - actual spawn locations

**/
public strictfp class RobotPlayer {
    static int turnCount = 0;

    static final Random rng = new Random(6147);

    static int builderTarget = 0; // which spawn point he builds at

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
        if (rc.readSharedArray(1) != 0) {
            findSpawnCenters(rc);
        }
        
        boolean isBuilder = (rc.getID() % 13) == 0; // is a builder

        if (rc.readSharedArray(0) == 0) {
            if (rc.canWriteSharedArray(0, 1)) {
                rc.writeSharedArray(0, 1);
            }
        } else {
            builderTarget = rc.readSharedArray(0) % 3;
            if (rc.canWriteSharedArray(0, builderTarget + 1)) {
                rc.writeSharedArray(0, builderTarget + 1);
            }
        }

        int profession = 0;
        boolean goHideFlag = false;

        MapLocation[] actualSpawns = {new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2)), new MapLocation(rc.readSharedArray(3), rc.readSharedArray(4)), new MapLocation(rc.readSharedArray(5), rc.readSharedArray(6))};

        if (isBuilder) {profession = 1;} else {if (rc.getID() % 2 == 0) {profession = 2;} else {profession = 0;}}

        while (true) {
            turnCount += 1;

            try {
                if (!rc.isSpawned()){
                    if (profession == 1) {
                        MapLocation randomLoc = actualSpawns[builderTarget];
                        if (rc.canSpawn(randomLoc)) rc.spawn(randomLoc);
                    } else {
                        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                        MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                        if (rc.canSpawn(randomLoc)) rc.spawn(randomLoc);
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
                        MapLocation firstLoc = spawnLocs[0];
                        Direction dir = rc.getLocation().directionTo(firstLoc);
                        if (rc.canMove(dir)) rc.move(dir);
                    }

                    if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS){
                        MapLocation[] crumbLocations = rc.senseNearbyCrumbs(-1);
                        if (crumbLocations.length != 0){
                            rc.setIndicatorString("There are nearby crumbs! Yum!");
                            Direction dir = rc.getLocation().directionTo(crumbLocations[0]);
                            if (rc.canMove(dir)) rc.move(dir);
                        }
                    }
                    
                    if (profession == 0) {
                        rc.setIndicatorString("I am a soldier!");

                        Direction dir = directions[rng.nextInt(directions.length)];
                        MapLocation nextLoc = rc.getLocation().add(dir);
                        if (rc.canMove(dir)){
                            rc.move(dir);
                        }
                        else if (rc.canAttack(nextLoc)){
                            rc.attack(nextLoc);
                            System.out.println("Soldier attacked an enemy!");
                        }
                    } else if (profession == 1) {
                        rc.setIndicatorString("I am a builder!");

                        boolean done = false;
                        MapLocation ml = actualSpawns[builderTarget];
                        MapLocation[] trapLocs = {ml.translate(-2, 2), ml.translate(-1, 2), ml.translate(0, 2), ml.translate(1, 2), ml.translate(2, 2), ml.translate(2, 1), ml.translate(2, 0), ml.translate(2, -1), ml.translate(2, -2), ml.translate(1, -2), ml.translate(0, -2), ml.translate(-1, -2), ml.translate(-2, -2), ml.translate(-2, -1), ml.translate(-2, 0), ml.translate(-2, 1)};
                        for (MapLocation trapLoc : trapLocs) {
                            if (rc.getLocation().distanceSquaredTo(trapLoc) <= 2) {
                                if (rc.senseMapInfo(trapLoc).getTrapType() == TrapType.NONE) {
                                    if (rc.getLocation().distanceSquaredTo(trapLoc) <= 2) {
                                        if (rc.canBuild(TrapType.EXPLOSIVE, trapLoc)) {
                                            rc.build(TrapType.EXPLOSIVE, trapLoc);
                                        } else {
                                            Direction tdir = rc.getLocation().directionTo(trapLoc);
                                            if (rc.canMove(tdir)) {
                                                rc.move(tdir);
                                            }
                                        }
                                        done = true;
                                    }
                                }
                            } else {
                                Direction tdir = rc.getLocation().directionTo(trapLoc);
                                if (rc.canMove(tdir)) {
                                    rc.move(tdir);
                                }
                            }
                        }
                        

                        if (!done) {
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
                        rc.setIndicatorString("I am a healer!");

                        Direction dir = directions[rng.nextInt(directions.length)];
                        MapLocation nextLoc = rc.getLocation().add(dir);
                        if (rc.canMove(dir)){
                            rc.move(dir);
                        }
                        else if (rc.canAttack(nextLoc)){
                            rc.attack(nextLoc);
                            System.out.println("Healer had to take arms!");
                        }

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
}
