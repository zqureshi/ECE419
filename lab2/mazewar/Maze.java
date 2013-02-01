/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/
  
import java.util.Iterator;

/**
 * An abstract class for representing mazes, and the operations a {@link Client}
 * in the {@link Maze} may wish to perform..
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Maze.java 343 2004-01-24 03:43:45Z geoffw $
 */

public abstract class Maze {

    /* Maze Information ****************************************************/

    /** 
     * Obtain a {@link Point} describing the size of the {@link Maze}.
     * @return A {@link Point} where the method <code>getX</code> returns the maximum X 
     * coordintate, and the method <code>getY</code> returns the maximum Y coordinate. 
     */
    public abstract Point getSize(); 

    /**
     * Check whether a {@link Point} is within the bounds of the {@link Maze}. 
     * @return <code>true</code> if the point lies within the {@link Maze}, <code>false</code> otherwise.
     */
    public abstract boolean checkBounds(Point point); 

    /** 
     * Obtain the {@link Cell} corresponding to a given {@link Point} in the {@link Maze}.
     * @param point Location in the {@link Maze}.
     * @return A {@link Cell} describing that location.
     */
    public abstract Cell getCell(Point point);

    /* Client functionality ************************************************/
    
    /** 
     * Add a {@link Client} at random location in the {@link Maze}. 
     * @param client {@link Client} to be added to the {@link Maze}.
     */
    public abstract void addClient(Client client);

    /** 
     * Create a new {@link Projectile} from the specified {@link Client}
     * @param client {@link Client} that is firing.
     * @return <code>false</code> on failure, <code>true</code> on success. */
    public abstract boolean clientFire(Client client);
    
    /** 
     * Remove the specified {@link Client} from the {@link Maze} 
     * @param client {@link Client} to be removed.
     */
    public abstract void removeClient(Client client);

    /** 
     * Find out where a specified {@link Client} is located 
     * in the {@link Maze}.
     * @param client The {@link Client} being located.
     * @return A {@link Point} describing the location of the client. */
    public abstract Point getClientPoint(Client client);
    
    /** 
     * Find out the cardinal direction a {@link Client} is facing.
     * @param client The {@link Client} being queried.
     * @return The orientation of the specific {@link Client} as a {@link Direction}.
     */
    public abstract Direction getClientOrientation(Client client);

    /** 
     * Attempt to move a {@link Client} in the {@link Maze} forward.
     * @param client {@link Client} to move.
     * @return <code>true</code> if successful, <code>false</code> if failure. 
     */
    public abstract boolean moveClientForward(Client client);
    
    /** Attempt to move a {@link Client} in the {@link Maze} backward.
     * @param client {@link Client} to move.
     * @return <code>true</code> if successful, false if failure. 
     */
    public abstract boolean moveClientBackward(Client client);

    /**
     * Obtain an {@link Iterator} over all {@link Client}s in the {@link Maze} 
     * @return {@link Iterator} over clients in the {@link Maze}. 
     */
    public abstract Iterator getClients();
    
    /* Maze Listeners ******************************************************/

    /** 
     * Register an object that wishes to be notified when the maze changes 
     * @param ml An object implementing the {@link MazeListener} interface.
     * */
    public abstract void addMazeListener(MazeListener ml);

    /** Remove an object from the notification queue
     * @param ml An object implementing the {@link MazeListener} interface.
     */
    public abstract void removeMazeListener(MazeListener ml);
    
}
