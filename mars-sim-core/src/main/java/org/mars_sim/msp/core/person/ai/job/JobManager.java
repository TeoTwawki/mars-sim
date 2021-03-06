/**
 * Mars Simulation Project
 * JobManager.java
 * @version 3.1.0 2017-08-30
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.ai.job.Chefbot;
import org.mars_sim.msp.core.robot.ai.job.Constructionbot;
import org.mars_sim.msp.core.robot.ai.job.Deliverybot;
import org.mars_sim.msp.core.robot.ai.job.Gardenbot;
import org.mars_sim.msp.core.robot.ai.job.Makerbot;
import org.mars_sim.msp.core.robot.ai.job.Medicbot;
import org.mars_sim.msp.core.robot.ai.job.Repairbot;
import org.mars_sim.msp.core.robot.ai.job.RobotJob;
import org.mars_sim.msp.core.structure.Settlement;

/**
 * The JobManager class keeps track of the settler jobs in a simulation.
 */
public final class JobManager implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(JobManager.class.getName());

	public static final String SETTLEMENT = "Settlement";
	public static final String MISSION_CONTROL = "Mission Control";
	public static final String USER = "User";
	//public static final String DIVISION_CHIEF = "Division Chief";

	// Data members
	/** List of the jobs in the simulation. */
	private static List<Job> jobs;
	private static List<RobotJob> robotJobs;

	/**
	 * Private constructor for static utility class.
	 */
	private JobManager() {}

	/**
	 * Initialize job list.
	 */
	private static void loadJobs() {
		jobs = new ArrayList<Job>();
		jobs.add(new Architect());
		jobs.add(new Areologist());
		jobs.add(new Astronomer());
		jobs.add(new Biologist());
		jobs.add(new Botanist());
		jobs.add(new Chef());
		jobs.add(new Chemist());
		jobs.add(new Doctor());
		jobs.add(new Driver());
		jobs.add(new Engineer());
		jobs.add(new Mathematician());
		jobs.add(new Manager());
		jobs.add(new Meteorologist());
		jobs.add(new Physicist());
		jobs.add(new Trader());
		jobs.add(new Technician());
	}

	/**
	 * Initialize robotJobs list.
	 */
	private static void loadRobotJobs() {
		robotJobs = new ArrayList<RobotJob>();
		robotJobs.add(new Chefbot());
		robotJobs.add(new Constructionbot());
		robotJobs.add(new Deliverybot());
		robotJobs.add(new Gardenbot());
		robotJobs.add(new Makerbot());
		robotJobs.add(new Medicbot());
		robotJobs.add(new Repairbot());

	}

	/**
	 * Gets a list of available jobs in the simulation.
	 * @return list of jobs
	 */
	public static List<Job> getJobs() {
		if (jobs == null) loadJobs();
		return new ArrayList<Job>(jobs);
	}

	/**
	 * Gets a list of available jobs in the simulation.
	 * @return list of jobs
	 */
	public static List<RobotJob> getRobotJobs() {
		if (robotJobs == null) loadRobotJobs();
		return new ArrayList<RobotJob>(robotJobs);
	}

	/**
	 * Gets a job from a job class name.
	 * @param jobName the name of the job.
	 * @return job or null if job with name not found.
	 */
	public static Job getJob(String jobClassName) {
		if (jobs == null) loadJobs();
		for (Job job : jobs) {
			if (job.getClass().getSimpleName().compareTo(jobClassName) == 0) {
				return job;
			}
		}
		return null;
	}

	public static RobotJob getRobotJob(String jobClassName) {
		if (robotJobs == null) loadRobotJobs();
		for (RobotJob robotJob : robotJobs) {
			if (robotJob.getClass().getSimpleName().compareToIgnoreCase(jobClassName) == 0) {
				return robotJob;
			}
		}
		return null;
	}

	/**
	 * Gets the need for a job at a settlement minus the capability of the inhabitants
	 * performing that job there.
	 * @param settlement the settlement to check.
	 * @param job the job to check.
	 * @return settlement need minus total job capability of inhabitants with job.
	 */
	public static double getRemainingSettlementNeed(Settlement settlement, Job job) {
		if (job == null) 
			logger.warning("job is null !");
		double result = job.getSettlementNeed(settlement);

		// Check all people associated with the settlement.
		Iterator<Person> i = settlement.getAllAssociatedPeople().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			if (person.getMind().getJob() == job)
				result-= job.getCapability(person);
		}

		result = result / 2D;

		return result;
	}

	// TODO: determine the need for this method since it promotes robotJob switching For robots
	public static double getRemainingSettlementNeed(Settlement settlement, RobotJob robotJob) {
		double result = robotJob.getSettlementNeed(settlement);

			// Check all Robots associated with the settlement.
			Iterator<Robot> j = settlement.getAllAssociatedRobots().iterator();
			while (j.hasNext()) {
				Robot robot = j.next();
				if (robot.getBotMind().getRobotJob() == robotJob) 
					result-= robotJob.getCapability(robot);
			}

			result = result/2D;

		return result;
	}

	/**
	 * Gets a new job for the person.
	 * Might be the person's current job.
	 * @param person the person to check.
	 * @return the new job.
	 */
	public static Job getNewJob(Unit unit) {
		Job newJob = null;
        Person person = (Person) unit;

		Job originalJob = person.getMind().getJob();
		// Determine person's associated settlement.
		Settlement settlement = null;
		if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT)
			settlement = person.getSettlement();
		else if (person.getMind().hasActiveMission())
			settlement = person.getMind().getMission().getAssociatedSettlement();

		// Find new job for person.
		double newJobProspect = Integer.MIN_VALUE;
		if (settlement != null) {
			Iterator<Job> i = getJobs().iterator();
			while (i.hasNext()) {
				Job job = i.next();
				// 2015-04-30 Exclude mayor/manager job
				if (!job.equals(JobManager.getJob("Manager"))) {
    				double jobProspect = getJobProspect(person, job, settlement, true);
    				if (jobProspect >= newJobProspect) {
    					newJob = job;
    					newJobProspect = jobProspect;
    				}
				}
			}

			if(logger.isLoggable(Level.FINEST)) {
				if ((newJob != null) && (newJob != originalJob))
					logger.finest(person.getName() + " changed jobs to " + newJob.getName(person.getGender()));
				else logger.finest(person.getName() + " keeping old job of " + originalJob.getName(person.getGender()));

			}
		}
		else newJob = originalJob;

		return newJob;
	}


	/**
	 * Gets a new job for the Robot.
	 * Might be the Robot's current job.
	 * @param Robot the Robot to check.
	 * @return the new job.
	 */
	public static RobotJob getNewRobotJob(Unit unit) {
		RobotJob newJob = null;
        Robot robot = (Robot) unit;

    	RobotJob originalJob = robot.getBotMind().getRobotJob();

		// Determine robot's associated settlement.
		Settlement settlement = null;
		if (robot.getLocationSituation() == LocationSituation.IN_SETTLEMENT)
			settlement = robot.getSettlement();
		else if (robot.getBotMind().hasActiveMission())
			settlement = robot.getBotMind().getMission().getAssociatedSettlement();

		// Find new job for robot.
		double newJobProspect = Integer.MIN_VALUE;
		if (settlement != null) {
			Iterator<RobotJob> i = getRobotJobs().iterator();
			while (i.hasNext()) {
				RobotJob robotJob = i.next();
				double jobProspect = getRobotJobProspect(robot, robotJob, settlement, true);
				if (jobProspect >= newJobProspect) {
					newJob = robotJob;
					newJobProspect = jobProspect;
				}
			}

			if(logger.isLoggable(Level.FINEST)) {
				if ((newJob != null) && (newJob != originalJob))
					logger.info("Notes: " + robot.getName() + " changed jobs to " 
							+ newJob.getName(robot.getRobotType())); // logger.finest(
				else 
					logger.info("Notes: " + robot.getName() + " keeping old job of " 
							+ originalJob.getName(robot.getRobotType()));
			}
		}
		else newJob = originalJob;

		return newJob;
	}


	/**
	 * Get the job prospect value for a person and a particular job at a settlement.
	 * @param person the person to check for
	 * @param job the job to check for
	 * @param settlement the settlement to do the job in.
	 * @param isHomeSettlement is this the person's home settlement?
	 * @return job prospect value (0.0 min)
	 */
	public static double getJobProspect(Unit unit, Job job, Settlement settlement, boolean isHomeSettlement) {
        Person person = null;
    	person = (Person) unit;

		double jobCapability = 0D;
		double remainingNeed = 0D;

    	if (job != null)
    		jobCapability = job.getCapability(person);

		remainingNeed = getRemainingSettlementNeed(settlement, job);

		if ((job == person.getMind().getJob()) && isHomeSettlement)
			remainingNeed += jobCapability;

		return (jobCapability + 1D) * remainingNeed;
	}

	public static double getRobotJobProspect(Unit unit, RobotJob robotJob, Settlement settlement, boolean isHomeSettlement) {
        Robot robot = null;
		double jobCapability = 0D;
		double remainingNeed = 0D;

    	robot = (Robot) unit;
    	if (robotJob != null) 
    		jobCapability = robotJob.getCapability(robot);
    	
		remainingNeed = getRemainingSettlementNeed(settlement, robotJob);
		
		if ((robotJob == robot.getBotMind().getRobotJob()) && isHomeSettlement) 
			remainingNeed+= jobCapability;

		return (jobCapability + 1D) * remainingNeed;
	}

	/**
	 * Gets the best job prospect value for a person at a settlement.
	 * @param person the person to check for
	 * @param settlement the settlement to do the job in
	 * @param isHomeSettlement is this the person's home settlement?
	 * @return best job prospect value
	 */
	public static double getBestJobProspect(Person person, Settlement settlement, boolean isHomeSettlement) {
		double bestProspect = Double.MIN_VALUE;
		Iterator<Job> i = getJobs().iterator();
		while (i.hasNext()) {
			Job job = i.next();
			double prospect = getJobProspect(person, job, settlement, isHomeSettlement);
			if (prospect > bestProspect) bestProspect = prospect;
		}
		return bestProspect;
	}

	public static double getBestRobotJobProspect(Robot robot, Settlement settlement, boolean isHomeSettlement) {
		double bestProspect = Double.MIN_VALUE;
		Iterator<RobotJob> i = getRobotJobs().iterator();
		while (i.hasNext()) {
			RobotJob robotJob = i.next();
			double prospect = getRobotJobProspect(robot, robotJob, settlement, isHomeSettlement);
			if (prospect > bestProspect) bestProspect = prospect;
		}
		return bestProspect;
	}
}