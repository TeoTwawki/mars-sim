/**
 * Mars Simulation Project
 * Technician.java
 * @version 3.07 2015-01-14
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;

import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.BuildingConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.BuildingSalvageMission;
import org.mars_sim.msp.core.person.ai.mission.EmergencySupplyMission;
import org.mars_sim.msp.core.person.ai.mission.RescueSalvageVehicle;
import org.mars_sim.msp.core.person.ai.mission.TravelToSettlement;
import org.mars_sim.msp.core.person.ai.task.ConsolidateContainers;
import org.mars_sim.msp.core.person.ai.task.DigLocalIce;
import org.mars_sim.msp.core.person.ai.task.DigLocalRegolith;
import org.mars_sim.msp.core.person.ai.task.LoadVehicleEVA;
import org.mars_sim.msp.core.person.ai.task.LoadVehicleGarage;
import org.mars_sim.msp.core.person.ai.task.MaintainGroundVehicleEVA;
import org.mars_sim.msp.core.person.ai.task.MaintainGroundVehicleGarage;
import org.mars_sim.msp.core.person.ai.task.Maintenance;
import org.mars_sim.msp.core.person.ai.task.MaintenanceEVA;
import org.mars_sim.msp.core.person.ai.task.ManufactureGood;
import org.mars_sim.msp.core.person.ai.task.RepairEVAMalfunction;
import org.mars_sim.msp.core.person.ai.task.RepairMalfunction;
import org.mars_sim.msp.core.person.ai.task.ReviewJobReassignment;
import org.mars_sim.msp.core.person.ai.task.SalvageGood;
import org.mars_sim.msp.core.person.ai.task.ToggleFuelPowerSource;
import org.mars_sim.msp.core.person.ai.task.ToggleResourceProcess;
import org.mars_sim.msp.core.person.ai.task.UnloadVehicleEVA;
import org.mars_sim.msp.core.person.ai.task.UnloadVehicleGarage;
import org.mars_sim.msp.core.person.ai.task.WriteReport;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.RoboticAttribute;
import org.mars_sim.msp.core.robot.RoboticAttributeManager;
import org.mars_sim.msp.core.structure.Settlement;

public class Technician
extends Job
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public Technician() {
		// Use Job constructor
		super(Technician.class);

		// Add technician-related tasks.
		jobTasks.add(ConsolidateContainers.class);
		jobTasks.add(LoadVehicleEVA.class);
        jobTasks.add(LoadVehicleGarage.class);
		jobTasks.add(Maintenance.class);
		jobTasks.add(MaintenanceEVA.class);
		jobTasks.add(MaintainGroundVehicleGarage.class);
		jobTasks.add(MaintainGroundVehicleEVA.class);
		jobTasks.add(ManufactureGood.class);
		jobTasks.add(RepairMalfunction.class);
		jobTasks.add(RepairEVAMalfunction.class);
		jobTasks.add(ToggleResourceProcess.class);
		jobTasks.add(ToggleFuelPowerSource.class);
		jobTasks.add(UnloadVehicleEVA.class);
		jobTasks.add(UnloadVehicleGarage.class);
		jobTasks.add(SalvageGood.class);

		// Add side tasks
		jobTasks.add(DigLocalRegolith.class);
		jobTasks.add(DigLocalIce.class);
		jobTasks.add(ReviewJobReassignment.class);
		jobTasks.add(WriteReport.class);

		// Add engineer-related missions.
		jobMissionStarts.add(TravelToSettlement.class);
		jobMissionJoins.add(TravelToSettlement.class);
		jobMissionStarts.add(RescueSalvageVehicle.class);
		jobMissionJoins.add(RescueSalvageVehicle.class);
		jobMissionJoins.add(BuildingConstructionMission.class);
		jobMissionJoins.add(BuildingSalvageMission.class);
		jobMissionStarts.add(EmergencySupplyMission.class);
		jobMissionJoins.add(EmergencySupplyMission.class);
	}

	/**
	 * Gets a person's capability to perform this job.
	 * @param person the person to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Person person) {

		double result = 0D;

		int mechanicSkill = person.getMind().getSkillManager().getSkillLevel(SkillType.MECHANICS);
		result = mechanicSkill;

		NaturalAttributeManager attributes = person.getNaturalAttributeManager();
		int experienceAptitude = attributes.getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
		result+= result * ((experienceAptitude - 50D) / 100D);

		if (person.getPhysicalCondition().hasSeriousMedicalProblems()) result = 0D;

		return result;
	}

	/**
	 * Gets the base settlement need for this job.
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public double getSettlementNeed(Settlement settlement) {

		double result = 0D;

		// Add number of buildings in settlement.
		result+= settlement.getBuildingManager().getNumBuilding() / 3D;

		// Add number of vehicles parked at settlement.
		result+= settlement.getParkedVehicleNum() / 3D;

		return result;
	}

	/**
	 * Gets a robot's capability to perform this job.
	 * @param robot the person to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Robot robot) {

		double result = 0D;

		int mechanicSkill = robot.getBotMind().getSkillManager().getSkillLevel(SkillType.MECHANICS);
		result = mechanicSkill;

		RoboticAttributeManager attributes = robot.getRoboticAttributeManager();
		int experienceAptitude = attributes.getAttribute(RoboticAttribute.EXPERIENCE_APTITUDE);
		result+= result * ((experienceAptitude - 50D) / 100D);

		//if (robot.getPhysicalCondition().hasSeriousMedicalProblems()) result = 0D;

		return result;
	}
}