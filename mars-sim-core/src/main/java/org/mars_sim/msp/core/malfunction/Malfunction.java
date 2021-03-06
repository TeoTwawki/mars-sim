/**
 * Mars Simulation Project
 * Malfunction.java
 * @version 3.1.0 2017-09-05
 * @author Scott Davis
 */

package org.mars_sim.msp.core.malfunction;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.person.medical.ComplaintType;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResource;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.building.function.Storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Malfunction class represents a
 * malfunction in a vehicle, structure or equipment.
 */
public class Malfunction implements Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(Malfunction.class.getName());
	
    private static String sourceName = logger.getName().substring(logger.getName().lastIndexOf(".") + 1, logger.getName().length());

    // Data members
    private int severity;
    private int incidentNum;
    
    private double probability;
    // Work time tracking
    private double workTime;
    private double workTimeCompleted;
    private double emergencyWorkTime;
    private double emergencyWorkTimeCompleted;
    private double EVAWorkTime;
    private double EVAWorkTimeCompleted;
    
    private String name;

    private Collection<String> systems;
    private Map<AmountResource, Double> resourceEffects;
    private Map<String, Double> lifeSupportEffects;
    private Map<ComplaintType, Double> medicalComplaints;
    private Map<Part, Integer> repairParts;

    private static MalfunctionConfig config;
 
    /**
     * Constructs a Malfunction object
     * @param name name of the malfunction
     */
    public Malfunction(String name, int incidentNum, int severity, double probability, double emergencyWorkTime,
		       double workTime, double EVAWorkTime, Collection<String> entities,
		       Map<AmountResource, Double> resourceEffects,
		       Map<String, Double> lifeSupportEffects, Map<ComplaintType, Double> medicalComplaints) {

        // Initialize data members
        this.name = name;
        this.incidentNum = incidentNum;
        this.severity = severity;
        this.probability = probability;
        this.emergencyWorkTime = emergencyWorkTime;
        this.workTime = workTime;
        this.EVAWorkTime = EVAWorkTime;
        this.systems = entities;
        this.resourceEffects = resourceEffects;
        this.lifeSupportEffects = lifeSupportEffects;
        this.medicalComplaints = medicalComplaints;

        repairParts = new HashMap<Part, Integer>();
        workTimeCompleted = 0D;
        emergencyWorkTimeCompleted = 0D;
        EVAWorkTimeCompleted = 0D;
        
    	config = SimulationConfig.instance().getMalfunctionConfiguration();
    	
    }

    /**
     * Returns the name of the malfunction.
     * @return name of the malfunction
     */
    public String getName() {
        return name;
    }

    /**
     * Returns true if malfunction is fixed.
     * @return true if malfunction is fixed
     */
    public boolean isFixed() {
        boolean result = true;

        if (workTimeCompleted < workTime) result = false;
        if (emergencyWorkTimeCompleted < emergencyWorkTime) result = false;
        if (EVAWorkTimeCompleted < EVAWorkTime) result = false;

        return result;
    }

    /**
     * Returns the severity level of the malfunction.
     * @return severity of malfunction (1 - 100)
     */
    public int getSeverity() {
        return severity;
    }

    /**
     * Returns the probability of this malfunction occuring.
     * @return probability
     */
    public double getProbability() {
        return probability;
    }

    public void setProbability(double p) {
        probability = p;
    }
    
    /**
     * Returns the work time required to repair the malfunction.
     * @return work time (in millisols)
     */
    public double getWorkTime() {
        return workTime;
    }

    /**
     * Returns the completed work time.
     * @return completed work time (in millisols)
     */
    public double getCompletedWorkTime() {
        return workTimeCompleted;
    }

    /**
     * Adds work time to the malfunction.
     * @param time work time (in millisols)
     * @return remaining work time not used (in millisols)
     */
    public double addWorkTime(double time) {
        workTimeCompleted += time;
        if (workTimeCompleted >= workTime) {
            double remaining = workTimeCompleted - workTime;
            workTimeCompleted = workTime;
            return remaining;
        }
        return 0D;
    }

    /**
     * Returns the emergency work time required to repair the malfunction.
     * @return emergency work time (in millisols)
     */
    public double getEmergencyWorkTime() {
        return emergencyWorkTime;
    }

    /**
     * Returns the completed emergency work time.
     * @return completed emergency work time (in millisols)
     */
    public double getCompletedEmergencyWorkTime() {
        return emergencyWorkTimeCompleted;
    }

    /**
     * Adds emergency work time to the malfunction.
     * @param time emergency work time (in millisols)
     * @return remaining work time not used (in millisols)
     */
    public double addEmergencyWorkTime(double time, String repairer) {
        emergencyWorkTimeCompleted += time;
        if (emergencyWorkTimeCompleted >= emergencyWorkTime) {
            double remaining = emergencyWorkTimeCompleted - emergencyWorkTime;
            emergencyWorkTimeCompleted = emergencyWorkTime;
            
            String id_string = " incident #" + incidentNum  + "";
            
        	LogConsolidated.log(logger, Level.INFO, 3000, sourceName, 
        			name + id_string + " - emergency repair finished by " + repairer  + ".", null);
            return remaining;
        }
        return 0D;
    }

    /**
     * Returns the EVA work time required to repair the malfunction.
     * @return EVA work time (in millisols)
     */
    public double getEVAWorkTime() {
        return EVAWorkTime;
    }

    /**
     * Returns the completed EVA work time.
     * @return completed EVA work time (in millisols)
     */
    public double getCompletedEVAWorkTime() {
        return EVAWorkTimeCompleted;
    }

    /**
     * Adds EVA work time to the malfunction.
     * @param time EVA work time (in millisols)
     * @return remaining work time not used (in millisols)
     */
    public double addEVAWorkTime(double time) {
        EVAWorkTimeCompleted += time;
        if (EVAWorkTimeCompleted >= EVAWorkTime) {
            double remaining = EVAWorkTimeCompleted - EVAWorkTime;
            EVAWorkTimeCompleted = EVAWorkTime;
            return remaining;
        }
        return 0D;
    }

    /**
     * Checks if a unit's scope strings have any matches
     * with the malfunction's scope strings.
     * @return true if any matches
     */
    public boolean isMatched(Collection<String> scopes) {
        boolean result = false;

        if ((systems.size() > 0) && (scopes.size() > 0)) {
            for (String s : systems) {
                for (String u : scopes) {
            	    if (s.equalsIgnoreCase(u)) 
            	    	result = true;
                }
            }
        }

        return result;
    }

    /**
     * Gets the resource effects of the malfunction.
     * @return resource effects as name-value pairs in Map
     */
    public Map<AmountResource, Double> getResourceEffects() {
        return resourceEffects;
    }

    /**
     * Gets the life support effects of the malfunction.
     * @return life support effects as name-value pairs in Map
     */
    public Map<String, Double> getLifeSupportEffects() {
        return lifeSupportEffects;
    }

    /**
     * Gets the medical complaints produced by this malfunction
     * and their probability of occurrence.
     * @return medical complaints as name-value pairs in Map
     */
    public Map<ComplaintType, Double> getMedicalComplaints() {
        return medicalComplaints;
    }

    /**
     * Gets a clone of this malfunction.
     * @return clone of this malfunction
     */
    public Malfunction getClone() {
    	int id =  MalfunctionFactory.getNewIncidentNum();
        Malfunction clone = new Malfunction(name, id, severity, probability, emergencyWorkTime,
            workTime, EVAWorkTime, systems, resourceEffects, lifeSupportEffects, medicalComplaints);

        String id_string = " incident #" + id  + "";
        
        if (emergencyWorkTime > 0D)
        	LogConsolidated.log(logger, Level.INFO, 3000, sourceName, 
            		name + id_string + " - emergency repair alert triggered.", null);

        return clone;
    }

    /**
     * Determines the parts that are required to repair this malfunction.
     * @throws Exception if error determining the repair parts.
     */
    void determineRepairParts() {
    	//MalfunctionConfig config = SimulationConfig.instance().getMalfunctionConfiguration();
    	String[] partNames = config.getRepairPartNamesForMalfunction(name);
        for (String partName : partNames) {
            if (RandomUtil.lessThanRandPercent(config.getRepairPartProbability(name, partName))) {
                int number = RandomUtil.getRandomRegressionInteger(config.getRepairPartNumber(name, partName));
                Part part = (Part) ItemResource.findItemResource(partName);
                repairParts.put(part, number);
                String id_string = " incident #" + incidentNum  + "";
            	LogConsolidated.log(logger, Level.INFO, 3000, sourceName, 
            			name + id_string + " - the repair requires " + part.getName() 
            			+ " (quantity: " + number + ").", null);
            }
        }
    }
/*
    public void produceSolidWaste(double amount, String name, Inventory inv) {

    	try {
            AmountResource ar = AmountResource.findAmountResource(name);
            double remainingCapacity = inv.getAmountResourceRemainingCapacity(ar, false, false);

            if (remainingCapacity < amount) {
                // if the remaining capacity is smaller than the harvested amount, set remaining capacity to full
            	amount = remainingCapacity;
                //logger.info(" storage is full!");
            }
            // TODO: consider the case when it is full
            inv.storeAmountResource(ar, amount, true);
            inv.addAmountSupplyAmount(ar, amount);
        }  catch (Exception e) {
    		logger.log(Level.SEVERE,e.getMessage());
        }
    }
*/
    
    /**
     * Gets the parts required to repair this malfunction.
     * @return map of parts and their number.
     */
    public Map<Part, Integer> getRepairParts() {
    	return new HashMap<Part, Integer>(repairParts);
    }

    /**
     * Repairs the malfunction with a number of a part.
     * @param part the part.
     * @param number the number used for repair.
     */
    public void repairWithParts(Part part, int number, Inventory inv) {
    	if (part == null) throw new IllegalArgumentException("part is null");
    	if (repairParts.containsKey(part)) {
    		int numberNeeded = repairParts.get(part);
    		if (number > numberNeeded) throw new IllegalArgumentException("number " + number +
    				" is greater that number of parts needed: " + numberNeeded);
    		else {
    			numberNeeded -= number;

    			// 2015-02-26 Added produceSolidWaste()
                if (part.getMassPerItem() > 0)
                	Storage.storeAnResource(part.getMassPerItem(), ResourceUtil.solidWasteAR, inv, sourceName + "::repairWithParts");

    			if (numberNeeded > 0) repairParts.put(part, numberNeeded);
    			else repairParts.remove(part);
    		}
    	}
    	else throw new IllegalArgumentException("Part " + part + " is not needed for repairs.");
    }

    /**
     * Gets the string value for the object.
     */
    public String toString() {
    	return name;
    }
    

    public void destroy() {
        systems = null;
        resourceEffects = null;
        lifeSupportEffects = null;
        medicalComplaints = null;
        repairParts = null;
        config = null;
    }
}