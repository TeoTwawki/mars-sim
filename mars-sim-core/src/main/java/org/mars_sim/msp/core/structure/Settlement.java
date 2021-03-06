/**
 * Mars Simulation Project
 * Settlement.java
 * @version 3.1.0 2017-08-30
 * @author Scott Davis
 */

package org.mars_sim.msp.core.structure;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.mars_sim.msp.core.Airlock;
import org.mars_sim.msp.core.CollectionUtils;
import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.LifeSupportType;
import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.location.LocationStateType;
import org.mars_sim.msp.core.mars.DustStorm;
import org.mars_sim.msp.core.mars.Weather;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.RadiationExposure;
import org.mars_sim.msp.core.person.ShiftType;
import org.mars_sim.msp.core.person.TaskSchedule;
import org.mars_sim.msp.core.person.ai.job.Astronomer;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionManager;
import org.mars_sim.msp.core.person.ai.mission.VehicleMission;
import org.mars_sim.msp.core.person.ai.task.HaveConversation;
import org.mars_sim.msp.core.person.ai.task.Maintenance;
import org.mars_sim.msp.core.person.ai.task.Read;
import org.mars_sim.msp.core.person.ai.task.Relax;
import org.mars_sim.msp.core.person.ai.task.Repair;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.person.ai.task.Workout;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.connection.BuildingConnector;
import org.mars_sim.msp.core.structure.building.connection.BuildingConnectorManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.EVA;
import org.mars_sim.msp.core.structure.building.function.PowerMode;
import org.mars_sim.msp.core.structure.building.function.farming.Crop;
import org.mars_sim.msp.core.structure.building.function.farming.Farming;
import org.mars_sim.msp.core.structure.construction.ConstructionManager;
import org.mars_sim.msp.core.structure.goods.GoodsManager;
import org.mars_sim.msp.core.structure.goods.GoodsUtil;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.core.vehicle.VehicleType;

/**
 * The Settlement class represents a settlement unit on virtual Mars. It
 * contains information related to the state of the settlement.
 */
public class Settlement
extends Structure
implements Serializable, LifeSupportType, Objective {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/* default logger. */
	private static Logger logger = Logger.getLogger(Settlement.class.getName());
	
    private static String sourceName = logger.getName().substring(logger.getName().lastIndexOf(".") + 1, logger.getName().length());

	/** Normal air pressure [in kPa] */
	private static final double NORMAL_AIR_PRESSURE = CompositionOfAir.SKYLAB_TOTAL_AIR_PRESSURE_IN_KPA;//101.325D;
	
	/** Normal temperature (celsius) */
	//private static final double NORMAL_TEMP = 22.5D;
	// maximum & minimal acceptable temperature for living space (arbitrary)
	//private static final double MIN_TEMP = -15.0D;
	//private static final double MAX_TEMP = 45.0D;

	public static final int SUPPLY_DEMAND_REFRESH = 10;

	private static final int RESOURCE_UPDATE_FREQ = 50;

	private static final int SAMPLING_FREQ = 250; // in millisols

	public static final int NUM_CRITICAL_RESOURCES = 9;

	private static final int RESOURCE_STAT_SOLS = 12;

	private static final int SOL_SLEEP_PATTERN_REFRESH = 3;

    public static final int MIN_REGOLITH_RESERVE = 10; // per person

	public static final int MIN_SAND_RESERVE = 5; // per person

    public static final int MIN_ICE_RESERVE = 200; // per person

	public static final double MIN_WATER_RESERVE = 400D; // per person

	public static final double SAFETY_TEMPERATURE = 7;
	
	//public static final double SAFETY_PRESSURE = 20;
	
	public static double water_consumption;

	public static double minimum_air_pressure;

	/** Amount of time (millisols) required for periodic maintenance. */
	//private static final double MAINTENANCE_TIME = 1000D;

	/** The initial population of the settlement. */
	private int initialPopulation;
	private int initialNumOfRobots;
	private int scenarioID;
	private int solCache = 0;//, counter30 = 1; // NOTE: can't be static since each settlement needs to keep tracking of it
	private int numShift;
	private int numA; // number of people with work shift A
	private int numB; // number of people with work shift B
	private int numX; // number of people with work shift X
	private int numY; // number of people with work shift Y
	private int numZ; // number of people with work shift Z
	private int numOnCall;
	private int sumOfCurrentManuProcesses = 0;
	private int cropsNeedingTendingCache = 5;
	private int millisolCache = -5;
	private int numConnectorsCache = 0;
	
	/** Goods manager update time. */
	private double goodsManagerUpdateTime = 0D;

	private double currentTemperature = 22.5;
	
	private double currentPressure = NORMAL_AIR_PRESSURE; //[in kPa], not Pascal
	
	/** Amount of time (millisols) that the settlement has had zero population.  */
	private double zeroPopulationTime;

	public double mealsReplenishmentRate = 0.6;

	public double dessertsReplenishmentRate = 0.7;

	private double iceProbabilityValue = 0;
	
	private double regolithProbabilityValue = 0;

	private double oxygenProbabilityValue = 0;
	
	private double methaneProbabilityValue = 0;

	private double outside_temperature; 
	
	// 2014-11-23 Added foodProductionOverride
	private boolean foodProductionOverride = false;
	// private boolean reportSample = true;
	/** Override flag for mission creation at settlement. */
	private boolean missionCreationOverride = false;
	/** Override flag for manufacturing at settlement. */
	private boolean manufactureOverride = false;
	/** Override flag for resource process at settlement. */
	private boolean resourceProcessOverride = false;
	/* Override flag for construction/salvage mission creation at settlement.*/
	private boolean constructionOverride = false;
	/* Flag showing if the instance of Settlement has just been deserialized */
	public transient boolean justReloadedPeople = true;

	public transient boolean justReloadedRobots = true;

	private boolean[] exposed = {false, false, false};

	private double[][] life_support_value = new double[2][7];
	
	private String sponsor;

	private String objectiveName;
	/** The settlement template name. */
	private String template;

	private String name;
	
	private ObjectiveType objectiveType;

	private final static String[] objectiveArray = new String[]{
			Msg.getString("ObjectiveType.crop"),
			Msg.getString("ObjectiveType.manu"),
			Msg.getString("ObjectiveType.research"),
			Msg.getString("ObjectiveType.transportation"),
			Msg.getString("ObjectiveType.trade"),
			Msg.getString("ObjectiveType.tourism")
			};

	private final static ObjectiveType[] objectives = new ObjectiveType[]{
			ObjectiveType.CROP_FARM,
			ObjectiveType.MANUFACTURING,
			ObjectiveType.RESEARCH_CENTER,
			ObjectiveType.TRANSPORTATION_HUB,
			ObjectiveType.TRADE_TOWN,
			ObjectiveType.TOURISM
			};
	
	//private int[] resourceArray = new int[9];
	//private int[] solArray = new int[30];
	//private double[] samplePointArray = new double[(int)1000/RECORDING_FREQUENCY];
	//private Object[][][] resourceObject = new Object[][][]{resourceArray, samplePointArray, solArray};

	/** The settlement's building manager. */
	protected BuildingManager buildingManager;
	/** The settlement's building connector manager. */
	protected BuildingConnectorManager buildingConnectorManager;
	/** The settlement's goods manager. */
	protected GoodsManager goodsManager;
	/** The settlement's construction manager. */
	protected ConstructionManager constructionManager;
	/** The settlement's building power grid. */
	protected PowerGrid powerGrid;
	// 2014-10-17 Added heating system
	/** The settlement's heating system. */
	protected ThermalSystem thermalSystem;
	//private Inventory inv = getInventory();
	private ChainOfCommand chainOfCommand;

	private CompositionOfAir compositionOfAir;

	private Coordinates location; 
	
	/** The settlement's achievement in scientific fields. */
	private Map<ScienceType, Double> scientificAchievement;
	//private Map<Integer, Double> resourceMapCache = new HashMap<>();
	private Map<Integer, Map<Integer, List<Double>>> resourceStat = new HashMap<>();
	// 2016-12-21 Added allAssociatedPeople
	private Collection<Person> allAssociatedPeople = new ConcurrentLinkedQueue<Person>();
	// 2016-12-22 Added allAssociatedRobots
	private Collection<Robot> allAssociatedRobots = new ConcurrentLinkedQueue<Robot>();

	private Map<Building, List<Building>> adjacentBuildingMap = new HashMap<>();
	
	private DustStorm storm;
	
	// 2017-04-10 WARNING: cannot use static or result in null
	//public AmountResource foodAR = ResourceUtil.foodAR;
	//public AmountResource waterAR = ResourceUtil.waterAR;
	//public AmountResource oxygenAR = ResourceUtil.oxygenAR;
	//public AmountResource carbonDioxideAR = ResourceUtil.carbonDioxideAR;

	// Static members
	private static Simulation sim = Simulation.instance();
	private static UnitManager unitManager = sim.getUnitManager();
	private static MissionManager missionManager = sim.getMissionManager();
	private static Weather weather;// = sim.getMars().getWeather();
	private static MarsClock marsClock;// = sim.getMasterClock().getMarsClock();
	
	//private ShiftType currentShift;
	
	// Constructor 1 called by ConstructionStageTest
	private Settlement() {
		super(null, null);
		location = getCoordinates();
		unitManager = Simulation.instance().getUnitManager();
		// 2016-12-21 Call updateAllAssociatedPeople()
		updateAllAssociatedPeople();
		updateAllAssociatedRobots();
	}


	/**
	 * The static factory method called by ConstructionStageTest to return a new instance of Settlement for maven testing.
	 */
	public static Settlement createConstructionStage() {
		return new Settlement();
	}

	// Constructor 2 for maven testing. Called by MockSettlement
	// 2014-10-28 Added settlement id
	public Settlement(String name, int scenarioID, Coordinates location) {
		// Use Structure constructor.
		super(name, location);
		this.name = name;
		this.scenarioID = scenarioID;
		this.location = location;
		if (unitManager == null) // for passing maven test
			unitManager = Simulation.instance().getUnitManager();
		if (missionManager == null) // for passing maven test
			missionManager = Simulation.instance().getMissionManager();
	}

	/**
	 * The static factory method called by ConstructionStageTest to return a new instance of Settlement for maven testing.
	 */
	public static Settlement createMockSettlement(String name, int scenarioID, Coordinates location) {
		return new Settlement(name, scenarioID, location);
	}

	/*
	 * Constructor 3 for creating settlements.
	 * Called by UnitManager to create the initial settlement
	 * Called by ArrivingSettlement to create a brand new settlement
	 */
	// 2014-10-29 Added settlement id
	private Settlement(String name, int id, String template, String sponsor, Coordinates location, int populationNumber, int initialNumOfRobots) {
		// Use Structure constructor
		super(name, location);
		this.name = name;
		this.template = template;
		this.sponsor = sponsor;
		this.location = location;
		this.scenarioID = id;
		this.initialNumOfRobots = initialNumOfRobots;
		this.initialPopulation = populationNumber;

		marsClock = sim.getMasterClock().getMarsClock();
		weather = sim.getMars().getWeather();

		//inv = getInventory();
		unitManager = Simulation.instance().getUnitManager();
		SettlementConfig settlementConfig = SimulationConfig.instance().getSettlementConfiguration();
		
		PersonConfig personConfig = SimulationConfig.instance().getPersonConfiguration();
		water_consumption = personConfig.getWaterConsumptionRate();
        minimum_air_pressure = personConfig.getMinAirPressure();

        life_support_value = settlementConfig.loadLifeSupportRequirements();
        
		// 2016-12-21 Call updateAllAssociatedPeople()
		updateAllAssociatedPeople();
		updateAllAssociatedRobots();

		// Set inventory total mass capacity.
		getInventory().addGeneralCapacity(Double.MAX_VALUE); //10_000_000);//
		
		double max = 500;
		// 2017-05-24 initialize inventory of this building for resource storage 
		for (AmountResource ar : ResourceUtil.getInstance().getAmountResources()) {
			double resourceCapacity = getInventory().getAmountResourceRemainingCapacity(ar, true, false);
			if (resourceCapacity >= 0) {
				getInventory().addAmountResourceTypeCapacity(ar, max);	
				//double cap = getInventory().getAmountResourceCapacity(ar, false);
				//double storedCache = getInventory().getTotalAmountResourcesStoredCache(false);
				//System.out.println(ar.getName() + "'s cap : " + cap + "   storedCache : " + storedCache);
				
			}
		}

		// Initialize building manager
		buildingManager = new BuildingManager(this);
		// Initialize building connector manager.
		buildingConnectorManager = new BuildingConnectorManager(this);
		// Initialize goods manager.
		goodsManager = new GoodsManager(this);
		// Initialize construction manager.
		constructionManager = new ConstructionManager(this);
		// Initialize power grid
		powerGrid = new PowerGrid(this);
		// 2014-10-17 Added thermal control system
		thermalSystem = new ThermalSystem(this);
		// Initialize scientific achievement.
		scientificAchievement = new HashMap<ScienceType, Double>(0);

		chainOfCommand = new ChainOfCommand(this);
		// 2015-12-29 Added CompositionOfAir
		compositionOfAir = new CompositionOfAir(this);

		// 2017-03-29 Set objective()
		if (template.equals("Trading Outpost"))
				setObjective(ObjectiveType.TRADE_TOWN);
		else if (template.equals("Mining Outpost"))
			setObjective(ObjectiveType.MANUFACTURING);
		else
			setObjective(ObjectiveType.CROP_FARM);

		LogConsolidated.log(logger, Level.INFO, 0, sourceName,
				"[" + this + "] Set development objective to " + objectiveType.toString() 
				+ " (based upon the '" + template + "' Template).", null);
		

	}

	/**
	 * The static factory method called by UnitManager and ArrivingSettlement to create a brand new settlement
	 */
	public static Settlement createNewSettlement(String name, int id, String template, String sponsor, Coordinates location, int populationNumber, int initialNumOfRobots) {
		return new Settlement(name, id, template, sponsor, location, populationNumber, initialNumOfRobots);
	}

	/**
	 * Create a map of buildings with their lists of building connectors attached to it
	 * @return a map
	 */
	public Map<Building, List<Building>> createBuildingConnectionMap() {
		adjacentBuildingMap.clear();
		for (Building b : buildingManager.getBuildings()) {
			List<Building> connectors = createAdjacentBuildingConnectors(b);
			//if (b == null)
			//	System.out.println("b = null");
			adjacentBuildingMap.put(b, connectors);
		}
		
		return adjacentBuildingMap;
	}
	
	/**
	 * Gets a list of building connectors attached to this building
	 * @param building
	 * @return
	 */
	public List<Building> getBuildingConnectors(Building building) {
		if (!adjacentBuildingMap.containsKey(building))
			adjacentBuildingMap = createBuildingConnectionMap();

		return adjacentBuildingMap.get(building);
	}
	
	/**
	 * Creates a list of building connectors attached to this building
	 * @param building
	 * @return a list of building connectors
	 */
	public List<Building> createAdjacentBuildingConnectors(Building building) {
		List<Building> buildings = new ArrayList<>();
		//List<String> names = new ArrayList<>();
		Set<BuildingConnector> connectors = getConnectionsToBuilding(building);
		for (BuildingConnector c : connectors) {
			Building b1 = c.getBuilding1();
			Building b2 = c.getBuilding2();
			if (b1 != building) {
				buildings.add(b1);
			}
			else if (b2 != building) {
				buildings.add(b2);		
			}
/*			
			//if (b1.equals(building) {
			if (b1.getNickName().equals(building.getNickName())) {
				buildings.add(b2);
				//names.add(b2.getNickName());
			}
			else {
				buildings.add(b1);
				//names.add(b1.getNickName());
			}
*/			
			
		}
/*		
		System.out.println("size of " + buildings.size());
		if (buildings.size() == 0)
			System.out.println(building.getNickName() + " has no adjacent buildings.");
		else if (buildings.size() == 1)
			System.out.println(building.getNickName() + " <=> " + names.get(0));
		else if (buildings.size() == 2)
			System.out.println(names.get(0) + " <=> " + building.getNickName() + " <=> " + names.get(1));
*/
			return buildings;
	}
	
	
	public Set<BuildingConnector> getConnectionsToBuilding(Building building)  {
		return getBuildingConnectorManager().getConnectionsToBuilding(building);
	}
	

	/**
	 * Gets the settlement's meals replenishment rate.
	 * @return mealsReplenishmentRate
	 */
	// 2015-01-12 Added getMealsReplenishmentRate
	public double getMealsReplenishmentRate() {
		return mealsReplenishmentRate;
	}

	/**
	 * Sets the settlement's meals replenishment rate.
	 * @param rate
	 */
	// 2015-01-12 Added setMealsReplenishmentRate
	public void setMealsReplenishmentRate(double rate) {
		mealsReplenishmentRate = rate;
	}

	/**
	 * Gets the settlement's desserts replenishment rate.
	 * @return DessertsReplenishmentRate
	 */
	// 2015-01-12 Added getDessertsReplenishmentRate
	public double getDessertsReplenishmentRate() {
		return dessertsReplenishmentRate;
	}

	/**
	 * Sets the settlement's desserts replenishment rate.
	 * @param rate
	 */
	// 2015-01-12 Added setDessertsReplenishmentRate
	public void setDessertsReplenishmentRate(double rate) {
		dessertsReplenishmentRate = rate;
	}

	/**
	 * Gets the settlement template's unique ID.
	 * @return ID number.
	 */
	// 2014-10-29 Added settlement id
	public int getID() {
		return scenarioID;
	}

	/**
	 * Gets the population capacity of the settlement
	 * @return the population capacity
	 */
	public int getPopulationCapacity() {
		int result = 0;
		List<Building> bs = buildingManager.getBuildings(FunctionType.LIVING_ACCOMODATIONS);
		for (Building building : bs) {
			result += building.getLivingAccommodations().getBeds();
		}

		return result;
	}

	// 2016-01-12 Added getSleepers()
	public int getSleepers() {
		int result = 0;
		List<Building> bs = buildingManager.getBuildings(FunctionType.LIVING_ACCOMODATIONS);
		for (Building building : bs) {
			result += building.getLivingAccommodations().getSleepers();
		}

		return result;
	}

	// 2016-01-12 Added getDesignatedBeds()
	public int getTotalNumDesignatedBeds() {
		int result = 0;
		List<Building> bs = buildingManager.getBuildings(FunctionType.LIVING_ACCOMODATIONS);
		for (Building building : bs) {
			result += building.getLivingAccommodations().getBedMap().size();
		}

		return result;
	}

	/**
	 * Gets the current population number of the settlement
	 * @return the number of inhabitants
	 */
	public int getNumCurrentPopulation() {
		int n = 0;
		Iterator<Unit> i = getInventory().getAllContainedUnits().iterator();
		while (i.hasNext()) {
			if (i.next() instanceof Person)
				n++;
		}
		//for (Unit u : getInventory().getAllContainedUnits()) { // why java.lang.NullPointerException when adding a new arriving settlment ?
		//	if (u instanceof Person)
		//		n++;
		//}
		return n;
		//return getInhabitants().size();
	}

	/**
	 * Gets a collection of the inhabitants of the settlement.
	 * @return Collection of inhabitants
	 */
	public Collection<Person> getInhabitants() {
		return CollectionUtils.getPerson(getInventory().getContainedUnits());
	}

	/**
	 * Gets a collection of people who are doing EVA outside the settlement.
	 * @return Collection of people
	 */
	public Collection<Person> getOutsideEVAPeople() {

		return allAssociatedPeople
				.stream()
				.filter(p-> p.getLocationStateType() == LocationStateType.SETTLEMENT_VICINITY)
				.collect(Collectors.toList());
		
	}
	
	/**
	 * Gets the number of people currently doing EVA outside the settlement
	 * @return the available population capacity
	 */
	public int getNumOutsideEVAPeople() {
		Collection<Person> ppl = getOutsideEVAPeople();
		if (ppl.isEmpty() || ppl == null)
			return 0;
		else
			return ppl.size();
	}
	
	/**
	 * Gets the current available population capacity of the settlement
	 * @return the available population capacity
	 */
	public int getAvailablePopulationCapacity() {
		return getPopulationCapacity() - getNumCurrentPopulation();
	}

	/**
	 * Gets an array of current inhabitants of the settlement
	 * @return array of inhabitants
	 */
	public Person[] getInhabitantArray() {
		Collection<Person> people = getInhabitants();
		Person[] personArray = new Person[people.size()];
		Iterator<Person> i = people.iterator();
		int count = 0;
		while (i.hasNext()) {
			personArray[count] = i.next();
			count++;
		}
		return personArray;
	}

	/**
	 * Gets the robot capacity of the settlement
	 * @return the robot capacity
	 */
	public int getRobotCapacity() {
		int result = 0;
		int stations = 0;
		//Iterator<Building> i = buildingManager.getBuildings().iterator();//getACopyOfBuildings().iterator();.getACopyOfBuildings().iterator();
		//while (i.hasNext()) {
		//	Building building = i.next();
		//List<Building> bs = buildingManager.getBuildings();
		//for (Building b : bs) {
		//	result++;
		//}
		//result = bs.size();
		//Iterator<Building> j = buildingManager.getBuildings(FunctionType.ROBOTIC_STATION).iterator();
		//while (j.hasNext()) {
		//	Building building = j.next();
		List<Building> bs = buildingManager.getBuildings(FunctionType.ROBOTIC_STATION);
		for (Building b : bs) {
			stations += b.getRoboticStation().getSlots();
			// stations++;
		}
		// stations = stations * 2;

		result = result + stations;

		return result;
	}

	/**
	 * Gets the current number of robots in the settlement
	 * @return the number of robots
	 */
	public int getNumCurrentRobots() {
		int n = 0;
		for (Unit u : getInventory().getAllContainedUnits()) {
			if (u instanceof Robot)
				n++;
		}
		return n;
		//return getRobots().size();
	}

	/**
	 * Gets a collection of the number of robots of the settlement.
	 *
	 * @return Collection of robots
	 */
	public Collection<Robot> getRobots() {
		return CollectionUtils.getRobot(getInventory().getContainedUnits());
	}

	/**
	 * Gets the current available robot capacity of the settlement
	 * @return the available robots capacity
	 */
	public int getAvailableRobotCapacity() {
		return getRobotCapacity() - getNumCurrentRobots();
	}

	/**
	 * Gets an array of current robots of the settlement
	 * @return array of robots
	 */
	public Robot[] getRobotArray() {
		Collection<Robot> robots = getRobots();
		Robot[] robotArray = new Robot[robots.size()];
		Iterator<Robot> i = robots.iterator();
		int count = 0;
		while (i.hasNext()) {
			robotArray[count] = i.next();
			count++;
		}
		return robotArray;
	}

	/**
	 * Gets a collection of vehicles parked at the settlement.
	 *
	 * @return Collection of parked vehicles
	 */
	public Collection<Vehicle> getParkedVehicles() {
		return CollectionUtils.getVehicle(getInventory().getContainedUnits());
	}

	/**
	 * Gets the number of vehicles parked at the settlement.
	 * @return parked vehicles number
	 */
	public int getParkedVehicleNum() {
		return getParkedVehicles().size();
	}

	/**
	 * Returns true if life support is working properly and is not out of oxygen or water.
	 * @return true if life support is OK
	 * @throws Exception if error checking life support.
	 */
	public boolean lifeSupportCheck() {
		//boolean result = true;
        try {
			//if (AmountResource.oxygenAR == null)
				// 2016-08-27 Restructure for avoiding NullPointerException during maven test
			//	oxygenAR = LifeSupportType.oxygenAR;
			//if (oxygenAR == null) System.out.println("o2");
			if (getInventory().getARStored(ResourceUtil.oxygenID, false) <= 0D)
				return false;

			//if (AmountResource.waterAR == null)
				// 2016-08-27 Restructure for avoiding NullPointerException during maven test
			//	waterAR = LifeSupportType.waterAR;
			//if (waterAR == null) System.out.println("h2o");
			if (getInventory().getARStored(ResourceUtil.waterID, false) <= 0D)
				return false;


			// TODO: check against indoor air pressure
			double p = getAirPressure();
			//if (p <= minimum_air_pressure) {// 25331.25)// NORMAL_AIR_PRESSURE) ?
			//System.out.println("life_support_req[0][0] is " + life_support_req[0][0]);
			//System.out.println("life_support_req[1][0] is " + life_support_req[1][0]);
			//if (p < life_support_value[0][0] - SAFETY_PRESSURE || p > life_support_value[1][0] + SAFETY_PRESSURE) {	
			if (p > PhysicalCondition.MAXIMUM_AIR_PRESSURE || p <= minimum_air_pressure) { 	
				LogConsolidated.log(logger, Level.SEVERE, 5000, sourceName, this.getName() + " detected improper air pressure at " + Math.round(p *10D)/10D + " kPa", null);
				return false;
			}

			double t = currentTemperature;
			if (t < life_support_value[0][4] - SAFETY_TEMPERATURE || t > life_support_value[1][4] + SAFETY_TEMPERATURE) {
				LogConsolidated.log(logger, Level.SEVERE, 5000, sourceName, this.getName() + " detected out-of-range temperature at " + Math.round(p *10D)/10D + " C", null);
				return false;
			}
			// result = false;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
		return true;
		//return result;
	}

	/**
	 * Gets the number of people the life support can provide for.
	 * @return the capacity of the life support system
	 */
	public int getLifeSupportCapacity() {
		return getPopulationCapacity();
	}

	/**
	 * Gets oxygen from the inventory.
	 * @param amountRequested
	 *            the amount of oxygen requested [kg]
	 * @return the amount of oxygen actually received [kg]
	 * @throws Exception
	 *             if error providing oxygen.
	 */
	public double provideOxygen(double amountRequested) {
		double oxygenTaken = amountRequested;
		try {
			double oxygenLeft = getInventory().getARStored(ResourceUtil.oxygenID, false);
			//System.out.println("oxygenLeft : " + oxygenLeft);
			if (oxygenTaken > oxygenLeft)
				oxygenTaken = oxygenLeft;
			// Note: do NOT retrieve O2 here since calculateGasExchange() in CompositionOfAir
			// is doing it for all inhabitants once per frame.
			//getInventory().retrieveAmountResource(oxygenAR, oxygenTaken);
			//getInventory().addAmountDemandTotalRequest(oxygenAR);
			//getInventory().addAmountDemand(oxygenAR, oxygenTaken);

			double carbonDioxideProvided = oxygenTaken;
			double carbonDioxideCapacity = getInventory().getAmountResourceRemainingCapacity(ResourceUtil.carbonDioxideAR, true, false);
			if (carbonDioxideProvided > carbonDioxideCapacity)
				carbonDioxideProvided = carbonDioxideCapacity;
			// Note: do NOT store CO2 here since calculateGasExchange() in CompositionOfAir
			// is doing it for all inhabitants once per frame.
			//getInventory().storeAmountResource(carbonDioxideAR, carbonDioxideProvided, true);
			//getInventory().addAmountSupplyAmount(carbonDioxideAR, carbonDioxideProvided);
		}
        catch (Exception e) {
        	LogConsolidated.log(logger, Level.SEVERE, 5000, sourceName, name + " - Error in providing O2/removing CO2: " 
        			+ e.getMessage(), null);
        }

		return oxygenTaken;
	}

	/**
	 * Gets water from the inventory
	 *
	 * @param amountRequested
	 *            the amount of water requested [kg]
	 * @return the amount of water actually received [kg]
	 * @throws Exception
	 *             if error providing water.
	 */
	public double provideWater(double amountRequested) {
		double waterTaken = amountRequested;
		try {
			double waterLeft = getInventory().getARStored(ResourceUtil.waterID, false);
			if (waterTaken > waterLeft)
				waterTaken = waterLeft;
			//Storage.retrieveAnResource(waterTaken, waterAR, getInventory(), true);//, sourceName + "::provideWater");
			//getInventory().retrieveAmountResource(waterAR, waterTaken);
			//getInventory().addAmountDemandTotalRequest(waterAR);
			//getInventory().addAmountDemand(waterAR, waterTaken);
		}
	    catch (Exception e) {
	    	LogConsolidated.log(logger, Level.SEVERE, 5000, sourceName, name + " - Error in providing H2O needs: " + e.getMessage(), null);
	    }

		return waterTaken;
	}

	/**
	 * Computes the average air pressure of the life support system.
	 * @return air pressure [kPa]
	 */
	public double computeAveragePressure() {
		double total = 0;
		List<Building> buildings = buildingManager.getBuildingsWithLifeSupport();
		int size = buildings.size();
        for (Building b : buildings) { 
			int id = b.getInhabitableID();
			total += compositionOfAir.getTotalPressure()[id] ;
		}
		// convert from atm to kPascal
		return total * CompositionOfAir.KPA_PER_ATM / size;
/*		
		double total_area = 0, total_p_area = 0;
		List<Building> buildings = buildingManager.getBuildingsWithLifeSupport();
        for (Building b : buildings) { 
			int id = b.getInhabitableID();
			double area = b.getFloorArea();
			total_area += area;
			total_p_area += compositionOfAir.getTotalPressure()[id] * area;
		}
		// convert from atm to kPascal
		return total_p_area * CompositionOfAir.kPASCAL_PER_ATM / total_area;
*/		
	}

	/**
	 * Gets the air pressure of the life support system.
	 * @return air pressure [in kPa] (not Pa)
	 */
	public double getAirPressure() {
		return currentPressure;
	}

	public double getTemperature() {
		return currentTemperature;
	}
	
	/**
	 * Computes the average temperature in the settlement (from the life support system of all buildings)
	 * @return temperature (degrees C)
	 */
	public double computeAverageTemperature() {
		List<Building> buildings = buildingManager.getBuildingsWithThermal();
		int size = buildings.size();
		double total = 0;
        for (Building b : buildings) {
            total += b.getCurrentTemperature();
        }

        return total / size;
 /*
		List<Building> buildings = buildingManager.getBuildingsWithThermal();

		double total_t_area = 0, total_area = 0;
		
        for (Building b : buildings) {    
            double a = b.getFloorArea();
            total_area += a;
            total_t_area += b.getCurrentTemperature() * a;
        }

        return total_t_area / total_area;
 */
	}


	public ShiftType getCurrentSettlementShift() {
		
	    if (marsClock == null)
	    	marsClock = Simulation.instance().getMasterClock().getMarsClock();
	    
	    int millisols = marsClock.getMsol0();
	    
	    int num = getNumShift();
	    
	    if (num == 2) {
	        
			if (millisols >= TaskSchedule.A_START && millisols <= TaskSchedule.A_END)
				return ShiftType.A;

			else if (millisols >= TaskSchedule.B_START && millisols <= TaskSchedule.B_END)
				return ShiftType.B;

	    }
	    
	    else if (num == 3) {

			if (millisols >= TaskSchedule.X_START && millisols <= TaskSchedule.X_END)
				return ShiftType.X;

			else if (millisols >= TaskSchedule.Y_START && millisols <= TaskSchedule.Y_END)
				return ShiftType.Y;


			else if (millisols >= TaskSchedule.Z_START && millisols <= TaskSchedule.Z_END)
				return ShiftType.Z;

	    }
	    else 		
	    	return ShiftType.ON_CALL;
	    
		return null;

	}
	
	/**
	 * Perform time-related processes
	 *
	 * @param time
	 *            the amount of time passing (in millisols)
	 * @throws Exception
	 *     error during time passing.
	 */
	public void timePassing(double time) {
		
		//currentShift = getCurrentSettlementShift();
								
		//inv = getInventory();
/*
        int m = (int) marsClock.getMillisol();
        if (millisolCache != m) {
        	millisolCache = m;
    	    // 2016-10-28 Added cropsNeedingTendingCache
    		cropsNeedingTendingCache = getCropsNeedingTending();
        }
*/
		// If settlement is overcrowded, increase inhabitant's stress.
		// TODO: should the number of robots be accounted for here?

		int overCrowding = getNumCurrentPopulation() - getPopulationCapacity();
		if (overCrowding > 0) {
			double stressModifier = .1D * overCrowding * time;
			for (Person p : getInhabitants()) {
				PhysicalCondition c = p.getPhysicalCondition();
				c.setStress(c.getStress() + stressModifier);
			}
		}

		// TODO: what to take into consideration the presence of robots ?
		// If no current population at settlement for one sol, power down the
		// building and turn the heat off.
		//if (getNumCurrentPopulation() == 0) {
		//	zeroPopulationTime += time;
		//	if (zeroPopulationTime > 1000D) {
		//		if (powerGrid.getPowerMode() != PowerMode.POWER_DOWN)
		//			powerGrid.setPowerMode(PowerMode.POWER_DOWN);
		//		if (thermalSystem.getHeatMode() != HeatMode.HEAT_OFF)
		//			thermalSystem.setHeatMode(HeatMode.HEAT_OFF);
		//	}
		//} else {
			zeroPopulationTime = 0D;
			if (powerGrid.getPowerMode() != PowerMode.POWER_UP)
				powerGrid.setPowerMode(PowerMode.POWER_UP);
			// TODO: check if POWER_UP is necessary
			// Question: is POWER_UP a prerequisite of FULL_POWER ?
			// thermalSystem.setHeatMode(HeatMode.POWER_UP);
		//}

		powerGrid.timePassing(time);

		thermalSystem.timePassing(time);

		buildingManager.timePassing(time);

		// 2015-01-09 Added makeDailyReport()
		performEndOfDayTasks(); // NOTE: also update solCache in makeDailyReport()

		// Sample a data point every SAMPLE_FREQ (in millisols)
	    int millisols = marsClock.getMsol0();

	    int remainder = millisols % SAMPLING_FREQ ;
	    if (remainder == 0)
	    	if (millisols != 1000) // will NOT check for radiation at the exact 1000 millisols in order to balance the simulation load
	    		// take a sample for each critical resource
	    		sampleAllResources();

		// Check every RADIATION_CHECK_FREQ (in millisols)
	    // Compute whether a baseline, GCR, or SEP event has occurred
	    remainder = millisols % RadiationExposure.RADIATION_CHECK_FREQ ;
	    if (remainder == 5)
	    	//if (millisols != 1000) // will NOT check for radiation at the exact 1000 millisols in order to balance the simulation load
	    		checkRadiationProbability(time);

	    // Updates the goodsManager twice per sol at random time.
	    updateGoodsManager(time);

		// 2015-04-18 Added updateRegistry();
		// updateRegistry();

	    // 2015-12-29 Added CompositionOfAir
	    compositionOfAir.timePassing(time);

	    currentPressure = computeAveragePressure() ;
	    
	    currentTemperature = computeAverageTemperature();

		outside_temperature = weather.getTemperature(location);

	    remainder = millisols % RESOURCE_UPDATE_FREQ ;
	    if (remainder == 5) {
	    	iceProbabilityValue = computeIceProbability();
	    }

	    if (remainder == 10) {
	    	regolithProbabilityValue = computeRegolithProbability();
	    }

	    //if (remainder == 15) {
	    //	oxygenProbabilityValue = computeOxygenProbability();
	    //}

	    //if (remainder == 20) {
	    //	methaneProbabilityValue = computeMethaneProbability();
	    //}
	    
	    if (!adjacentBuildingMap.isEmpty()) {
			int numConnectors = adjacentBuildingMap.size();
		    
		    if (numConnectorsCache != numConnectors) {
		    	numConnectorsCache = numConnectors;
		    	createBuildingConnectionMap();
		    }
	    }
	    else {
	    	createBuildingConnectionMap();
	    }
		
	}

	public void sampleAllResources() {

        for (int i= 0; i < NUM_CRITICAL_RESOURCES; i++) {
        	sampleOneResource(i);
        }
	}

	public void sampleOneResource(int resourceType) {
		AmountResource resource = null;

		if (resourceType == 0) {
			resource = ResourceUtil.oxygenAR;//LifeSupportType.OXYGEN;
		}
		else if (resourceType == 1) {
			resource = ResourceUtil.hydrogenAR;//"hydrogen";
		}
		else if (resourceType == 2) {
			resource = ResourceUtil.carbonDioxideAR;//"carbon dioxide";
		}
		else if (resourceType == 3) {
			resource = ResourceUtil.methaneAR;//"methane";
		}
		else if (resourceType == 4) {
			resource = ResourceUtil.waterAR;//LifeSupportType.WATER;
		}
		else if (resourceType == 5) {
			resource = ResourceUtil.greyWaterAR;//"grey water";
		}
		else if (resourceType == 6) {
			resource = ResourceUtil.blackWaterAR;//"black water";
		}
		else if (resourceType == 7) {
			resource = ResourceUtil.rockSamplesAR;//"rock samples";
		}
		else if (resourceType == 8) {
			resource = ResourceUtil.iceAR;//"ice";
		}


		if (resourceStat.containsKey(solCache)) {
			Map<Integer, List<Double>> todayMap = resourceStat.get(solCache);

			if (todayMap.containsKey(resourceType)) {
				List<Double> list = todayMap.get(resourceType);
				double newAmount = getInventory().getAmountResourceStored(resource, false);
				list.add(newAmount);

			}

			else {
				List<Double> list = new ArrayList<>();
				double newAmount = getInventory().getAmountResourceStored(resource, false);
				list.add(newAmount);
				todayMap.put(resourceType, list);
			}

		} 
		
		else {
			List<Double> list = new ArrayList<>();
			Map<Integer, List<Double>> todayMap = new HashMap<>();
			double newAmount = getInventory().getAmountResourceStored(resource, false);
			list.add(newAmount);
			todayMap.put(resourceType, list);
			resourceStat.put(solCache, todayMap);
		}
	}


	/*
	 * Gets the average amount of a resource on a particular sol
	 */
	// Called by getOneResource() in MarqueeTicker.java
	public double getAverage(int solType, int resourceType) {
		int sol = 0;
		if (solType == 0) // today's average
			sol = solCache;
		else if (solType == -1) // yesterday's average
			sol = solCache - 1;
		else if (solType == -3) // average from 3 sols ago
			sol = solCache - 3;
		else if (solType == -10) // average from 10 sols ago
			sol = solCache - 10;

		int size = 0;
		double average = 0;

		if (resourceStat.containsKey(sol)) {
			Map<Integer, List<Double>> map = resourceStat.get(sol);
			//System.out.println("Sol " + solCache + " : yes to resourceStat.containsKey(" + sol + ")");
			//System.out.println("map.containsKey(resourceType) is " + map.containsKey(resourceType));
			if (map.containsKey(resourceType)) {
				List<Double> list = map.get(resourceType);
				//System.out.println("sol : " + solCache + "   solType : "  + solType + "   list is " + list);
				size = list.size();
				for (int i = 0; i < size; i++) {
					average += list.get(i);
					//System.out.println("list.get(i) is " + list.get(i));
				}

				//System.out.println("size is " + size + "   average is " + average);
				average = average/size;

			}
			else {
				average = 0; // how long will it be filled ? ?
			}

		}
		else
			average = 0;

		//if (size != 0)
		//	average = average/size;

		return average;
	}

	/*
	 * // 2015-04-18 updateRegistry() public void updateRegistry() {
	 *
	 * List<SettlementRegistry> settlementList =
	 * MultiplayerClient.getInstance().getSettlementRegistryList();
	 *
	 * int clientID = Integer.parseInt( st.nextToken().trim() );
	 *
	 * String template = st.nextToken().trim(); int pop = Integer.parseInt(
	 * st.nextToken().trim() ); int bots = Integer.parseInt(
	 * st.nextToken().trim() ); double lat = Double.parseDouble(
	 * st.nextToken().trim() ); double lo = Double.parseDouble(
	 * st.nextToken().trim() );
	 *
	 *
	 * settlementList.forEach( s -> { String pn = s.getPlayerName(); String sn =
	 * s.getName(); if (pn.equals(playerName) && sn.equals(name))
	 * s.updateRegistry(playerName, clientID, name, template, pop, bots, lat,
	 * lo); });
	 *
	 * }
	 */
	/**
	 * Provides the daily statistics on inhabitant's food energy intake
	 *
	 */
	// 2015-01-09 Added getFoodEnergyIntakeReport()
	public void getFoodEnergyIntakeReport() {
		// System.out.println("\n<<< Sol " + solCache + " End of Day Food Energy
		// Intake Report at " + this.getName() + " >>>");
		// System.out.println("** An settler on Mars is estimated to consume
		// about 10100 kJ per sol **");
		// Iterator<Person> i = getInhabitants().iterator();
		Iterator<Person> i = getAllAssociatedPeople().iterator();
		while (i.hasNext()) {
			Person p = i.next();
			PhysicalCondition condition = p.getPhysicalCondition();
			double energy = Math.round(condition.getEnergy() * 100.0) / 100.0;
			String name = p.getName();
			System.out.print(name + " : " + energy + " kJ" + "\t");
		}
	}

	/**
	 * Provides the daily reports for the settlement
	 */
	// 2015-01-09 Added makeDailyReport()
	public void performEndOfDayTasks() {
		// check for the passing of each day
		int solElapsed = marsClock.getMissionSol();
		if (solCache != solElapsed) {
			
			// getFoodEnergyIntakeReport();

			reassignWorkShift();

			refreshResourceStat();

			refreshSleepMap(solElapsed);

			//getSupplyDemandSampleReport(solElapsed);

			refreshDataMap(solElapsed);

			solCache = solElapsed;

		}
	}


	public void refreshResourceStat() {
		// Remove the resourceStat map data from 12 days ago
		if (resourceStat.size() > RESOURCE_STAT_SOLS)
			resourceStat.remove(0);
		//if (counter30 == 31) {
		//	resourceStat.remove(0);
			//resourceStat.clear();
			//resourceStat = new HashMap<>();
			//counter30--;
		//}
		//else
		//	counter30++;
	}
	/***
	 * Refreshes the sleep map for each person in the settlement
	 * @param solElapsed
	 */
	public void refreshSleepMap(int solElapsed) {
		// Update the sleep pattern once every x number of days
		if (solElapsed % SOL_SLEEP_PATTERN_REFRESH == 0) {
			Collection<Person> people = getInhabitants();
			for (Person p : people) {
				p.getCircadianClock().inflateSleepHabit();
			}
		}
	}

	/***
	 * Prints out the work shift status for this settlement
	 * @param sol
	 */
	public void printWorkShift(String sol) {
		logger.info(sol+ " " + getName() + "'s Work Shift " +  "-- A:" + numA + " B:" + numB
				+ ", X:" + numX + " Y:" + numY + " Z:" + numZ + ", OnCall:" + numOnCall);// + " Off:" + off);
	}

	/*
	 * Reassigns the work shift for all
	 */
	// TODO: should call this method at, say, 800 millisols, not right at 1000 millisols
	public void reassignWorkShift() {

		Collection<Person> people = getInhabitants();
		int pop = people.size();

		int nShift = 0;

		if (pop == 1) {
			nShift = 1;
		} else if (pop < UnitManager.THREE_SHIFTS_MIN_POPULATION) {
			nShift = 2;
		} else {// if pop => 6
			nShift = 3;
		}

		if (numShift != nShift)
			numShift = nShift;

		for (Person p : people) {

			if (!p.isBuried() || !p.isDeclaredDead() || !p.getPhysicalCondition().isDead()) {
				
				if (p.getMind().getMission() == null && p.isInSettlement()) {
	
					// 2015-12-05 Check if person is an astronomer.
		            boolean isAstronomer = (p.getMind().getJob() instanceof Astronomer);
	
					ShiftType oldShift = p.getTaskSchedule().getShiftType();
					
		            if (isAstronomer) {
		            	// TODO: find the darkest time of the day
		            	// and set work shift to cover time period
	
		            	// For now, we may assume it will usually be X or Z, but Y
		            	// since Y is usually where midday is at unless a person is at polar region.
	
		            	if (oldShift == ShiftType.Y) {
	
		            		boolean x_ok = isWorkShiftSaturated(ShiftType.X, false);
		            		boolean z_ok = isWorkShiftSaturated(ShiftType.Z, false);
	
			            	// TODO: Instead of throwing a dice,
		            		// take the shift that has less sunlight
		            		int rand;
		                	ShiftType newShift = null;
	
		            		if (x_ok && z_ok) {
		            			rand = RandomUtil.getRandomInt(1);
			            		if (rand == 0)
			            			newShift = ShiftType.X;
			            		else
			            			newShift = ShiftType.Z;
		            		}
		            		
		            		else if (x_ok)
		            			newShift = ShiftType.X;
		            		
		            		else if (z_ok)
		            			newShift = ShiftType.Z;
	
		            		p.setShiftType(newShift);
							//System.out.println(p + " old shift : " + oldShift + " new shift : " + newShift);
	
		            	}
	
		            } // end of if (isAstronomer)
	
		            else {
						// if a person's shift is saturated, he will need to change shift
						ShiftType newShift = getAnEmptyWorkShift(pop);
										
						int tendency = p.getTaskSchedule().getShiftChoice(newShift);
						// TODO: should find the person with the highest tendency to take this shift
	
						if (oldShift == ShiftType.ON_CALL) {
							// TODO: check a person's sleep habit map and request changing his work shift
							// to avoid taking a work shift that overlaps his sleep hour
			
							if (newShift != oldShift && tendency > 50) { // sanity check
								//System.out.println(this.getName() + "-- " + p + "'s old shift : " + oldShift + ",  new shift : " + newShift);
								p.setShiftType(newShift);
							}
						}
	
						else {
							// Note: if a person's shift is NOT saturated, he doesn't need to change shift
							boolean oldShift_ok = isWorkShiftSaturated(oldShift, true);
	
							// TODO: check a person's sleep habit map and request changing his work shift
							// to avoid taking a work shift that overlaps his sleep hour
	
							if (!oldShift_ok) {
						
								if (newShift != oldShift && tendency > 50) { // sanity check
									//System.out.println(this.getName() + "-- " + p + "'s old shift : " + oldShift + ",  new shift : " + newShift);
									p.setShiftType(newShift);
								}
							}
						}
		            } // end of if (isAstronomer)
				}
				// Just for sanity check for those on a vehicle mission
				// Note: shouldn't be needed this way but currently, when currently when starting a trade mission,
				// the code fails to change a person's work shift to On-call.
				else if (p.getMind().getMission() != null && p.isInVehicle()) {
					
					ShiftType oldShift = p.getTaskSchedule().getShiftType();
					
					if (oldShift != ShiftType.ON_CALL) {
						//System.out.println(p + " old shift : " + oldShift + " new shift : " + ShiftType.ON_CALL);
						p.setShiftType(ShiftType.ON_CALL);
					}
				}
				
			} // end of dead loop
		} // end of for loop
	}

	/**
	 * Provides the daily demand statistics on sample amount resources
	 */
	// 2015-01-15 Added supply data
	public void getSupplyDemandSampleReport(int solElapsed) {
		logger.info("<<< Sol " + solElapsed + " at " + this.getName()
				+ " End of Day Report of Amount Resource Supply and Demand Statistics >>>");

		String sample1 = "polyethylene";
		String sample2 = "concrete";

		// Sample supply and demand data on Potato and Water

		double supplyAmount1 = getInventory().getAmountSupplyAmount(sample1);
		double supplyAmount2 = getInventory().getAmountSupplyAmount(sample2);

		int supplyRequest1 = getInventory().getAmountSupplyRequest(sample1);
		int supplyRequest2 = getInventory().getAmountSupplyRequest(sample2);

		double demandAmount1 = getInventory().getAmountDemandAmount(sample1);
		double demandAmount2 = getInventory().getAmountDemandAmount(sample2);

		// int totalRequest1 = getInventory().getDemandTotalRequest(sample1);
		// int totalRequest2 = getInventory().getDemandTotalRequest(sample2);

		int demandSuccessfulRequest1 = getInventory().getAmountDemandMetRequest(sample1);
		int demandSuccessfulRequest2 = getInventory().getAmountDemandMetRequest(sample2);

		// int numOfGoodsInDemandAmountMap = getInventory().getDemandAmountMapSize();
		// int numOfGoodsInDemandTotalRequestMap =
		// getInventory().getDemandTotalRequestMapSize();
		// int numOfGoodsInDemandSuccessfulRequestMap =
		// getInventory().getDemandSuccessfulRequestMapSize();

		// logger.info(" numOfGoodsInDemandRequestMap : " +
		// numOfGoodsInDemandTotalRequestMap);
		// logger.info(" numOfGoodsInDemandSuccessfulRequestMap : " +
		// numOfGoodsInDemandSuccessfulRequestMap);
		// logger.info(" numOfGoodsInDemandAmountMap : " +
		// numOfGoodsInDemandAmountMap);

		logger.info(sample1 + " Supply Amount : " + Math.round(supplyAmount1 * 100.0) / 100.0);
		logger.info(sample1 + " Supply Request : " + supplyRequest1);

		logger.info(sample1 + " Demand Amount : " + Math.round(demandAmount1 * 100.0) / 100.0);
		// logger.info(sample1 + " Demand Total Request : " + totalRequest1);
		logger.info(sample1 + " Demand Successful Request : " + demandSuccessfulRequest1);

		logger.info(sample2 + " Supply Amount : " + Math.round(supplyAmount2 * 100.0) / 100.0);
		logger.info(sample2 + " Supply Request : " + supplyRequest2);

		logger.info(sample2 + " Demand Amount : " + Math.round(demandAmount2 * 100.0) / 100.0);
		// logger.info(sample2 + " Demand Total Request : " + totalRequest2);
		logger.info(sample2 + " Demand Successful Request : " + demandSuccessfulRequest2);

	}

	/**
	 * Refreshes and clears settlement's data on the supply/demand and weather
	 * @param solElapsed # of sols since the start of the sim
	 */
	// 2015-02-13 Added refreshMapDaily()
	public void refreshDataMap(int solElapsed) {
		// Clear maps once every x number of days
		if (solElapsed % SUPPLY_DEMAND_REFRESH == 0) {
			// True if solElapsed is an exact multiple of x
			// Carry out the daily average of the previous 5 days
			getInventory().compactAmountSupplyAmountMap(SUPPLY_DEMAND_REFRESH);
			getInventory().clearAmountSupplyRequestMap();
			// Carry out the daily average of the previous 5 days
			getInventory().compactAmountDemandAmountMap(SUPPLY_DEMAND_REFRESH);
			getInventory().clearAmountDemandTotalRequestMap();
			getInventory().clearAmountDemandMetRequestMap();
			// 2015-03-06 Added clearing of weather data map
			weather.clearMap();
			//logger.info(name + " : Compacted the settlement's supply demand data & cleared weather data.");
		}
	}

	/**
	 * Updates the GoodsManager twice per day
	 *
	 * @param time
	 */
	private void updateGoodsManager(double time) {

	    goodsManagerUpdateTime += time;

		// Randomly update goods manager twice per Sol.
	    double timeThreshold = 250D + RandomUtil.getRandomDouble(250D);
		if (!goodsManager.isInitialized() || (goodsManagerUpdateTime > timeThreshold)) {
		    goodsManager.timePassing(time);
		    goodsManagerUpdateTime = 0D;
		}
	}

	/**
	 * Gets a collection of people affected by this entity.
	 *
	 * @return person collection
	 */
	// TODO: will this method be called by robots?
	public Collection<Person> getAffectedPeople() {
		Collection<Person> people = new ConcurrentLinkedQueue<Person>(getInhabitants());

		// Check all people.
		Iterator<Person> i = unitManager.getPeople().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			Task task = person.getMind().getTaskManager().getTask();

			// Add all people maintaining this settlement.
			if (task instanceof Maintenance) {
				if (((Maintenance) task).getEntity() == this) {
					if (!people.contains(person))
						people.add(person);
				}
			}

			// Add all people repairing this settlement.
			if (task instanceof Repair) {
				if (((Repair) task).getEntity() == this) {
					if (!people.contains(person))
						people.add(person);
				}
			}
		}

		return people;
	}


    /**
     * Gets a collection of people who are available for social conversation in the same/another building
     * in the same/another settlement
     * @param initiator the initiator of this conversation
     * @param checkIdle true if the invitee is idling/relaxing (false if the invitee is in a chat)
     * @param sameBuilding true if the invitee is at the same building as the initiator (false if it doesn't matter)
     * @param allSettlements true if the collection includes all settlements (false if only the initiator's settlement)
     * @return person a collection of invitee(s)
     */
    // 2016-03-01 Added getChattingPeople()
    public Collection<Person> getChattingPeople(Person initiator, boolean checkIdle, boolean sameBuilding, boolean allSettlements) {
        Collection<Person> people = new ConcurrentLinkedQueue<Person>();
        Iterator<Person> i;
        // TODO: set up rules that allows

        if (allSettlements) {
        	// could be either radio (non face-to-face) conversation, don't care
        	i = unitManager.getPeople().iterator();
        	sameBuilding = false;
        }
        else {
        	// the only initiator's settlement
        	// may be radio or face-to-face conversation
        	i = getInhabitants().iterator();
        }

        while (i.hasNext()) {
            Person person = i.next();
            if (person.getLocationStateType() == LocationStateType.INSIDE_BUILDING
            	&& initiator.getLocationStateType() == LocationStateType.INSIDE_BUILDING) {
            	Task task = person.getMind().getTaskManager().getTask();

            	if (sameBuilding) {
            	// face-to-face conversation
                    if (initiator.getBuildingLocation().equals(person.getBuildingLocation())) {
                    	if (checkIdle) {
                    		if (task instanceof Relax
                    			//| task instanceof Read
                    			//| task instanceof Workout
                    			) {
                    			if (!person.equals(initiator))
                    				people.add(person);
                    		}
    		            }
    	                else {
    	               		if (task instanceof HaveConversation) {
    	               			//boolean isOff = person.getTaskSchedule().getShiftType().equals(ShiftType.OFF);
    	               			//if (isOff)
    	               				if (!person.equals(initiator))
    	               					people.add(person);
    	               		}
    	                }
                    }
	            }

	            else {
            	// may be radio (non face-to-face) conversation
                     //if (!initiator.getBuildingLocation().equals(person.getBuildingLocation())) {
                	if (checkIdle) {
                		if (task instanceof Relax
                			| task instanceof Read
                			| task instanceof Workout) {
                			if (!person.equals(initiator))
                				people.add(person);
                		}
		            }
	                else {
	               		if (task instanceof HaveConversation) {
	               			//boolean isOff = person.getTaskSchedule().getShiftType().equals(ShiftType.OFF);
	               			//if (isOff)
                			if (!person.equals(initiator))
	               				people.add(person);
	               		}
	                }
            	}
            }
        }

        return people;
    }


	/**
	 * Gets the settlement's building manager.
	 *
	 * @return building manager
	 */
	public BuildingManager getBuildingManager() {
		return buildingManager;
	}

	/**
	 * Gets the settlement's building connector manager.
	 *
	 * @return building connector manager.
	 */
	public BuildingConnectorManager getBuildingConnectorManager() {
		return buildingConnectorManager;
	}

	/**
	 * Gets the settlement's goods manager.
	 *
	 * @return goods manager
	 */
	public GoodsManager getGoodsManager() {
		return goodsManager;
	}

	/**
	 * Gets the closest available airlock to a person.
	 *
	 * @param person
	 *            the person.
	 * @return airlock or null if none available.
	 */
	public Airlock getClosestAvailableAirlock(Person person) {
		Airlock result = null;

		double leastDistance = Double.MAX_VALUE;
		BuildingManager manager = buildingManager;
		Iterator<Building> i = manager.getBuildings(FunctionType.EVA).iterator();
		while (i.hasNext()) {
			Building building = i.next();

			double distance = Point2D.distance(building.getXLocation(), building.getYLocation(), person.getXLocation(),
					person.getYLocation());
			if (distance < leastDistance) {
				//EVA eva = (EVA) building.getFunction(BuildingFunction.EVA);
				result = building.getEVA().getAirlock();
				leastDistance = distance;
			}
		}

		return result;
	}

	public Airlock getClosestAvailableAirlock(Robot robot) {
		Airlock result = null;

		double leastDistance = Double.MAX_VALUE;
		BuildingManager manager = buildingManager;
		Iterator<Building> i = manager.getBuildings(FunctionType.EVA).iterator();
		while (i.hasNext()) {
			Building building = i.next();

			double distance = Point2D.distance(building.getXLocation(), building.getYLocation(), robot.getXLocation(),
					robot.getYLocation());
			if (distance < leastDistance) {
				//EVA eva = (EVA) building.getFunction(BuildingFunction.EVA);
				result = building.getEVA().getAirlock();
				leastDistance = distance;
			}
		}

		return result;
	}

	/**
	 * Gets the closest available airlock at the settlement to the given
	 * location. The airlock must have a valid walkable interior path from the
	 * person's current location.
	 *
	 * @param person
	 *            the person.
	 * @param xLocation
	 *            the X location.
	 * @param yLocation
	 *            the Y location.
	 * @return airlock or null if none available.
	 */
	public Airlock getClosestWalkableAvailableAirlock(Person person, double xLocation, double yLocation) {
		Airlock result = null;
		Building currentBuilding = BuildingManager.getBuilding(person);

		if (currentBuilding == null) {
			//throw new IllegalStateException(person.getName() + " is not currently in a building.");	//throw new IllegalStateException(robot.getName() + " is not currently in a building.");
			// this major bug is due to getBuilding(robot) above in BuildingManager
			// what if a person is out there in ERV building for maintenance. ERV building has no LifeSupport function. currentBuilding will be null
			System.err.println(person.getName() + " is not currently in a building.");
			return null;
		}

		result = getAirlock(currentBuilding, xLocation, yLocation);

		return result;
	}

	public Airlock getClosestWalkableAvailableAirlock(Robot robot, double xLocation, double yLocation) {
		Airlock result = null;
		Building currentBuilding = BuildingManager.getBuilding(robot);

		if (currentBuilding == null) {
			//throw new IllegalStateException(robot.getName() + " is not currently in a building.");
			// this major bug is due to getBuilding(robot) above in BuildingManager
			// need to refine the concept of where a robot can go. They are thought to need RoboticStation function to "survive",
			// much like a person who needs LifeSupport function
			System.err.println(robot.getName() + " is not currently in a building.");
			return null;
		}

		result = getAirlock(currentBuilding, xLocation, yLocation);

		return result;
	}

	public Airlock getAirlock(Building currentBuilding, double xLocation, double yLocation) {
		Airlock result = null;

		double leastDistance = Double.MAX_VALUE;
		BuildingManager manager = buildingManager;
		Iterator<Building> i = manager.getBuildings(FunctionType.EVA).iterator();
		while (i.hasNext()) {
			Building building = i.next();

			if (buildingConnectorManager.hasValidPath(currentBuilding, building)) {

				double distance = Point2D.distance(building.getXLocation(), building.getYLocation(), xLocation,
						yLocation);
				if (distance < leastDistance) {
					//EVA eva = (EVA) building.getFunction(BuildingFunction.EVA);
					result = building.getEVA().getAirlock();
					leastDistance = distance;
				}
			}
		}
		return result;
	}

	/**
	 * Gets the closest available airlock at the settlement to the given
	 * location. The airlock must have a valid walkable interior path from the
	 * given building's current location.
	 *
	 * @param building
	 *            the building in the walkable interior path.
	 * @param xLocation
	 *            the X location.
	 * @param yLocation
	 *            the Y location.
	 * @return airlock or null if none available.
	 */
	public Airlock getClosestWalkableAvailableAirlock(Building building, double xLocation, double yLocation) {
		Airlock result = null;

		double leastDistance = Double.MAX_VALUE;
		BuildingManager manager = buildingManager;
		Iterator<Building> i = manager.getBuildings(FunctionType.EVA).iterator();
		while (i.hasNext()) {
			Building nextBuilding = i.next();

			if (buildingConnectorManager.hasValidPath(building, nextBuilding)) {

				double distance = Point2D.distance(nextBuilding.getXLocation(), nextBuilding.getYLocation(), xLocation,
						yLocation);
				if (distance < leastDistance) {
					EVA eva = nextBuilding.getEVA();
					if (eva != null) {
						result = eva.getAirlock();
						leastDistance = distance;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Checks if a building has a walkable path from it to an airlock.
	 *
	 * @param building
	 *            the building.
	 * @return true if an airlock is walkable from the building.
	 */
	public boolean hasWalkableAvailableAirlock(Building building) {
		return (getClosestWalkableAvailableAirlock(building, 0D, 0D) != null);
	}

	/**
	 * Gets the number of airlocks at the settlement.
	 *
	 * @return number of airlocks.
	 */
	public int getAirlockNum() {
		return buildingManager.getBuildings(FunctionType.EVA).size();
	}

	/**
	 * Gets the settlement's power grid.
	 *
	 * @return the power grid.
	 */
	public PowerGrid getPowerGrid() {
		return powerGrid;
	}

	/**
	 * Gets the settlement's heating system.
	 *
	 * @return thermalSystem.
	 */
	public ThermalSystem getThermalSystem() {
		return thermalSystem;
	}

	/**
	 * Gets the settlement template.
	 *
	 * @return template as string.
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * Gets all people associated with this settlement, even if they are out on
	 * missions.
	 *
	 * @return collection of associated people.
	 */
	public Collection<Person> getAllAssociatedPeople() {
		if (!justReloadedPeople)
		//if (!allAssociatedPeople.isEmpty())
			return allAssociatedPeople;

		else {
			//System.out.println("allAssociatedPeople.isEmpty() is true");
			// using java 8 stream
			return updateAllAssociatedPeople();
/*
			Collection<Person> result = new ConcurrentLinkedQueue<Person>();
			//if (unitManager == null) // for passing maven test
			//	unitManager = Simulation.instance().getUnitManager();
			Iterator<Person> i = unitManager.getPeople().iterator();
			while (i.hasNext()) {
				Person person = i.next();
				if (person.getAssociatedSettlement() == this)
					result.add(person);
			}
*/
		}

	}

	/**
	 * Updates all people associated with this settlement
	 * @return collection of associated people.
	 */
	// 2016-12-21 Added updateAllAssociatedPeople()
	public Collection<Person> updateAllAssociatedPeople() {
		//System.out.println("running updateAllAssociatedPeople()");
		//Collection<Person> result = new ConcurrentLinkedQueue<Person>();
		//if (unitManager == null) // NOT needed for passing maven test
		//	unitManager = Simulation.instance().getUnitManager();

		// using java 8 stream
		Collection<Person> result = unitManager.getPeople()
				.stream()
				.filter(p-> p.getAssociatedSettlement() == this)
				.collect(Collectors.toList());
/*
		Iterator<Person> i = unitManager.getPeople().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			if (person.getAssociatedSettlement() == this)
				result.add(person);
		}
*/
		allAssociatedPeople = result;
		justReloadedPeople = false;
		return result;
	}

	public void addPerson(Person p){
		allAssociatedPeople.add(p);
	}

	public void removePerson(Person p){
		allAssociatedPeople.remove(p);
	}

	public void addRobot(Robot r){
		allAssociatedRobots.add(r);
	}

	public void removeRobot(Robot r){
		allAssociatedRobots.remove(r);
	}

	/**
	 * Checks if the settlement has a particular person
	 * @param aPerson
	 * @return boolean
	 */
	//2015-12-01 Added hasPerson()
	public boolean hasPerson(Person aPerson) {
/*
		boolean result = false;
		Collection<Person> list = getAllAssociatedPeople();

		Iterator<Person> i = list.iterator();
		while (i.hasNext()) {
			Person person = i.next();
			if (person.equals(aPerson))
				result = true;
		}

		return result;
*/
 		return getAllAssociatedPeople()
				.stream()
				//.filter(p -> p.equals(aPerson))
				//.findFirst() != null;
				.anyMatch(p -> p.equals(aPerson));
	}

	/**
	 * Checks if the settlement contains a particular person with a given name (first or last)
	 * @param a person's first/last name
	 * @return 0 if none found, 1 if uniquely found, -1 if dead, 2 to n if not uniquely found

	//2015-12-01 Added hasPersonName()
	public int hasPersonName(String aName) {
		aName = aName.trim();
		String initial = null;
		boolean hasASpace = aName.contains(" ");
		int found = 0;
		int s_Index = 0;
		int dead = 0;

		int len = aName.length();
		boolean hasInitial = len > 3 && hasASpace;

		if (hasInitial) {
			for (int i = 0 ; i < len ; i++) {
		        if (aName.charAt(i) == ' ')
		        	s_Index = i;
			}

			if (s_Index == len-2) {
				// e.g. Cory_S
				initial = aName.substring(len-1, len);
				aName = aName.substring(0, len-2);
		    	//System.out.println("initial is " + initial);
		    	//System.out.println("aName is " + aName);
			}
			else if (s_Index == 1) {
				// e.g. S_Cory
				initial = aName.substring(0);
				aName = aName.substring(2, len);
		    	//System.out.println("initial is " + initial);
		    	//System.out.println("aName is " + aName);
			}
		}

		Collection<Person> list = getAllAssociatedPeople();

		Iterator<Person> i = list.iterator();
		while (i.hasNext()) {
			Person person = i.next();
			// Case 1: if aName is a full name
			if (hasASpace && person.getName().equalsIgnoreCase(aName)){
				found++;
				if (person.getPhysicalCondition().isDead())
					dead--;
			}
			else if (hasInitial) {
				// Case 2: if aName is a first name + space + last initial
				if (person.getName().toLowerCase().contains((aName + " " + initial).toLowerCase())) {
					found++;
				}
				// Case 3: if aName is a first initial + space + last name
				else if (person.getName().toLowerCase().contains((initial + " " + aName).toLowerCase())) {
					found++;
				}
			}
			else {
				String first = null;
				String last = null;
				String full = person.getName();
				int len1 = full.length();
				//int index1 = 0;

				for (int j = 0 ; j < len1 ; j++) {
			        if (full.charAt(j) == ' ') {
			        	//index1 = j;
				        first = full.substring(0, j);
				        last = full.substring(j+1, len1);
				        break;
			        }
				}


				// Case 4: if aName is a last name
				if (first.equalsIgnoreCase(aName)) {
					found++;
				}
				// Case 5: if aName is a first name
				else if (last.equalsIgnoreCase(aName)) {
					found++;
				}
			}
		}

		if (dead == -1)
			return -1;
		else
			return found;
	}
*/

	/**
	 * Returns a list of persons with a given name (first or last)
	 * @param aName a person's first/last name
	 * @return a list of persons
	 */
	//2015-12-21 Added returnPersonList()
	public List<Person> returnPersonList(String aName) {
		List<Person> personList = new ArrayList<>();
		aName = aName.trim();

		// 2016-06-15 Checked if "," is presented in  "last, first"
		if (aName.contains(", ")) {
			int index = aName.indexOf(",");
			String last = aName.substring(0, index);
			String first = aName.substring(index + 2, aName.length());
			aName = first + " " + last;
		}

		String initial = null;
		boolean hasASpace = aName.contains(" ");
		//int found = 0;
		int s_Index = 0;
		//int dead = 0;

		int len = aName.length();
		boolean hasInitial = len > 3 && hasASpace;

		if (hasInitial) {
			for (int i = 0 ; i < len ; i++) {
		        if (aName.charAt(i) == ' ')
		        	s_Index = i;
			}

			if (s_Index == len-2) {
				// e.g. Cory_S
				initial = aName.substring(len-1, len);
				aName = aName.substring(0, len-2);
		    	//System.out.println("initial is " + initial);
		    	//System.out.println("aName is " + aName);
			}
			else if (s_Index == 1) {
				// e.g. S_Cory
				initial = aName.substring(0);
				aName = aName.substring(2, len);
		    	//System.out.println("initial is " + initial);
		    	//System.out.println("aName is " + aName);
			}
		}

		Collection<Person> list = getAllAssociatedPeople();

		Iterator<Person> i = list.iterator();
		while (i.hasNext()) {
			Person person = i.next();
			// Case 1: if aName is a full name
			if (hasASpace && person.getName().equalsIgnoreCase(aName)){
				//found++;
				personList.add(person);
				//if (person.getPhysicalCondition().isDead())
					//dead--;
				//	personList.add(person);
			}
			else if (hasInitial) {
				// Case 2: if aName is a first name + space + last initial
				if (person.getName().toLowerCase().contains((aName + " " + initial).toLowerCase())) {
					//found++;
					personList.add(person);
				}
				// Case 3: if aName is a first initial + space + last name
				else if (person.getName().toLowerCase().contains((initial + " " + aName).toLowerCase())) {
					//found++;
					personList.add(person);
				}
			}
			else {
				String first = "";
				String last = "";
				String full = person.getName();
				int len1 = full.length();
				//int index1 = 0;

				for (int j = len1-1 ; j > 0 ; j--) {
					// Note: finding the whitespace from the end to 0 (from right to left) works better than from left to right
					// e.g. Mary L. Smith (last name should be "Smith", not "L. Smith"
			        if (full.charAt(j) == ' ') {
			        	//index1 = j;
				        first = full.substring(0, j);
				        last = full.substring(j+1, len1);
				        break;
			        }
			        else {
			        	first = full;
			        }
				}

				// Case 4: if aName is a last name
				if (first.equalsIgnoreCase(aName)) {
					//found++;
					personList.add(person);
				}

				// Case 5: if aName is a first name
				else if (last != null)
					if (last.equalsIgnoreCase(aName)) {
						//found++;
						personList.add(person);
				}
			}
		}


		return personList;
	}

	/**
	 * Checks if the settlement contains a bot
	 * @param a bot's name
	 * @return 0 if none found, 1 if uniquely found, 2 if uniquely found but dead, -1...-n if not uniquely found
	 *

	//2015-12-01 Added hasRobotName()
	public int hasRobotName(String aName) {
		aName = aName.trim();
		//boolean isFullName = aName.trim().contains(" ");
		aName = aName.replace(" ", "");
		int found = 0;
		int dead = 0;
		int size = aName.length();

		Collection<Robot> list = getAllAssociatedRobots();

		Iterator<Robot> i = list.iterator();
		while (i.hasNext()) {
			Robot robot = i.next();
			// Case 1: exact match e.g. chefbot001
			if (robot.getName().replace(" ", "").equalsIgnoreCase(aName)){
				found++;
				if (robot.getPhysicalCondition().isDead())
					dead--;
			}
			// Case 2: some parts are matched
			else {
				// Case 2 e.g. chefbot, chefbot0_, chefbot0__, chefbot1_, chefbot00_, chefbot01_
				// need more information !
				if (robot.getName().replace(" ", "").contains(aName)) {
					found++;
				}

				else { // Case 3 e.g. filter out semi-good names such as chefbot01, chefbot1,  chefbot10 from bad/invalid name

					String last4digits = aName.substring(size-4, size-1);
					char first = last4digits.charAt(0);
					char second = last4digits.charAt(1);
					char third = last4digits.charAt(2);
					char fourth = last4digits.charAt(3);

					boolean firstIsDigit = (first >= '0' && first <= '9');
					boolean secondIsDigit = (second >= '0' && second <= '9');
					boolean thirdIsDigit = (third >= '0' && third <= '9');
					boolean fourthIsDigit = (fourth >= '0' && fourth <= '9');

					// Case 3A : e.g. chefbot0003 -- a typo having an extra zero
					if (size >= 11 && !firstIsDigit && secondIsDigit && thirdIsDigit && fourthIsDigit) {
						if (first == '0' && second == '0') {
							aName = aName.substring(0, size-4) + aName.substring(size-3, size-1);
							int result = checkRobotName(robot, aName);
							if (result < 0)
								dead = -1;
							else
								found = result;
						}
					}

					// Case 3B : e.g. chefbot01 -- a typo missing a zero
					else if (size >= 10 && !firstIsDigit && !secondIsDigit && thirdIsDigit && fourthIsDigit) {
						aName = aName.substring(0, size-2) + "0" + aName.substring(size-2, size-1);
						int result = checkRobotName(robot, aName);
						if (result < 0)
							dead = -1;
						else
							found = result;
					}

					// Case 3C : e.g. chefbot1 -- a typo missing two zeroes
					else if (size >= 9 && !firstIsDigit && !secondIsDigit && !thirdIsDigit && fourthIsDigit) {
						aName = aName.substring(0, size-2) + "0" + aName.substring(size-2, size-1);
						int result = checkRobotName(robot, aName);
						if (result < 0)
							dead = -1;
						else
							found = result;
					}
				}

			}
		}
		if (dead < 0)
			return -1;
		else
			return found;
	}
*/
	/**
	 * Returns a list of robots containing a particular name
	 * @param aName bot's name
	 * @return a list of robots
	 *
	 */
	//2015-12-21 Added returnRobotList()
	public List<Robot> returnRobotList(String aName) {
		List<Robot> robotList = new ArrayList<>();
		aName = aName.trim();
		aName = aName.replace(" ", "");

		String last4digits = null;
		char first = '?';
		char second = '?';
		char third = '?';
		char fourth = '?';

		boolean firstIsDigit = false;
		boolean secondIsDigit = false;
		boolean thirdIsDigit = false;
		boolean fourthIsDigit = false;

		int size = aName.length();

		if (size >= 8) {
			last4digits = aName.substring(size-4, size);
			first = last4digits.charAt(0);
			second = last4digits.charAt(1);
			third = last4digits.charAt(2);
			fourth = last4digits.charAt(3);

			//System.out.println("The last 4 are : " + first + second + third + fourth);

			firstIsDigit = (first >= '0' && first <= '9');
			secondIsDigit = (second >= '0' && second <= '9');
			thirdIsDigit = (third >= '0' && third <= '9');
			fourthIsDigit = (fourth >= '0' && fourth <= '9');
		}

		Collection<Robot> list = getAllAssociatedRobots();

		Iterator<Robot> i = list.iterator();
		while (i.hasNext()) {
			Robot robot = i.next();
			// Case 1: exact match e.g. chefbot001
			if (robot.getName().replace(" ", "").equalsIgnoreCase(aName)){
				robotList.add(robot);
			}
			// Case 2: some parts are matched
			else {
				// Case 2 e.g. chefbot, chefbot0_, chefbot0__, chefbot1_, chefbot00_, chefbot01_
				// need more information !
				if (robot.getName().replace(" ", "").toLowerCase().contains(aName.toLowerCase())) {
					//System.out.println("aName is a part of " + robot.getName().replace(" ", ""));
					robotList.add(robot);
				}

				else if (size >= 8) { // Case 3 e.g. filter out semi-good names such as chefbot01, chefbot1,  chefbot10 from bad/invalid name

					// Case 3A : e.g. chefbot0003 -- a typo having an extra zero before the digit
					if (size >= 11 && !firstIsDigit && secondIsDigit && thirdIsDigit && fourthIsDigit) {
						if (first == '0' && second == '0') {
							String newName = aName.substring(0, size-4) + aName.substring(size-3, size);
							//System.out.println("Case 3A: newName is : " + newName);
							boolean result = checkRobotName(robot, newName);
							if (result)
								robotList.add(robot);
						}
					}

					// Case 3B : e.g. chefbot01 or chefbot11 -- a typo missing a zero before the digit
					else if (size >= 9 && !firstIsDigit && !secondIsDigit && thirdIsDigit && fourthIsDigit) {
						String newName = aName.substring(0, size-2) + "0" + aName.substring(size-2, size);
						//System.out.println("Case 3B: newName is : " + newName);
						boolean result = checkRobotName(robot, newName);
						if (result)
							robotList.add(robot);
					}

					// Case 3C : e.g. chefbot1 -- a typo missing two zeroes before the digit
					else if (size >= 8 && !firstIsDigit && !secondIsDigit && !thirdIsDigit && fourthIsDigit) {
						String newName = aName.substring(0, size-1) + "00" + aName.substring(size-1, size);
						//System.out.println("Case 3C: newName is : " + newName);
						boolean result = checkRobotName(robot, newName);
						if (result)
							robotList.add(robot);
					}
				}

			}
		}

		//System.out.println("robotList's size is " + robotList.size());
		return robotList;
	}

	/**
	 * Checks against the input name with the robot name
	 * @param robot
	 * @param aName
	 * @return
	 */
	// 2015-12-16 Added checkRobotName()
	public boolean checkRobotName(Robot robot, String aName) {

		//System.out.println("modified aName is " + aName);
		//aName = aName.substring(0, size-2) + "0" + aName.substring(size-2, size-1);
		if (robot.getName().replace(" ", "").equalsIgnoreCase(aName)){
			return true;
		}
		else
			return false;
	}

	/**
	 * Gets all Robots associated with this settlement, even if they are out on missions.
	 * @return collection of associated Robots.
	 */
	public Collection<Robot> getAllAssociatedRobots() {
		if (!justReloadedRobots)
		//if (!allAssociatedPeople.isEmpty())
			return allAssociatedRobots;

		else {
			//System.out.println("allAssociatedPeople.isEmpty() is true");
			// using java 8 stream
			return updateAllAssociatedRobots();
		}
/*
		Collection<Robot> result = new ConcurrentLinkedQueue<Robot>();
		Iterator<Robot> i = unitManager.getRobots().iterator();
		while (i.hasNext()) {
			Robot robot = i.next();
			if (robot.getAssociatedSettlement() == this)
				result.add(robot);
		}

		return result;
*/
	}

	/**
	 * Updates all robots associated with this settlement
	 * @return collection of associated robots.
	 */
	// 2016-12-22 Added updateAllAssociatedRobots()
	public Collection<Robot> updateAllAssociatedRobots() {
		// using java 8 stream
		Collection<Robot> result = unitManager.getRobots()
				.stream()
				.filter(p-> p.getAssociatedSettlement() == this)
				.collect(Collectors.toList());

		allAssociatedRobots = result;
		justReloadedRobots = false;
		return result;
	}

	/**
	 * Gets all vehicles currently on mission and are associated with this settlement.
	 *
	 * @return collection of vehicles on mission.
	 */
	public Collection<Vehicle> getMissionVehicles() {
		Collection<Vehicle> result = new ArrayList<Vehicle>();

		Iterator<Mission> i = missionManager.getMissionsForSettlement(this).iterator();
		while (i.hasNext()) {
			Mission mission = i.next();
			if (mission instanceof VehicleMission) {
				Vehicle vehicle = ((VehicleMission) mission).getVehicle();
				if ((vehicle != null) && !this.equals(vehicle.getSettlement()))
					result.add(vehicle);
			}
		}

		return result;
	}

	/**
	 * Gets all vehicles associated with this settlement, even if they are out
	 * on missions.
	 *
	 * @return collection of associated vehicles.
	 */
	public Collection<Vehicle> getAllAssociatedVehicles() {
		Collection<Vehicle> result = getParkedVehicles();
		if (missionManager == null) // needed for passing maven test
			missionManager = Simulation.instance().getMissionManager();
		// Also add vehicle mission vehicles not parked at settlement.
		List<Mission> missions = missionManager.getMissionsForSettlement(this);
		if (!missions.isEmpty()) {
			Iterator<Mission> i = missions.iterator();
			while (i.hasNext()) {
				Mission mission = i.next();
				if (mission instanceof VehicleMission) {
					Vehicle vehicle = ((VehicleMission) mission).getVehicle();
					if ((vehicle != null) && !this.equals(vehicle.getSettlement()))
						result.add(vehicle);
				}
			}
		}

		return result;
	}

	// 2016-10-06 Created getLUVs()
	public Collection<Vehicle> getLUVs(int mode) {
		Collection<Vehicle> LUVs = new ArrayList<Vehicle>();

		if (mode == 0 || mode == 1) {
			Collection<Vehicle> parked = getParkedVehicles();
			Iterator<Vehicle> i = parked.iterator();
			while (i.hasNext()) {
				Vehicle vehicle = i.next();
				if (vehicle instanceof LightUtilityVehicle) {
					LUVs.add(vehicle);
				}
			}
		}

		if (mode == 0 || mode == 2) {
			Collection<Vehicle> onMission = getMissionVehicles();
			Iterator<Vehicle> j = onMission.iterator();
			while (j.hasNext()) {
				Vehicle vehicle = j.next();
				if (vehicle instanceof LightUtilityVehicle) {
					LUVs.add(vehicle);
				}
			}
		}
		return LUVs;
	}

	// 2016-10-06 Created getCargoRovers()
	public List<Vehicle> getCargoRovers(int mode) {

		List<Vehicle> rovers = new ArrayList<Vehicle>();

		if (mode == 0 || mode == 1) {
			Collection<Vehicle> parked = getParkedVehicles();
			Iterator<Vehicle> i = parked.iterator();
			while (i.hasNext()) {
				Vehicle vehicle = i.next();
				//if (vehicle instanceof Rover) {
					String d = vehicle.getVehicleType();
					//System.out.println("type is " + d);
					if (d.equals(VehicleType.CARGO_ROVER.getName()))
						rovers.add(vehicle);
				//}
			}
		}

		if (mode == 0 || mode == 2) {
			Collection<Vehicle> onMission = getMissionVehicles();
			Iterator<Vehicle> j = onMission.iterator();
			while (j.hasNext()) {
				Vehicle vehicle = j.next();
				//if (vehicle instanceof Rover) {
					String d = vehicle.getVehicleType();
					if (d.equals(VehicleType.CARGO_ROVER.getName()))
						rovers.add(vehicle);
				//}
			}
		}
		return rovers;
	}

	// 2016-10-06 Created getTransportRovers()
	public List<Vehicle> getTransportRovers(int mode) {
		List<Vehicle> rovers = new ArrayList<Vehicle>();

		if (mode == 0 || mode == 1) {
			Collection<Vehicle> parked = getParkedVehicles();
			Iterator<Vehicle> i = parked.iterator();
			while (i.hasNext()) {
				Vehicle vehicle = i.next();
				//if (vehicle instanceof Rover) {
					String d = vehicle.getVehicleType();
					//System.out.println("type is " + d);
					if (d.equals(VehicleType.TRANSPORT_ROVER.getName()))
						rovers.add(vehicle);
				//}
			}
		}

		if (mode == 0 || mode == 2) {
			Collection<Vehicle> onMission = getMissionVehicles();
			Iterator<Vehicle> j = onMission.iterator();
			while (j.hasNext()) {
				Vehicle vehicle = j.next();
				//if (vehicle instanceof Rover) {
					String d = vehicle.getVehicleType();
					if (d.equals(VehicleType.TRANSPORT_ROVER.getName()))
						rovers.add(vehicle);
				//}
			}
		}
		return rovers;
	}

	// 2016-10-06 Created getExplorerRovers()
	public List<Vehicle> getExplorerRovers(int mode) {
		List<Vehicle> rovers = new ArrayList<Vehicle>();

		if (mode == 0 || mode == 1) {
			Collection<Vehicle> parked = getParkedVehicles();
			Iterator<Vehicle> i = parked.iterator();
			while (i.hasNext()) {
				Vehicle vehicle = i.next();
				//if (vehicle instanceof Rover) {
					String d = vehicle.getVehicleType();
					//System.out.println("type is " + d);
					if (d.equals(VehicleType.EXPLORER_ROVER.getName()))
						rovers.add(vehicle);
				//}
			}
		}

		if (mode == 0 || mode == 2) {
			Collection<Vehicle> onMission = getMissionVehicles();
			Iterator<Vehicle> j = onMission.iterator();
			while (j.hasNext()) {
				Vehicle vehicle = j.next();
				//if (vehicle instanceof Rover) {
					String d = vehicle.getVehicleType();
					if (d.equals(VehicleType.EXPLORER_ROVER.getName()))
						rovers.add(vehicle);
				//}
			}
		}
		return rovers;
	}

	/**
	 * Sets the mission creation override flag.
	 *
	 * @param missionCreationOverride
	 *            override for settlement mission creation.
	 */
	public void setMissionCreationOverride(boolean missionCreationOverride) {
		this.missionCreationOverride = missionCreationOverride;
	}

	/**
	 * Gets the mission creation override flag.
	 *
	 * @return override for settlement mission creation.
	 */
	public boolean getMissionCreationOverride() {
		return missionCreationOverride;
	}

	/**
	 * Sets the construction override flag.
	 *
	 * @param constructionOverride
	 *            override for settlement construction/salvage mission creation.
	 */
	public void setConstructionOverride(boolean constructionOverride) {
		this.constructionOverride = constructionOverride;
	}

	/**
	 * Gets the construction override flag.
	 *
	 * @return override for settlement construction mission creation.
	 */
	public boolean getConstructionOverride() {
		return constructionOverride;
	}

	/**
	 * Sets the FoodProduction override flag.
	 *
	 * @param foodProductionOverride override for FoodProduction.
	 */
	public void setFoodProductionOverride(boolean foodProductionOverride) {
		this.foodProductionOverride = foodProductionOverride;
	}

	/**
	 * Gets the FoodProduction override flag.
	 *
	 * @return override for settlement FoodProduction.
	 */
	public boolean getFoodProductionOverride() {
		return foodProductionOverride;
	}

	/**
	 * Sets the manufacture override flag.
	 *
	 * @param manufactureOverride
	 *            override for manufacture.
	 */
	public void setManufactureOverride(boolean manufactureOverride) {
		this.manufactureOverride = manufactureOverride;
	}

	/**
	 * Gets the manufacture override flag.
	 *
	 * @return override for settlement manufacture.
	 */
	public boolean getManufactureOverride() {
		return manufactureOverride;
	}

	/**
	 * Sets the resource process override flag.
	 *
	 * @param resourceProcessOverride
	 *            override for resource processes.
	 */
	public void setResourceProcessOverride(boolean resourceProcessOverride) {
		this.resourceProcessOverride = resourceProcessOverride;
	}

	/**
	 * Gets the resource process override flag.
	 *
	 * @return override for settlement resource processes.
	 */
	public boolean getResourceProcessOverride() {
		return resourceProcessOverride;
	}

	/**
	 * Gets the settlement's construction manager.
	 *
	 * @return construction manager.
	 */
	public ConstructionManager getConstructionManager() {
		return constructionManager;
	}

	/**
	 * Gets the settlement's achievement credit for a given scientific field.
	 *
	 * @param science
	 *            the scientific field.
	 * @return achievement credit.
	 */
	public double getScientificAchievement(ScienceType science) {
		double result = 0D;

		if (scientificAchievement.containsKey(science))
			result = scientificAchievement.get(science);

		return result;
	}

	/**
	 * Gets the settlement's total scientific achievement credit.
	 *
	 * @return achievement credit.
	 */
	public double getTotalScientificAchievement() {
		double result = 0D;

		Iterator<Double> i = scientificAchievement.values().iterator();
		while (i.hasNext())
			result += i.next();

		return result;
	}

	/**
	 * Add achievement credit to the settlement in a scientific field.
	 * @param achievementCredit  the achievement credit.
	 * @param science the scientific field.
	 */
	public void addScientificAchievement(double achievementCredit, ScienceType science) {
		if (scientificAchievement.containsKey(science))
			achievementCredit += scientificAchievement.get(science);

		scientificAchievement.put(science, achievementCredit);
	}

	/**
	 * Gets the initial population of the settlement.
	 * @return initial population number.
	 */
	public int getInitialPopulation() {
		return initialPopulation;
	}

	/**
	 * Gets the initial number of robots the settlement.
	 * @return initial number of robots.
	 */
	public int getInitialNumOfRobots() {
		return initialNumOfRobots;
	}


	/**
	 * Returns the chain of command
	 * @return chainOfCommand
	 */
	public ChainOfCommand getChainOfCommand() {
		return chainOfCommand;
	}

	/**
	 * Decrements a particular shift type
	 * @param shiftType
	 */
	public void decrementShiftType(ShiftType shiftType) {

		if (shiftType.equals(ShiftType.A)) {
			numA--;
		}

		else if (shiftType.equals(ShiftType.B)) {
			numB--;
		}

		else if (shiftType.equals(ShiftType.X)) {
			numX--;
		}

		else if (shiftType.equals(ShiftType.Y)) {
			numY--;
		}

		else if (shiftType.equals(ShiftType.Z)) {
			numZ--;
		}
		else if (shiftType.equals(ShiftType.ON_CALL)) {
			numOnCall--;
		}
	}


	/**
	 * Increments a particular shift type
	 * @param shiftType
	 */
	public void incrementShiftType(ShiftType shiftType) {

		if (shiftType.equals(ShiftType.A)) {
			numA++;
		}

		else if (shiftType.equals(ShiftType.B)) {
			numB++;
		}

		else if (shiftType.equals(ShiftType.X)) {
			numX++;
		}

		else if (shiftType.equals(ShiftType.Y)) {
			numY++;
		}

		else if (shiftType.equals(ShiftType.Z)) {
			numZ++;
		}
		
		else if (shiftType.equals(ShiftType.ON_CALL)) {
			numOnCall++;
		}
	}

 
	/**
	 * Checks if a particular work shift has been saturated
	 * @param st The ShiftType
	 * @param inclusiveChecking
	 * @return true/false
	 */
	// 2015-12-05 isWorkShiftSaturated
	public boolean isWorkShiftSaturated(ShiftType st, boolean inclusiveChecking) {
		boolean result = false;

		// Reduce the shiftType of interest to find out if it's saturated
		if (inclusiveChecking)
			decrementShiftType(st);

		int pop = getNumCurrentPopulation();
		int quotient = pop / numShift;
		int remainder = pop % numShift;

		switch (numShift) {
			case 1: // (numShift == 1)
				if (st != ShiftType.ON_CALL)
					result = false;
				break;
			case 2: // else if (numShift == 2) {

				switch (remainder) {
					case 0: // if (remainder == 0) {
						if (numA < quotient && numB < quotient) {
							result = true;
							break;
						}
						if (quotient == 1) {
							if (numA < 1) { // allow only 1 person with "A shift"
								if (st == ShiftType.A)
									result = true;
							} else {
								if (st == ShiftType.B)
									result = true;
							}
						}
						else if (quotient == 2) {
							if (numA < 2) { // allow 2 persons with "A shift"
								if (st == ShiftType.A)
									result = true;
							} else {
								if (st == ShiftType.B)
									result = true;
							}
						}
						break;
					case 1: // else { //if (remainder == 1) {
						if (numA < quotient && numB < quotient) {
							result = true;
							break;
						}
						if (quotient == 1) {
							if (numA < 2) { // allow 1 person with "A shift"
								if (st == ShiftType.A)
									result = true;
							} else {
								if (st == ShiftType.B)
									result = true;
							}
						}
						else if (quotient == 2) {
							if (numA < 3) { // allow 2 persons with "A shift"
								if (st == ShiftType.A)
									result = true;
							} else {
								if (st == ShiftType.B)
									result = true;
							}
						}
						break;
					} // end of switch (remainder)
					break;
			case 3: // else if (numShift == 3) {
				switch (remainder) {
					case 0: // if (remainder == 0) {
						if (numX < quotient && numY < quotient && numZ < quotient) {
							result = true;
							break;
						}
						if (numX < quotient + 1) { // allow up to q persons with "X shift"
							if (st == ShiftType.X)
								result = true;
						} else if (numY < quotient + 1) { // allow up to q persons with  "Y shift"
							if (st == ShiftType.Y)
								result = true;
						} else {
							if (st == ShiftType.Z)
								result = true;
						}
						break;
					case 1: // else if (remainder == 1) {
						if (numX < quotient && numY < quotient && numZ < quotient) {
							result = true;
							break;
						}
						if (numX < quotient + 1) { // allow up to q persons with "X shift"
							if (st == ShiftType.X)
								result = true;
						}
						else if (numY < quotient + 2) { // allow up to q + 1 persons  with "Y shift"
							if (st == ShiftType.Y)
								result = true;
						}
						else {
							if (st == ShiftType.Z)
								result = true;
						}
						break;
					case 2: // else {//if (remainder == 2) {
						if (numX < quotient && numY < quotient && numZ < quotient) {
							result = true;
							break;
						}
						if (numX < quotient + 2) { // allow up to q+1 persons with "X										// shift"
							if (st == ShiftType.X)
								result = true;
						}
						else if (numY < quotient + 2) { // allow up to q+1 persons with "Y shift"
							if (st == ShiftType.Y)
								result = true;
						}
						else {
							if (st == ShiftType.Z)
								result = true;
						}
						break;
					} // end of switch for case 3
				break;
			} // end of switch

		// fill back the shiftType of interest
		if (inclusiveChecking)
			incrementShiftType(st);

		return result;
	}

	/**
	 * Finds an empty work shift in a settlement
	 * @param pop If it wasn't known, use -1 to obtain the latest figure
	 * @return The ShifType
	 */
	// 2015-11-01 Edited getAEmptyWorkShift
	public ShiftType getAnEmptyWorkShift(int population) {
		int pop = 0;
		if (population == -1)
			pop = getNumCurrentPopulation();
		else
			pop = population;

		int rand = -1;
		ShiftType shiftType = ShiftType.OFF;
		int quotient = pop / numShift;
		int remainder = pop % numShift;
		
		int limX = 0;
		int limY = 0;
		int limZ = 0;

		if (pop >= 12) {
			limX = quotient -1;
			if (remainder == 0) 
				limZ = limX - 1;
			else
				limZ = limX;
		}
		else {
			limX = 2;	
			limZ = 2;		
		}
	
		limY = pop - limX - limZ;
		
		switch (numShift) {

			case 1: // for numShift = 1
				shiftType = ShiftType.ON_CALL;
			break;
			
			case 2: // for umShift = 2
				switch (remainder) {
					case 0: // if (remainder == 0) {
						if (numA < quotient && numB < quotient) {
							rand = RandomUtil.getRandomInt(1);
							if (rand == 0) {
								shiftType = ShiftType.A;
							} else if (rand == 1) {
								shiftType = ShiftType.B;
							}
							break;
						}
						if (quotient == 1) {
							if (numA < 1) { // allow only 1 person with "A shift"
								shiftType = ShiftType.A;
							} else {
								shiftType = ShiftType.B;
							}
							break;
						}
						else if (quotient == 2) {
							if (numA < 2) { // allow 2 persons with "A shift"
								shiftType = ShiftType.A;
							} else {
								shiftType = ShiftType.B;
							}
							break;
						}
					break;
					
					case 1: // else { //if (remainder == 1) {
						if (numA < quotient && numB < quotient) {
							rand = RandomUtil.getRandomInt(1);
							if (rand == 0) {
								shiftType = ShiftType.A;
							} else if (rand == 1) {
								shiftType = ShiftType.B;
							}
							break;
						}
						if (quotient == 1) {
							if (numA < 2) { // allow 1 person with "A shift"
								shiftType = ShiftType.A;
							} else {
								shiftType = ShiftType.B;
							}
							break;
						}
						else if (quotient == 2) {
							if (numA < 3) { // allow 2 persons with "A shift"
								shiftType = ShiftType.A;
							} else {
								shiftType = ShiftType.B;
							}
							break;
						}
					break;
					
				} // end of switch (remainder)
			break;
				
			case 3: //for numShift = 3
				
				if (numX < limX && numZ < limZ) {
					rand = RandomUtil.getRandomInt(3);
					if (rand == 0)
						shiftType = ShiftType.X;
					else if (rand == 1) 
						shiftType = ShiftType.Z;
					else
						shiftType = ShiftType.Y;
				}
				else if (numX < limX) {
					rand = RandomUtil.getRandomInt(2);
					if (rand == 0)
						shiftType = ShiftType.X;
					else {
						if (numY <= limY)
							shiftType = ShiftType.Y;
						else
							shiftType = ShiftType.X;
					}
						
				}
				else if (numZ < limZ) {
					rand = RandomUtil.getRandomInt(2);
					if (rand == 0)
						shiftType = ShiftType.Z;
					else {
						if (numY <= limY)
							shiftType = ShiftType.Y;
						else
							shiftType = ShiftType.Z;
					}
				}
				else
					shiftType = ShiftType.Y;
				
	/*
				switch (remainder) {
				case 0: // if (remainder == 0) {
					if (numX < limX && numZ < limZ) {
						rand = RandomUtil.getRandomInt(3);
						if (rand == 0) {
							shiftType = ShiftType.X;
						} else if (rand == 1) {
							shiftType = ShiftType.Z;
						} else {
							shiftType = ShiftType.Y;
						}
						break;
					}
					if (numX <= limX) {
						shiftType = ShiftType.X;
					} else if (numZ <= limZ) {
						shiftType = ShiftType.Z;
					} else {
						shiftType = ShiftType.Y;
					}
					break;
				case 1: // else if (remainder == 1) {
					if (numX < limX && numZ < limZ) {
						rand = RandomUtil.getRandomInt(3);
						if (rand == 0) {
							shiftType = ShiftType.X;
						} else if (rand == 1) {
							shiftType = ShiftType.Z;
						} else {
							shiftType = ShiftType.Y;
						}
						break;
					}
					if (numX <= limX) { 
						shiftType = ShiftType.X;
					}
					else if (numZ <= limZ) { 
						shiftType = ShiftType.Z;
					}
					else {
						shiftType = ShiftType.Y;
					}
					break;
				case 2: // else {//if (remainder == 2) {
					if (numX < limX && numZ < limZ) {
						rand = RandomUtil.getRandomInt(3);
						if (rand == 0) {
							shiftType = ShiftType.X;
						} else if (rand == 1) {
							shiftType = ShiftType.Z;
						} else {
							shiftType = ShiftType.Y;
						}
						break;
					}
					if (numX <= limX) { 
						shiftType = ShiftType.X;
					}
					else if (numZ <= limZ) { 
						shiftType = ShiftType.Z;
					}
					else {
						shiftType = ShiftType.Y;
					}
					break;
				} // end of switch for case 3
	*/			
				break;
			} // end of switch

		return shiftType;
	}
	

	/**
	 * Sets the number of shift of a settlement
	 * @param numShift
	 */
	public void setNumShift(int numShift) {
		this.numShift = numShift;
	}

	/**
	 * Gets the current number of work shifts in a settlement
	 * @return a number, either 2 or 3
	 */
	public int getNumShift() {
		return numShift;
	}

	/*
	 * Restores the previous shift type
	 *
	 * public String reassignShiftType() { String shiftType = "None"; int rand =
	 * -1; int pop = getCurrentPopulationNum();
	 *
	 * if (numShift == 1) { ; // do nothing } else if (numShift == 2) { //
	 * examine numA and numB if (numA > ..pop.)
	 *
	 *
	 * rand = RandomUtil.getRandomInt(1); if (rand != -1) { if (rand == 0)
	 * shiftType = ShiftType.A; else if (rand == 1) shiftType = ShiftType.B;
	 *
	 * } } else if (numShift == 3) { // examine numX , numY and numZ
	 *
	 * rand = RandomUtil.getRandomInt(2); if (rand != -1) { if (rand == 0)
	 * shiftType = ShiftType.X; else if (rand == 1) shiftType = ShiftType.Y;
	 * else if (rand == 2) shiftType = ShiftType.Z;; } } return shiftType; }
	 */

	/*
	 * Increments the number of people in a particular work shift
	 *
	 * @param shiftType
	 */
	public void incrementAShift(ShiftType shiftType) {
		if (shiftType != null) {
			if (shiftType.equals(ShiftType.A))
				numA++;
			else if (shiftType.equals(ShiftType.B))
				numB++;
			else if (shiftType.equals(ShiftType.X))
				numX++;
			else if (shiftType.equals(ShiftType.Y))
				numY++;
			else if (shiftType.equals(ShiftType.Z))
				numZ++;
			else if (shiftType.equals(ShiftType.ON_CALL))
				numOnCall++;
		}
	}

	/*
	 * Decrements the number of people in a particular work shift
	 *
	 * @param shiftType
	 */
	public void decrementAShift(ShiftType shiftType) {
		if (shiftType != null) {
			if (shiftType.equals(ShiftType.A))
				numA--;
			else if (shiftType.equals(ShiftType.B))
				numB--;
			else if (shiftType.equals(ShiftType.X))
				numX--;
			else if (shiftType.equals(ShiftType.Y))
				numY--;
			else if (shiftType.equals(ShiftType.Z))
				numZ--;
			else if (shiftType.equals(ShiftType.ON_CALL))
				numOnCall--;
		}
	}

	/*
	public Map<Integer, Double> getResourceMapCache() {
		return resourceMapCache;
	}

	public void setOneResourceCache(int resourceType, double newAmount) {
		resourceMapCache.put(resourceType, newAmount);
		// System.out.println(" done with setOneResourceCache(). new amount is "
		// + newAmount);
	}
*/

	public Map<Integer, Map<Integer, List<Double>>> getResourceStat() {
		return resourceStat;
	}

	public int getSolCache() {
		return solCache;
	}

	public boolean[] getExposed() {
		return exposed;
	}

	/*
	 * Compute the probability of radiation exposure during EVA/outside walk
	 */
	public void checkRadiationProbability(double time) {
   	    //boolean[] exposed = {false, false, false};
   	    //double exposure = 0;
   	    double ratio = time / RadiationExposure.RADIATION_CHECK_FREQ ;
	    double mag_variation1 = 1 + RandomUtil.getRandomDouble(RadiationExposure.GCR_CHANCE_SWING) - RandomUtil.getRandomDouble(RadiationExposure.GCR_CHANCE_SWING);
	    if (mag_variation1 < 0)
	    	mag_variation1 = 0;
	    double mag_variation2 = 1 + RandomUtil.getRandomDouble(RadiationExposure.SEP_CHANCE_SWING) - RandomUtil.getRandomDouble(RadiationExposure.SEP_CHANCE_SWING);
	    if (mag_variation2 < 0)
	    	mag_variation2 = 0;

	    // Galactic cosmic rays (GCRs) event
  		double chance1 = RadiationExposure.GCR_PERCENT * ratio * mag_variation1; // normally 1.22%
	    // Solar energetic particles (SEPs) event
		double chance2 = RadiationExposure.SEP_PERCENT * ratio * mag_variation2; // 0.122 %
		// Baseline radiation event
		double chance0 = 100 - chance1 - chance2; //RadiationExposure.BASELINE_PERCENT * ratio * (variation1 + variation2); // average 3.53%
 		if (chance0 < 0)
 			chance0 = 0;

		// Baseline radiation event
   	    // Note: RadiationExposure.BASELINE_CHANCE_PER_100MSOL_DURING_EVA * time / 100D
		if (RandomUtil.lessThanRandPercent(chance0)) {
	    	//System.out.println("chance0 : " + chance0);
	    	exposed[0] = true;
	    	//logger.info("An unspecified low-dose radiation event is detected by the radiation sensor grid on " + getName());
	    	this.fireUnitUpdate(UnitEventType.LOW_DOSE_EVENT);
	    }
	    else
	    	exposed[0] = false;

	    // Galactic cosmic rays (GCRs) event
	    //double rand2 = Math.round(RandomUtil.getRandomDouble(100) * 100.0)/100.0;
	   	//System.out.println("chance1 : " + chance1);
	    if (RandomUtil.lessThanRandPercent(chance1)) {
	    	exposed[1] = true;
			LogConsolidated.log(logger, Level.WARNING, 0, sourceName,
					"[" + name + "] A GCR event is detected by the radiation sensor grid.", null);
	    	this.fireUnitUpdate(UnitEventType.GCR_EVENT);
	    }
	    else
	    	exposed[1] = false;


	    // ~ 300 milli Sieverts for a 500-day mission
	    // Solar energetic particles (SEPs) event
    	//System.out.println("chance2 : " + chance2);
	    if (RandomUtil.lessThanRandPercent(chance2)) {
	    	exposed[2] = true;
			LogConsolidated.log(logger, Level.WARNING, 0, sourceName,
					"[" + name + "] A SEP event is detected by the radiation sensor grid.", null);
	    	this.fireUnitUpdate(UnitEventType.SEP_EVENT);
	    }
	    else
	    	exposed[2] = false;
	}

	public CompositionOfAir getCompositionOfAir() {
		return compositionOfAir;
	}

	/**
     * Sets water ration level at the settlement due to low water supplies.
     * @return level of water ration.
     */
    public int waterRationLevel() {
        int result = 0;

        double storedWater = getInventory().getARStored(ResourceUtil.waterID, false);
        double requiredDrinkingWaterOrbit = water_consumption * getNumCurrentPopulation() *
                MarsClock.SOLS_IN_ORBIT_NON_LEAPYEAR;

        // If stored water is less than 20% of required drinking water for Orbit, wash water should be rationed.
        if (storedWater < (requiredDrinkingWaterOrbit * .0025D)) {
            result = 11;
            GoodsManager.WATER_VALUE_MODIFIER = GoodsManager.WATER_VALUE_MODIFIER * 64;
        }
        else if (storedWater < (requiredDrinkingWaterOrbit * .005D)) {
            result = 10;
            GoodsManager.WATER_VALUE_MODIFIER = GoodsManager.WATER_VALUE_MODIFIER * 48;
        }
        else if (storedWater < (requiredDrinkingWaterOrbit * .01D)) {
            result = 9;
            GoodsManager.WATER_VALUE_MODIFIER = GoodsManager.WATER_VALUE_MODIFIER * 32;
        }
        else if (storedWater < (requiredDrinkingWaterOrbit * .015D)) {
            result = 8;
            GoodsManager.WATER_VALUE_MODIFIER = GoodsManager.WATER_VALUE_MODIFIER * 24;
        }
        else if (storedWater < (requiredDrinkingWaterOrbit * .025D)) {
            result = 7;
            GoodsManager.WATER_VALUE_MODIFIER = GoodsManager.WATER_VALUE_MODIFIER * 16;
        }
        else if (storedWater < (requiredDrinkingWaterOrbit * .05D)) {
            result = 6;
            GoodsManager.WATER_VALUE_MODIFIER = GoodsManager.WATER_VALUE_MODIFIER * 12;
        }
        else if (storedWater < (requiredDrinkingWaterOrbit * .075D)) {
            result = 5;
            GoodsManager.WATER_VALUE_MODIFIER = GoodsManager.WATER_VALUE_MODIFIER * 8;
        }
        else if (storedWater < (requiredDrinkingWaterOrbit * .1D)) {
            result = 4;
            GoodsManager.WATER_VALUE_MODIFIER = GoodsManager.WATER_VALUE_MODIFIER * 6;
        }
        else if (storedWater < (requiredDrinkingWaterOrbit * .125D)) {
            result = 3;
            GoodsManager.WATER_VALUE_MODIFIER = GoodsManager.WATER_VALUE_MODIFIER * 4;
        }
        else if (storedWater < (requiredDrinkingWaterOrbit * .15D)) {
            result = 2;
            GoodsManager.WATER_VALUE_MODIFIER = GoodsManager.WATER_VALUE_MODIFIER * 3;
        }
        else if (storedWater < (requiredDrinkingWaterOrbit * .2D)) {
            result = 1;
            GoodsManager.WATER_VALUE_MODIFIER = GoodsManager.WATER_VALUE_MODIFIER * 2;
        }
        return result;
    }

	//@Override
	public void setObjective(ObjectiveType objectiveType) {
		//System.out.println(name + "'s objective is " + objectiveType.toString());
		this.objectiveType = objectiveType;

		// reset all to 1
		goodsManager.setCropFarmFactor(1);
		goodsManager.setManufacturingFactor(1);
		goodsManager.setResearchFactor(1);
		goodsManager.setTransportationFactor(1);
		goodsManager.setTradeFactor(1);
		//goodsManager.setFreeMarketFactor(1);

		if (objectiveType == ObjectiveType.CROP_FARM) {
			goodsManager.setCropFarmFactor(1.5);
		}

		else if (objectiveType == ObjectiveType.MANUFACTURING) {
			goodsManager.setManufacturingFactor(1.5);
		}

		else if (objectiveType == ObjectiveType.RESEARCH_CENTER) {
			goodsManager.setResearchFactor(1.5);
		}

		else if (objectiveType == ObjectiveType.TRANSPORTATION_HUB) {
			goodsManager.setTransportationFactor(1.5);
		}

		else if (objectiveType == ObjectiveType.TRADE_TOWN) {
			goodsManager.setTradeFactor(1.5);
		}

		else if (objectiveType == ObjectiveType.TOURISM) {
			goodsManager.setTourismFactor(1.5);
		}

		//else if (objectiveType == ObjectiveType.FREE_MARKET) {
		//	goodsManager.setFreeMarketFactor(1.5);
		//}

	}

	//@Override
	public ObjectiveType getObjective() {
		return objectiveType;
	}

	// 2016-05-08 Added getObjectiveBuildingType()
	public String getObjectiveBuildingType() {

		// TODO: check if a particular building has existed, if yes, build the next relevant building
		if (objectiveType == ObjectiveType.CROP_FARM)
			return "inflatable greenhouse";//"inground greenhouse";//"Inflatable Greenhouse";
		// alternatives : "Large Greenhouse"
		else if (objectiveType == ObjectiveType.MANUFACTURING)
			return "manufacturing shed" ;//"Workshop";
		// alternatives : "Manufacturing Shed", MD1, MD4
		else if (objectiveType == ObjectiveType.RESEARCH_CENTER)
			return "mining lab"; //Laboratory";
		// alternatives : "Mining Lab", "Astronomy Observatory"
		else if (objectiveType == ObjectiveType.TRANSPORTATION_HUB)
			return "loading dock garage";
		// alternatives :"Garage";
		else if (objectiveType == ObjectiveType.TRADE_TOWN)
			return "storage shed";
		else if (objectiveType == ObjectiveType.TOURISM)
			return "loading dock garage";
		//else if (objectiveType == ObjectiveType.FREE_MARKET)
		//	return "";


		// Future alternatives :

		//else if (objectiveType == ObjectiveType.POWER_HUB)
		//	return "";
		//else if (objectiveType == ObjectiveType.RESIDENTIAL_DISTRICT)
		//	return "bunkhouse" or "outpost hub";

		else
			return null;
	}

	//public ObservableList<String> getObjectivesOList() {
	//	return objectivesOList;
	//}

	public String[] getObjectiveArray() {
		return objectiveArray;
	}

	public ObjectiveType[] getObjectives() {
		return objectives;
	}
	
	public String getSponsor() {
		return sponsor;
	}

	public int getSumOfManuProcesses() {
		return sumOfCurrentManuProcesses;
	}

	public void addManuProcesses(int value) {
		sumOfCurrentManuProcesses = sumOfCurrentManuProcesses + value;
	}


    /**
     * Gets the number of crops that currently need work this Sol.
     * @return number of crops.
     */
    // 2016-10-28 Modified, added caching and relocated from TendGreenhouse
    public int getCropsNeedingTending() {
        int result = 0;

        int m = marsClock.getMsol0();
        if (millisolCache + 5 >= m) {
        	result = cropsNeedingTendingCache;
    	}

    	else {
        	millisolCache = m;
	        for (Building b : buildingManager.getBuildings(FunctionType.FARMING)) {
	            Farming farm = b.getFarming();
	            for (Crop c : farm.getCrops()){
	                if (c.requiresWork()) {
	                    result++;
	                }
	            }
	        }
        	cropsNeedingTendingCache = result;
    	}
        //System.out.println("getCropsNeedingTending() : result is " + result);
        return result;
    }

    // 2016-10-28 Added getCropsNeedingTendingCache()
    public int getCropsNeedingTendingCache() {
    	return cropsNeedingTendingCache;
    }

    public String toString() {
    	return name;
    }

    /***
     * Computes the probability of the presence of regolith  
     * @return probability of finding regolith
     */
    public double computeRegolithProbability() {
    	double result = 0;

        double regolith_value = goodsManager.getGoodValuePerItem(GoodsUtil.getResourceGood(ResourceUtil.regolithAR));
        regolith_value = regolith_value * GoodsManager.REGOLITH_VALUE_MODIFIER;
    	if (regolith_value > 2000)
    		regolith_value = 2000;
    	else if (regolith_value <= 5)
    		return 0;

        double sand_value = goodsManager.getGoodValuePerItem(GoodsUtil.getResourceGood(ResourceUtil.sandAR));
        sand_value = sand_value * GoodsManager.SAND_VALUE_MODIFIER;
        if (sand_value > 2000)
    		sand_value = 2000;
        else if (sand_value <= 3)
        	return 0;

        int pop = getAllAssociatedPeople().size();//getCurrentPopulationNum();

        double regolith_available = getInventory().getAmountResourceStored(ResourceUtil.regolithAR, false);
        double sand_available = getInventory().getAmountResourceStored(ResourceUtil.sandAR, false);

        if (regolith_available < MIN_REGOLITH_RESERVE * pop + regolith_value/10 && sand_available < MIN_SAND_RESERVE * pop + sand_value/10) {
        	result = (MIN_REGOLITH_RESERVE * pop /2.5D - regolith_available) / 10D;
        }

        // Prompt the collect ice mission to proceed more easily if water resource is dangerously low,
        if (regolith_available > MIN_REGOLITH_RESERVE * pop ) {
        	;// no change to missionProbability
        }
        else if (regolith_available > MIN_REGOLITH_RESERVE * pop / 1.5 ) {
        	result = 20D * result + (MIN_REGOLITH_RESERVE * pop - regolith_available);
        }
        else if (regolith_available > MIN_REGOLITH_RESERVE * pop / 2D) {
        	result = 10D * result + (MIN_REGOLITH_RESERVE * pop - regolith_available);
        }
        else
        	result = 5D * result + (MIN_REGOLITH_RESERVE * pop - regolith_available);

    	return result;
    }

    /***
     * Computes the probability of the presence of ice
     * @return probability of finding ice
     */
    public double computeIceProbability() {
    	double result = 0;

        double ice_value = goodsManager.getGoodValuePerItem(GoodsUtil.getResourceGood(ResourceUtil.iceAR));
        ice_value = ice_value * GoodsManager.ICE_VALUE_MODIFIER;
    	if (ice_value > 8000)
    		ice_value = 8000;
    	if (ice_value < 1)
    		ice_value = 1;

        double water_value = goodsManager.getGoodValuePerItem(GoodsUtil.getResourceGood(ResourceUtil.waterAR));
        water_value = water_value * GoodsManager.WATER_VALUE_MODIFIER;
        if (water_value > 8000)
    		water_value = 8000;
    	if (water_value < 1)
    		water_value =  1;
/*
        double oxygen_value = goodsManager.getGoodValuePerItem(GoodsUtil.getResourceGood(ResourceUtil.oxygenAR));
        oxygen_value = oxygen_value * GoodsManager.OXYGEN_VALUE_MODIFIER;
        if (oxygen_value > 4000)
        	oxygen_value = 4000;
    	if (oxygen_value < 1)
    		oxygen_value = 1;
*/
    	// 2016-10-14 Compare the available amount of water and ice reserve
        double ice_available = getInventory().getARStored(ResourceUtil.iceID, false);
        double water_available = getInventory().getARStored(ResourceUtil.waterID, false);
        //double oxygen_available = getInventory().getAmountResourceStored(ResourceUtil.oxygenAR, false);
        
        int pop = getAllAssociatedPeople().size();//getCurrentPopulationNum();

        // TODO: create a task to find local ice and simulate the probability of finding local ice and its quantity
        if (ice_available < MIN_ICE_RESERVE * pop + ice_value/10D && water_available < MIN_WATER_RESERVE * pop + water_value/10D) {
        	result = (MIN_ICE_RESERVE * pop - ice_available + MIN_WATER_RESERVE * pop - water_available) * 2D + water_value + ice_value ;
        }

        // Prompt the collect ice mission to proceed more easily if water resource is dangerously low,
        if (water_available > MIN_WATER_RESERVE * pop ) {
        	;// no change to missionProbability
        }
        else if (water_available > MIN_WATER_RESERVE * pop / 1.5 ) {
        	result = 20D * result + (MIN_WATER_RESERVE * pop - water_available);
        }
        else if (water_available > MIN_WATER_RESERVE * pop / 2D) {
        	result = 10D * result + (MIN_WATER_RESERVE * pop - water_available);
        }
        else
        	result = 5D * result + (MIN_WATER_RESERVE * pop - water_available);

    	return result;
    }

	public double getIceProbabilityValue() {
		return iceProbabilityValue;
	}

	public double getRegolithProbabilityValue() {
		return regolithProbabilityValue;
	}

	public double getOxygenProbabilityValue() {
		return oxygenProbabilityValue;
	}

	public double getMethaneProbabilityValue() {
		return methaneProbabilityValue;
	}
	
	public double getOutsideTemperature() {
		return outside_temperature;
	}

	public DustStorm getDustStorm() {
		return storm;
	}
	
	public void setDustStorm(DustStorm storm) {
		this.storm = storm;
	}

	//public ShiftType getCurrentShift() {	
	//	return currentShift;
	//}
	
	@Override
	public void destroy() {
		super.destroy();

		if (buildingManager != null) {
			buildingManager.destroy();
		}
		buildingManager = null;
		if (buildingConnectorManager != null) {
			buildingConnectorManager.destroy();
		}
		buildingConnectorManager = null;
		if (goodsManager != null) {
			goodsManager.destroy();
		}
		goodsManager = null;
		if (constructionManager != null) {
			constructionManager.destroy();
		}
		constructionManager = null;
		if (powerGrid != null) {
			powerGrid.destroy();
		}
		powerGrid = null;

		if (thermalSystem != null) {
			thermalSystem.destroy();
		}
		thermalSystem = null;

		template = null;
		//if (scientificAchievement != null) {
		//	scientificAchievement.clear();
		//}
		scientificAchievement = null;
	}


}