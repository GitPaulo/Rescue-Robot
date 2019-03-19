import java.util.Arrays;

import lejos.hardware.Button;
import lejos.hardware.Sound;

/**
 *  Color Sampler Class
 *  Class built to handle the color sampling use by the robot.
 *  A color model is first built by sampling each color multiple times.
 *  Then colors are classified by measuring the euclidian distance of the RGB values on a 3D space.
 * @author Group 16
 *
 */
public class ColorSampler {
    // Constants
    private static final int NUM_SAMPLES = 50000;
    public static final int MAX_COLORS   = 6; // yellow, cyan, green, burgandy, white and black
    public static final int RIGHT_SENSOR = 1;
    public static final int LEFT_SENSOR  = 2;
    
    // Instance Samples
    private String[]  colorMap;
    private float[][] rightSampleSpace;
    private float[][] leftSampleSpace;
    
    // State
    private Robot robot;
    private int pointer;
    
    public ColorSampler(Robot robot) { // size is actually from 0 to 1 ?
        rightSampleSpace = new float[MAX_COLORS][3]; // RGB
        leftSampleSpace  = new float[MAX_COLORS][3];
        colorMap    = new String[MAX_COLORS];
        this.robot  = robot;
    }

    
    /**
     * Samples the color and stores the average value in the sample space.
     * @param colorName - The name for reference
     */
    private void sample(String colorName) {
        robot.getMonitor().setText("Sampling: " + colorName);
        Button.waitForAnyPress();
        
        colorMap[pointer] = colorName;
        
        // Sample with left color sensor
        float laverage[] = new float[3];
        float raverage[] = new float[3];
        
        for ( int i = 0; i < NUM_SAMPLES; i++ ) {
            float[] sample = robot.getLeftColor();
            laverage[0] += sample[0];
            laverage[1] += sample[1];
            laverage[2] += sample[2];
        }
        
        laverage[0] = laverage[0]/NUM_SAMPLES;
        laverage[1] = laverage[1]/NUM_SAMPLES;
        laverage[2] = laverage[2]/NUM_SAMPLES;
        
        leftSampleSpace[pointer] = laverage;
        
        // Sample with right color sensor
        for ( int i = 0; i < NUM_SAMPLES; i++ ) {
            float[] sample = robot.getRightColor();
            raverage[0] += sample[0];
            raverage[1] += sample[1];
            raverage[2] += sample[2];
        }
        
        raverage[0] = raverage[0]/NUM_SAMPLES;
        raverage[1] = raverage[1]/NUM_SAMPLES;
        raverage[2] = raverage[2]/NUM_SAMPLES;
        
        rightSampleSpace[pointer] = raverage;
        
        Sound.twoBeeps();
        pointer++;
    }
    
    /**
     * Automate Sampling for the colors used in the assignment.
     */
    public void startSampling() {
        if ( pointer >= MAX_COLORS ) {
            System.err.println("Error! We already sampled colors!");
            return;
        }
        
        sample("yellow");
        sample("cyan");
        sample("burgandy");
        sample("green");
        sample("black");
        sample("white");
    }
    
    /**
     * Given a sample from the sensors (RGB) and the sensor type (right/left)
     * It will proceed to approximate which color the sample is.
     * @param sample RGB float array
     * @param SENSOR_TYPE Right/Left sensor
     * @return Color String of reference
     */
    public String calculateColorClassification(float[] sample, int SENSOR_TYPE) {
        float[][] sampleSpace = null;
        
        if ( SENSOR_TYPE == RIGHT_SENSOR ) {
            sampleSpace = rightSampleSpace;
        }else if ( SENSOR_TYPE == LEFT_SENSOR ) {
            sampleSpace = leftSampleSpace;
        } else {
            System.err.println("Error, invalid sensor type!");
            return null;
        }
        
        int probable_index = -1;
        float sdis         = Integer.MAX_VALUE;
        
        for( int i=0; i < MAX_COLORS; i++ ) {
            float dr = sampleSpace[i][0] - sample[0]; // red
            float dg = sampleSpace[i][1] - sample[1]; // green
            float db = sampleSpace[i][2] - sample[2]; // blue
            
            float ed = (float) Math.sqrt( (dr*dr) + (dg*dg) + (db*db) );
            
            if ( ed < sdis ) {
                sdis = ed;
                probable_index = i;
            }
        }
        
        return colorMap[probable_index];
    }
    
    /**
     * Debug method, prints out sample spaces.
     * @return Debug String
     */
    public String debug() {
        String s1 = Arrays.toString(colorMap) + "\n";
        
        String s2 = "";
        for ( int i=0; i<rightSampleSpace.length; i++ ) {
            s2 += Arrays.toString(rightSampleSpace[i]) + "\n";
        }
        
        String s3 = "";
        for ( int i=0; i<leftSampleSpace.length; i++ ) {
            s3 += Arrays.toString(leftSampleSpace[i]) + "\n";
        }
        
        String s4 = "Pointer: " + pointer;
        
        return s1 + "RSS:\n" + s2 + "LSS:\n" + s3 + s4;
    }
}
