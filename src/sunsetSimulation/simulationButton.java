/**
 * simulationButton.java
 * 
 * David Smith
 * Joseph Davidson
 * Logan McCollough
 * Electric Life Engineering
 * 602-920-8257
 * david.earl.smith@gmail.com
 * 
 * See sunsetSimulation.java for information.
 */
package sunsetSimulation;

import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * @author David Smith
 * @version 1.0
 * 
 * simulationButton creates a single
 * clickable button on the overlay interface
 * in sunsetSimulation. Needed mainly to provide
 * additional state information specific to this
 * application, and for convenient asset-switching.
 */
public class simulationButton extends JLabel {

	private static final long serialVersionUID = 1L;
	public final static int IDLE = 0;
	public final static int CLICKED = 1;
	public final static int ACTIVE = 2;
	public ImageIcon idleIcon;
	public ImageIcon clickedIcon;
	public ImageIcon activeIcon;
	public int key;
	public boolean isMomentary;
	public int curState;

	
	/**
	 * @param idleIcon      asset to draw when button is idle
	 * @param clickedIcon   asset to draw when button is clicked
	 * @param key           keyboard key to simulate on click
	 * @param drawPosition  screen position for the button
	 * Constructor to create a button that has no radio-button
	 * style behavior, and sends a continuous rather than
	 * momentary key event.
	 */
	public simulationButton(ImageIcon idleIcon, ImageIcon clickedIcon, int key,
			Rectangle drawPosition) {
		super(idleIcon);
		this.isMomentary = false;
		this.idleIcon = idleIcon;
		this.clickedIcon = clickedIcon;
		this.key = key;
		this.curState = IDLE;
		setBounds(drawPosition);
	}

	/**
	 * @param idleIcon      asset to draw when button is idle
	 * @param clickedIcon   asset to draw when button is clicked
	 * @param activeIcon    asset to draw when button is selected but not clicked
	 * @param key           keyboard key to simulate on click
	 * @param drawPosition  screen position for the button
	 * Constructor to create a button that has radio-button
	 * style behavior, and sends a momentary key event when
	 * selected.
	 */
	public simulationButton(ImageIcon idleIcon, ImageIcon clickedIcon, ImageIcon activeIcon, 
			int key, Rectangle drawPosition) {
		super(idleIcon);
		this.isMomentary = true;
		this.idleIcon = idleIcon;
		this.clickedIcon = clickedIcon;
		this.activeIcon = activeIcon;
		this.key = key;
		this.curState = IDLE;
		setBounds(drawPosition);
	}
	
	/**
	 * @param newState  the state the button is now in
	 * Setter for button state.
	 */
	public void setState(int newState) {
		curState = newState;
		if( curState == ACTIVE ) {
			setIcon(activeIcon);
		} else if( curState == CLICKED ) {
			setIcon(clickedIcon);
		} else {
			setIcon(idleIcon);
		}
	}
	
}
