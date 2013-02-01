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
 * Simple class for representing projectiles.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Projectile.java 350 2004-01-24 05:31:17Z geoffw $
 */

public class Projectile {

        /**
         * The {@link Client} that owns this {@link Projectile}.
         */
        private final Client owner;
        
        /**
         * Create a new {@link Projectile} owned by the specified
         * {@link Client}.
         * @param client The owner.
         */
        public Projectile(Client client) {
                assert(client != null);
                this.owner = client;
        }

        /**
         * Find out the owner of this {@link Projectile}.
         * @return The owner.
         */
        public Client getOwner() {
                return this.owner;
        }
}
