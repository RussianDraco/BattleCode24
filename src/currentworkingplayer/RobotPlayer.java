package currentworkingplayer;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;


//later, bit-shifting should be implemented in order to use shared array better
/*Shared array allocation

1,2,3,4,5,6 - enemy spawn locations
7 - number of flags collected
8,9,10 - soldiers report on whether enemy spawn should be visited ((1,2), (3,4), (5,6)) respectively {1 = no, 0 = yes} //should probably create a quality system later maybe

14,15 - soldier found enemy with flag
16,17,18,19,20,21,22,23,24 - potential escort locations + flag escorter id: (x,y,id)


61 - temporary int for military engineer assignment || temporary int for escort assignment 1
62 - temporary int for guardian creation || temporary int for escort assignment 2
63 - temporary int for scout creation || temporary int for escort assignment 3
**/
public strictfp class RobotPlayer {
    static int turnCount = 0;

    static Random rng;

    static int profession; // 0 = soldier, 1 = guardian, 2 = healer, 3 - scout(temp profession -> soldier), 4 - escort(temp profession -> soldier/healer)
    static boolean recordedBuildDone = false;

    static int guardianId = -1;

    static boolean goingAround = false;

    static Direction bugodir = null;

    static int scoutNum;
    static int[] scoutDest;

    static int escortFollowingIndex = -1; //the index a escort is following(18, 21, 24)

    static MapLocation militaryDest = null;
    static boolean militaryEngineer = false;

    static MapLocation mapCenter = null;

    static int SMALL_MAP_MIN = 1200; //min map area for small map
    static boolean mapSizeSmall = false; //the current player plays badly on small  maps due to large enemy concentrations, small maps will have better escort protection

    static final float HEALER_CHANCE = 0.5f;

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

        int guardianInt = rc.readSharedArray(62);
        if (guardianInt < 3) {
            if (guardianInt == 2) {
                if (rc.canWriteSharedArray(62, 100)) {
                    rc.writeSharedArray(62, 100);
                }
            } else if (rc.canWriteSharedArray(62, guardianInt + 1)) {
                rc.writeSharedArray(62, guardianInt + 1);
            }
            
            profession = 1;
            guardianId = guardianInt;
        }

        if ((rc.getMapWidth() * rc.getMapHeight()) <= SMALL_MAP_MIN) {mapSizeSmall = true;}

        if (rc.readSharedArray(8) != 0) {
            if (rc.canWriteSharedArray(8, 0)) {
                rc.writeSharedArray(8, 0);
            }
            if (rc.canWriteSharedArray(9, 0)) {
                rc.writeSharedArray(9, 0);
            }
            if (rc.canWriteSharedArray(10, 0)) {
                rc.writeSharedArray(10, 0);
            }
        }

        mapCenter = new MapLocation(Math.round(rc.getMapWidth()/2), Math.round(rc.getMapHeight()/2));
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation[] actualSpawns = findActualSpawns(rc.getAllySpawnLocations());
        spawnLocs = null;

        if (!(profession == 1)) {
            if (rng.nextFloat() >= HEALER_CHANCE) {profession = 2;} else {
            if (rc.readSharedArray(63) < 7) { // 7 scouts
                profession = 3;
                scoutNum = rc.readSharedArray(63);

                if (rc.readSharedArray(63) == 6) {
                    if (rc.canWriteSharedArray(63, 100)) {
                        rc.writeSharedArray(63, 100);
                    }
                }
                if (rc.canWriteSharedArray(63, rc.readSharedArray(63) + 1)) {
                    rc.writeSharedArray(63, rc.readSharedArray(63) + 1);
                }
            } else {
                profession = 0;
            }
            }
        }

        if (profession == 3) {
            float spawnAvgX = (actualSpawns[0].x + actualSpawns[1].x + actualSpawns[2].x) / 3;
            float spawnAvgY = (actualSpawns[0].y + actualSpawns[1].y + actualSpawns[2].y) / 3;
            float scoutDIncr = (rc.getMapHeight() + rc.getMapWidth())/7; //uses scout number: 7
            float incredScoutNum = scoutNum * scoutDIncr;

            if (spawnAvgX > rc.getMapWidth() / 2) {
                if (spawnAvgY > rc.getMapHeight() / 2) {
                    //width, height
                    if (incredScoutNum < rc.getMapWidth()) {
                        scoutDest = new int[] {scoutClamp(rc.getMapWidth() - Math.round(incredScoutNum) - 1, rc.getMapWidth()), 0};
                    } else if (incredScoutNum == rc.getMapWidth()) {
                        scoutDest = new int[] {0, 0};
                    } else {
                        scoutDest = new int[] {0, scoutClamp(Math.round(incredScoutNum) - rc.getMapWidth() - 1, rc.getMapHeight())};
                    }
                } else {
                    //width, 0
                    if (incredScoutNum < rc.getMapWidth()) {
                        scoutDest = new int[] {scoutClamp(rc.getMapWidth() - Math.round(incredScoutNum) - 1, rc.getMapWidth()), rc.getMapHeight() - 1};
                    } else if (incredScoutNum == rc.getMapWidth()) {
                        scoutDest = new int[] {0, rc.getMapHeight() - 1};
                    } else {
                        scoutDest = new int[] {0, scoutClamp(rc.getMapHeight() - (Math.round(incredScoutNum) - rc.getMapWidth()) - 1, rc.getMapHeight())};
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
                        scoutDest = new int[] {rc.getMapWidth() - 1, scoutClamp(Math.round(incredScoutNum) - rc.getMapWidth() - 1, rc.getMapHeight())};
                    }
                } else {
                    //0, 0
                    if (incredScoutNum < rc.getMapWidth()) {
                        scoutDest = new int[] {Math.round(incredScoutNum), rc.getMapHeight() - 1};
                    } else if (incredScoutNum == rc.getMapWidth()) {
                        scoutDest = new int[] {rc.getMapWidth() - 1, rc.getMapHeight() - 1};
                    } else {
                        scoutDest = new int[] {rc.getMapWidth() - 1, scoutClamp(rc.getMapHeight() - (Math.round(incredScoutNum) - rc.getMapWidth()) - 1, rc.getMapHeight())};
                    }
                }
            }
        }

        while (true) {
            Pathfinding.Update(rc, rc.getLocation());
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
            } else if (rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
                rc.buyGlobal(GlobalUpgrade.ATTACK);
            } else if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
                rc.buyGlobal(GlobalUpgrade.CAPTURING);
            }

            try {
                if (!rc.isSpawned()){
                    Pathfinding.resetBug();

                    burnEscortRecords(rc);

                    if (profession == 1) {
                        if (rc.canSpawn(actualSpawns[guardianId])) {
                            rc.spawn(actualSpawns[guardianId]);
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
                        rc.setIndicatorString("I am a guardian!");
                    } else if (profession == 2) {
                        rc.setIndicatorString("I am a healer!");
                    } else if (profession == 3) {
                        rc.setIndicatorString("I am a scout! Destination: " + scoutDest[0] + " " + scoutDest[1] + "");
                    } else if (profession == 4) {
                        rc.setIndicatorString("I am a escort!");
                    }

                    FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
                    if (flags.length != 0){
                        for (FlagInfo flag : flags) {
                            if (flag.isPickedUp()) {
                                if (rc.getLocation().distanceSquaredTo(flag.getLocation()) <= 20) {
                                    if (rc.senseRobotAtLocation(flag.getLocation()).getTeam() == rc.getTeam()) {
                                        break;
                                    } else {
                                        if (rc.canWriteSharedArray(14, flag.getLocation().x)) {
                                            rc.writeSharedArray(14, flag.getLocation().x);
                                        }
                                        if (rc.canWriteSharedArray(15, flag.getLocation().y)) {
                                            rc.writeSharedArray(15, flag.getLocation().y);
                                        }

                                        Direction d = rc.getLocation().directionTo(flag.getLocation());
                                        if (rc.canMove(d)) {
                                            rc.move(d);
                                        }

                                        if (rc.canAttack(flag.getLocation())) {
                                            rc.attack(flag.getLocation());
                                            System.out.println("Near flag attack!");
                                        }
                                    }
                                }
                            }

                            if (rc.canPickupFlag(flag.getLocation())){
                                rc.pickupFlag(flag.getLocation());
                                rc.setIndicatorString("Holding a flag!");   
                            } else {
                                Direction dir = rc.getLocation().directionTo(flag.getLocation());
                                MapLocation nextLoc = rc.getLocation().add(dir);
                                if (rc.canMove(dir)) {
                                    rc.move(dir);
                                } else if (rc.canFill(nextLoc)) {
                                    rc.fill(nextLoc);
                                    if (rc.canMove(dir)) {
                                        rc.move(dir);
                                    }
                                } else if (rc.canAttack(nextLoc)){
                                    rc.attack(nextLoc);
                                    System.out.println("Near flag attack!");
                                }
                            }
                        }
                    }

                    if (rc.canPickupFlag(rc.getLocation())){
                        if (rc.senseNearbyFlags(1, rc.getTeam().opponent()).length != 0){
                            rc.pickupFlag(rc.getLocation());
                            if (profession == 4) {profession = 0;}
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

                        Pathfinding.pathfind(rc, closestLoc, null, true);

                        //escort (x,y)s - 16,17 19,20 22,23
                        //escort ids - 18, 21, 24
                        int x = rc.getLocation().x, y = rc.getLocation().y; int myId = rc.getID();
                        if (rc.readSharedArray(18) == myId) {
                            if (rc.canWriteSharedArray(16, x)) {
                                rc.writeSharedArray(16, x);
                            }
                            if (rc.canWriteSharedArray(17, y)) {
                                rc.writeSharedArray(17, y);
                            }
                        } else if (rc.readSharedArray(21) == myId) {
                            if (rc.canWriteSharedArray(19, x)) {
                                rc.writeSharedArray(19, x);
                            }
                            if (rc.canWriteSharedArray(20, y)) {
                                rc.writeSharedArray(20, y);
                            }
                        } else if (rc.readSharedArray(24) == myId) {
                            if (rc.canWriteSharedArray(22, x)) {
                                rc.writeSharedArray(22, x);
                            }
                            if (rc.canWriteSharedArray(23, y)) {
                                rc.writeSharedArray(23, y);
                            }
                        } else {
                            if (rc.readSharedArray(18) < 1) {
                                if (rc.canWriteSharedArray(18, myId)) {
                                    rc.writeSharedArray(18, myId);
                                }
                                if (rc.canWriteSharedArray(16, x)) {
                                    rc.writeSharedArray(16, x);
                                }
                                if (rc.canWriteSharedArray(17, y)) {
                                    rc.writeSharedArray(17, y);
                                }
                                if (rc.canWriteSharedArray(61, 0)) {
                                    rc.writeSharedArray(61, 0);
                                }
                            } else if (rc.readSharedArray(21) < 1) {
                                if (rc.canWriteSharedArray(21, myId)) {
                                    rc.writeSharedArray(21, myId);
                                }
                                if (rc.canWriteSharedArray(19, x)) {
                                    rc.writeSharedArray(19, x);
                                }
                                if (rc.canWriteSharedArray(20, y)) {
                                    rc.writeSharedArray(20, y);
                                }
                                if (rc.canWriteSharedArray(62, 0)) {
                                    rc.writeSharedArray(62, 0);
                                }
                            } else if (rc.readSharedArray(24) < 1) {
                                if (rc.canWriteSharedArray(24, myId)) {
                                    rc.writeSharedArray(24, myId);
                                }
                                if (rc.canWriteSharedArray(22, x)) {
                                    rc.writeSharedArray(22, x);
                                }
                                if (rc.canWriteSharedArray(23, y)) {
                                    rc.writeSharedArray(23, y);
                                }
                                if (rc.canWriteSharedArray(63, 0)) {
                                    rc.writeSharedArray(63, 0);
                                }
                            }
                        }
                    } else {burnEscortRecords(rc);}

                    MapLocation[] crumbLocations = rc.senseNearbyCrumbs(-1);
                    if (profession != 3 && crumbLocations.length != 0){
                        Direction dir = rc.getLocation().directionTo(crumbLocations[0]);
                        if (rc.canMove(dir)) rc.move(dir);
                    }

                    if (profession == 1) {
                        if (rc.getRoundNum() > GameConstants.SETUP_ROUNDS && rc.getLocation().equals(actualSpawns[guardianId])) {
                            MapLocation[] buildLocs = {rc.getLocation().add(Direction.NORTH), rc.getLocation().add(Direction.NORTHEAST), rc.getLocation().add(Direction.EAST), rc.getLocation().add(Direction.SOUTHEAST), rc.getLocation().add(Direction.SOUTH), rc.getLocation().add(Direction.SOUTHWEST), rc.getLocation().add(Direction.WEST), rc.getLocation().add(Direction.NORTHWEST)};

                            for (int i = 0; i < buildLocs.length; i++) {
                                if (rc.canBuild(TrapType.EXPLOSIVE, buildLocs[i])) {
                                    rc.build(TrapType.EXPLOSIVE, buildLocs[i]);
                                    break;
                                }
                            }
                        }

                        if (!tacticalAttack(rc)) {
                            if (!rc.getLocation().equals(actualSpawns[guardianId])) {
                                if (rc.canMove(rc.getLocation().directionTo(actualSpawns[guardianId]))) {
                                    rc.move(rc.getLocation().directionTo(actualSpawns[guardianId]));
                                }

                                RobotInfo[] nearbyEnems = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                                for (RobotInfo ri : nearbyEnems) {
                                    if (rc.canAttack(ri.location)) {
                                        rc.attack(ri.location);
                                    }
                                }
                            }
                        }
                    } else if (profession == 2) {
                        for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
                            if (rc.canHeal(robot.location)) {
                                rc.heal(robot.location);
                                //System.out.println("Healer healed a robot!");
                            }
                        }
                    } else if (profession == 3) {
                        if (rc.getLocation().x == scoutDest[0] && rc.getLocation().y == scoutDest[1]) {
                            profession = 0; //scout returns to being an ordinary soldier
                        }

                        MapLocation dest = new MapLocation(scoutDest[0], scoutDest[1]);

                        if (rc.getLocation().distanceSquaredTo(dest) <= 20) {
                            if (!rc.senseMapInfo(dest).isPassable()) {
                                profession = 0;
                            }
                        }


                        //rc.setIndicatorLine(rc.getLocation(), new MapLocation(scoutDest[0], scoutDest[1]), 0, 255, 0);
                        Pathfinding.pathfind(rc, dest, null, false);
                        scoutEnemyDetect(rc);
                    } else if (profession == 4) {
                        boolean terminated = false;
                        if (rc.readSharedArray(escortFollowingIndex) < 1) {profession = 0; terminated = true;}

                        if (!terminated) {
                            MapLocation escortee = new MapLocation(rc.readSharedArray(escortFollowingIndex - 2), rc.readSharedArray(escortFollowingIndex - 1));

                            Pathfinding.pathfind(rc, escortee, null, false);

                            if (rc.getLocation().distanceSquaredTo(escortee) <= 20) {
                                if (rc.canHeal(escortee)) {
                                    rc.heal(escortee);
                                }
                            }

                            tacticalAttack(rc);
                        }
                    }

                    if (profession == 0 || profession == 2) {
                        int[] possibleEscorts = {18, 21, 24};
                        int[] escortSetters = {61, 62, 63};

                        for (int id = 0; id < 3; id++) {
                            if (!(rc.readSharedArray(possibleEscorts[id]) < 0)) {
                                int escortSet = rc.readSharedArray(escortSetters[id]); if (escortSet == 100) {escortSet = 0;}
                                if (escortSet < 5) {
                                    profession = 4;
                                    escortFollowingIndex = possibleEscorts[id];
                                    if (rc.canWriteSharedArray(escortSetters[id], escortSet + 1)) {
                                        rc.writeSharedArray(escortSetters[id], escortSet + 1);
                                    }
                                }
                            }
                        }
                    }

                    if (profession == 0 || profession == 2) {
                        militaryPathfinding(rc);
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
            if (mi.getSpawnZoneTeamObject() == rc.getTeam().opponent()) {
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

    public static void updateMilitaryRecords(RobotController rc) throws GameActionException{
        MapInfo mi = rc.senseMapInfo(rc.getLocation());
        if (mi.getSpawnZoneTeamObject() == rc.getTeam().opponent()) {
            int spwnIndx = -1;

            for (int enemIndx = 0; enemIndx < 3; enemIndx++) {
                if ((Math.abs(rc.readSharedArray(enemIndx * 2 + 1) - rc.getLocation().x) <= 3) &&
                    (Math.abs(rc.readSharedArray(enemIndx * 2 + 2) - rc.getLocation().y) <= 3)) {
                    spwnIndx = enemIndx;
                    break;
                }
            }

            if (spwnIndx == -1) {return;}

            if (rc.senseNearbyFlags(20, rc.getTeam().opponent()).length != 0) {
                if (rc.canWriteSharedArray(spwnIndx + 8, 0)) {
                    rc.writeSharedArray(spwnIndx + 8, 0);
                }
                return;
            }

            for (MapInfo mi_ : rc.senseNearbyMapInfos()) {
                if (mi_.getSpawnZoneTeamObject() == rc.getTeam().opponent()) {
                    if (mi_.getTrapType() == TrapType.NONE) {
                        if (rc.canWriteSharedArray(spwnIndx + 8, 0)) {
                            rc.writeSharedArray(spwnIndx + 8, 0);
                        }
                        return;
                    }
                }
            }

            if (rc.canWriteSharedArray(spwnIndx + 8, 1)) {
                rc.writeSharedArray(spwnIndx + 8, 1);
            }
        }
    }

    private static MapLocation computeStartingMilitaryDest(RobotController rc) throws GameActionException{
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation[] actualSpawns = findActualSpawns(rc.getAllySpawnLocations());
        float spawnAvgX = (actualSpawns[0].x + actualSpawns[1].x + actualSpawns[2].x) / 3;
        float spawnAvgY = (actualSpawns[0].y + actualSpawns[1].y + actualSpawns[2].y) / 3;

        boolean playerCornerNormal = false;

        if (spawnAvgX > rc.getMapWidth() / 2) {
            if (spawnAvgY > rc.getMapHeight() / 2) {
                playerCornerNormal = false;
            } else {
                playerCornerNormal = true;
            }
        } else {
            if (spawnAvgY > rc.getMapHeight() / 2) {
                playerCornerNormal = true;
            } else {
                playerCornerNormal = false;
            }
        }

        int a = rng.nextInt(rc.getMapWidth());
        if (playerCornerNormal) {
            return (new MapLocation(a, Math.round((rc.getMapHeight()/rc.getMapWidth()) * a)));
        } else {
            return (new MapLocation(a, Math.round(rc.getMapHeight() - ((rc.getMapHeight()/rc.getMapWidth()) * a))));
        }
    }
    
    public static void militaryPathfinding(RobotController rc) throws GameActionException{
        if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS + 4) {
            if (rc.getRoundNum() >= (GameConstants.SETUP_ROUNDS + 2)) {
                tacticalAttack(rc);
                return;
            } else if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS) {
                if (!tacticalAttack(rc)) {
                    Direction dir = rc.getLocation().directionTo(mapCenter).opposite();
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                }
                return;
            }
        }

        if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) {
            if (rc.getRoundNum() > (GameConstants.SETUP_ROUNDS - Math.max(rc.getMapWidth(), rc.getMapHeight()))) {
                if (militaryDest == null) {
                    militaryDest = computeStartingMilitaryDest(rc);
                }

                Pathfinding.pathfind(rc, militaryDest, null, false);
            } else {militaryMovement(rc);}

            if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length != 0) {
                int mEngSet = rc.readSharedArray(61);
                if (mEngSet < 7) {
                    if (mEngSet == 6) {
                        if (rc.canWriteSharedArray(61, 100)) {
                            rc.writeSharedArray(61, 100);
                        }
                    } else if (rc.canWriteSharedArray(61, mEngSet + 1)) {
                        rc.writeSharedArray(61, mEngSet + 1);
                    }
                    militaryEngineer = true;
                } 
            } else {
                if (militaryEngineer) {
                    int mEngSet = rc.readSharedArray(61);
                    if (mEngSet == 100) {
                        if (rc.canWriteSharedArray(61, 6)) {
                            rc.writeSharedArray(61, 6);
                        }
                    } else {
                        if (rc.canWriteSharedArray(61, mEngSet - 1)) {
                            rc.writeSharedArray(61, mEngSet - 1);
                        }
                    }
                    militaryEngineer = false;
                }
            }

            if (militaryEngineer) {
                if (rc.getRoundNum() >= (GameConstants.SETUP_ROUNDS - 20)) {
                    MapLocation myLoc = rc.getLocation();
                    for (MapLocation tLoc : new MapLocation[]{myLoc.add(Direction.EAST), myLoc.add(Direction.NORTHEAST), myLoc.add(Direction.NORTHWEST), myLoc, myLoc.add(Direction.NORTH), myLoc.add(Direction.SOUTH), myLoc.add(Direction.SOUTHEAST), myLoc.add(Direction.SOUTHWEST), myLoc.add(Direction.WEST)}) {
                        if (rng.nextFloat() >= 0.7) {
                            if (rc.canBuild(TrapType.STUN, tLoc)) {
                                rc.build(TrapType.STUN, tLoc);
                            }
                        } else {
                            if (rc.canBuild(TrapType.EXPLOSIVE, tLoc)) {
                                rc.build(TrapType.EXPLOSIVE, tLoc);
                            }
                        }
                    }
                }
            }
            return;
        } else {
            if (rc.getCrumbs() > 1000) {
                if (rc.canBuild(TrapType.STUN, rc.getLocation())) {
                    rc.build(TrapType.STUN, rc.getLocation());
                }
            }
        }

        MapInfo mi = rc.senseMapInfo(rc.getLocation());
        if ((mi.getSpawnZoneTeamObject() == rc.getTeam().opponent()) && rc.getCrumbs() > 700) {
            if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation())) {
                rc.build(TrapType.EXPLOSIVE, rc.getLocation());
            }
        }

        scoutEnemyDetect(rc);
        updateMilitaryRecords(rc);

        if (tacticalAttack(rc)) {return;}

        MapLocation moveTarget = null;
        boolean checkNullFlag = false;
        boolean makeShiftEscort = false;

        MapLocation[] flagDetections = rc.senseBroadcastFlagLocations();

        if (mapSizeSmall && !(rc.readSharedArray(18) < 1)) {
            moveTarget = new MapLocation(rc.readSharedArray(16), rc.readSharedArray(17)); makeShiftEscort = true;
        } else if ((mapSizeSmall && !(rc.readSharedArray(21) < 1))) {
            moveTarget = new MapLocation(rc.readSharedArray(19), rc.readSharedArray(20)); makeShiftEscort = true;
        } else if ((mapSizeSmall && !(rc.readSharedArray(24) < 1))) {
            moveTarget = new MapLocation(rc.readSharedArray(22), rc.readSharedArray(23)); makeShiftEscort = true;
        } else if (flagDetections.length != 0) {
            moveTarget = flagDetections[0];
        } else if (!(rc.readSharedArray(14) < 1)) {
            moveTarget = new MapLocation(rc.readSharedArray(14), rc.readSharedArray(15)); checkNullFlag = true;
        } else if (!(rc.readSharedArray(5) < 1) && (rc.readSharedArray(8) == 0)) {
            moveTarget = new MapLocation(rc.readSharedArray(5), rc.readSharedArray(6));
        } else if (!(rc.readSharedArray(3) < 1) && (rc.readSharedArray(9) == 0)) {
            moveTarget = new MapLocation(rc.readSharedArray(3), rc.readSharedArray(4));
        } else if (!(rc.readSharedArray(1) < 1) && (rc.readSharedArray(10) == 0)) {
            moveTarget = new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2));
        }

        if (moveTarget != null) {
            rc.setIndicatorLine(rc.getLocation(), moveTarget, 255, 0, 0);
            if (makeShiftEscort) {
                Pathfinding.pathfind(rc, moveTarget, Pathfinding.calculateNextMove(rc, moveTarget, findEscortDest(rc, moveTarget)), false);
            } else {
                Pathfinding.pathfind(rc, moveTarget, null, false);
            }

            if (checkNullFlag) {
                if (rc.getLocation().equals(moveTarget)) {
                    if (rc.senseNearbyFlags(-1, rc.getTeam().opponent()).length == 0) {
                        if (rc.canWriteSharedArray(14, 0)) {
                            rc.writeSharedArray(14, 0);
                        }
                        if (rc.canWriteSharedArray(15, 0)) {
                            rc.writeSharedArray(15, 0);
                        }
                    }
                }
            }
        } else {
            militaryMovement(rc);
        }

        if (makeShiftEscort) {
            if (rc.getLocation().distanceSquaredTo(moveTarget) <= 20) {
                if (rc.canHeal(moveTarget)) {
                    rc.heal(moveTarget);
                }
            }
        }
    }

    public static boolean tacticalAttack(RobotController rc) throws GameActionException{
        RobotInfo[] nearbyEnems = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        MapLocation attacked = null;

        if (nearbyEnems.length != 0) {
            for (RobotInfo ri : nearbyEnems) {
                if (rc.canAttack(ri.location)) {
                    rc.attack(ri.location);
                    attacked = ri.location;
                    break;
                }
            }
        } else {return false;}

        if (attacked != null) {
            Direction moveAway = rc.getLocation().directionTo(attacked).opposite();
            if (rc.canMove(moveAway)) {
                rc.move(moveAway);
                return true;
            }
        }
        return false;
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
            //System.out.println("Healer/Soldier attacked!");
        }
    }

    public static int scoutClamp(int n, int N) {if (n < 0) {return 0;} else if (n > N) {return N;} else {return n;}}

    public static void burnEscortRecords(RobotController rc) throws GameActionException{
        int myId = rc.getID();
        if (rc.readSharedArray(18) == myId) {
            if (rc.canWriteSharedArray(16, 0)) {
                rc.writeSharedArray(16, 0);
            }
            if (rc.canWriteSharedArray(17, 0)) {
                rc.writeSharedArray(17, 0);
            }
            if (rc.canWriteSharedArray(18, 0)) {
                rc.writeSharedArray(18, 0);
            }
            if (rc.canWriteSharedArray(61, 0)) {
                rc.writeSharedArray(61, 0);
            }
        } else if (rc.readSharedArray(21) == myId) {
            if (rc.canWriteSharedArray(19, 0)) {
                rc.writeSharedArray(19, 0);
            }
            if (rc.canWriteSharedArray(20, 0)) {
                rc.writeSharedArray(20, 0);
            }
            if (rc.canWriteSharedArray(21, 0)) {
                rc.writeSharedArray(21, 0);
            }
            if (rc.canWriteSharedArray(62, 0)) {
                rc.writeSharedArray(62, 0);
            }
        } else if (rc.readSharedArray(24) == myId) {
            if (rc.canWriteSharedArray(22, 0)) {
                rc.writeSharedArray(22, 0);
            }
            if (rc.canWriteSharedArray(23, 0)) {
                rc.writeSharedArray(23, 0);
            }
            if (rc.canWriteSharedArray(24, 0)) {
                rc.writeSharedArray(24, 0);
            }
            if (rc.canWriteSharedArray(63, 0)) {
                rc.writeSharedArray(63, 0);
            }
        }
    }

    public static MapLocation findEscortDest(RobotController rc, MapLocation scoutCoords) throws GameActionException{            
        MapLocation closestLoc = null;
        int closestDist = 999999;

        for (MapLocation ml : rc.getAllySpawnLocations()) {
            int d = scoutCoords.distanceSquaredTo(ml);
            if (d < closestDist) {
                closestDist = d;
                closestLoc = ml;
            }
        }

        return closestLoc;
    }

    public static MapLocation[] findActualSpawns(MapLocation[] spawns) throws GameActionException{
        int spwn1 = 0;
        int spwn2 = 0;
        int spwn3 = 0;
        
        MapLocation spwnFirst1 = null;
        MapLocation spwnFirst2 = null;
        MapLocation spwnFirst3 = null;

        MapLocation c1 = null;
        MapLocation c2 = null;
        MapLocation c3 = null;

        for (MapLocation ml : spawns) {
            if (spwn1 == 0) {
                spwn1 += 1;
                spwnFirst1 = ml;
                continue;
            } else {
                if (spwnFirst1.distanceSquaredTo(ml) <= 9) {
                    spwn1 += 1;
                    if (spwn1 == 5) {
                        c1 = ml;
                    }
                    continue;
                }
            }

            if (spwn2 == 0) {
                spwn2 += 1;
                spwnFirst2 = ml;
                continue;
            } else {
                if (spwnFirst2.distanceSquaredTo(ml) <= 9) {
                    spwn2 += 1;
                    if (spwn2 == 5) {
                        c2 = ml;
                    }
                    continue;
                }
            }

            if (spwn3 == 0) {
                spwn3 += 1;
                spwnFirst3 = ml;
                continue;
            } else {
                if (spwnFirst3.distanceSquaredTo(ml) <= 9) {
                    spwn3 += 1;
                    if (spwn3 == 5) {
                        c3 = ml;
                    }
                    continue;
                }
            }
        }

        return (new MapLocation[]{c1, c2, c3});
    }
}
