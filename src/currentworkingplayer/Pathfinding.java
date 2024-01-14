package currentworkingplayer;

import battlecode.common.*;

import java.util.HashSet;

import com.google.flatbuffers.FlexBuffers.Map;


public strictfp class Pathfinding {
    private static int bugstate = 0; // 0 - head towards destination, 1 - bug around obstacle
    //private static MapLocation closestObstacle = null; // for bugOne thats not being used rn (and closestObstacleDist)
    //private static int closestObstacleDist = 99999;
    private static Direction bugDir = null;

    private static MapLocation prevDest = null;
    private static HashSet<MapLocation> line = null;
    private static int obstacleStartDist = 0;

    private static final int MAX_LOG_LENGTH = 12;
    private static final int MAX_LOC_OCCUR = 2;
    private static final int BUG_TWO_TURN_COUNT = 10;
    private static final boolean USE_BUG_TWO = false;

    private static HashSet<MapLocation> oldLocations = new HashSet<>();

    private static int bugTwoTurnCount = -1;

    public static void resetBug() {
        bugstate = 0;
        //closestObstacle = null;
        //closestObstacleDist = 99999;
        bugDir = null;
        prevDest = null;
        line = null;
        obstacleStartDist = 0;

        oldLocations = new HashSet<>();
    }

    private static int locNumber(MapLocation loc) {
        int occurrences = 0;
        for (MapLocation move : oldLocations) {
            if (move.equals(loc)) {
                occurrences++;
                if (occurrences >= MAX_LOC_OCCUR) {return occurrences;}
            }
        }
        return occurrences;
    }
    public static void Update(RobotController rc, MapLocation myPos) throws GameActionException{
        if (!USE_BUG_TWO) {return;}
        rc.setIndicatorDot(new MapLocation(locNumber(myPos), 2), 0, 255, 0);

        if (locNumber(myPos) >= MAX_LOC_OCCUR) {
            bugTwoTurnCount = BUG_TWO_TURN_COUNT;
        }

        oldLocations.add(myPos);

        if (oldLocations.size() > MAX_LOG_LENGTH) {
            oldLocations.remove(oldLocations.iterator().next());
        }
    }

    public static MapLocation calculateNextMove(RobotController rc, MapLocation start, MapLocation destination) throws GameActionException{
        return bugZeroNextMove(rc, start, destination);
    }

    public static void pathfind(RobotController rc, MapLocation destination, MapLocation illegalLoc) throws GameActionException{
        if (bugTwoTurnCount != -1) {
            bugTwo(rc, destination);
            bugTwoTurnCount--;
        } else {
            bugZero(rc, destination, illegalLoc);
        }
    }

    public static void escapeLocation(RobotController rc, Direction loopDir) throws GameActionException{
        for (int i = 0; i < 8; i++) {
            if (rc.canMove(loopDir)) {
                rc.move(loopDir);
                break;
            } else {
                loopDir = loopDir.rotateLeft();
            }
        }
    }

    private static boolean isPassable(RobotController rc, MapLocation loc) throws GameActionException{
        if (rc.getLocation().distanceSquaredTo(loc) <= 20) {
            return rc.sensePassability(loc);
        } else {
            return true;
        }
    }
    private static MapLocation bugZeroNextMove(RobotController rc, MapLocation start, MapLocation destination) throws GameActionException{
        if (start.equals(destination)) {return start;}
        Direction bugDir = start.directionTo(destination);

        if (isPassable(rc, start.add(bugDir))) {
            return start.add(bugDir);
        } else {
            for (int i = 0; i < 8; i++) {
                if (isPassable(rc, start.add(bugDir))) {
                    return start.add(bugDir);
                } else {
                    bugDir = bugDir.rotateLeft();
                }
            }
        }
        return null;
    }
    public static void bugZero(RobotController rc, MapLocation destination, MapLocation illegalLoc) throws GameActionException{
        if (illegalLoc == null) {

            if (rc.getLocation().equals(destination)) {System.out.println("REQUESTING PATHFINDING TO CURRENT LOC. " + destination.x + " " + destination.y); return;}
            Direction bugDir = rc.getLocation().directionTo(destination);

            if (rc.canMove(bugDir)) {
                rc.move(bugDir);
            } else if (rc.canFill(rc.getLocation().add(bugDir))) {
                rc.fill(rc.getLocation().add(bugDir));
                if (rc.canMove(bugDir)) {
                    rc.move(bugDir);
                } else {
                    for (int i = 0; i < 8; i++) {
                        if (rc.canMove(bugDir)) {
                            rc.move(bugDir);
                            break;
                        } else {
                            bugDir = bugDir.rotateLeft();
                        }
                    }
                }
            } else {
                for (int i = 0; i < 8; i++) {
                    if (rc.canMove(bugDir)) {
                        rc.move(bugDir);
                        break;
                    } else {
                        bugDir = bugDir.rotateLeft();
                    }
                }
            }

        }
        else
        {
            if (rc.getLocation().equals(illegalLoc)) {escapeLocation(rc, Direction.NORTH); return;}
            if (rc.getLocation().equals(destination)) {System.out.println("REQUESTING PATHFINDING TO CURRENT LOC. " + destination.x + " " + destination.y); return;}
            Direction bugDir = rc.getLocation().directionTo(destination);

            if (rc.canMove(bugDir) && !rc.getLocation().add(bugDir).equals(illegalLoc)) {
                rc.move(bugDir);
            } else if (rc.canFill(rc.getLocation().add(bugDir))) {
                rc.fill(rc.getLocation().add(bugDir));
                if (rc.canMove(bugDir) && !rc.getLocation().add(bugDir).equals(illegalLoc)) {
                    rc.move(bugDir);
                } else {
                    for (int i = 0; i < 8; i++) {
                        if (rc.canMove(bugDir) && !rc.getLocation().add(bugDir).equals(illegalLoc)) {
                            rc.move(bugDir);
                            break;
                        } else {
                            bugDir = bugDir.rotateLeft();
                        }
                    }
                }
            } else {
                for (int i = 0; i < 8; i++) {
                    if (rc.canMove(bugDir) && !rc.getLocation().add(bugDir).equals(illegalLoc)) {
                        rc.move(bugDir);
                        break;
                    } else {
                        bugDir = bugDir.rotateLeft();
                    }
                }
            }

        }
    }


    /*
    public static void bugOne(RobotController rc, MapLocation destination) throws GameActionException{
        if (bugstate == 0) {
            bugDir = rc.getLocation().directionTo(destination);
            if (rc.canMove(bugDir)) {
                rc.move(bugDir);
            } else {
                bugstate = 1;
                closestObstacle = null;
                closestObstacleDist = 99999;
            }
        } else {
            if (rc.getLocation().equals(closestObstacle)) {
                bugstate = 0;
            }

            if (rc.getLocation().distanceSquaredTo(destination) < closestObstacleDist) {
                closestObstacleDist = rc.getLocation().distanceSquaredTo(destination);
                closestObstacle = rc.getLocation();
            }

            for (int i = 0; i < 9; i++) {
                if (rc.canMove(bugDir)) {
                    rc.move(bugDir);
                    bugDir = bugDir.rotateRight();
                    bugDir = bugDir.rotateRight();
                    break;
                } else {
                    bugDir = bugDir.rotateLeft();
                }
            }
        }
    }**/


    private static HashSet<MapLocation> createLine(MapLocation a, MapLocation b) {
        HashSet<MapLocation> locs = new HashSet<>();
        int x = a.x, y = a.y;
        int dx = b.x - a.x;
        int dy = b.y - a.y;
        int sx = (int) Math.signum(dx);
        int sy = (int) Math.signum(dy);
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        int d = Math.max(dx, dy);
        int r = d / 2;
        if (dx > dy) {
            for (int i = 0; i < d; i++) {
                locs.add(new MapLocation(x, y));
                x += sx;
                r += dy;
                if (r >= dx) {
                    locs.add(new MapLocation(x, y));
                    y += sy;
                    r -= dx;
                }
            }
        } else {
            for (int i = 0; i < d; i++) {
                locs.add(new MapLocation(x, y));
                y += sy;
                r += dx;
                if (r >= dy) {
                    locs.add(new MapLocation(x, y));
                    x += sx;
                    r -= dy;
                }
            }
        }
        locs.add(new MapLocation(x, y));
        return locs;
    }
    public static void bugTwo(RobotController rc, MapLocation destination) throws GameActionException{
        if (!destination.equals(prevDest)) {
            prevDest = destination;

            line = createLine(rc.getLocation(), destination);
        }

        if (bugstate == 0) {
            bugDir = rc.getLocation().directionTo(destination);
            if (rc.canMove(bugDir)) {
                rc.move(bugDir);
            } else if (rc.canFill(rc.getLocation().add(bugDir))) {
                rc.fill(rc.getLocation().add(bugDir));
                if (rc.canMove(bugDir)) {
                    rc.move(bugDir);
                } else {
                    bugstate = 1;
                    obstacleStartDist = rc.getLocation().distanceSquaredTo(destination);
                    bugDir = rc.getLocation().directionTo(destination);
                }
            } else {
                bugstate = 1;
                obstacleStartDist = rc.getLocation().distanceSquaredTo(destination);
                bugDir = rc.getLocation().directionTo(destination);
            }
        } else {
            if (line.contains(rc.getLocation()) && rc.getLocation().distanceSquaredTo(destination) < obstacleStartDist) {
                bugstate = 0;
            }

            for (int i = 0; i < 9; i++) {
                if (rc.canMove(bugDir)) {
                    rc.move(bugDir);
                    bugDir = bugDir.rotateRight();
                    bugDir = bugDir.rotateRight();
                    break;
                } else if (rc.canFill(rc.getLocation().add(bugDir))) {
                    rc.fill(rc.getLocation().add(bugDir));
                    if (rc.canMove(bugDir)) {
                        rc.move(bugDir);
                        bugDir = bugDir.rotateRight();
                        bugDir = bugDir.rotateRight();
                        break;
                    } else {
                        bugDir = bugDir.rotateLeft();
                    }
                } else {
                    bugDir = bugDir.rotateLeft();
                }
            }
        }
    }
}