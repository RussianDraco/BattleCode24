package hideanddefend;

import battlecode.common.*;

import java.util.*;

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

    static final int NUM_LOOKERS = 5;
    static final Map<Integer, MapLocation> originalPositions = new HashMap<>();
    static boolean returnToOriginalPositions = false;

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
                        if (returnToOriginalPositions) {
                            returnToOriginalPositions(rc);
                            returnToOriginalPositions = false;
                        } else {
                            moveAndAttack(rc);
                        }
                    } else {
                        if (rc.getRoundNum() < 150) {
                            exploreMap(rc);
                        } else {
                            if (returnToOriginalPositions) {
                                returnToOriginalPositions(rc);
                                returnToOriginalPositions = false;
                            } else {
                                deployLookers(rc);
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

    public static void moveAndAttack(RobotController rc) throws GameActionException {
        MapLocation[] crumbLocations = rc.senseNearbyCrumbs(-1);
        if (crumbLocations.length != 0) {
            rc.setIndicatorString("There are nearby crumbs! Yum!");
            Direction dir = rc.getLocation().directionTo(crumbLocations[0]);
            if (rc.canMove(dir)) rc.move(dir);
        } else {
            Direction dir = directions[rng.nextInt(directions.length)];
            MapLocation nextLoc = rc.getLocation().add(dir);
            if (rc.canMove(dir)) {
                rc.move(dir);
            } else if (rc.canAttack(nextLoc)) {
                rc.attack(nextLoc);
                System.out.println("Take that! Damaged an enemy that was in our way!");
            }
        }
    }

    public static void exploreMap(RobotController rc) throws GameActionException {
        Direction randomDir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(randomDir)) {
            rc.move(randomDir);
        }
    }

    public static void deployLookers(RobotController rc) throws GameActionException {
        if (originalPositions.isEmpty()) {
            for (int i = 0; i < NUM_LOOKERS; i++) {
                originalPositions.put(i, rc.getLocation());
            }
        }

        int lookerIndex = rc.getID() % NUM_LOOKERS;
        MapLocation originalPos = originalPositions.get(lookerIndex);
        Direction dir = rc.getLocation().directionTo(originalPos);

        if (rc.getRoundNum() > 150) {
            if (rc.getLocation().distanceSquaredTo(originalPos) > 0) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                } else {
                    returnToOriginalPositions = true;
                }
            } else {
                returnToOriginalPositions = true;
            }
        } else {
            if (rc.getLocation().distanceSquaredTo(originalPos) > 0) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        }
    }

    public static void returnToOriginalPositions(RobotController rc) throws GameActionException {
        for (int i = 0; i < NUM_LOOKERS; i++) {
            int lookerID = (rc.getID() + i) % NUM_LOOKERS;
            MapLocation originalPos = originalPositions.get(i);
            Direction dir = rc.getLocation().directionTo(originalPos);

            if (rc.getLocation().distanceSquaredTo(originalPos) > 0) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        }
    }
}
