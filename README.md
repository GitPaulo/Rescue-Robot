# Rescue-Robot
An instruction robot built with the lejos ev3 API networked and communicating with agents via the JSON environment.
The task is for the robot car to navigate itself around the arena in order to find and retrieve victims to the hospital.
Victims will be prioritized by status. Their locations are unknown (only possible locations known to the paramedic agent). 

## Program Structure
The AgentSpeak agent should minimally be responsible for:

    Communication with the doctor agent, to accept the CNP contract, obtain beliefs, and to check critical status of a victim at a given position.
    Determining the next intention of the agent.  This is typically to go to some location based on.
      -> If a critical victim is found (go to hospital).
      -> If a non-critical victim is found (go to hospital if no more non-critical victims are left).
      -> If a non-critical victim should be rescued (if all victim locations are found, and there are no critical victims rescued, then go to nearest victim).
      -> If all victims have been rescued, go home.
    Informing the status of a discovered victim.

The JASON Environment should minimally be responsible for:

    Communicating with the EV3 brick.
    Displaying the map representing the current status of the mission.
    Determining the next closest victim location to be explored, or closest victim to be rescued.
    Pathfinding.

The EV3 Reactive agent should minimally be responsible for:

    Movement and localisation.
    Avoiding obstacles.
    Reporting the location and colour of a victim.

## Environment Information
- Each square arena is 6x6 cells in size, where each cell is 25x25cm.
- The arena has three coloured squares (cyan/teal or burgundy). These squares denote the existence of a victim, and its critical status.
- In one of the corners of the arena (denoted by a yellow square and an Infra-Red beacon), the hospital is located.
- The arena will have obstacles placed at random grid cells. (Depending on the run).

## Robot Car Configuration
- A 4 wheeled lego car.
- A gyro scope is positioned in the center below the car chasis.
- A range sensor is positioned at the front of the robot that can rotate between -90 and +90 degrees. Tested with both: 
  - Infra Red sensor, can detect the beacon at the hospital, but its range is poor (typically no more than approximately 50cm);
  - Ultrasound, cannot detect the beacon at the hospital but has a good range (typically greater than 1m).
- Two light sensors positioned in the front of the car pointing downwards. These were used to scan the arena's checkpoints and grid black lines. 

## How to use this? 
Recommended way:
- Build a robot with the configuration described above.
- Build an arena with the configuration described above.
- Install Eclipse with lejos ev3 packages installed.
- Download and install Jason & JEdit -> jason.sourceforge.net/mini-tutorial/-started/
- Open JEdit and import the files from [Jason Environment/](Jason Environment/)
- Run [Ev3 Robot/Core.java](Ev3 Robot/Core.java)
- Wait for the files to be uploaded to the robot.
- Robot should enter sampling mode - sample each of the colors (as instructed by the LCD) by pressing the center button on the Ev3 Brick.
- Place the robot at the hospital square.
- Press start and let the robot go!
