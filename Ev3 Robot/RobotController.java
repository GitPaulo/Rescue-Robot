import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import lejos.hardware.Sound;

/**
 * Robot class which handles networking, and robot actions.
 * This class parses instructions received through the network and makes the robot act accordingly.
 * @author group 16
 */
public class RobotController {
	public static int PORT = 1234;

	private Robot robot;
	private Monitor monitor;
	private ServerSocket serverSocket;
	private Socket client;
	private OutputStream os;
	private InputStream is;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;

	public RobotController(Robot robot, Monitor monitor) {
		this.robot = robot;
		this.monitor = monitor;
	}
	
	/**
	 * Moves the robot for a specified distance.
	 * @param distance Movement distance (cm)
	 */
	private void move(double distance) {
		robot.getPilot().travel(distance);

		while (robot.getPilot().isMoving())
			Thread.yield();
	}
	
	/**
	 * Rotates the robot by a specified angle.
	 * Note: Robot corrects rotation by use of the gyro!
	 * We do this by checking the initial and end angle and comparing it to the rotation angle.
	 * @param rotation_angle Rotation angle (deg)
	 * @return logging data
	 */
	private String rotate(float rotation_angle) {
		float start_angle = robot.getAngle();
		robot.getPilot().rotate(rotation_angle);

		while (robot.getPilot().isMoving())
			Thread.yield();

		float end_angle  = robot.getAngle();
		float difference = end_angle - start_angle;
		difference = (difference + 180) % 360 - 180;

		float error = Utility.shortestRotationAngle(rotation_angle - difference);

		String info = "SA:" + start_angle 
				   + " EA:" + end_angle 
				   + " RA:" + rotation_angle 
				   + " D:"  + difference 
				   + " ER:" + error;
		monitor.print(info);

		robot.getPilot().rotate(error);

		while (robot.getPilot().isMoving())
			Thread.yield();

		robot.updateHeading(rotation_angle);

		return info;
	}
	
	/**
	 * Returns the color value by using the color sampler.
	 * @param SENSOR_TYPE RIGHT/LEFT
	 * @param SAMPLE_RATE How many samples to take.
	 * @return String color reference (green, black ...)
	 */
	private String getColorValue(final int SENSOR_TYPE, final float SAMPLE_RATE) {
		float[] avg = new float[3];

		for (int i = 0; i < SAMPLE_RATE; i++) {
			float[] sample = SENSOR_TYPE == ColorSampler.RIGHT_SENSOR ? robot.getRightColor() : robot.getLeftColor();
			avg[0] += sample[0];
			avg[1] += sample[1];
			avg[2] += sample[2];
		}

		avg[0] /= SAMPLE_RATE;
		avg[1] /= SAMPLE_RATE;
		avg[2] /= SAMPLE_RATE;

		String color_class = robot.getColorSampler().calculateColorClassification(avg, SENSOR_TYPE);
		
		if (color_class.equals("yellow"))
			Sound.beep();
		
		if (color_class.equals("burgandy"))
			Sound.twoBeeps();
		
		if (color_class.equals("cyan"))
			Sound.beep();
			
		return color_class; // + " CC: " + Arrays.toString(avg);
	}
	
	/**
	 * Subset of the actions involving odometry correction.
	 * This action rotates the robot in an arc until it scans a black line.
	 * @param type right/left
	 * @param aspeed Angular Speed
	 */
	private void blackLineRotation(String type, double aspeed) {
		if (type.equals("right")) {
			robot.getPilot().arcForward(5);
		} else if (type.equals("left")) {
			robot.getPilot().arcForward(-5);
		} else {
			robot.getMonitor().setLog("josh this will never happen :(");
		}

		int sensor = type.equals("right") ? ColorSampler.LEFT_SENSOR : ColorSampler.RIGHT_SENSOR;
		String sensorColor = null;

		do {
			sensorColor = getColorValue(sensor, 20);
			robot.getMonitor().setLog(sensorColor);
		} while (!sensorColor.equals("black"));

		robot.getPilot().stop();
		robot.getMonitor().print("STOPPED!");

		// double check
		sensorColor = getColorValue(sensor, 20);
		if (!sensorColor.equals("black")) {
			robot.getMonitor().setLog("the end of the world is near");
		}

		robot.getPilot().setLinearSpeed(aspeed);
	}
	
	/**
	 * Action which performs odometry correction by centering the robot on the axis it is currently facing.
	 * This method is recursive in order to ensure it will always attempt to correct itself as best it can.
	 * Gyro is also reset during this procedure. 
	 * @param c parameter used in recusion (number of correction moves)
	 * @param aspeed Angular Speed
	 * @param pspeed Linear Speed
	 * @return Logging Data
	 */
	private String blackLineCorrection(int c, double aspeed, double pspeed) {
		c++;

		robot.getPilot().setAngularSpeed(aspeed / 10);
		robot.getPilot().setLinearSpeed(pspeed / 8);

		robot.getPilot().forward();

		String leftColor  = null;
		String rightColor = null;

		do {
			leftColor  = getColorValue(ColorSampler.LEFT_SENSOR, 8);
			rightColor = getColorValue(ColorSampler.RIGHT_SENSOR, 8);

			robot.getMonitor().setLog(leftColor + " | " + rightColor);
		} while (!leftColor.equals("black") && !rightColor.equals("black"));

		robot.getPilot().stop();
		robot.getMonitor().print("STOPPED!");

		// double check
		leftColor  = getColorValue(ColorSampler.LEFT_SENSOR, 8);
		rightColor = getColorValue(ColorSampler.RIGHT_SENSOR, 8);
		
		if ( !leftColor.equals("black") && !rightColor.equals("black") )
			blackLineCorrection(c, aspeed, pspeed);
		
		if (leftColor.equals("black") && rightColor.equals("black")) {
			robot.getMonitor().print("Resiting gyro!");
			robot.resetGyro();
			robot.getPilot().setLinearSpeed(pspeed / 2);

			final int mov_dis = -8;
			robot.getPilot().travel(mov_dis);

			while (robot.getPilot().isMoving())
				Thread.yield();
		} else if (leftColor.equals("black")) {
			blackLineRotation("left", aspeed);
			blackLineCorrection(c, aspeed, pspeed);
		} else if (rightColor.equals("black")) {
			blackLineRotation("right", aspeed);
			blackLineCorrection(c, aspeed, pspeed);
		} else {
			robot.getMonitor().setLog("Oh no josh, this isn't good! (probably wont happen)");
			Sound.buzz();
			return "Oh no!";
		}

		robot.getPilot().setLinearSpeed(pspeed);
		robot.getPilot().setAngularSpeed(aspeed);
		
		robot.getMonitor().clearLcd();
		robot.getPilot().stop();
		
		return "#CORRECTIONS: " + c;
	}
	
	/**
	 * Procedure to center the robot on a cell. Calls blackLineCorrection()
	 * @param rotation_angle Angle that determines the next centering axis.
	 * @return Completed flag
	 */
	private String cellCentering(float rotation_angle) {
		blackLineCorrection(0, robot.getPilot().getAngularSpeed(), robot.getPilot().getLinearSpeed());
		rotate(rotation_angle);
		blackLineCorrection(0, robot.getPilot().getAngularSpeed(), robot.getPilot().getLinearSpeed());
		rotate(-rotation_angle);
		return "Completed!";
	}
	
	/**
	 * Plays a siren from the Ev3 brick file base.
	 * @return Log data.
	 */
	private String playSiren() {
		File neenaw = new File("neenaw.wav");
		Sound.playSample(neenaw);
		return "Played sample from: " + neenaw.getAbsolutePath();
	}

	// String (cmd) grammar: cmd%arg1,arg2,arg3,...,argN
	/**
	 * Parser.
	 * Parses a string of text and executes the command that it corresponds to.
	 * Parameters may be present.
	 * @param cmd
	 * @return
	 */
	private String parse(String cmd) {
		String returnValue  = "DEFAULT";
		int separator_index = cmd.indexOf("%");

		String prefix    = null;
		String arguments = null;
		String args[] 	 = {};

		if (separator_index == -1) {
			prefix    = cmd;
			arguments = "";
		} else {
			prefix    = cmd.substring(0, separator_index);
			arguments = cmd.substring(separator_index + 1, cmd.length());
			args      = arguments.split(",");
		}

		prefix = prefix.toUpperCase();
		monitor.print("CMD: " + cmd);

		if ("MOVE".equals(prefix)) {
			move(Double.parseDouble(args[0]));
		} else if ("ROTATE".equals(prefix)) {
			returnValue = rotate(Float.parseFloat(args[0]));
		} else if ("BEEP".equals(prefix)) {
			Sound.beep();
		} else if ("PRINT".equals(prefix)) {
			robot.getMonitor().print(arguments);
		} else if ("RCOLOR".equals(prefix)) {
			returnValue = getColorValue(ColorSampler.RIGHT_SENSOR, 1000);
		} else if ("LCOLOR".equals(prefix)) {
			returnValue = getColorValue(ColorSampler.LEFT_SENSOR, 1000);
		} else if ("DISTANCE".equals(prefix)) {
			String dis = Float.toString(robot.getDistance());
			robot.getMonitor().print(dis);
			returnValue = dis;
		} else if ("BLCORRECTION".equals(prefix)) {
			returnValue = blackLineCorrection(0, robot.getPilot().getAngularSpeed(), robot.getPilot().getLinearSpeed());
		} else if ("CENTERING".equals(prefix)) {
			returnValue = cellCentering(Float.parseFloat(args[0]));
		} else if ("DBGCS".equals(prefix)) {
			returnValue = robot.getColorSampler().debug();
		} else if ("PLAYSIREN".equals(prefix)) {
			returnValue = playSiren();
		} else if ("LAPCOMPLETED".equals(prefix)) {
			Sound.beepSequence();
			robot.getMonitor().setText("LAP COMPLETED!");
			returnValue = "yay!";
		} else {
			robot.getMonitor().print("Error! Invalid command!");
			returnValue = "ERROR";
		}
		
		robot.newInstruction(cmd);
		
		return returnValue;
	}
	
	/**
	 * Starts the Socket Server and awaits for client connection.
	 */
	public void start() {
		monitor.setText("Waiting for Client...");

		try {
			this.serverSocket = new ServerSocket(PORT);
			this.client 	  = serverSocket.accept();
			this.os  = client.getOutputStream();
			this.is  = client.getInputStream();
			this.oos = new ObjectOutputStream(os);
			this.ois = new ObjectInputStream(is);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Sound.twoBeeps();
		monitor.print("Connected!");
		monitor.setText("GROUP 16 - RESCUE");

		while (true) {
			String returnValue = null;
			Object o = null;

			try {
				o = ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (o instanceof String) {
				String txt = (String) o;

				if (txt.equals("%EXIT%"))
					break;

				returnValue = parse((String) o);
			} else if (o instanceof ArrayList) {
				@SuppressWarnings("unchecked")
				ArrayList<String> list = (ArrayList<String>) o;
				String[] rvalues 	   = new String[list.size()];

				int i = 0;
				for (String cmd : list) {
					rvalues[i] = parse(cmd);
					i++;
				}

				returnValue = Arrays.toString(rvalues);
			} else {
				monitor.print("Invalid net object!");
			}

			try {
				oos.writeObject(returnValue);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Sound.twoBeeps();
		monitor.print("Disconnected from network.");
	}
}
