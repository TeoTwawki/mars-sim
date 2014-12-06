/**
 * Mars Simulation Project
 * Crewable.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */
package org.mars_sim.msp.core.vehicle;

import java.util.Collection;

import org.mars_sim.msp.core.person.Person;

/**
 * The Crewable interface represents a vehicle that is capable
 * of having a crew of people.
 */
public interface Crewable {

	/**
	 * Gets the number of crewmembers the vehicle can carry.
	 * @return capacity
	 */
	public int getCrewCapacity();

	/**
	 * Gets the current number of crewmembers.
	 * @return number of crewmembers
	 */
	public int getCrewNum();

	/**
	 * Gets a collection of the crewmembers.
	 * @return crewmembers as Collection
	 */
	public Collection<Person> getCrew();

	/**
	 * Checks if person is a crewmember.
	 * @param person the person to check
	 * @return true if person is a crewmember
	 */
	public boolean isCrewmember(Person person);
}
