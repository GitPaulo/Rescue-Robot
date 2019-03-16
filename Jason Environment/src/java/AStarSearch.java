import java.util.LinkedList;
import java.util.List;

/**
 * A class that is used to calculate a
 * path between two AStarNodes by using
 * A* search algorithm.
 * @author group 16
 */
class AStarSearch {
  public static class PriorityList extends LinkedList {
      public void add( Comparable object ) {
          for (int i=0; i<size(); i++) {
              if (object.compareTo(get(i)) <= 0) {
                  add(i, object);
                  return;
              }
          }
          addLast(object);
      }
  }

  protected List constructPath( AStarNode node ) {
      LinkedList path = new LinkedList();

      while (node.pathParent != null) {
          path.addFirst(node);
          node = node.pathParent;
      }

      return path;
  }
  
  public List findPath( AStarNode startNode, AStarNode goalNode ) {
      PriorityList openList = new PriorityList();
      LinkedList closedList = new LinkedList();

      startNode.costFromStart       = 0;
      startNode.estimatedCostToGoal = startNode.getEstimatedCost(goalNode);
      startNode.pathParent          = null;
      
      openList.add(startNode);

      while (!openList.isEmpty()) {
          PathResolver.Node node1 = (PathResolver.Node)startNode;
          PathResolver.Node node2 = (PathResolver.Node)goalNode;
          
          AStarNode node = (AStarNode)openList.removeFirst();
          
          // construct the path from start to goal
          if (node == goalNode)
              return constructPath(goalNode);

          List neighbours = node.getNeighbours();

          for (int i=0; i<neighbours.size(); i++) {
              AStarNode neighbourNode = (AStarNode)neighbours.get(i);
              boolean isOpen         = openList.contains(neighbourNode);
              boolean isClosed       = closedList.contains(neighbourNode);
              
              float costFromStart    = node.costFromStart + node.getCost(neighbourNode);

              // check if the neighbour node has not been
              // traversed or if a shorter path to this
              // neighbour node is found.

              if ((!isOpen && !isClosed) || costFromStart < neighbourNode.costFromStart){

                  neighbourNode.pathParent          = node;
                  neighbourNode.costFromStart       = costFromStart;
                  neighbourNode.estimatedCostToGoal = neighbourNode.getEstimatedCost(goalNode);
                  
                  if (isClosed) 
                      closedList.remove(neighbourNode);
                  
                  if (!isOpen)
                      openList.add(neighbourNode);
              }
          }

          closedList.add(node);
      }

      // no path found
      return null;
  }
}