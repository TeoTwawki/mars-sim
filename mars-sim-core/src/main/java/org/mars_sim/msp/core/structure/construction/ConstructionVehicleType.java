/**
 * Mars Simulation Project
 * ConstructionVehicleType.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */

package org.mars_sim.msp.core.structure.construction;

import org.mars_sim.msp.core.resource.Part;

import java.io.Serializable;
import java.util.List;

/**
 * Construction vehicle information.
 */
public class ConstructionVehicleType
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private String vehicleType;
	private Class<?> vehicleClass;
	private List<Part> attachmentParts;

	/**
	 * Constructor.
	 * @param vehicleType the vehicle type.
	 * @param vehicleClass the vehicle class.
	 * @param attachmentParts list of attachment parts.
	 */
	public ConstructionVehicleType(
		String vehicleType, Class<?> vehicleClass, 
		List<Part> attachmentParts
	) {

		this.vehicleType = vehicleType;
		this.vehicleClass = vehicleClass;
		this.attachmentParts = attachmentParts;
	}

	/**
	 * Gets the attachment parts.
	 * @return list of parts.
	 */
	public List<Part> getAttachmentParts() {
		return attachmentParts;
	}

	/**
	 * Gets the vehicle class.
	 * @return class.
	 */
	public Class<?> getVehicleClass() {
		return vehicleClass;
	}

	/**
	 * Gets the vehicle type.
	 * @return vehicle type.
	 */
	public String getVehicleType() {
		return vehicleType;
	}

	@Override
	public String toString() {
		return vehicleType;
	}
}