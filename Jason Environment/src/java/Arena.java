import java.util.List;
import java.util.ArrayList;

/**
 * Arena data structure. Will be used to hold the state of the arena as
 * the robot goes around completing the run.
 * @author group 16
 */
public class Arena {
    // Size of the arena
    public final int WIDTH;
    public final int HEIGHT;
    
    // Directions Used for maping angles.
    public final float FORWARD  = 0f;
    public final float BACKWARD = 180f;
    public final float RIGHT 	= 90f;
    public final float LEFT 	= 270f;
    
    /**
     * Subclass to represent an arena cell.
     * @author group 16
     */
    public class Cell {
        public final int x;
        public final int y;

        private boolean visited; // If a cell has been visited or not by the robot.
        private boolean blocked;
        private boolean hasVictim;
        private boolean isHospital;

        public Cell(int x, int y, boolean blocked, boolean hasVictim, boolean isHospital) {
            this.x = x;
            this.y = y;
            this.blocked = blocked;
            this.hasVictim = hasVictim;
            this.visited = false;
            this.setHospital(false);
        }

        public void setVisited(boolean b) {
            this.visited = b;
        }

        public boolean isVisited() {
            return this.visited;
        }

        public boolean isNextToWall() {
            return this.x == 0 || this.y == 0 || this.x == WIDTH - 1 || this.y == HEIGHT - 1;
        }

        public void setBlocked(boolean blocked) {
            this.blocked = blocked;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public void setHasVictim(boolean hasVictim) {
            this.hasVictim = hasVictim;
        }

        public boolean hasVictim() {
            return hasVictim;
        }

        public boolean isHospital() {
            return isHospital;
        }

        public void setHospital(boolean isHospital) {
            this.isHospital = isHospital;
        }
        
        public void print() {
            System.out.println("[" + this.x + "][" + this.y + "] = " + "{ B=" + this.blocked + ", P=" + this.hasVictim
                    + ", H=" + this.isHospital + ", V=" + this.isVisited() + ", W=" + this.isNextToWall() + " }");
        }
    }
    
    // Arena data 2D Array
    private Cell[][] arena;

    public Arena(int w, int h) {
        this.WIDTH = w;
        this.HEIGHT = h;

        arena = new Cell[WIDTH][HEIGHT];

        for (int row = 0; row < WIDTH; row++) {
            for (int col = 0; col < HEIGHT; col++) {
                arena[row][col] = new Cell(row, col, false, false, false);
            }
        }
    }
    
    /**
     * Returns a Arena.Cell object. If it doesn't exists, it will return null.
     * @param x
     * @param y
     * @return Object
     */
    public Cell getCell(int x, int y) {
        if (x >= WIDTH || y >= HEIGHT || x < 0 || y < 0)
            return null;
        return arena[x][y];
    }
    
    /**
     * Gets how many times the robot has visited the cells.
     * @return num_visits
     */
    public int getNumVisits() {
        int total = 0;
        for (int row = 0; row < this.WIDTH; row++)
            for (int col = 0; col < this.HEIGHT; col++)
                if (arena[row][col].isVisited())
                    total += 1;
        return total;
    }
    
    /**
     * Returns an array list of Arena.Cell objects. 
     * These represent all the POSSIBLE victim cells on the current map instance.
     * @return Possible Vicitm Cells List
     */
    public List calculatePossibleVictimCells() {
        List<Arena.Cell> rl = new ArrayList<Arena.Cell>();

        for (int row = 0; row < WIDTH; row++) {
            for (int col = 0; col < HEIGHT; col++) {
                Arena.Cell c = arena[row][col];
                if (c.hasVictim())
                    rl.add(c);
            }
        }

        return rl;
    }
    
    /**
     * Resolves the angle the robot should turn to face a cell.
     * Assumes robot is turned towards the starting heading. (thus requires heading offset when used!)
     * @param currentCell
     * @param nextCell
     * @return Rotation Angle
     */
    public float resolveMappedAngle(Arena.Cell currentCell, Arena.Cell nextCell) {
        double dy = nextCell.y - currentCell.y;
        double dx = nextCell.x - currentCell.x;

        if (dy > 0 && dy != 0)
            return FORWARD;

        if (dx > 0 && dx != 0)
            return RIGHT;

        if (dy < 0 && dy != 0)
            return BACKWARD;

        if (dx < 0 && dx != 0)
            return LEFT;

        return -1;
    }
    
    /**
     * Returns each neighbour by using the heading of the robot.
     * @param currentCell
     * @param headingAngle
     * @param direction
     * @return The neighbour cell of which the heading refers to.
     */
    public Arena.Cell getNeighbourByHeading(Arena.Cell currentCell, float headingAngle, float direction) {
        float actual_direction = (headingAngle + direction) % 360;

        if (actual_direction == FORWARD)
            return getCell(currentCell.x, currentCell.y + 1);

        if (actual_direction == BACKWARD)
            return getCell(currentCell.x, currentCell.y - 1);

        if (actual_direction == LEFT)
            return getCell(currentCell.x - 1, currentCell.y);

        if (actual_direction == RIGHT)
            return getCell(currentCell.x + 1, currentCell.y);

        return null;
    }
    
    /**
     * Print method.
     */
    public void print() {
        for (int row = 0; row < WIDTH; row++) {
            for (int col = 0; col < HEIGHT; col++) {
                getCell(row, col).print();
            }
        }
    }
}
