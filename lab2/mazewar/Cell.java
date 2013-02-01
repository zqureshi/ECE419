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
 
/**
 * An abstract class for individual cells of the maze. 
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Cell.java 334 2004-01-23 16:08:35Z geoffw $
 */

public abstract class Cell {

    /** 
     * Does this cell have a wall in the specified direction? 
     * @param direction Cardinal direction to check.
     * @return <code>true</code> if there is a wall, <code>false</code> otherwise.
     */
    public abstract boolean isWall(Direction direction);

    /** 
     * Obtain the contents of the cell.  
     * @return <code>null</code> if the cell is empty, otherwise some object
     */
    public abstract Object getContents();
    
}
