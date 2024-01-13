package currentworkingplayer;

import battlecode.common.*;

import java.util.HashSet;


public strictfp class Pathfinding {
    private static int bugstate = 0; // 0 - head towards destination, 1 - bug around obstacle
    //private static MapLocation closestObstacle = null; // for bugOne thats not being used rn (and closestObstacleDist)
    //private static int closestObstacleDist = 99999;
    private static Direction bugDir = null;

    private static MapLocation prevDest = null;
    private static HashSet<MapLocation> line = null;
    private static int obstacleStartDist = 0;

    public static void resetBug() {
        bugstate = 0;
        //closestObstacle = null;
        //closestObstacleDist = 99999;
        bugDir = null;
        prevDest = null;
        line = null;
        obstacleStartDist = 0;
    }

    public static void pathfind(RobotController rc, MapLocation destination) throws GameActionException{
        bugZero(rc, destination);
    }

    public static void bugZero(RobotController rc, MapLocation destination) throws GameActionException{
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