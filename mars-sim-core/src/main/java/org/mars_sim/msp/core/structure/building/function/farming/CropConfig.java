/**
 * Mars Simulation Project
 * CropConfig.java
 * @version 3.08 2015-04-08
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function.farming;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;

/**
 * Provides configuration information about greenhouse crops. Uses a DOM document to get the information.
 */
//2014-10-14 mkung: added new attribute: edibleBiomass, inedibleBiomass, edibleWaterContent.
// commented out ppf and photoperiod
public class CropConfig
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	public static final double INCUBATION_PERIOD  = 2D;
	// Element names
	// 2015-12-04 Added a number of elements below
	private static final String OXYGEN_CONSUMPTION_RATE = "oxygen-consumption-rate";
	private static final String WATER_CONSUMPTION_RATE = "water-consumption-rate";
	private static final String CARBON_DIOXIDE_CONSUMPTION_RATE = "carbon-dioxide-consumption-rate";
	private static final String VALUE= "value";
	
	private static final String CROP_LIST = "crop-list";
	private static final String CROP = "crop";
	private static final String NAME = "name";
	private static final String GROWING_TIME = "growing-time";
	private static final String CROP_CATEGORY = "crop-category";
	//private static final String PPF = "ppf";
	//private static final String PHOTOPERIOD = "photoperiod";
	private static final String EDIBLE_BIOMASS = "edible-biomass";
	private static final String EDIBLE_WATER_CONTENT = "edible-water-content";
	private static final String INEDIBLE_BIOMASS = "inedible-biomass";
	private static final String DAILY_PAR = "daily-PAR";
	//private static final String HARVEST_INDEX = "harvest-index";

	private Document cropDoc;
	private List<CropType> cropList;

	private List<CropCategoryType> cropCategoryTypes = new ArrayList<CropCategoryType>(Arrays.asList(CropCategoryType.values()));

	/**
	 * Constructor.
	 * @param cropDoc the crop DOM document.
	 */
	public CropConfig(Document cropDoc) {
		this.cropDoc = cropDoc;

	}

	/**
	 * Gets a list of crop types.
	 * @return list of crop types
	 * @throws Exception when crops could not be parsed.
	 */
	@SuppressWarnings("unchecked")
	public List<CropType> getCropList() {

		if (cropList == null) {
			cropList = new ArrayList<CropType>();

			Element root = cropDoc.getRootElement();
			Element cropElement = root.getChild(CROP_LIST);
			List<Element> crops = cropElement.getChildren(CROP);

			for (Element crop : crops) {
				String name = "";
				// Get name.
				name = crop.getAttributeValue(NAME).toLowerCase();

				// Get growing time.
				String growingTimeStr = crop.getAttributeValue(GROWING_TIME);
				double growingTime = Double.parseDouble(growingTimeStr);

				// Get crop category
				String cropCategory = crop.getAttributeValue(CROP_CATEGORY);

				// 2016-07-01 Added checking against the crop category enum
				boolean known = false;
				CropCategoryType cat = null;
				// check to see if this crop category is recognized in mars-sim
				for (CropCategoryType c : cropCategoryTypes) {
					if (CropCategoryType.getType(cropCategory) == c) {
						known = true;
						cat = c;
						//System.out.println("cat is "+ cat);
					}
				}
					
				if (!known)
					throw new IllegalArgumentException("no such crop category : " + cropCategory);
								
				// Get ppf
				//String ppfStr = crop.getAttributeValue(PPF);
				//double ppf = Double.parseDouble(ppfStr);

				// Get photoperiod
				//String photoperiodStr = crop.getAttributeValue(PHOTOPERIOD);
				//double photoperiod = Double.parseDouble(photoperiodStr);

				// Get edibleBiomass
				String edibleBiomassStr = crop.getAttributeValue(EDIBLE_BIOMASS);
				double edibleBiomass = Double.parseDouble(edibleBiomassStr);

				// Get edible biomass water content [ from 0 to 1 ]
				String edibleWaterContentStr = crop.getAttributeValue(EDIBLE_WATER_CONTENT);
				double edibleWaterContent = Double.parseDouble(edibleWaterContentStr);

				// Get inedibleBiomass
				String inedibleBiomassStr = crop.getAttributeValue(INEDIBLE_BIOMASS);
				double inedibleBiomass = Double.parseDouble(inedibleBiomassStr);

				// 2015-04-08 Added daily PAR
				String dailyPARStr = crop.getAttributeValue(DAILY_PAR);
				double dailyPAR = Double.parseDouble(dailyPARStr);

				// Get harvestIndex
				//String harvestIndexStr = crop.getAttributeValue(HARVEST_INDEX);
				//double harvestIndex = Double.parseDouble(harvestIndexStr);

				// 2016-06-29 Set up the default growth phases of a crop
				Map<Integer, Phase> phases = new HashMap<>();

				if (cat == CropCategoryType.TUBERS) {

					phases.put(0, new Phase(PhaseType.INCUBATION, INCUBATION_PERIOD, 0));
					phases.put(1, new Phase(PhaseType.PLANTING, 0.5D, 0));
					phases.put(2, new Phase(PhaseType.SPROUTING, 1D, 14D));
					phases.put(3, new Phase(PhaseType.LEAF_DEVELOPMENT, 1D, 5D));
					phases.put(4, new Phase(PhaseType.TUBER_INITIATION, 1D, 14D));
					phases.put(5, new Phase(PhaseType.TUBER_FILLING, 1D, 40D));
					phases.put(6, new Phase(PhaseType.MATURING, 1D, 27D));
					phases.put(7, new Phase(PhaseType.HARVESTING, 0.75, 0));
					phases.put(8, new Phase(PhaseType.FINISHED, 0.1, 0));

				} else if (cat == CropCategoryType.FRUITS) {

					phases.put(0, new Phase(PhaseType.INCUBATION, INCUBATION_PERIOD, 0));
					phases.put(1, new Phase(PhaseType.PLANTING, 0.5D, 0));
					phases.put(2, new Phase(PhaseType.GERMINATION, 1D, 5D));
					phases.put(3, new Phase(PhaseType.VEGETATIVE_DEVELOPMENT, 1D, 35D));
					phases.put(4, new Phase(PhaseType.FLOWERING, 1D, 25D));
					phases.put(5, new Phase(PhaseType.FRUITING, 1D, 35D));
					phases.put(6, new Phase(PhaseType.HARVESTING, 0.75, 0));
					phases.put(7, new Phase(PhaseType.FINISHED, 0.1, 0));
				
				} else if (cat == CropCategoryType.LEAVES) {

					phases.put(0, new Phase(PhaseType.INCUBATION, INCUBATION_PERIOD, 0));
					phases.put(1, new Phase(PhaseType.PLANTING, 0.5D, 0));
					phases.put(2, new Phase(PhaseType.GERMINATION, 1D, 5D));
					phases.put(3, new Phase(PhaseType.POST_EMERGENCE, 1D, 5D));
					phases.put(4, new Phase(PhaseType.HEAD_DEVELOPMENT, 1D, 40D));
					phases.put(5, new Phase(PhaseType.FIFTY_PERCENT_HEAD_SIZE_REACHED, 1D, 50D));
					phases.put(6, new Phase(PhaseType.HARVESTING, 0.75, 0));
					phases.put(7, new Phase(PhaseType.FINISHED, 0.1, 0));

				} else if (cat == CropCategoryType.BULBS) {

					phases.put(0, new Phase(PhaseType.INCUBATION, INCUBATION_PERIOD, 0));
					phases.put(1, new Phase(PhaseType.PLANTING, 0.5D, 0));
					phases.put(2, new Phase(PhaseType.CLOVE_SPROUTING, 1D, 5D));
					phases.put(3, new Phase(PhaseType.POST_EMERGENCE, 1D, 15D));
					phases.put(4, new Phase(PhaseType.LEAFING, 1D, 25D));
					phases.put(5, new Phase(PhaseType.BULB_INITIATION, 1D, 25D));
					phases.put(6, new Phase(PhaseType.MATURATION, 1D, 30D));					
					phases.put(7, new Phase(PhaseType.HARVESTING, 0.75, 0));
					phases.put(8, new Phase(PhaseType.FINISHED, 0.1, 0));

				} else if (cat == CropCategoryType.LEGUMES) {

					phases.put(0, new Phase(PhaseType.INCUBATION, INCUBATION_PERIOD, 0));
					phases.put(1, new Phase(PhaseType.PLANTING, 0.5D, 0));
					phases.put(2, new Phase(PhaseType.GERMINATION, 1D, 5D));
					phases.put(3, new Phase(PhaseType.LEAFING, 1D, 35D));
					phases.put(4, new Phase(PhaseType.FLOWERING, 1D, 20D));
					phases.put(5, new Phase(PhaseType.SEED_FILL, 1D, 15D));
					phases.put(6, new Phase(PhaseType.POD_MATURING, 1D, 25D));					
					phases.put(7, new Phase(PhaseType.HARVESTING, 0.75, 0));
					phases.put(8, new Phase(PhaseType.FINISHED, 0.1, 0));

				} else if (cat == CropCategoryType.GRAINS) {

					phases.put(0, new Phase(PhaseType.INCUBATION, INCUBATION_PERIOD, 0));
					phases.put(1, new Phase(PhaseType.PLANTING, 0.5D, 0));
					phases.put(2, new Phase(PhaseType.GERMINATION, 1D, 15D));
					phases.put(3, new Phase(PhaseType.TILLERING, 1D, 20D));
					phases.put(4, new Phase(PhaseType.STEM_ELONGATION, 1D, 15D));
					phases.put(5, new Phase(PhaseType.FLOWERING, 1D, 20D));					
					phases.put(6, new Phase(PhaseType.MILK_DEVELOPMENT, 1D, 5D));
					phases.put(7, new Phase(PhaseType.DOUGH_DEVELOPING, 1D, 10D));
					phases.put(8, new Phase(PhaseType.MATURATION, 1D, 15D));					
					phases.put(9, new Phase(PhaseType.HARVESTING, 0.75, 0));
					phases.put(10, new Phase(PhaseType.FINISHED, 0.1, 0));

				} else {

					phases.put(0, new Phase(PhaseType.INCUBATION, INCUBATION_PERIOD, 0));
					phases.put(1, new Phase(PhaseType.PLANTING, 0.5, 0));
					phases.put(2, new Phase(PhaseType.GERMINATION, 1D, 5D));
					phases.put(3, new Phase(PhaseType.GROWING, 1D, 95D));
					phases.put(4, new Phase(PhaseType.HARVESTING, 0.75, 0));
					phases.put(5, new Phase(PhaseType.FINISHED, 0.1, 0));

				}

				CropType cropType = new CropType(name, growingTime * 1000D, cat,
							edibleBiomass, edibleWaterContent, inedibleBiomass, 
							dailyPAR, phases);
	
				cropList.add(cropType); 
			}

		}
		return cropList;
	}

	/**
	 * Gets the carbon doxide consumption rate.
	 * @return carbon doxide rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	// 2015-12-04 Added getCarbonDioxideConsumptionRate()
	public double getCarbonDioxideConsumptionRate() {
		return getValueAsDouble(CARBON_DIOXIDE_CONSUMPTION_RATE);
	}

	/**
	 * Gets the oxygen consumption rate.
	 * @return oxygen rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	// 2015-12-04 Added getOxygenConsumptionRate()
	public double getOxygenConsumptionRate() {
		return getValueAsDouble(OXYGEN_CONSUMPTION_RATE);
	}

	/**
	 * Gets the water consumption rate.
	 * @return water rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	// 2015-12-04 Added getWaterConsumptionRate()
	public double getWaterConsumptionRate() {
		return getValueAsDouble(WATER_CONSUMPTION_RATE);
	}
	
	/*
	 * Gets the value of an element as a double
	 * @param an element
	 * @return a double 
	 */
	// 2015-12-04 Added getValueAsDouble()
	private double getValueAsDouble(String child) {
		Element root = cropDoc.getRootElement();
		Element element = root.getChild(child);
		String str = element.getAttributeValue(VALUE);
		return Double.parseDouble(str);
	}
	
/*	
	public Map<Integer, Phase> getPhases() {
		try {
			return shallowCopy(phases);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return phases;
	}

	
	static final Map shallowCopy(final Map source) throws Exception {
	    final Map newMap = source.getClass().newInstance();
	    newMap.putAll(source);
	    return newMap;
	}
*/
	
	public List<CropCategoryType> getCropCategoryTypes() {
		return cropCategoryTypes;
	}
	
	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		cropDoc = null;
		if(cropList != null){
			cropList.clear();
			cropList = null;
		}
	}
}
