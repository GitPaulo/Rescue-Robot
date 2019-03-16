// ========================================================================
// Paramedic Agent - Group 16 - COMP239 
// ========================================================================

price(_Service,X) :- .random(R) & X = (10*R)+100.

// the name of the agent who is the initiator in the CNP
plays(initiator,doctor).

// Initial belieifs
currentPosition(0,0).
movesTaken(0).

// Plans for the CNP
+plays(initiator,In)
   :  .my_name(Me)
   <- .send(In,tell,introduction(participant,Me)).

// answer to Call For Proposal
@c1 +cfp(CNPId,Task,C,NC)[source(A)]
   :  plays(initiator,A) & price(Task,Offer)
   <- +proposal(CNPId,Task,C,NC,Offer);		// remember my proposal
      .send(A,tell,propose(CNPId,Offer)).

// Handling an Accept message
@r1 +accept_proposal(CNPId)[source(A)]
		: proposal(CNPId,Task,C,NC,Offer)
		<- !getScenario(A);
		    +startRescueMission(A,C,NC).
 
// Handling a Reject message
@r2 +reject_proposal(CNPId)
		<- .print("I lost CNP ",CNPId, ".");
		// clear memory
		-proposal(CNPId,_,_).

+startRescueMission(D,C,NC) : location(hospital,X,Y) & 
							  location(victim,_,_) &
							  location(obstacle,_,_)
    <- .count(location(victim,_,_),Vcount);		// Determine the victims
       .count(location(obstacle,_,_),Ocount);	// Determine the obstacles
       .print("Start the Resuce mission for ",C," critical and ",NC, " non-critical victims; Hospital is at (",X,",",Y,"), and we have ", Vcount, " victims and ", Ocount," known obstacles");
	   ?currentPosition(X, Y);
	   startRun(X, Y, Vcount, C, NC, Ocount).
 
// This is our recursive plan that only executes if we have yet to receive the beliefs    
+startRescueMission(D,C,NC)
    <- .wait(2000);  				// wait for the beliefs to be obtained
       -+startRescueMission(D,C,NC).// replace the mental note
 
// belief added when the enviornment connects to the brick
+connected_to_brick 
	<- .print("Connected to Ev3 brick! Starting movement loop.");
		?currentPosition(X, Y);
	    moveToNextGoal(X, Y).

// everytime the robot picks a goal
+goal(X, Y, T) 
	: location(victim, X, Y) | location(hospital, X, Y)
	<- .print("Verified correct goal Goal(", X, ", " , Y, ") at time ", T, "ms");
		scan(X, Y).
	
+goal(X, Y, T) 
	<- .print("Oh no something went wrong :C!!! (goal not right)").

// everytime the robot finds a color
+color(Color, X, Y)
	<- .print("Found a color: ", Color, " at ", X, Y); 
		!scannedColor(Color).
	
// Plans linking to the ParamedicEnv - they provide us with information to build the arena datastructure
+location(victim,X,Y)[source(D)]
	: plays(initiator,D)
    <- .print("Victim could be at ",X,", ",Y); 
		addVictim(X,Y).

+location(obstacle,X,Y)[source(D)]
	: plays(initiator,D)
    <- .print("Obstacle is at ",X,", ",Y); 
		addObstacle(X,Y).
    
+location(hospital,X,Y)[source(D)]
	: plays(initiator,D)
    <- .print("Hospital is at ",X,", ",Y); 
		addHospital(X,Y).

// Plan for responding to the critical status of victims at certain locations in the environment. 
+critical(X,Y)
    <- .print("The victim at ", X, ",", Y, " is critical (source: Doctor)");
		victimStatus(X,Y,critical).
	   
+~critical(X,Y)
    <- .print("The victim at ", X, ",", Y, " is not critical (source: Doctor)");
		victimStatus(X,Y,notcritical).

// Determines when the robot needs to correct itself (every 3 movements should work fine)
+movesTaken(N) 
	: (N mod 2 == 0) & (N \== 1) & (N \== 0)
	<- correctionNeeded(N).

+movesTaken(N)
	<- .print("No correction needed!").

// In order to keep an updated state for logic to work
+updatePosition(X,Y)
	<-	?movesTaken(N);
		-movesTaken(N);
		+movesTaken(N+1);
		.print("Moves taken: ", N);
		!updateCurrentPosition(X,Y).

// State were the robot will seek to continue searching for victims
+continueVictimSearch(N,X,Y) 
	<- .print("Continuing victim search....");
		!updateCurrentPosition(X,Y);
		.print("Current position is ---------> ", X, Y);
		moveToNextGoal(X, Y).

// ========================================================================
// Plan Library for achievement goals 
// ========================================================================
// Request the beliefs of the locations.  D is the doctor agent
+!getScenario(D) <- .send(D,askAll,location(_,_,_)).
	
// Check the status of a victim
// D is the doctor agent, X, Y are cell coordinates, and
// C is a colour from the set {burgandy,cyan}
// If the doctor knows the colour, it will send a belief for the 
// location X,Y to say if the victim is critical or not
//
// For example:
//			!requestVictimStatus(A,3,1,burgandy);
//			!requestVictimStatus(A,3,2,cyan);

// Always ran on each movement
+!updateCurrentPosition(X,Y)
    <-	?currentPosition(X2, Y2);
	   -currentPosition(X2, Y2);
	   +currentPosition(X, Y);
	   .print("Updated current position from ", X2, ",", Y2, " to ", X, "," , Y).
	   
+!requestVictimStatus(D,X,Y,C)
    <-  .print("Asking the doctor for the type of cell");
		.send(D, tell, requestVictimStatus(X,Y,C)).

// robot scans a color that the doctor cares about
+!scannedColor(Color) 
	: Color == cyan | Color == burgandy
	<- .print("Robot has found a cell with a valid color for a vicitm: ", Color);
		?currentPosition(X, Y);
		!requestVictimStatus(doctor, X, Y, Color).

// robot finds that there is no vitim on the cell
+!scannedColor(Color) 
	: Color == white 
	<- .print("Robot has found an empty cell.");
		?currentPosition(X, Y);
		noVictim(X, Y).
		
+!scannedColor(Color) 
	<- .print("Problem, wrong scan!! Color was: ", Color).

// ========================================================================
// End 
// ========================================================================
    