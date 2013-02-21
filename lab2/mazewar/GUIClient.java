package mazewar;

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

import com.google.common.eventbus.EventBus;
import mazewar.server.MazePacket;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import static mazewar.server.MazePacket.PacketAction;

/**
 * An implementation of {@link LocalClient} that is controlled by the keyboard
 * of the computer on which the game is being run.
 *
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: GUIClient.java 343 2004-01-24 03:43:45Z geoffw $
 */

public class GUIClient extends LocalClient implements KeyListener {

    /**
     * Create a GUI controlled {@link LocalClient}.
     */
    public GUIClient(String name) {
        super(name);
    }

    /**
     * Handle a key press.
     *
     * @param e The {@link KeyEvent} that occurred.
     */
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_Q:
                Mazewar.quit();
                break;

            case KeyEvent.VK_UP:
                eventBus.post(PacketAction.FORWARD);
                break;

            case KeyEvent.VK_DOWN:
                eventBus.post(PacketAction.BACKUP);
                break;

            case KeyEvent.VK_LEFT:
                eventBus.post(PacketAction.LEFT);
                break;

            case KeyEvent.VK_RIGHT:
                eventBus.post(PacketAction.RIGHT);
                break;

            case KeyEvent.VK_SPACE:
                eventBus.post(PacketAction.FIRE);
                break;
        }
    }

    /**
     * Handle a key release. Not needed by {@link GUIClient}.
     *
     * @param e The {@link KeyEvent} that occurred.
     */
    public void keyReleased(KeyEvent e) {
    }

    /**
     * Handle a key being typed. Not needed by {@link GUIClient}.
     *
     * @param e The {@link KeyEvent} that occurred.
     */
    public void keyTyped(KeyEvent e) {
    }

}
