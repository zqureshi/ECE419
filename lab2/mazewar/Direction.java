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
  
import java.util.Random;

/**
 * A representation of the for Cardinal Directions with associated utility methods.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Direction.java 339 2004-01-23 20:06:22Z geoffw $
 */

public class Direction {
        
        /* Internals ******************************************************/
        
        /**
         * Create a random number generator to produce random directions.
         */
        private static Random randomGen = new Random();
        
        /** 
         * Internal representation of directions
         */
        private static final int NORTH  = 0;
        private static final int EAST  = 1;
        private static final int SOUTH = 2;
        private static final int WEST  = 3;
        
        /**
         * The internal representation
         */
        private final int direction;
   
        /** 
         * Create a new direction from an internal representation
         */
        private Direction(int direction) {
                assert((direction >= 0) && (direction < 4));
                this.direction = direction;
        }
        
        /**
         * The Northward Cardinal {@link Direction}.
         */
        public static final Direction North = new Direction(NORTH);
        
        /**
         * The Eastward Cardinal {@link Direction}.
         */
        public static final Direction East = new Direction(EAST);
       
        /**
         * The Southward Cardinal {@link Direction}.
         */
        public static final Direction South = new Direction(SOUTH);
        
        /**
         * The Westward Cardinal {@link Direction}.
         */
        public static final Direction West = new Direction(WEST);
        
        /**
         * Compare {@link Direction}s for equality.
         */
        public boolean equals(Object o) {
                if(o instanceof Direction) {
                    Direction d = (Direction)o;
                    return (this.direction == d.direction);
                } else {
                        return false;
                }
        }
        
        /**
         * Create a {@link Direction} randomly.
         * @return A random Cardinal {@link Direction}.
         */
        public static Direction random() {
                switch(randomGen.nextInt(4)) {
                        case NORTH:
                                return South;
                        case EAST:
                                return West;
                        case SOUTH:
                                return North;
                        case WEST:
                                return East;
                }
                /* Impossible */
                return null;
        }
        
        /** 
         * Create a new {@link Direction} by rotating this one 
         * ninety degrees counter-clockwise.
         * @return A {@link Direction} resulting from turning left.
         */
        public Direction turnLeft() {
                switch(this.direction) {
                        case NORTH:
                                return West;
                        case EAST:
                                return North;
                        case SOUTH:
                                return East;
                        case WEST:
                                return South;
                }
                /* Impossible */
                return null;
        }
        
        /**
         * Create a new {@link Direction} by rotating this one
         * ninety degrees clockwise.
         * @return A {@link Direction} resulting from turning right.
         */
        public Direction turnRight() {
                switch(this.direction) {
                        case NORTH:
                                return East;
                        case EAST:
                                return South;
                        case SOUTH:
                                return West;
                        case WEST:
                                return North;
                }
                /* Impossible */
                return null;
        }
       
        /** Create a new direction by rotating this one
         * one hundred eighty degrees.
         * @return A {@link Direction} that has been flipped.
         */
        public Direction invert() {
                switch(this.direction) {
                        case NORTH:
                                return South;
                        case EAST:
                                return West;
                        case SOUTH:
                                return North;
                        case WEST:
                                return East;
                }
                /* Impossible */
                return null;
        }
        
        /** 
         * Produce a {@link String} representation of a direction.
         * @return This {@link Direction}'s {@link String} representation.
         */
        public String toString() {
                switch(this.direction) {
                        case NORTH:
                                return "North";
                        case EAST:
                                return "East";
                        case SOUTH:
                                return "South";
                        case WEST:
                                return "West";
                }
                /* Impossible */
                return null;
        }
}
