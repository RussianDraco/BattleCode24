package NewHide;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;
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

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
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

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

      
        System.out.println("I'm alive");
        rc.setIndicatorString("Hello world!");

        boolean isHider = ((rc.getID() % 2) == 0);
        int gobackboys = 150;
        MapLocation originalSpawnLocation = null;

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1; 

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // Make sure you spawn your robot in before you attempt to take any actions!
                // Robots not spawned in do not have vision of any tiles and cannot perform any actions.
                if (!rc.isSpawned()){
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    // Pick a random spawn location to attempt spawning in.
                    for (int i = 0; i < spawnLocs.length; i++) {
                        MapLocation location = spawnLocs[i];
                        if (rc.canSpawn(location)){
                            rc.spawn(location);
                            originalSpawnLocation = location;
                            break;
                        }
                    }
                    
                }
                else{
                   
    
                    if (rc.getRoundNum() <= gobackboys){

                        MapLocation[] crumbLocations = rc.senseNearbyCrumbs(-1);
                    
                        if (crumbLocations.length != 0) {
                            // Sort crumbs by distance, so we can move towards the closest one

                            // Move towards the closest crumb
                            Direction dir = rc.getLocation().directionTo(crumbLocations[0]);
                            if (rc.canMove(dir)) {
                                rc.move(dir);
                                rc.setIndicatorString("There are nearby crumbs! Yum!");
                            } else {
                                // If can't move, try to fill the location
                                MapLocation nextLoc = rc.getLocation().add(dir);
                                if (rc.canFill(nextLoc)) {
                                    rc.fill(nextLoc);
                                    rc.setIndicatorString("MUST FILL");
                                }
                            }
                        } else {
                           
                            Direction randomDir = directions[rng.nextInt(directions.length)];
                            MapLocation randomLoc = rc.getLocation().add(randomDir);
                            if (rc.canMove(randomDir)) {
                                rc.move(randomDir);
                            } 
                        }
                    }else{

                    Direction moveDirection = rc.getLocation().directionTo(originalSpawnLocation);

                    if (rc.canMove(moveDirection)) {
                        rc.move(moveDirection);
                        rc.setIndicatorString("Moving back to spawn zone");
                    }


                    RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

                    if (nearbyEnemies.length > 0) {
                        // If there are enemies nearby, prioritize attacking over moving
                        rc.attack(nearbyEnemies[0].getLocation());
                        System.out.println("Attacking nearby enemy!");
                    }
                    
                    
                    RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());

                    for (RobotInfo robot : friendlyRobots) {
                            if (rc.canHeal(robot.getLocation())) {
                                rc.heal(robot.getLocation());
                                System.out.println("Healed a friendly robot!");
                        }
                    }
            
                    for (Direction dir : directions) {
                        MapLocation trapLocation = rc.getLocation().add(dir);
                        if (rc.canBuild(TrapType.STUN, trapLocation)) {
                            rc.build(TrapType.STUN, trapLocation);
                            System.out.println("Planted a stun trap!");
                        }
                    }

                    if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
                        rc.buyGlobal(GlobalUpgrade.HEALING);
                        System.out.println("BOUGHT UPGRADE");
                    }
                    }
                    
                }
                
            }
            catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }
    public static void updateEnemyRobots(RobotController rc) throws GameActionException{
        // Sensing methods can be passed in a radius of -1 to automatically 
        // use the largest possible value.
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0){
            rc.setIndicatorString("There are nearby enemy robots! Scary!");
            // Save an array of locations with enemy robots in them for future use.
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++){
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            // Let the rest of our team know how many enemy robots we see!
            if (rc.canWriteSharedArray(0, enemyRobots.length)){
                rc.writeSharedArray(0, enemyRobots.length);
                int numEnemies = rc.readSharedArray(0);
            }
        }
    }
}
