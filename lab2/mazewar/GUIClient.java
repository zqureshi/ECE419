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

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * An implementation of {@link LocalClient} that is controlled by the keyboard
 * of the computer on which the game is being run.  
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: GUIClient.java 343 2004-01-24 03:43:45Z geoffw $
 */

public class GUIClient extends LocalClient implements KeyListener {

    private ObjectOutputStream out;

    /**
     * Create a GUI controlled {@link LocalClient}.
     */
    public GUIClient(String name, ObjectOutputStream out) {
        super(name);
        this.out = out;
    }

    /**
     * Handle a key press.
     * @param e The {@link KeyEvent} that occurred.
     */
    public void keyPressed(KeyEvent e) {
        MazewarPacket packetToServer = new MazewarPacket();
        packetToServer.ClientName = this.getName();
        packetToServer.type = MazewarPacket.MAZE_REQUEST;

        // If the user pressed Q, invoke the cleanup code and quit.
        if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
            // TODO: Send BYE packet to the server
            Mazewar.quit();
            return;
        } else if(e.getKeyCode() == KeyEvent.VK_UP) {
            packetToServer.Event = "F";
        } else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
            packetToServer.Event = "B";
        } else if(e.getKeyCode() == KeyEvent.VK_LEFT) {
            packetToServer.Event = "L";
        } else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
            packetToServer.Event = "R";
        } else if(e.getKeyCode() == KeyEvent.VK_SPACE) {
            packetToServer.Event = "S";
        }

        try {
            out.writeObject(packetToServer);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Handle a key release. Not needed by {@link GUIClient}.
     * @param e The {@link KeyEvent} that occurred.
     */
    public void keyReleased(KeyEvent e) {
    }

    /**
     * Handle a key being typed. Not needed by {@link GUIClient}.
     * @param e The {@link KeyEvent} that occurred.
     */
    public void keyTyped(KeyEvent e) {
    }


}
