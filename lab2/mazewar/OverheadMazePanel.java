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
  
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Font;
import java.awt.font.GlyphVector;
import java.awt.font.FontRenderContext;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * A {@link JPanel} that has been extended so that is will display an overhead view
 * of a {@link Maze} from a specified {@link Client}'s viewpoint.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: OverheadMazePanel.java 351 2004-01-24 05:40:54Z geoffw $
 */

public class OverheadMazePanel extends JPanel implements MazeListener {
        /**
         * Our handle to the {@link Maze}.
         */
        private final Maze maze;

        /**
         * Our handle to the {@link Client}.:
         */
        private final Client client;
        
        /**
         * The wall cache.
         */
        private ArrayList wallList = null;

        /** 
         * The player shape cache.
         */
        private Shape player = null;

        /**
         * The projectile shape cache.
         */
        private Shape projectile = null;

        /**
         * The panel size cache.
         */
        private Dimension panelSize = null;
       
        /**
         * Create a Panel that will display an overhead view of a {@link Maze} 
         * from a specified {@link Client}'s viewpoint.
         * @param maze The {@link Maze} that is to be displayed.
         * @param client The {@link Client} whose viewpoint should be considered.
         */
        public OverheadMazePanel(Maze maze, Client client) {
                assert(maze != null);
                assert(client != null);
                this.maze = maze;
                this.client = client;
                setBackground(Color.white);
                setMinimumSize(new Dimension(200,200));
                setPreferredSize(new Dimension(400,400));
        }
        
        public void paintComponent(Graphics g) {    
                super.paintComponent(g); //paint background
                Graphics2D g2 = (Graphics2D) g;

                // Turn on antialiasing
                Map m = new HashMap();
                m.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.addRenderingHints(m);
                
                // Figure out how large the maze is
                Point p = maze.getSize();
                
                double llx = 0.0, lly = 0.0, width = 0.0, height = 0.0, cellwidth = 0.0, cellheight = 0.0;
                // Do we need to update our cache?
                if((panelSize == null) || (panelSize != getSize())) {
                        panelSize = getSize();
                        llx = (double)panelSize.width*0.025;
                        lly = (double)panelSize.height*0.025;
                        width = (double)panelSize.width - (double)panelSize.width*0.05 ;
                        height = (double)panelSize.height - (double)panelSize.height*0.05 ;
                        cellwidth = width/(double)p.getX();
                        cellheight = height/(double)p.getY();
                        buildWalls(llx, lly, cellwidth, cellheight);
                        double diameter = java.lang.Math.min(cellwidth, cellheight)*0.75;
                        player = new Arc2D.Double(
                                        new Rectangle2D.Double(-diameter/2.0,-diameter/2.0,diameter,diameter),  
                                        30.0, 
                                        300.0, 
                                        Arc2D.PIE);
                        diameter = java.lang.Math.min(cellwidth, cellheight)*0.30;
                        projectile = new Arc2D.Double(
                                        new Rectangle2D.Double(-diameter/2.0,-diameter/2.0,diameter,diameter),  
                                        0.0, 
                                        360.0, 
                                        Arc2D.PIE);
                }
                
                // Flip coordinate system
                g2.translate(0.0, (double)panelSize.height);	    
                g2.scale(1.0, -1.0);
                
                // Draw the maze walls
                g2.setStroke(new BasicStroke(2.0f));
                g2.setColor(Color.black);
                g2.draw(new Rectangle2D.Double(llx, lly, width, height));
                
                Iterator it = wallList.iterator();
                while(it.hasNext()) {
                        Object o = it.next();
                        if(o instanceof Shape) {
                                g2.draw((Shape)o);
                        } else {
                                throw new Error();
                        }
                }

                Font font = new Font("Arial", Font.PLAIN, 9);
                FontRenderContext frc = g2.getFontRenderContext(); 
                
                // Obtain the location of the distinguished client
                Point cp = maze.getClientPoint(client);
                
                for(int i = 0; i < p.getY(); i++) {
                        for(int j = 0; j < p.getX(); j++) {
                                boolean cellVisible = true;
                                Line2D visLine = new Line2D.Double(llx + (cp.getX() + 0.5)*cellwidth,
                                                                  lly + (cp.getY() + 0.5)*cellheight,
                                                                  llx + (j + 0.5)*cellwidth,
                                                                  lly + (i + 0.5)*cellheight);

                                /* Visibility testing */
                                /* Iterator visIt = wallList.iterator();
                                while(visIt.hasNext()) {
                                        Object o = visIt.next();
                                        if(o instanceof Line2D) {
                                                Line2D l = (Line2D)o;
                                                if(l.intersectsLine(visLine)) {
                                                        cellVisible = false;
                                                }
                                        } else {
                                                throw new Error();
                                        }
                                        
                                } */
                                if(cellVisible) {
                                        Cell cell = maze.getCell(new Point(j,i));
                                        Object o = cell.getContents();
                                        if(o != null) {
                                                if(o instanceof Client) {
                                                        Client c = (Client)o;
                                                        if(c instanceof GUIClient) {
                                                                g2.setColor(Color.green);
                                                        } else if(c instanceof RobotClient) {
                                                                g2.setColor(Color.red);
                                                        } else if(c instanceof RemoteClient) {
                                                                g2.setColor(Color.magenta);
                                                        }
                                        
                                                                
                                                        double xoffset = llx + j*cellwidth + (cellwidth/2.0);
                                                        double yoffset = lly + i*cellheight + (cellheight/2.0);
                                                        Direction orient = c.getOrientation();
                                                        g2.translate(xoffset, yoffset);
                                                        double rotation = 0.0; 
                                                        if(orient == Direction.South) {
                                                                rotation=-java.lang.Math.PI/2.0;
                                                        } else if (orient == Direction.North) {
                                                                rotation=java.lang.Math.PI/2.0;
                                                        } else if (orient == Direction.West) {
                                                                rotation=java.lang.Math.PI;
                                                        }
                                                        g2.rotate(rotation);
                                                        g2.fill(player);
                                                        g2.rotate(-rotation);
                                                        
                                                        
                                                        GlyphVector name = font.createGlyphVector(frc, c.getName());
                                                        g2.scale(1.0, -1.0);
                                                        g2.setColor(Color.black);
                                                        g2.drawGlyphVector(name, 0.0f, 0.0f);
                                                        g2.scale(1.0, -1.0);
                                                        g2.translate(-xoffset, -yoffset);
                                                        
                                                } else {
                                                        if(o instanceof Projectile) {
                                                                g2.setColor(Color.yellow);
                                                                double xoffset = llx + j*cellwidth + (cellwidth/2.0);
                                                                double yoffset = lly + i*cellheight + (cellheight/2.0);
                                                                g2.translate(xoffset, yoffset);
                                                                g2.fill(projectile);
                                                                g2.translate(-xoffset, -yoffset);
                                                        }
                                                }
                                        }
                                }
                        }
                }	
               
                
        }
       
        
        
        public void mazeUpdate() {
                // Maze has changed, repaint.
                this.repaint();
        }

        public void clientAdded(Client c) {
                // Doesn't need to do anything
        }

        public void clientRemoved(Client c) {
                // Doesn't need to do anything

        }
        
        public void clientKilled(Client source, Client target) {
                // Doesn't need to do anything
        }
        
        public void clientFired(Client c) {
                // Doesn't need to do anything
        }

        /**
         * Cache the {@link Line2D} objects making up the maze.
         * @param x Initial x coordinate.
         * @param y Initial y coordinate.
         * @param width Width of a cell.
         * @param height Height of a cell.
         */
        private void buildWalls(double x, double y, double width, double height) {
                Point p = maze.getSize();
                wallList = new ArrayList(p.getX() * p.getY());
                for(int i = 0; i < p.getY(); i++) {
                        for(int j = 0; j < p.getX(); j++) {
                                Cell cell = maze.getCell(new Point(j,i));
                                if(cell.isWall(Direction.North)) {
                                        wallList.add(new Line2D.Double(x + j*width, 
                                                                y + (i+1)*height, 
                                                                x + (j+1)*width, 
                                                                y + (i+1)*height));
                                } 		    
                                if(cell.isWall(Direction.East)) {
                                        wallList.add(new Line2D.Double(x + (j+1)*width, 
                                                                y + i*height, 
                                                                x + (j+1)*width, 
                                                                y + (i+1)*height));
                                } 
                        }	    
                }

        }
        
}
