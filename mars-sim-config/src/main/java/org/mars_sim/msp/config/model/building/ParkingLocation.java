/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.3.1</a>, using an XML
 * Schema.
 * $Id$
 */

package org.mars_sim.msp.config.model.building;

/**
 * Class ParkingLocation.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings("serial")
public class ParkingLocation implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _xLocation.
     */
    private double _xLocation;

    /**
     * keeps track of state for field: _xLocation
     */
    private boolean _has_xLocation;

    /**
     * Field _yLocation.
     */
    private double _yLocation;

    /**
     * keeps track of state for field: _yLocation
     */
    private boolean _has_yLocation;


      //----------------/
     //- Constructors -/
    //----------------/

    public ParkingLocation() {
        super();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     */
    public void deleteXLocation(
    ) {
        this._has_xLocation= false;
    }

    /**
     */
    public void deleteYLocation(
    ) {
        this._has_yLocation= false;
    }

    /**
     * Returns the value of field 'xLocation'.
     * 
     * @return the value of field 'XLocation'.
     */
    public double getXLocation(
    ) {
        return this._xLocation;
    }

    /**
     * Returns the value of field 'yLocation'.
     * 
     * @return the value of field 'YLocation'.
     */
    public double getYLocation(
    ) {
        return this._yLocation;
    }

    /**
     * Method hasXLocation.
     * 
     * @return true if at least one XLocation has been added
     */
    public boolean hasXLocation(
    ) {
        return this._has_xLocation;
    }

    /**
     * Method hasYLocation.
     * 
     * @return true if at least one YLocation has been added
     */
    public boolean hasYLocation(
    ) {
        return this._has_yLocation;
    }

    /**
     * Method isValid.
     * 
     * @return true if this object is valid according to the schema
     */
    public boolean isValid(
    ) {
        try {
            validate();
        } catch (org.exolab.castor.xml.ValidationException vex) {
            return false;
        }
        return true;
    }

    /**
     * 
     * 
     * @param out
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void marshal(
            final java.io.Writer out)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Marshaller.marshal(this, out);
    }

    /**
     * 
     * 
     * @param handler
     * @throws java.io.IOException if an IOException occurs during
     * marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     */
    public void marshal(
            final org.xml.sax.ContentHandler handler)
    throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Marshaller.marshal(this, handler);
    }

    /**
     * Sets the value of field 'xLocation'.
     * 
     * @param xLocation the value of field 'xLocation'.
     */
    public void setXLocation(
            final double xLocation) {
        this._xLocation = xLocation;
        this._has_xLocation = true;
    }

    /**
     * Sets the value of field 'yLocation'.
     * 
     * @param yLocation the value of field 'yLocation'.
     */
    public void setYLocation(
            final double yLocation) {
        this._yLocation = yLocation;
        this._has_yLocation = true;
    }

    /**
     * Method unmarshal.
     * 
     * @param reader
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @return the unmarshaled
     * org.mars_sim.msp.config.model.building.ParkingLocation
     */
    public static org.mars_sim.msp.config.model.building.ParkingLocation unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (org.mars_sim.msp.config.model.building.ParkingLocation) org.exolab.castor.xml.Unmarshaller.unmarshal(org.mars_sim.msp.config.model.building.ParkingLocation.class, reader);
    }

    /**
     * 
     * 
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void validate(
    )
    throws org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Validator validator = new org.exolab.castor.xml.Validator();
        validator.validate(this);
    }

}