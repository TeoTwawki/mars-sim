/**
 * Mars Simulation Project
 * PersonalityTraitManager.java
 * @version 3.1.0 2016-11-05
 * @author Manny Kung
 */

package org.mars_sim.msp.core.person;

import java.io.Serializable;
import java.util.Hashtable;

import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.person.ai.Mind;

/**
 * The PersonalityTraitManager class manages a person's big five personalities.
 * There is one personality trait manager for each person.
 */
public class PersonalityTraitManager
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
	private static final int RANGE = 100;

	/** List of the person's big five personalities keyed by unique name. */
	private Hashtable<PersonalityTraitType, Integer> personalityTraits;

	private Mind mind;
	
	/**
	 * Constructor.
	 * @param person.
	 */
	public PersonalityTraitManager(Person person) {

		this.mind = person.getMind();
		
		personalityTraits = new Hashtable<PersonalityTraitType, Integer>();

		int numberOfIterations = 2;

		// Create big five personality traits using random values
		for (PersonalityTraitType type : PersonalityTraitType.values()) {
			int value = 0;
			for (int y = 0; y < numberOfIterations; y++) 
				value += RandomUtil.getRandomInt(RANGE);
			value /= numberOfIterations;
			personalityTraits.put(type, value);
		}

	}
	
	/*	
	 * Sync up with the I-E pair score in MBTI	 	
	public void syncUpExtraversion() {	
		int value = mind.getMBTI().getScores().get(0);		
		personalityTraits.put(PersonalityTraitType.EXTRAVERSION, value);		
	}
	*/
	
	/**
	 * Adds a random modifier to an personality trait.
	 * @param type the name of the personality trait
	 * @param modifier the random ceiling of the modifier
	 */
	private void addPersonalityTraitModifier(PersonalityTraitType type, int modifier) {
		int random = RandomUtil.getRandomInt(Math.abs(modifier));
		if (modifier < 0) random *= -1;
		setPersonalityTrait(type, getPersonalityTrait(type) + random);
	}

	/**
	 * Returns the number of big five personalities.
	 * @return the number of big five personalities
	 */
	public int getPersonalityTraitNum() {
		return personalityTraits.size();
	}

	/**
	 * Gets the integer value of a named personality trait if it exists.
	 * Returns 0 otherwise.
	 * @param type {@link PersonalityTraitType} the personalityTrait
	 * @return the value of the personalityTrait
	 */
	public int getPersonalityTrait(PersonalityTraitType type) {
		int result = 0;
		if (personalityTraits.containsKey(type)) 
			result = personalityTraits.get(type);
		return result;
	}

	public Hashtable<PersonalityTraitType, Integer> getPersonalityTraitMap() {
		return personalityTraits;
	}
	
	/**
	 * Sets an personality trait's value.
	 * @param attrib {@link PersonalityTraitType} the personality trait
	 * @param value the value the personality trait is to be set
	 */
	public void setPersonalityTrait(PersonalityTraitType type, int value) {
		if (value > 100) value = 100;
		if (value < 0) value = 0;
		personalityTraits.put(type, value);
	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		personalityTraits.clear();
		personalityTraits = null;
	}
}