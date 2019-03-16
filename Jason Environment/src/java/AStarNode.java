import java.util.List;

/**
 * This is an abstract class to be used 
 * to implement an Astar-Node.
 * It is abstract to be easier to modify to our specific
 * situation with the robot pathfinding.
 * @author group 16
 */
abstract class AStarNode implements Comparable {
    AStarNode pathParent;
    float costFromStart;
    float estimatedCostToGoal;
  
    public float getCost() {
        return costFromStart + estimatedCostToGoal;
    }
  
    public int compareTo( Object other ) {
        float thisValue = this.getCost();
        float otherValue = ((AStarNode)other).getCost();
  
        float v = thisValue - otherValue;
        return (v>0) ? 1 : (v<0) ? - 1:0; // sign function
    }
  
    public abstract float getCost( AStarNode node );
    public abstract float getEstimatedCost( AStarNode node );
    public abstract List getNeighbours();
}  