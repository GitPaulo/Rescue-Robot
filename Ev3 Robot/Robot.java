import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;

/**
 * Robot class that intialises robot componets and stores the state of the robot.
 * @author group 16
 */
public class Robot {
    // Constants
    public final int DEFAULT_LINEAR_SPEED    	     = 16;
    public final int DEFAULT_ANGULAR_SPEED   	     = 60;
    public final double DEFAULT_LINEAR_ACCELERATION  = 6.5;
    public final double DEFAULT_ANGULAR_ACCELERATION = 2;
    
    public final float WHEEL_DIAMETER = 4.2f;
    public final float CHASSIS_OFFSET = 5.32f;
    private final long START_TIME;
    
    // Sensors
    private EV3ColorSensor lColorSensor, rColorSensor;	
    private EV3UltrasonicSensor uSonicSensor;
    private EV3GyroSensor gyroSensor;
    
    // Robot State
    private float gyro_offset;
    private float heading;
    private int   num_instructions;
    private String last_instruction;
    
    // Sample arrays
    private SampleProvider leftSP, rightSP, distSP, gyroSP;	
    private float[] leftSample, rightSample, distSample, angleSample; 
    
    // Move Pilot
    private MovePilot pilot;
    
    // Motors
    private EV3MediumRegulatedMotor motorC;
    private EV3LargeRegulatedMotor motorL, motorR;
    
    // Monitor
    private Monitor monitor;
    private ColorSampler colorSampler;
    
    public Robot() {
        Brick myEV3 = BrickFinder.getDefault();
        
        // Set up sensors to their respective ports
        lColorSensor = new EV3ColorSensor(myEV3.getPort("S1"));
        gyroSensor   = new EV3GyroSensor(myEV3.getPort("S2"));
        uSonicSensor = new EV3UltrasonicSensor(myEV3.getPort("S3"));
        rColorSensor = new EV3ColorSensor(myEV3.getPort("S4"));

        leftSP  = lColorSensor.getRGBMode();
        rightSP = rColorSensor.getRGBMode();
        distSP  = uSonicSensor.getDistanceMode();
        gyroSP  = gyroSensor.getAngleMode();
        
        leftSample  = new float[leftSP.sampleSize()];	// Size is 3 (RGB)
        rightSample = new float[rightSP.sampleSize()];	// Size is 3 (RGB)
        distSample  = new float[distSP.sampleSize()];	// Size is 1
        angleSample = new float[gyroSP.sampleSize()];	// Size is 1
        
        motorC = new EV3MediumRegulatedMotor(myEV3.getPort("C"));
        motorL = new EV3LargeRegulatedMotor(myEV3.getPort("B"));
        motorR = new EV3LargeRegulatedMotor(myEV3.getPort("D"));
        
        // Set up chasis for the pilot
        Wheel leftWheel   = WheeledChassis.modelWheel(motorL, WHEEL_DIAMETER).offset(-CHASSIS_OFFSET);
        Wheel rightWheel  = WheeledChassis.modelWheel(motorR, WHEEL_DIAMETER).offset(CHASSIS_OFFSET);
        Chassis myChassis = new WheeledChassis(new Wheel[]{leftWheel, rightWheel}, WheeledChassis.TYPE_DIFFERENTIAL);

        pilot = new MovePilot(myChassis);
        pilot.setLinearSpeed(DEFAULT_LINEAR_SPEED); // cm per second
        pilot.setAngularSpeed(DEFAULT_ANGULAR_SPEED);
        pilot.setLinearAcceleration(DEFAULT_LINEAR_ACCELERATION);
        pilot.setLinearAcceleration(DEFAULT_ANGULAR_ACCELERATION);
        
        START_TIME = System.currentTimeMillis();
    }
    
    /**
     * Initialises the color sampling process.
     */
    public void startSampler() {
        // Color sampling start
        colorSampler = new ColorSampler(this);
        colorSampler.startSampling();
        
        monitor.setText(">> PRESS START <<");
        Button.waitForAnyPress();
        
        // Reset the value of the gyroscope to zero (run start)
        gyro_offset = 0;
        heading     = 0;
        gyroSensor.reset();
    }
    
    /**
     * Closes the robots resources.
     */
    public void closeRobot() {
        lColorSensor.close();
        rColorSensor.close();
        uSonicSensor.close();
        gyroSensor  .close();
    }
    
    public float[] getRightColor() {
        rightSP.fetchSample(rightSample, 0);
        return rightSample;
    }
    
    public float[] getLeftColor() {
        leftSP.fetchSample(leftSample, 0);
        return leftSample;
    }
    
    public ColorSampler getColorSampler() {
        return this.colorSampler;
    }
    
    public float getDistance() {
        distSP.fetchSample(distSample, 0);
        return distSample[0];
    }
    
    public float getAngle() {
        gyroSP.fetchSample(angleSample, 0);
        
        float ang_inc = angleSample[0];
        
        return (ang_inc+gyro_offset)%360;
    }

    public MovePilot getPilot() {
        return pilot;
    }
    
    public long getElapsedTime() {
        return System.currentTimeMillis() - START_TIME;
    }
    
    public void installMonitor(Monitor monitor) {
        this.monitor = monitor;
    }
    
    public Monitor getMonitor() {
        return this.monitor;
    }
    
    public EV3MediumRegulatedMotor getUSMotor() {
        return this.motorC;
    }

    public void resetGyro() {
        gyro_offset = heading;
        gyroSensor.reset();
    }

    public float getHeading() {
        return heading;
    }

    public void updateHeading(float mappedAngle) {
        this.heading += mappedAngle;
    }

    public int getNumInstructions() {
        return num_instructions;
    }

    public void newInstruction(String cmd) {
        this.last_instruction = cmd;
        this.num_instructions++;
    }
    
    public String getLastInstruction() {
        return this.last_instruction;
    }
}