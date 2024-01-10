package hideanddefend;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
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

    public static void run(RobotController rc) throws GameActionException {
        boolean isHider = ((rc.getID() % 2) == 0);
        rc.setIndicatorString(Boolean.toString(isHider));

        System.out.println("I'm alive");
        rc.setIndicatorString("Hello world!");

        while (true) {
            turnCount += 1;

            try {
                if (!rc.isSpawned()) {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                    if (rc.canSpawn(randomLoc)) rc.spawn(randomLoc);
                } else {
                    if (!isHider) {
                        MapLocation[] crumbLocations = rc.senseNearbyCrumbs(-1);
                        if (crumbLocations.length != 0){
                            rc.setIndicatorString("There are nearby crumbs! Yum!");
                            Direction dir = rc.getLocation().directionTo(crumbLocations[0]);
                            if (rc.canMove(dir)) rc.move(dir);
                        } else {
                            Direction dir = directions[rng.nextInt(directions.length)];
                            MapLocation nextLoc = rc.getLocation().add(dir);
                            if (rc.canMove(dir)){
                                rc.move(dir);
                            } else if (rc.canAttack(nextLoc)){
                                rc.attack(nextLoc);
                                System.out.println("Take that! Damaged an enemy that was in our way!");
                            }
                        }
                    } else {
                        if (rc.getRoundNum() >= 10) {
                            for (Direction d : directions) {
                                MapLocation prevLoc = rc.getLocation().subtract(d);
                                if (rc.canBuild(TrapType.EXPLOSIVE, prevLoc)) {
                                    rc.build(TrapType.EXPLOSIVE, prevLoc);
                                }
                            }

                            for (Direction d : directions) {
                                MapLocation atLoc = rc.getLocation().subtract(d);
                                if (rc.canAttack(atLoc)) {
                                    rc.attack(atLoc);
                                    System.out.println("Take that! Damaged an enemy that was in our way!");
                                }
                            }

                            for (Direction d : directions) {
                                MapLocation healLoc = rc.getLocation().subtract(d);
                                if (rc.canHeal(healLoc)) {
                                    rc.heal(healLoc);
                                    System.out.println("Healed a dude");
                                }
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