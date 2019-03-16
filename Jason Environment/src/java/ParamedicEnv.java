import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import jason.asSyntax.*;
import jason.environment.*;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import java.util.logging.*;
import java.io.IOException;

/**
 * Main class of the Jason Environment.
 * All communication with the agents and the Ev3 brick passes through here.
 * @author group 16
 *
 */
public class ParamedicEnv extends Environment {
	// Init Constants
	public static final long START_TIME = System.currentTimeMillis();
	public static final int GSize 	    = 6; // The bay is a 6x6 grid
	public static final int HOSPITAL    = 8; // hospital code in grid model
	public static final int VICTIM      = 16; // victim code in grid model
	
	// Agent Constants & ID
	private static final String PARAMEDIC = "paramedic";
	private static final String DOCTOR 	  = "doctor";
	private static final int PARAMEDIC_ID = 1;
	
	// Functor String Constants
	public static final String ADD_VICTIM    = "addVictim";
	public static final String ADD_OBSTACLE  = "addObstacle";
	public static final String ADD_HOSPITAL  = "addHospital";
	public static final String START_RUN 	 = "startRun";
	public static final String NEXT_GOAL	 = "moveToNextGoal";
	public static final String SCAN 	  	 = "scan";
	public static final String VICTIM_STATUS = "victimStatus";
	public static final String NO_VICTIM 	 = "noVictim";
	public static final String CR_NEEDED 	 = "correctionNeeded";

	// Other
	private Client client;
	private Logger logger = getLogger();
	private ArrayList<Arena.Cell> rememberedVictims = new ArrayList<Arena.Cell>();
	
	// Instance Objects
	private Arena arena;
	private GUIArena gui;
	
	// Data obtained from the paramedic
	private int POSSIBLE_VICTIM_COUNT;
	private int OBSTACLE_COUNT;
	private int CRITICAL_VICTIM_COUNT;
	private int NON_CRITICAL_VICTIM_COUNT;
	
	// State variables
	private Arena.Cell currentCell     = null;
	private float headingAngle         = 0;
	private int logic_iterations	   = 0;
	private int critical_victims_found = 0;
	private int victims_recovered 	   = 0;
	private boolean correction_needed  = false;

	/************************/
	// 	  Helper Methods	//
	/************************/
	
	public void log(String str) {
		long t = System.currentTimeMillis() - START_TIME;
		logger.info("[" + t + "] " + str);
	}

	private void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
		}
	}

	private void percerptDoctor(String data) {
		addPercept(DOCTOR, Literal.parseLiteral(data));
	}

	private void perceptParamedic(String data) {
		addPercept(PARAMEDIC, Literal.parseLiteral(data));
	}

	private void perceptAgents(String data) {
		addPercept(Literal.parseLiteral(data));
	}

	private void updatePosition(int x, int y) {
		perceptParamedic("updatePosition(" + x + ", " + y + ")");
		sleep(1000);
	}

	private void continueVictimSearch(int x, int y) {
		logic_iterations++;
		perceptParamedic("continueVictimSearch(" + logic_iterations + "," + x + "," + y + ")");
	}
	
	/************************/
	// 	  External Actions	//
	/************************/
	
	/** Called before the MAS execution with the args informed in .mas2j */
	
	@Override
	public void init(String[] args) {
		super.init(args);
		arena = new Arena(GSize, GSize);
	}
	
	/**
	 * Instantiates & Initialises the GUI for the assignment.
	 * @param startCell
	 */
	private void initGUI(Arena.Cell startCell) {
		gui = new GUIArena(arena);
		gui.setStatus("Initialising...");
		gui.setCurrentPosition(startCell, headingAngle);
	}
	
	/**
	 * Attempts to connect to the server socket on the Ev3 Brick.
	 * Updates Belief Base once connected.
	 */
	private void connectToBrick() {
		try {
			client = new Client("192.168.70.173", 1234, logger);
			client.connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		perceptAgents("connected_to_brick");
		log("Connected successfully to EV3 Brick!");
	}
	
	/**
	 * Starts the run. The loop concerning the main logic starts here.
	 * @param x
	 * @param y
	 */
	public void startRun(int x, int y) {
		Arena.Cell startCell = arena.getCell(x, y);
		currentCell = startCell;

		arena.print();
		initGUI(startCell);

		gui.setStatus("Connecting to EV3 brick...");
		connectToBrick(); // should be blocking
	}
	
	/**
	 * Ran once the robot has completed the Lap.
	 * Will launch the completed GUI.
	 */
	public void completedRun() {
		client.sendInstruction("lapcompleted%");
		GUILapCompleted guiLC = new GUILapCompleted("Elapsed time: " + (System.currentTimeMillis() - START_TIME) + "\n"
				+ "Logic Iterations: " + logic_iterations + "\n" + "Victims Recovered: " + victims_recovered
				+ " out of " + (CRITICAL_VICTIM_COUNT + NON_CRITICAL_VICTIM_COUNT) + "\n"
				+ "Critical Victims Recovered: " + critical_victims_found + " out of " + CRITICAL_VICTIM_COUNT + "\n"
				+ "Victims Remembered: " + rememberedVictims.size() + "\n");
		gui.setStatus("Lap Completed!");
	}
	
	/**
	 * Method concerning all the logic in moving form the current cell to the next cell.
	 * This encapsulates the mechanics of sending the instructions necessary to the Ev3 Brick.
	 * This method also will wait until the brick is completed and will update the belief base of the paramedic agent.
	 * @param nextCell
	 */
	private void moveToNextCell(Arena.Cell nextCell) {
		float mappedAngle = arena.resolveMappedAngle(currentCell, nextCell);
		float rotationAngle = mappedAngle - headingAngle;

		if (rotationAngle != 0)
			headingAngle += rotationAngle;

		log("RA: " + rotationAngle + " MA:" + mappedAngle + " HA:" + headingAngle);

		ArrayList<String> cmds = new ArrayList<String>();
		rotationAngle = Utility.shortestRotationAngle(rotationAngle);

		if (rotationAngle != 0)
			cmds.add("rotate%" + rotationAngle);

		if (correction_needed) {
			float correction_angle = -1;
			Arena.Cell rightCell = arena.getNeighbourByHeading(currentCell, headingAngle, arena.RIGHT);
			Arena.Cell leftCell = arena.getNeighbourByHeading(currentCell, headingAngle, arena.LEFT);
			Arena.Cell forwardCell = arena.getNeighbourByHeading(currentCell, headingAngle, arena.FORWARD);

			boolean cantRight = rightCell == null || rightCell.isBlocked();
			boolean cantLeft = leftCell == null || leftCell.isBlocked();
			boolean cantForward = forwardCell == null || forwardCell.isBlocked();

			log("[CORRECTION] cantRight: " + cantRight + " | cantLeft: " + cantLeft + " | cantForward: " + cantForward);

			if ((cantRight && cantLeft) || cantForward) {
				correction_angle = -1;
				log("Can't perform correction because there are blocks/wall present!");
			} else if (cantRight) {
				correction_angle = -90;
			} else if (cantLeft) {
				correction_angle = 90;
			}

			if (correction_angle != -1) {
				log("[CORRECTION] Correction can be performed. Queuing instruction.");
				cmds.add("centering%" + correction_angle);
				correction_needed = false;
			}
		}

		cmds.add("move%25");

		for (String cmd : cmds)
			System.out.println(">> " + cmd);

		String returnResult = client.sendInstructions(cmds);

		currentCell = nextCell;
		currentCell.setVisited(true);
		gui.setCurrentPosition(currentCell, headingAngle);
		updatePosition(nextCell.x, nextCell.y);
	}
	
	/**
	 * Method used to calculate the next goal.
	 * Here the pathfinding will take place.
	 * We use A* algorithm to find a path to all the interested goals.
	 * We then pick the shortest path of the calcualted paths and transverse through it.
	 * @param x
	 * @param y
	 */
	public void resolveNextGoal(int x, int y) {
		gui.setStatus("Calculating...");

		currentCell = arena.getCell(x, y);
		PathResolver vpr = new PathResolver(arena);

		if (victims_recovered == CRITICAL_VICTIM_COUNT + NON_CRITICAL_VICTIM_COUNT) {
			completedRun();
			return;
		}

		if (victims_recovered == CRITICAL_VICTIM_COUNT) {
			removePerceptsByUnif(PARAMEDIC, Literal.parseLiteral("goal"));
			for (Arena.Cell rememberedCell : rememberedVictims) {
				rememberedCell.setHasVictim(true);
			}
		}

		List<Arena.Cell> victimCells = arena.calculatePossibleVictimCells();

		List pickedPath = null;
		int bs = Integer.MAX_VALUE;
		for (Arena.Cell goalCell : victimCells) {
			List l = vpr.calculatePath(currentCell, goalCell);

			if (l.size() < bs) {
				pickedPath = l;
				bs = l.size();
			}
		}

		gui.setStatus("Pathfinding...");
		gui.setGoalCell(((PathResolver.Node) pickedPath.get(bs - 1)).getCell());
		PathResolver.printNodes(pickedPath);

		// Decide if we want to highlight the path or network cell by cell.
		// gui simulation
		for (Object o : pickedPath) {
			Arena.Cell pathCell = ((PathResolver.Node) o).getCell();
			log(currentCell.x + "," + currentCell.y + " | " + pathCell.x + "," + pathCell.y);
			moveToNextCell(pathCell);
		}

		long t = System.currentTimeMillis();

		gui.setStatus("Reached goal! goal(" + currentCell.x + ", " + currentCell.y + ")");
		gui.setGoalCell(null);

		perceptParamedic("goal(" + currentCell.x + ", " + currentCell.y + "," + t + ")");
		updatePosition(currentCell.x, currentCell.y); // might wanna change that m8 (think about this)
	}
	
	/**
	 * Method used to request the Ev3 for information about the color of the cell the robot is currently on.
	 * @return
	 */
	private String[] requestColorData() {
		ArrayList<String> cmds = new ArrayList<String>();
		cmds.add("rcolor%");
		cmds.add("lcolor%");

		String returnValue = client.sendInstructions(cmds).replaceAll("\\s", "");
		String[] colorArray = returnValue.substring(1, returnValue.length() - 1).split(","); // 0 right, 1 left

		return colorArray;
	}
	
	/**
	 * Method used to perform a scan on an goal cell.
	 * @param x
	 * @param y
	 */
	public void performScan(int x, int y) {
		Arena.Cell possibleVictimCell = arena.getCell(x, y);

		// if we have all critical victims, treat the remembered cells as critical
		// escape the request for doctor of status (move staight into movement logic)
		log(victims_recovered + " ************************************ " + CRITICAL_VICTIM_COUNT);
		if (victims_recovered >= CRITICAL_VICTIM_COUNT) {
			possibleVictimCell.setHasVictim(false);
			moveToHospital(x, y);
			return;
		}

		gui.setStatus("Scanning goal cell...");

		int attempts = 0;
		final int max_attempts = 5;

		do {
			String[] colorArray = requestColorData();
			if (colorArray[0].equals(colorArray[1])) {
				perceptParamedic("color(" + colorArray[0] + "," + x + "," + y + ")");
				break;
			} else {
				log("[SCAN ATTEMPT:" + attempts + "] Oh no! Colors don't match, scanning again! " + colorArray[0]
						+ " != " + colorArray[1]);
				attempts++;
			}
		} while (attempts < max_attempts);
	}
	
	/**
	 * Method used to move the Ev3 Brick back to the hospital.
	 * @param x
	 * @param y
	 */
	public void moveToHospital(int x, int y) {
		currentCell = arena.getCell(x, y);
		Arena.Cell hospitalCell = arena.getCell(0, 0);

		gui.setStatus("Calculating...");

		PathResolver vpr = new PathResolver(arena);
		List path = vpr.calculatePath(currentCell, hospitalCell);

		gui.setStatus("Moving to the hospital...");
		gui.setGoalCell(hospitalCell);

		client.sendInstruction("playSiren%");

		// gui simulation
		for (Object o : path) {
			Arena.Cell pathCell = ((PathResolver.Node) o).getCell();
			log(currentCell.x + "," + currentCell.y + " | " + pathCell.x + "," + pathCell.y);
			moveToNextCell(pathCell);
		}

		// check if correct cell (YELLOW RETURN VALUE!)
		gui.setStatus("Scanning hospital cell to determine if at right place...");
		int attempts = 0;
		final int max_attempts = 3;

		do {
			String[] colorArray = requestColorData();
			if (colorArray[0].equals(colorArray[1]) && colorArray[0] == "yellow") {
				log("Confirmed location at hospital via color sensors!");
				break;
			} else {
				log("[SCAN ATTEMPT:" + attempts + "] Oh no! Colors don't match, scanning again! " + colorArray[0]
						+ " != " + colorArray[1]);
				attempts++;
			}
		} while (attempts < max_attempts);

		if (attempts == max_attempts) {
			System.err.println("Error! We are not at the hospital.");
			// localise??
			return;
		}

		victims_recovered++;
		gui.setStatus("Reached goal! goal(" + currentCell.x + ", " + currentCell.y + ")");
		gui.setVictimsRecovered(Integer.toString(victims_recovered));

		log("Reached hospital at Vector(" + currentCell.x + ", " + currentCell.y + ")");
		gui.setGoalCell(null);

		continueVictimSearch(currentCell.x, currentCell.y);
	}
	
	/**
	 * Method use to parse the information obtained from the doctor/paramedic agents and to act on it.
	 * @param x
	 * @param y
	 * @param critical
	 */
	public void resolveVictimStatus(int x, int y, boolean critical) {
		Arena.Cell possibleVictimCell = arena.getCell(x, y);
		possibleVictimCell.setHasVictim(false);
		if (critical) {
			gui.setStatus("Critical victim resolved!");
			critical_victims_found++;
			sleep(1000);
			moveToHospital(x, y);
		} else {
			gui.setStatus("Non-Critical victim resolved!");
			sleep(1000);
			if (critical_victims_found == CRITICAL_VICTIM_COUNT) {
				moveToHospital(x, y);
			} else {
				rememberedVictims.add(possibleVictimCell);
				gui.setVictimsRemembered(Integer.toString(rememberedVictims.size()));
				continueVictimSearch(x, y);
			}
		}
	}
	
	/************************/
	// 	 Environment Method	//
	/************************/
	
	@Override
	public boolean executeAction(String agName, Structure action) {
		try {
			String functor = action.getFunctor();
			if (ADD_VICTIM.equals(functor)) {
				int x = (int) ((NumberTerm) action.getTerm(0)).solve();
				int y = (int) ((NumberTerm) action.getTerm(1)).solve();
				arena.getCell(x, y).setHasVictim(true);
				log("Adding victim to arena DS: Vector(" + x + "," + y + ")");
			} else if (ADD_OBSTACLE.equals(functor)) {
				int x = (int) ((NumberTerm) action.getTerm(0)).solve();
				int y = (int) ((NumberTerm) action.getTerm(1)).solve();
				arena.getCell(x, y).setBlocked(true);
				log("Adding obstacle to arena DS: Vector(" + x + "," + y + ")");
			} else if (ADD_HOSPITAL.equals(functor)) {
				int x = (int) ((NumberTerm) action.getTerm(0)).solve();
				int y = (int) ((NumberTerm) action.getTerm(1)).solve();
				arena.getCell(x, y).setHospital(true);
				log("Adding hospital to arena DS: Vector(" + x + "," + y + ")");
			} else if (START_RUN.equals(functor)) {
				int x = (int) ((NumberTerm) action.getTerm(0)).solve();
				int y = (int) ((NumberTerm) action.getTerm(1)).solve();
				POSSIBLE_VICTIM_COUNT = (int) ((NumberTerm) action.getTerm(2)).solve();
				CRITICAL_VICTIM_COUNT = (int) ((NumberTerm) action.getTerm(3)).solve();
				NON_CRITICAL_VICTIM_COUNT = (int) ((NumberTerm) action.getTerm(4)).solve();
				OBSTACLE_COUNT = (int) ((NumberTerm) action.getTerm(5)).solve();
				log("Starting run, we are at Vector(" + x + ", " + y + ")");
				startRun(x, y);
			} else if (NEXT_GOAL.equals(functor)) {
				int x = (int) ((NumberTerm) action.getTerm(0)).solve();
				int y = (int) ((NumberTerm) action.getTerm(1)).solve();
				log("Starting to calculate and move to next goal from Vector(" + x + ", " + y + ")");
				resolveNextGoal(x, y);
			} else if (SCAN.equals(functor)) {
				int x = (int) ((NumberTerm) action.getTerm(0)).solve();
				int y = (int) ((NumberTerm) action.getTerm(1)).solve();
				log("Starting to scan Vector(" + x + ", " + y + ")");
				performScan(x, y);
			} else if (VICTIM_STATUS.equals(functor)) {
				int x = (int) ((NumberTerm) action.getTerm(0)).solve();
				int y = (int) ((NumberTerm) action.getTerm(1)).solve();
				String s = action.getTerm(2).toString(); // double check this
				log("-----------------> " + s);
				boolean b = (s.equals("critical")) ? true : false;
				log("Resolving victim status for Vector(" + x + ", " + y + ") for " + s + "(" + b + ")");
				resolveVictimStatus(x, y, b);
			} else if (NO_VICTIM.equals(functor)) {
				int x = (int) ((NumberTerm) action.getTerm(0)).solve();
				int y = (int) ((NumberTerm) action.getTerm(1)).solve();
				log("Cell had no victim, continue search!");
				arena.getCell(x, y).setHasVictim(false);
				continueVictimSearch(x, y);
			} else if (CR_NEEDED.equals(functor)) {
				int n = (int) ((NumberTerm) action.getTerm(0)).solve();
				correction_needed = true;
			} else {
				log("FUNCTOR -----> " + functor);
				log("executing: " + action + ", but not implemented!");
				return true;
				// Note that technically we should return false here. But that could lead to the
				// following Jason error (for example):
				// [ParamedicEnv] executing: addObstacle(2,2), but not implemented!
				// [paramedic] Could not finish intention: intention 6:
				// +location(obstacle,2,2)[source(doctor)] <- ... addObstacle(X,Y) / {X=2, Y=2,
				// D=doctor}
				// This is due to the action failing, and there being no alternative.
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		informAgsEnvironmentChanged();
		return true;
	}

	/** Called before the end of MAS execution */
	@Override
	public void stop() {
		super.stop();
	}
}
