/**
 * Mars Simulation Project
 * UnitToolbar.java
 * @version 3.1.0 2017-10-11
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.ui.javafx.MainScene;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;

/**
 * The UnitToolBar class is a UI toolbar for holding unit buttons. There should
 * only be one instance and it is contained in the MainWindow instance.
 */
//public class UnitToolBar extends TransparentToolBar implements ActionListener {
public class UnitToolBar extends JToolBar implements ActionListener {

	private static final long serialVersionUID = 1L;
	// Data members
	private List<UnitButton> unitButtons; // List of unit buttons
	
	private MainWindow parentMainWindow; // Main window that contains this toolbar.
	private MainScene mainScene;
	
	/**
	 * Constructs a UnitToolBar object
	 * 
	 * @param parentMainWindow
	 *            the main window pane
	 */
	public UnitToolBar(MainWindow parentMainWindow) {

		// Use JToolBar constructor
		super();

		setOpaque(false);
	    setBackground(new Color(0,0,0,128));
	    
		// Initialize data members
		unitButtons = new ArrayList<UnitButton>();
		this.parentMainWindow = parentMainWindow;
		
		// Set name
		setName("Unit Toolbar");

		// Fix tool bar
		setFloatable(false);

		// Set preferred height to 57 pixels.
		setPreferredSize(new Dimension(0, 57));

		// Set border around toolbar
		setBorder(new BevelBorder(BevelBorder.RAISED));	

	}

	/**
	 * Constructs a UnitToolBar object
	 * 
	 * @param parentMainWindow
	 *            the main window pane
	 */
	public UnitToolBar(MainScene scene) {

		// Use JToolBar constructor
		super();

		setOpaque(false);
	    setBackground(new Color(0,0,0,128));
	    
		// Initialize data members
		unitButtons = new ArrayList<UnitButton>();
		this.mainScene = mainScene;
		
		// Set name
		setName("Unit Toolbar");

		// Fix tool bar
		setFloatable(false);

		// Set preferred height to 57 pixels.
		setPreferredSize(new Dimension(0, 57));

		// Set border around toolbar
		setBorder(new BevelBorder(BevelBorder.RAISED));	

	}
	
	/**
	 * Create a new unit button in the toolbar.
	 * 
	 * @param unit
	 *            the unit to make a button for.
	 */
	public void createUnitButton(Unit unit) {

		// Check if unit button already exists
		boolean alreadyExists = false;
		//Iterator<UnitButton> i = unitButtons.iterator();
		//while (i.hasNext()) {
		for (UnitButton unitButton : unitButtons) {//= i.next();
			if (unitButton.getUnit() == unit)
				alreadyExists = true;
		}

		if (!alreadyExists) {
			UnitButton tempButton = new UnitButton(unit);
			tempButton.addActionListener(this);
			add(tempButton);
			validate();
			repaint();
			unitButtons.add(tempButton);
		}
	}

	/**
	 * Disposes a unit button in toolbar.
	 * 
	 * @param unit the unit whose button is to be removed.
	 */
	public void disposeUnitButton(Unit unit) {
		Iterator<UnitButton> i = unitButtons.iterator();
		while (i.hasNext()) {
			UnitButton unitButton = i.next();
			
			unitButton.setBorderPainted( false );
			unitButton.setContentAreaFilled( false );
			
			if (unitButton.getUnit() == unit) {
				remove(unitButton);
				validate();
				repaint();
				i.remove();
			}
		}
	}

	/** ActionListener method overridden */
	public void actionPerformed(ActionEvent event) {
		// show unit window on desktop
		Unit unit = ((UnitButton) event.getSource()).getUnit();
		parentMainWindow.getDesktop().openUnitWindow(unit, false);
	}


	/**
	 * Gets all the units in the toolbar.
	 * 
	 * @return array of units.
	 */
	public Unit[] getUnitsInToolBar() {
		Unit[] result = new Unit[unitButtons.size()];
		for (int x = 0; x < unitButtons.size(); x++)
			result[x] = unitButtons.get(x).getUnit();
		return result;
	}
	
	@Override
	protected void addImpl(Component comp, Object constraints, int index) {
		super.addImpl(comp, constraints, index);
		if (comp instanceof JButton) {
			((JButton) comp).setContentAreaFilled(false);
		}
	}
	
    @Override
    protected JButton createActionComponent(Action a) {
        JButton jb = super.createActionComponent(a);
        jb.setOpaque(false);
        return jb;
    }
    
}