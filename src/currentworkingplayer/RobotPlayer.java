package currentworkingplayer;

import battlecode.common.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public strictfp class RobotPlayer {

    static int turnCount = 0;

    static final Random rng = new Random(6147);

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

        int profession = rc.getID() % 3; // 0 - soldier, 1 - builder, 2 - healer

        while (true) {
            turnCount += 1;

            try {
                if (!rc.isSpawned()){
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                    if (rc.canSpawn(randomLoc)) rc.spawn(randomLoc);
                }
                else {
                    if (rc.canPickupFlag(rc.getLocation())){
                        rc.pickupFlag(rc.getLocation());
                        rc.setIndicatorString("Holding a flag!");
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

                    Direction dir = directions[rng.nextInt(directions.length)];
                    MapLocation nextLoc = rc.getLocation().add(dir);
                    if (rc.canMove(dir)){
                        rc.move(dir);
                    }
                    else if (rc.canAttack(nextLoc)){
                        rc.attack(nextLoc);
                        System.out.println("Take that! Damaged an enemy that was in our way!");
                    }
                    
                    if (profession == 1) {
                        rc.setIndicatorString("I am a builder!");
                        MapLocation prevLoc = rc.getLocation().subtract(dir);
                        if (rc.canBuild(TrapType.EXPLOSIVE, prevLoc) && rng.nextInt() % 37 == 1) {
                            rc.build(TrapType.EXPLOSIVE, prevLoc);
                        }
                    } else if (profession == 2) {
                        rc.setIndicatorString("I am a healer!");
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
}
