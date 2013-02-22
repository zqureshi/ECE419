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

import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import java.util.Iterator;
import java.lang.Integer;
import java.lang.Class;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
  
/**
 * An implementation of a {@link TableModel} that is designed to listen to
 * {@link Maze} events an update the score appropriately.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: ScoreTableModel.java 354 2004-01-26 02:15:32Z geoffw $
 */

public class ScoreTableModel implements TableModel, MazeListener {
     
        /**
         * {@link Client} gets eleven points for a kill.
         */
        private final int scoreAdjKill = 11;

        /**
         * {@link Client} loses one point per shot.
         */
        private final int scoreAdjFire = -1;

        /**
         * {@link Client} loses five points if killed.
         */
        private final int scoreAdjKilled = -5;
        
        /**
         * A wrapper class for pairing a {@link Client} with its
         * score.
         */
        private class ScoreWrapper implements Comparable {
                int score = 0;
                Client client = null;
                public ScoreWrapper(Client client) {
                        this.client = client;
                }
        
                public Client getClient() {
                        return client;
                }
                
                public int getScore() {
                        return score;
                }

                public void adjustScore(int mod) {
                        score = score + mod;
                }
                
                public int compareTo(Object o) {
                        assert(o instanceof ScoreWrapper);
                        ScoreWrapper s = (ScoreWrapper)o;
                        if(score < s.getScore()) {
                                return 1;
                        } else if(score == s.getScore()) {
                                return 0;
                        } else if(score > s.getScore()) {
                                return -1;
                        } else {
                                throw new Error();
                        }
                }
        }
        
        private Set listenerSet = new HashSet();
        private SortedSet scoreSet = new SortedMultiSet();
        private Map clientMap = new HashMap();
                
        public void addTableModelListener(TableModelListener l) {
                assert(l != null);
                listenerSet.add(l);
        }
        
        public Class getColumnClass(int columnIndex) {
                assert((columnIndex >= 0) && (columnIndex <= 2));
                if(columnIndex == 0) {
                        return String.class;
                } else if(columnIndex == 1) {
                        return Integer.class;
                } else if(columnIndex == 2) {
                        return String.class;
                } 
                // Not possible
                return null;
        }
        
        public int getColumnCount() {
                return 3;
        }
        
        public String getColumnName(int columnIndex) {
                assert((columnIndex >= 0) && (columnIndex <= 2));
                if(columnIndex == 0) {
                        return "Name";
                } else if(columnIndex == 1) {
                        return "Score";
                } else if(columnIndex == 2) {
                        return "Type";
                } 
                // Not possible
                return null;
        }
        
        public int getRowCount() {
                return scoreSet.size();
        }
        
        public Object getValueAt(int rowIndex, int columnIndex) {
                assert((columnIndex >= 0) && (columnIndex <= 2));
                Iterator i = scoreSet.iterator();
                int j = 0;
                while(i.hasNext()) {
                        if(j == rowIndex) {
                                Object o = i.next();
                                assert(o instanceof ScoreWrapper);
                                ScoreWrapper s = (ScoreWrapper) o;
                                Client c = s.getClient();
                                if(columnIndex == 0) {
                                        return c.getName();
                                } else if(columnIndex == 1) {
                                        return new Integer(s.getScore());
                                } else if(columnIndex == 2) {
                                        if(c instanceof GUIClient) {
                                                return "GUI";
                                        } else if(c instanceof RemoteClient) {
                                                return "Remote";
                                        } else if(c instanceof RobotClient) {
                                                return "Robot";
                                        } else {
                                                return "Unknown";
                                        }
                                }
                        } else {
                                i.next();
                                j++;
                        }
                }
                return null;
        }
        
        public boolean isCellEditable(int rowIndex, int columnIndex) {
                assert((columnIndex >= 0) && (columnIndex <= 2));
                return false;
        }

        
        public void removeTableModelListener(TableModelListener l) {
                assert(l != null);
                listenerSet.remove(l);
        }   
        
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                assert((columnIndex >= 0) && (columnIndex <= 2));
                /* Shouldn't be setting things this way */
                throw new Error();
        }

        public void mazeUpdate() {

        }

        public void clientAdded(Client client) {
                assert(client != null);
                ScoreWrapper s = new ScoreWrapper(client);  
                scoreSet.add(s);
                clientMap.put(client, s);
                notifyListeners();
        } 
        
        public void clientFired(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof ScoreWrapper);
                scoreSet.remove(o);
                ScoreWrapper s = (ScoreWrapper)o;
                s.adjustScore(scoreAdjFire);
                scoreSet.add(s);
                notifyListeners();
        }
        
        public void clientKilled(Client source, Client target) {
                assert(source != null);
                assert(target != null);
                Object o = clientMap.get(source);
                assert(o instanceof ScoreWrapper);
                scoreSet.remove(o);
                ScoreWrapper s = (ScoreWrapper)o;
                s.adjustScore(scoreAdjKill);
                scoreSet.add(s);
                o = clientMap.get(target);
                assert(o instanceof ScoreWrapper);
                scoreSet.remove(o);
                s = (ScoreWrapper)o;
                s.adjustScore(scoreAdjKilled);
                scoreSet.add(s);
                notifyListeners();
        } 

        public void clientRemoved(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof ScoreWrapper);
                scoreSet.remove(o);
                clientMap.remove(o);
                notifyListeners();
        }

        private void notifyListeners() {
                Iterator i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof TableModelListener);
                        TableModelListener tml = (TableModelListener)o;
                        tml.tableChanged(new TableModelEvent(this));
                } 
        }
}
