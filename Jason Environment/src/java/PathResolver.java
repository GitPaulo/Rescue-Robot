import java.util.ArrayList;
import java.util.List;

/**
 * Class which resolves pathfinding.
 * This class has been tailored to adapt the A* algorithm to work 
 * in order to find the shortest paths through the Arena data structure.
 * @author group 16
 */
public class PathResolver {
    /**
     * Class which extends the abstract AStarNode to a node that we can use.
     * @author group 16
     */
    public class Node extends AStarNode {
        private Arena.Cell cell;
        private ArrayList<AStarNode> neighbours;

        public Node(Arena.Cell cell) {
            this.cell = cell;
            this.neighbours = new ArrayList<AStarNode>();
        }

        public float getCost(AStarNode node) {
            return 1;
        }

        public float getEstimatedCost(AStarNode node) {
            int x1 = this.cell.x;
            int y1 = this.cell.y;
            int x2 = ((PathResolver.Node) node).getCell().x;
            int y2 = ((PathResolver.Node) node).getCell().y;

            return Math.abs(y2 - y1) + Math.abs(x2 - x1);
        }

        public void addNeighbour(AStarNode node) {
            this.neighbours.add(node);
        }

        public List getNeighbours() {
            return this.neighbours;
        }

        public Arena.Cell getCell() {
            return this.cell;
        }
    }

    private Arena grid;
    private PathResolver.Node[] map;
    
    /**
     * Generates a graph out of the arena (grid) instance.
     * This is needed in order to apply the A* search algorithm.
     * @param grid
     */
    public PathResolver(Arena grid) {
        final int GRID_SIZE = grid.HEIGHT * grid.WIDTH;
        this.map = new Node[GRID_SIZE];
        this.grid = grid;

        // instantiate map nodes
        for (int i = 0; i < grid.WIDTH; i++) {
            for (int j = 0; j < grid.HEIGHT; j++) {
                this.map[(i * grid.HEIGHT) + j] = new PathResolver.Node(grid.getCell(i, j));
            }
        }

        // add neighbours to map nodes
        for (int i = 0; i < grid.WIDTH; i++) {
            for (int j = 0; j < grid.HEIGHT; j++) {
                for (int ni = Math.max(0, i - 1); ni <= Math.min(i + 1, grid.WIDTH - 1); ++ni) {
                    for (int nj = Math.max(0, j - 1); nj <= Math.min(j + 1, grid.HEIGHT - 1); ++nj) {
                        if (!(ni == i && nj == j) && !(Math.abs(ni - i) > 0 && Math.abs(nj - j) > 0)) { // don't process
                                                                                                        // itself or
                                                                                                        // consider
                                                                                                        // diagonals
                            PathResolver.Node neighbour = this.map[ni * grid.HEIGHT + nj];
                            if (!neighbour.getCell().isBlocked()) { // nodes only through cells we have scanned! (safety
                                                                    // first)
                                this.map[i * grid.HEIGHT + j].addNeighbour(neighbour); // In other words, only path find
                                                                                        // on current map!
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * This method is used to find the shortest path between two cells of the arena data structure.
     * @param startCell
     * @param goalCell
     * @return List of nodes for the path (or null if non existant)
     */
    public List calculatePath(Arena.Cell startCell, Arena.Cell goalCell) {
        AStarSearch search = new AStarSearch();

        int pos1 = startCell.x * grid.HEIGHT + startCell.y;
        int pos2 = goalCell.x * grid.HEIGHT + goalCell.y;

        return search.findPath(this.map[pos1], this.map[pos2]);
    }
    
    /**
     * Returns the graph
     * @return
     */
    public PathResolver.Node[] getMap() {
        return map;
    }
    
    /**
     * Debug method to print the nodes.
     * @param nodes
     */
    public static void printNodes(List nodes) {
        int i = 1;
        for (Object o : nodes) {
            Arena.Cell pathCell = ((PathResolver.Node) o).getCell();
            System.out.println("Cell #" + i + ": C(" + pathCell.x + ", " + pathCell.y + ")");
            i++;
        }
    }
}