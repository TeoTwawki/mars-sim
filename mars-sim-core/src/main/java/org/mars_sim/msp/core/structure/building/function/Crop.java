/**
 * Mars Simulation Project
 * Crop.java
 * @version 3.08 2015-02-14
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;

/**
 * The Crop class is a food crop grown on a farm.
 */
public class Crop
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(Crop.class.getName());

	// TODO Static members of crops should be initialized from some xml instead of being hard coded.
	/** Amount of waste water needed per harvest mass. */
	public static final double WASTE_WATER_NEEDED = 5D;
	/** Amount of carbon dioxide needed per harvest mass. */
	public static final double CARBON_DIOXIDE_NEEDED = 2D;
	/** Amount of oxygen needed per harvest mass. */
	public static final double OXYGEN_NEEDED = 2D;
	 	
	//  Be sure that FERTILIZER_NEEDED is static, but NOT "static final"
	public static double FERTILIZER_NEEDED = 0.0005D; // 0.5D ;
	
	public static final double WATER_RECLAMATION_RATE = .8D;
	public static final double OXYGEN_GENERATION_RATE = .9D;
	public static final double CO2_GENERATION_RATE = .9D;

	// TODO Crop phases should be an internationalizable enum.
	public static final String PLANTING = "Planting";
	public static final String GERMINATION = "Germination"; // include initial sprouting of a seedling
	public static final String GROWING = "Growing";
	// TODO: add FLOWERING phase
	public static final String HARVESTING = "Harvesting";
	public static final String FINISHED = "Finished";

    private static final double T_TOLERANCE = 3D;
    
    private static final double PERCENT_IN_GERMINATION_PHASE = 5; // Assuming the first 5% of a crop's life is in germination phase 
    
	// Data members
	/** The type of crop. */
	private CropType cropType;

	private Inventory inv;
	/** Farm crop being grown in. */
	private Farming farm;
	/** The settlement the crop is located at. */
	private Settlement settlement;
	/** Current phase of crop. */
	private String phase;
	/** Maximum possible food harvest for crop. (kg) */
	private double maxHarvest;
	/** Required work time for planting (millisols). */
	private double plantingWorkRequired;
	/** Required work time to tend crop daily (millisols). */
	private double dailyTendingWorkRequired;
	/** Required work time for harvesting (millisols). */
	private double harvestingWorkRequired;
	/** Completed work time in current phase (millisols). */
	private double currentPhaseWorkCompleted;
	/** Actual food harvest for crop. (kg) */
	private double actualHarvest;
	/** Growing phase time completed thus far (millisols). */
	private double growingTimeCompleted; // randomly generated and assigned for each crop at the beginning of a sim
	/** Current sol of month. */
	private int currentSol;
	
	private double growingArea;
	
	private double totalGrowingDay;

	private double fractionalGrowthCompleted;
	
	/**
	 * Constructor.
	 * @param cropType the type of crop.
	 * @param maxHarvest - Maximum possible food harvest for crop. (kg)
	 * @param farm - Farm crop being grown in.
	 * @param settlement - the settlement the crop is located at.
	 * @param newCrop - true if this crop starts in it's planting phase.
	 * called by Farming.java constructor and timePassing()
	 */
	public Crop(CropType cropType, double growingArea, double maxHarvest, Farming farm, Settlement settlement, boolean newCrop) {
		this.cropType = cropType;
				//logger.info("constructor : the new crop is " + cropType.getName());
		this.maxHarvest = maxHarvest;
		this.farm = farm;
		this.settlement = settlement;
		this.growingArea = growingArea;

		inv = settlement.getInventory();
		
		// Determine work required.
		plantingWorkRequired = maxHarvest;
		dailyTendingWorkRequired = maxHarvest;
		harvestingWorkRequired = maxHarvest * 3D; // old default is 5. why?

		totalGrowingDay = cropType.getGrowingTime();
		
		if (newCrop) {
			phase = PLANTING;
			actualHarvest = 0D;
		}
		else {
			// set up a crop's "initial" percentage of growth randomly when the simulation gets started
			growingTimeCompleted = RandomUtil.getRandomDouble(totalGrowingDay);
			fractionalGrowthCompleted = growingTimeCompleted/totalGrowingDay;
			
			if ( fractionalGrowthCompleted * 100D <= PERCENT_IN_GERMINATION_PHASE) {	// assuming the first 10% growing day of each crop is germination		
				phase = GERMINATION;
			}		
			else if ( fractionalGrowthCompleted * 100D > PERCENT_IN_GERMINATION_PHASE) {
				phase = GROWING;
			}			
			actualHarvest = maxHarvest * fractionalGrowthCompleted;	
		}
	}

	public double getGrowingArea() {
		return growingArea;
	}
	
	/**
	 * Gets the type of crop.
	 *
	 * @return crop type
	 */
	public CropType getCropType() {
		return cropType;
	}
	
	/**
	 * Gets the phase of the crop.
	 * @return phase
	 */
	// Called by BuildingPanelFarming.java to retrieve the phase of the crop
	public String getPhase() {
		return phase;
	}


	/**
	 * Gets the crop category
	 * @return category
	 * 2014-10-10 by mkung: added this method for UI to show crop category
	 */
	// Called by BuildingPanelFarming.java to retrieve the crop category
	public String getCategory() {
		return cropType.getCropCategory();
	}
	
	/**
	 * Gets the maximum possible food harvest for crop.
	 * @return food harvest (kg.)
	 */
	public double getMaxHarvest() { return maxHarvest; }

	/**
	 * Gets the amount of growing time completed.
	 * @return growing time (millisols)
	 */
	public double getGrowingTimeCompleted() { return growingTimeCompleted; }

	/**
	 * Checks if crop needs additional work on current sol.
	 * @return true if more work needed.
	 */
	public boolean requiresWork() {
		boolean result = false;
		if (phase.equals(PLANTING) || phase.equals(HARVESTING)) result = true;
		if (phase.equals(GROWING) || phase.equals(GERMINATION) ) {
			if (dailyTendingWorkRequired > currentPhaseWorkCompleted) result = true;
		}

		return result;
	}

	/**
	 * Gets the overall health condition of the crop.
	 * 
	 * @return condition as value from 0 (poor) to 1 (healthy)
	 */
	// Called by BuildingPanelFarming.java to retrieve the health condition status
	public double getCondition() {
		// O:bad, 1:good
		double result = 0D;
			
		if (phase.equals(PLANTING)) result = 1D;
		
		else if (phase.equals(GERMINATION) ) {
			if ((maxHarvest == 0D) || (growingTimeCompleted == 0D)) result = 1D;
			else result = (actualHarvest * totalGrowingDay) / (maxHarvest * growingTimeCompleted);
		}

		else if (phase.equals(GROWING) ) {
			if ((maxHarvest == 0D) || (growingTimeCompleted == 0D)) result = 1D;
			else result = (actualHarvest * totalGrowingDay) / (maxHarvest * growingTimeCompleted);
		}
		
		else if (phase.equals(HARVESTING) || phase.equals(FINISHED)) {
			result = actualHarvest / maxHarvest;
		}

		if (result > 1D) result = 1D;
		else if (result < 0D) result = 0D;
		
		//logger.info("getCondition() : crop's condition is "+ result);
				
		return result;
	}

	/**
	 * Adds work time to the crops current phase.
	 * @param workTime - Work time to be added (millisols)
	 * @return workTime remaining after working on crop (millisols)
	 * @throws Exception if error adding work.
	 */ 
	// Called by Farming.java's addWork()
	public double addWork(double workTime) {
		double remainingWorkTime = workTime;

		if (phase.equals(PLANTING)) {
			currentPhaseWorkCompleted += remainingWorkTime;
			if (currentPhaseWorkCompleted >= plantingWorkRequired) {
				remainingWorkTime = currentPhaseWorkCompleted - plantingWorkRequired;
				currentPhaseWorkCompleted = 0D;
				currentSol = Simulation.instance().getMasterClock().getMarsClock().getSolOfMonth();
				phase = GERMINATION;	
			}
			else {
				remainingWorkTime = 0D;
			}
		}
		
		// 2015-02-15 Added GERMINATION
		if (phase.equals(GERMINATION) || phase.equals(GROWING)) {
			currentPhaseWorkCompleted += remainingWorkTime;
			if (currentPhaseWorkCompleted >= dailyTendingWorkRequired) {
				remainingWorkTime = currentPhaseWorkCompleted - dailyTendingWorkRequired;
				currentPhaseWorkCompleted = dailyTendingWorkRequired;
			}
			else {
				remainingWorkTime = 0D;
			}
		}

		if (phase.equals(HARVESTING)) {
				//logger.info("addWork() : crop is in Harvesting phase");
			currentPhaseWorkCompleted += remainingWorkTime;
			if (currentPhaseWorkCompleted >= harvestingWorkRequired) {
				// Harvest is over. Close out this phase
				//logger.info("addWork() : done harvesting. remainingWorkTime is " + Math.round(remainingWorkTime));
				double overWorkTime = currentPhaseWorkCompleted - harvestingWorkRequired;
				// 2014-10-07 modified parameter list to include crop name
				double lastHarvest = actualHarvest * (remainingWorkTime - overWorkTime) / harvestingWorkRequired;
				// Store the crop harvest
				storeAnResource(lastHarvest, cropType.getName());
				logger.info("addWork() : " + cropType.getName() + " lastHarvest " + Math.round(lastHarvest * 1000.0)/1000.0);
				remainingWorkTime = overWorkTime;
				
				phase = FINISHED;
				
				generateCropWaste(lastHarvest);
	
			}
			else { 	// continue the harvesting process
				// 2014-10-07 modified parameter list to include crop name
				double modifiedHarvest = actualHarvest * workTime / harvestingWorkRequired;
				logger.info("addWork() : " + cropType.getName() + " modifiedHarvest is " + Math.round(modifiedHarvest * 1000.0)/1000.0);
				// Store the crop harvest
				storeAnResource(modifiedHarvest, cropType.getName());
				remainingWorkTime = 0D;
				
				generateCropWaste(modifiedHarvest);
			}
		}

		return remainingWorkTime;
	}
	
	public void generateCropWaste(double harvestMass) {
		// 2015-02-06 Added Crop Waste
		double amountCropWaste = harvestMass * cropType.getInedibleBiomass() / (cropType.getInedibleBiomass() +cropType.getEdibleBiomass());
		storeAnResource(amountCropWaste, "Crop Waste");
		logger.info("addWork() : " + cropType.getName() + " amountCropWaste " + Math.round(amountCropWaste * 1000.0)/1000.0);
	}
	
	
	// 2015-02-06 Added storeAnResource()
	   public void storeAnResource(double amount, String name) {
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

	   
	/**
	 * Time passing for crop.
	 * @param time - amount of time passing (millisols)
	 */
	public void timePassing(double time) {

		if (time > 0D) {
			if (phase.equals(GROWING)|| phase.equals(GERMINATION)) {
				growingTimeCompleted += time;
				
				if (growingTimeCompleted <= totalGrowingDay * PERCENT_IN_GERMINATION_PHASE / 100D) {
					phase = GERMINATION;
					currentPhaseWorkCompleted = 0D;
				}
				if (growingTimeCompleted > totalGrowingDay * PERCENT_IN_GERMINATION_PHASE / 100D) {
					phase = GROWING;
					currentPhaseWorkCompleted = 0D;
				}
				
				
				if (growingTimeCompleted > totalGrowingDay) {
					phase = HARVESTING;
					currentPhaseWorkCompleted = 0D;
				}
				else { // still in phase.equals(GROWING)|| phase.equals(GERMINATION)

					// Modify actual harvest amount based on daily tending work.
					int newSol = Simulation.instance().getMasterClock().getMarsClock().getSolOfMonth();
					if (newSol != currentSol) {
						double maxDailyHarvest = maxHarvest / (totalGrowingDay / 1000D);
						double dailyWorkCompleted = currentPhaseWorkCompleted / dailyTendingWorkRequired;
						actualHarvest += (maxDailyHarvest * (dailyWorkCompleted - .5D));
						currentSol = newSol;
						currentPhaseWorkCompleted = 0D;
					}

					double maxPeriodHarvest = maxHarvest * (time / totalGrowingDay);
					double harvestModifier = calculateHarvestModifier(maxPeriodHarvest);
					
					// Modify harvest amount.
					actualHarvest += maxPeriodHarvest * harvestModifier;	
					
					//System.out.println("Farming.java: maxPeriodHarvest is " + maxPeriodHarvest);
					//System.out.println("Farming.java: harvestModifier is " + harvestModifier);
					//System.out.println("Farming.java: actualHarvest is " + actualHarvest);
					//System.out.println("Farming.java: maxHarvest is " + maxHarvest);				
					
					
					// Check on the health of a grown crop (> 25%)
					if (((fractionalGrowthCompleted) > .25D) &&
							(getCondition() < .1D)) {
						phase = FINISHED;
						logger.info("Crop " + cropType.getName() + " at " + settlement.getName() + " died.");
						// 2015-02-06 Added Crop Waste
						double amountCropWaste = actualHarvest * cropType.getInedibleBiomass() / ( cropType.getInedibleBiomass() + cropType.getEdibleBiomass());
						storeAnResource(amountCropWaste, "Crop Waste");
						logger.info("timePassing() : " + amountCropWaste + " kg Crop Waste generated from the dead "+ cropType.getName());
					}
					
					// Seedlings are less resilient and more prone to environmental factors
					else if ((fractionalGrowthCompleted > .1D) &&
							(getCondition() < .3D)) {
						phase = FINISHED;
						logger.info("The seedlings of " + cropType.getName() + " at " + settlement.getName() + " did not survive.");
						// 2015-02-06 Added Crop Waste
						double amountCropWaste = actualHarvest * cropType.getInedibleBiomass() / ( cropType.getInedibleBiomass() + cropType.getEdibleBiomass());
						storeAnResource(amountCropWaste, "Crop Waste");
						logger.info("timePassing() : " + amountCropWaste + " kg Crop Waste generated from the dead "+ cropType.getName());
					}
				}
			}
		}
	}

	// 2015-02-16 Added calculateHarvestModifier()
	public double calculateHarvestModifier(double maxPeriodHarvest) {		
		double harvestModifier = 1D;

		// TODO mkung: Modify harvest modifier according to the moisture level	
		
		// TODO mkung: Modify harvest modifier according to the pollination by the  number of bees in the greenhouse

		// TODO mkung: Modify harvest modifier by amount of artificial light available to the greenhouse
		
		// Determine harvest modifier according to amount of sunlight.
		SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
		double sunlight = surface.getSurfaceSunlight(settlement.getCoordinates());
		//System.out.println("Farming.java: sunlight is " + sunlight);
		// Note: .55D will push sunlightModifier to potential greater than 1 since strong sunlight adds to the harvest.
		double sunlightModifier = (sunlight * .55D) + .5D;			
		if (phase.equals(GROWING)) {	
			harvestModifier = harvestModifier * sunlightModifier;		
			//System.out.println("Farming.java: sunlight harvestModifier is " + harvestModifier);
		}
							
		double T_NOW = farm.getBuilding().getTemperature();
		double t = Building.GREENHOUSE_TEMPERATURE;
		if (T_NOW > (t + T_TOLERANCE))
			harvestModifier = harvestModifier * (t / T_NOW);		
		else if (T_NOW < (t - T_TOLERANCE))
			harvestModifier = harvestModifier * (T_NOW / t);						
		else // if (T_NOW < (t + T_TOLERANCE ) && T_NOW > (t - T_TOLERANCE )) {
			// TODO: implement optimal growing temperature for each particular crop
			harvestModifier *= harvestModifier *1.01;
			
		//System.out.println("Farming.java: temp harvestModifier is " + harvestModifier);
		
		// Determine harvest modifier according to amount of waste water available.

		double factor = 0;
		// amount of wastewater/water needed is also based on % of growth
		if (phase.equals(GERMINATION))
			factor = .1;				
		else if (fractionalGrowthCompleted < .1 ) 
			factor = .2;
		else if (fractionalGrowthCompleted < .2 ) 
			factor = .25;
		else if (fractionalGrowthCompleted < .3 ) 
			factor = .3;
		else if (phase.equals(GROWING))
			factor = fractionalGrowthCompleted;
			
		double waterUsed = 0;
		double wasteWaterRequired = factor * maxPeriodHarvest * WASTE_WATER_NEEDED;
		AmountResource wasteWater = AmountResource.findAmountResource("waste water");
		double wasteWaterAvailable = inv.getAmountResourceStored(wasteWater, false);
		double wasteWaterUsed = wasteWaterRequired;
		if (wasteWaterUsed > wasteWaterAvailable) {
			// 2015-01-25 Added diff, waterUsed and consumeWater() when waste water is not available
			double diff = wasteWaterUsed - wasteWaterAvailable;
			waterUsed = consumeWater(diff);
			wasteWaterUsed = wasteWaterAvailable;
		}
		retrieveAnResource(wasteWater, wasteWaterUsed);
		
		// 2015-01-25 Added waterUsed and combinedWaterUsed	
		double combinedWaterUsed = wasteWaterUsed + waterUsed;
		double fractionUsed = combinedWaterUsed / wasteWaterRequired;
			
		harvestModifier = harvestModifier * ((( fractionUsed) * .5D) + .5D);
		//System.out.println("Farming.java: wasteWater harvestModifier is " + harvestModifier);
		
		// Amount of water generated through recycling				
		double waterAmount = wasteWaterUsed * WATER_RECLAMATION_RATE;					
		storeAnResource(waterAmount, org.mars_sim.msp.core.LifeSupport.WATER);					

		if (sunlightModifier <= .5) {
			AmountResource o2ar = AmountResource.findAmountResource(org.mars_sim.msp.core.LifeSupport.OXYGEN);
			double o2Required = factor * maxPeriodHarvest * OXYGEN_NEEDED;
			double o2Available = inv.getAmountResourceStored(o2ar, false);
			double o2Used = o2Required;
			if (o2Used > o2Available) {
				o2Used = o2Available;
			}					
			retrieveAnResource(o2ar, o2Used);								
			harvestModifier = harvestModifier * (((o2Used / o2Required) * .5D) + .5D);
			
			// Determine the amount of co2 generated via gas exchange.							
			double co2Amount = o2Used * CO2_GENERATION_RATE;					
			storeAnResource(co2Amount, "carbon dioxide");	
		}
		
		else if (sunlightModifier > .5) {	
			// TODO: gives a better modeling of how the amount of light available will trigger photosynthesis that converts co2 to o2
			// Determine harvest modifier by amount of carbon dioxide available.
			AmountResource carbonDioxide = AmountResource.findAmountResource("carbon dioxide");
			double carbonDioxideRequired = factor * maxPeriodHarvest * CARBON_DIOXIDE_NEEDED;
			double carbonDioxideAvailable = inv.getAmountResourceStored(carbonDioxide, false);
			double carbonDioxideUsed = carbonDioxideRequired;
			if (carbonDioxideUsed > carbonDioxideAvailable) {
				carbonDioxideUsed = carbonDioxideAvailable;
			}					
			retrieveAnResource(carbonDioxide, carbonDioxideUsed);								
			// TODO: allow higher concentration of co2 to be pumped to increase the harvest modifier to the harvest.
			harvestModifier = harvestModifier * (((carbonDioxideUsed / carbonDioxideRequired) * .5D) + .5D);
			//System.out.println("Farming.java: carbonDioxide harvestModifier is " + harvestModifier);
			
			// Determine the amount of oxygen generated via gas exchange.							
			double oxygenAmount = carbonDioxideUsed * OXYGEN_GENERATION_RATE;					
			storeAnResource(oxygenAmount, org.mars_sim.msp.core.LifeSupport.OXYGEN);									

		}
	
		return harvestModifier;
	}
	
	/**
	 * Retrieves an amount from water.
	 * @param waterRequired
	 */
	// 2015-01-25 consumeWater()
	public double consumeWater(double waterRequired) {
		AmountResource water = AmountResource.findAmountResource("water");
		double waterAvailable = inv.getAmountResourceStored(water, false);
		AmountResource fertilizer = AmountResource.findAmountResource("fertilizer");
		double fertilizerAvailable = inv.getAmountResourceStored(fertilizer, false);		
		double waterUsed = waterRequired;
		double fertilizerUsed = FERTILIZER_NEEDED;
		if (waterUsed < waterAvailable)
			retrieveAnResource(water, waterUsed);
		else 
			waterUsed = waterAvailable;
			
		if (fertilizerUsed < fertilizerAvailable)
			retrieveAnResource(fertilizer, fertilizerUsed);
		else
			fertilizerUsed = fertilizerAvailable;			
			//TODO: if not enough fertilizer is available
			// should it send out an alert and/or have impact in crop growing?	
	    return waterUsed;
	}
	
	/**
	 * Retrieves an amount from an Amount Resource.
	 * @param AmountResource resource
	 * @param double amount
	 */
	// 2015-01-25 Added retrieveAnResource()
	public void retrieveAnResource(AmountResource resource, double amount) {
		try {
			inv.retrieveAmountResource(resource, amount);
		    inv.addAmountDemandTotalRequest(resource);
		    inv.addAmountDemand(resource, amount);
				
	    } catch (Exception e) {
	        logger.log(Level.SEVERE,e.getMessage());
		}
	}
	
	/**
	 * Gets a random crop type.
	 * @return crop type
	 * @throws Exception if crops could not be found.
	 */
   	// 2014-12-09 Added new param cropInQueue and changed method name to getNewCrop()
	public static CropType getNewCrop(String cropInQueue) {
		CropConfig cropConfig = SimulationConfig.instance().getCropConfiguration();
		List<CropType> cropTypes = cropConfig.getCropList();
		//cropTypeList = cropTypes;
		if (cropInQueue.equals("0")) {
			int r = RandomUtil.getRandomInt(cropTypes.size() - 1);
			return cropTypes.get(r);
		} else {
			CropType crop = null;
			Iterator<CropType> i = cropTypes.iterator();
			while (i.hasNext()) {
				CropType c = i.next();
				if (c.getName() == cropInQueue)
					crop = c;
			}
			return crop;	
		}
	}

	/**
	 * Gets the average growing time for a crop.
	 * @return average growing time (millisols)
	 * @throws Exception if error reading crop config.
	 */
	public static double getAverageCropGrowingTime() {
		CropConfig cropConfig = SimulationConfig.instance().getCropConfiguration();
		double totalGrowingTime = 0D;
		List<CropType> cropTypes = cropConfig.getCropList();
		Iterator<CropType> i = cropTypes.iterator();
		while (i.hasNext()) totalGrowingTime += i.next().getGrowingTime();
		return totalGrowingTime / cropTypes.size();
	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		cropType = null;
		farm = null;
		inv = null;
		settlement = null;
		phase = null;
	}
}
