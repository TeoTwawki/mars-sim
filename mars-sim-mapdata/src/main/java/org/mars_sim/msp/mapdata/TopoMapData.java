/**
 * Mars Simulation Project
 * TopoMapData.java
 * @version 3.02 2011-11-09
 * @author Scott Davis
 */

package org.mars_sim.msp.mapdata;

/**
 * Topographical map data.
 */
public class TopoMapData extends IntegerMapData {

    // Static members.
    private static final String INDEX_FILE = "TopoMarsMap.index";
    private static final String MAP_FILE = "TopoMarsMap.dat";
    
    /**
     * Constructor
     */
    public TopoMapData() {
        super(INDEX_FILE, MAP_FILE);
    }
}
