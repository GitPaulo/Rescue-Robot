/**
 * Execution of the EV3 Brick starts here.
 * @author Group 16
 */
public class Core {
    
    // Constants
    public static final int MONITOR_DELAY = 1000;
    public static final int SERVER_PORT	  = 1234;
    
    public static void main(String[] args) {
        Robot robot 	   = new Robot();
        Monitor monitor    = new Monitor(robot, MONITOR_DELAY);	
        RobotController rc = new RobotController(robot, monitor);
        
        // Install monitor so the robot can reference it
        robot.installMonitor(monitor);
        
        // Start the Pilot Monitor
           monitor.start();
           
           // Start the Color Sampling
        robot.startSampler();  
           
           // Start Robot Controller
           rc.start();
    }
}