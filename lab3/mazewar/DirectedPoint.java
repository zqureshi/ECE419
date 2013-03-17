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
 * An integral representation of a point in two dimensional space,
 * with direction.  Sort of like a vector, but zero magnitude.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: DirectedPoint.java 339 2004-01-23 20:06:22Z geoffw $
 */

public class DirectedPoint extends Point {

        /**
         * The {@link Direction}
         */ 
        private final Direction direction;
        
        /**
         * Create a {@link DirectedPoint} from coordinates and a {@link Direction}.
         * @param x X-coordinate of this {@link DirectedPoint}.
         * @param y Y-coordinate of this {@link DirectedPoint}.
         * @param direction The {@link Direction} of this {@link DirectedPoint}.
         */
        public DirectedPoint(int x, int y, Direction direction) {
                super(x, y);
                assert(direction != null);
                this.direction = direction;
        }
       
        /**
         * Create a {@link DirectedPoint} from a {@link Point} and a {@link Direction}.
         * @param point Location of this {@link DirectedPoint}.
         * @param direction The {@link Direction} of this {@link DirectedPoint}.
         */
        public DirectedPoint(Point point, Direction direction) {
                super(point);
                assert(point != null);
                assert(direction != null);
                this.direction = direction;
        }
        
        /** 
         * Create a {@link DirectedPoint} from another {@link DirectedPoint}.
         * @param dp The {@link DirectedPoint} to copy from.
         */
        public DirectedPoint(DirectedPoint dp) {
                super(dp);
                assert(dp != null);
                this.direction = dp.direction;
        }

        /**
         * Obtain the {@link Direction} of this {@link DirectedPoint}.
         * @return This {@link DirectedPoint}'s Cardinal {@link Direction}.
         */
        public Direction getDirection(){
                return direction;
        }
}
