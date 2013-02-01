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
  
import java.io.Serializable;

/**
 * An integral representation of a point in two dimensional space.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Point.java 339 2004-01-23 20:06:22Z geoffw $
 */

public class Point implements Serializable {
        private final int x;
        private final int y;

        /**
         * Create a new {@link Point} from coordinates.
         * @param x Coordinate in the X-dimension.
         * @param y Coordinate in the Y-dimension.
         */
        public Point(int x, int y) {
                this.x = x;
                this.y = y;
        }

        /**
         * Create a new {@link Point} from another {@link Point}.
         * @param point {@link Point} to copy from.
         */
        public Point(Point point) {
                assert(point != null);
                this.x = point.x;
                this.y = point.y;
        }

        /**
         * Obtain the X-coordinate of this {@link Point}.
         * @return The X-coordinate of this {@link Point}.
         */
        public int getX() {
                return x;
        }

        /**
         * Obtain the Y-coordinate of this {@link Point}.
         * @return The Y-coordinate of this {@link Point}.
         */
        public int getY() {
                return y;
        }
        
        /**
         * Create a new {@link Point} by moving from this
         * one by a single unit in a given {@Direction}. 
         * North corresponds to postive Y-movement.
         * East corresponds to postive X-movement.
         * South corresponds to negative Y-movement.
         * West corresponds to negative X-movement.
         * @return A new {@link Point} one unit away from this one in the given {@link Direction}.
         */
        Point move(Direction d) {
                assert(d != null);
                if(d.equals(Direction.North)) {	   
                        return new Point(x, y + 1);
                } else if(d.equals(Direction.East)) {
                        return new Point(x + 1, y);
                } else if(d.equals(Direction.South)) {
                        return new Point(x, y - 1);
                } else if(d.equals(Direction.West)) {
                        return new Point(x - 1, y);
                } 
                /* Impossible */
                return null;
        }
}
