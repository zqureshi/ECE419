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
 * An interface for objects wishing to subscribe to notifications about events occurring in a {@link Maze}.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: MazeListener.java 335 2004-01-23 16:37:37Z geoffw $
 */
public interface MazeListener {

        /**
         * General notification that the state of the maze 
         * has changed.
         */
        void mazeUpdate();

        /**
         * Notification that client <code>source</code> has killed client <code>target</code>.
         * @param source Client that fired the projectile.
         * @param target Client that was killed.
         */
        void clientKilled(Client source, Client target);
        
        /**
         * Notification that new client has been added to the maze.
         * @param client Client that was added.
         */
        void clientAdded(Client client);

        /**
         * Notification that a client has fired a projectile.
         * @param client Client that fired.
         */
        void clientFired(Client client);
        
        /**
         * Notification that a client has been removed, or exiting the maze.
         * @param client Client that left.
         */
        void clientRemoved(Client client);
        
}
