package org.mars_sim.msp.ui.standard.tool.map;

import java.awt.Color;
import java.awt.Graphics;

import org.mars_sim.msp.simulation.Coordinates;

public class CenteredCircleLayer implements MapLayer {

	private int radius;
	private Color color;
	private boolean displayCircle;
	
	public CenteredCircleLayer(Color color) {
		this.color = color;
	}
	
	public void setRadius(int radius) {
		this.radius = radius;
	}
	
	public void setDisplayCircle(boolean displayCircle) {
		this.displayCircle = displayCircle;
	}
	
	public void displayLayer(Coordinates mapCenter, String mapType, Graphics g) {
		if (displayCircle && (radius > 0)) {
			g.setColor(color);
			g.drawOval((150 - radius), (150 - radius), (radius * 2), (radius * 2));
		}
	}
}