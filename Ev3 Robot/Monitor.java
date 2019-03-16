import java.text.SimpleDateFormat;
import java.util.Date;

import lejos.hardware.Battery;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;

/**
 * Monitor class tied in to the LCD screen of the robot.
 * Ran on a daemon thread.
 * @author group 16
 */
public class Monitor extends Thread {

	private final GraphicsLCD lcd = LocalEV3.get().getGraphicsLCD();
	private final long PRINT_TIME = 4000;
	
	private final String DEFAULT_TEXT = "GROUP 16 - RESCUE";
	private String text 			  = DEFAULT_TEXT;
	private String log				  = "(...)";
	
	private boolean suppress;
	private int delay;
	public Robot robot;

	public Monitor(Robot r, int d) {
		this.setDaemon(true);

		this.delay = d;
		this.robot = r;
		this.suppress = false;
	}
	
	/*
	 * Returns the current clock-time as a string.
	 */
	private String getCurrentTimeStamp() {
	    return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
	}
	
	/**
	 * Method that temporarily suppresses the lcd screen to display a message of text.
	 * @param txt
	 */
	public void print(String txt) {
		suppress = true;

		lcd.clear();
		lcd.setFont(Font.getSmallFont());
		lcd.drawString(txt, 0, lcd.getHeight() / 2, 0);

		try {
			sleep(PRINT_TIME);
			suppress = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the title text of the LCD screen
	 * @param text
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	/**
	 * Sets the log text of the LCD screen
	 * @param text
	 */
	public void setLog(String text) {
		this.log = text;
	}
	
	/**
	 * Clears the lcd screen
	 */
	public void clearLcd() {
		System.out.flush();  
		System.err.flush();
		lcd.clear();
	}
	
	/**
	 * Thread run method. 
	 * Ran each lcd frame.
	 */
	public void run() {
		while (true) {
			if (suppress)
				continue;

			lcd.clear();
			lcd.setFont(Font.getDefaultFont());
			lcd.drawString(text, 0, lcd.getHeight() / 2, 0);
			
			if (text.equals(DEFAULT_TEXT)) {
				lcd.setFont(Font.getSmallFont());
				lcd.drawString(getCurrentTimeStamp(), lcd.getWidth()-30, 2, 0);
				lcd.drawString(Battery.getBatteryCurrent() + "%", 5, 2, 0);
				lcd.drawString(	"H: "   + robot.getHeading() 
							+ " | A: "  + robot.getAngle() 
							+ " | #i: " + robot.getNumInstructions() 
							+ " | "
				, 0, lcd.getHeight() / 2 + 20, 0);
				lcd.drawString( "last_cmd: " + robot.getLastInstruction(), 0, lcd.getHeight() / 2 + 32, 0);
				lcd.drawString( "log: " + log, 0, lcd.getHeight() / 2 + 42, 0);
			}

			try {
				sleep(delay);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}