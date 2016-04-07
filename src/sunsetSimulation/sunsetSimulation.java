/**
 * sunsetSimulation.java
 * 
 * David Smith
 * Joseph Davidson
 * Logan McCollough
 * Electric Life Engineering
 * 602-920-8257
 * david.earl.smith@gmail.com
 * 
 * Modernization of existing Sunset Crater Visitor Center
 * exhibit featuring a 3D simulation of the eruption.
 * Modified to augment the original closed-source program
 * to provide touchscreen controls.
 * 
 * The original simulation is a Macromedia/Adobe Director
 * file whose source is unavailable, created circa 2004.
 * 
 * This overlay expects the original executable to be
 * available locally at:
 * "C:/ERUPTION SEQUENCE/Sunset Eruption Sony No Skybox.exe"
 * See class constants below.
 * 
 * Please contact the authors for licensing information.
 */
package sunsetSimulation;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.ImageIcon;

import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

/**
 * @author David Smith
 * @version 1.0
 * 
 * sunsetSimulation creates the fullscreen
 * transparent overlay, adds clickable buttons,
 * takes focus, hides the mouse, and executes the
 * original simulation.
 * 
 * Button clicks cause keypress events to be faked
 * via the Robot class and passed to the simulation
 * executable.
 * 
 * Takes focus after ten seconds without input, to
 * ensure a zero-maintenance kiosk state even in
 * unexpected circumstances.
 * 
 * Focus and keypress passing are handled via JNA,
 * and make this software compatible with Microsoft
 * Windows only.
 */
public class sunsetSimulation extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final String ourName = "Sunset Simulation Button Overlay";
	private static final String simulationName = "Sunset Eruption Sony No Skybox";
	private static final String simulationPath = "C:/ERUPTION SEQUENCE/" + simulationName + ".exe";
	private static final int simulationWidth = 1360;
	private static final int simulationHeight = 768;
	private static final int[] pressableKeys = new int[]{
			KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
			KeyEvent.VK_5, KeyEvent.VK_C, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT,
			KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT, KeyEvent.VK_X
	};
	private JPanel contentPane;
	private simulationButton activeButton;
	private simulationButton clickedButton;
	private Timer ifaceTimer;
	private Timer keyTimer;
	
	/**
	 * @param args  Command-line arguments, discarded.
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					sunsetSimulation frame = new sunsetSimulation();
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					frame.setUndecorated(true);
					frame.setBackground(new Color(0, 0, 0, 1));
					frame.setAlwaysOnTop(true);
					frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
					frame.setTitle(ourName);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Constructor, see class documentation.
	 */
	public sunsetSimulation() {
		JPanel parentPane = new JPanel();
		parentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		parentPane.setLayout(null);
		setContentPane(parentPane);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		contentPane.setOpaque(false);
		contentPane.setLayout(null);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double contentXOff = screenSize.getWidth();
		double contentYOff = screenSize.getHeight();
		contentXOff = ((contentXOff - simulationWidth) / 2);
		contentYOff = ((contentYOff - simulationHeight) / 2);
		contentPane.setBounds( (int)contentXOff, (int)contentYOff, simulationWidth, simulationHeight );
		parentPane.add(contentPane);
		
		// Make the mouse invisible
		BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
		    cursorImg, new Point(0, 0), "blank cursor");
		parentPane.setCursor(blankCursor);
		
		// Timer that checks focus every ten idle seconds
		ActionListener interfaceChecker = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setState(Frame.NORMAL);
				setForegroundWindowByName(ourName, true);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				checkForeground();
				allKeysUp();
			}
		};
		ifaceTimer = new Timer(10000, interfaceChecker);
		ifaceTimer.setRepeats(true);
		ifaceTimer.start();
		
		// Timer that lifts key after momentary delay for radio buttons
		ActionListener keyLifter = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(clickedButton != null) { liftKey(clickedButton.key); }
			}
		};
		keyTimer = new Timer(100, keyLifter);
		keyTimer.setRepeats(false);

		ImageIcon leftIconIdle = createImageIcon("assets/left_icon_idle.png", "Spin left");
		ImageIcon leftIconClicked = createImageIcon("assets/left_icon_clicked.png", "Spinning left");
		simulationButton btnLeft = new simulationButton(leftIconIdle, leftIconClicked,
				KeyEvent.VK_RIGHT, new Rectangle(0, 0, 100, 100));
		addClickListeners(btnLeft);
		contentPane.add(btnLeft);

		ImageIcon rightIconIdle = createImageIcon("assets/right_icon_idle.png", "Spin right");
		ImageIcon rightIconClicked = createImageIcon("assets/right_icon_clicked.png", "Spinning right");
		simulationButton btnRight = new simulationButton(rightIconIdle, rightIconClicked,
				KeyEvent.VK_LEFT, new Rectangle(100, 0, 100, 100));
		addClickListeners(btnRight);
		contentPane.add(btnRight);

		ImageIcon upIconIdle = createImageIcon("assets/up_icon_idle.png", "Pan up");
		ImageIcon upIconClicked = createImageIcon("assets/up_icon_clicked.png", "Panning up");
		simulationButton btnUp = new simulationButton(upIconIdle, upIconClicked,
				KeyEvent.VK_UP, new Rectangle(0, 100, 100, 120));
		addClickListeners(btnUp);
		contentPane.add(btnUp);

		ImageIcon downIconIdle = createImageIcon("assets/down_icon_idle.png", "Pan down");
		ImageIcon downIconClicked = createImageIcon("assets/down_icon_clicked.png", "Panning down");
		simulationButton btnDown = new simulationButton(downIconIdle, downIconClicked,
				KeyEvent.VK_DOWN, new Rectangle(100, 100, 100, 120));
		addClickListeners(btnDown);
		contentPane.add(btnDown);

		ImageIcon inIconIdle = createImageIcon("assets/in_icon_idle.png", "Zoom in");
		ImageIcon inIconClicked = createImageIcon("assets/in_icon_clicked.png", "Zooming in");
		simulationButton btnIn = new simulationButton(inIconIdle, inIconClicked,
				KeyEvent.VK_5, new Rectangle(0, 220, 100, 150));
		addClickListeners(btnIn);
		contentPane.add(btnIn);

		ImageIcon outIconIdle = createImageIcon("assets/out_icon_idle.png", "Zoom out");
		ImageIcon outIconClicked = createImageIcon("assets/out_icon_clicked.png", "Zooming out");
		simulationButton btnOut = new simulationButton(outIconIdle, outIconClicked,
				KeyEvent.VK_C, new Rectangle(100, 220, 100, 150));
		addClickListeners(btnOut);
		contentPane.add(btnOut);

		// Position of the timeline vertically
		int timelineOffset = ((simulationWidth - 938) / 2);
		
		ImageIcon tSummIconIdle = createImageIcon("assets/timeline_summ_idle.png", "Start entire eruption");
		ImageIcon tSummIconClicked = createImageIcon("assets/timeline_summ_clicked.png", "Selected entire eruption");
		ImageIcon tSummIconActive = createImageIcon("assets/timeline_summ_active.png", "Playing entire eruption");
		simulationButton btnSummary = new simulationButton(tSummIconIdle, tSummIconClicked, tSummIconActive,
				KeyEvent.VK_X, new Rectangle(timelineOffset, 525, 168, 200));
		addClickListeners(btnSummary);
		contentPane.add(btnSummary);
		
		ImageIcon tPreIconIdle = createImageIcon("assets/timeline_pre_idle.png", "Start pre-eruption");
		ImageIcon tPreIconClicked = createImageIcon("assets/timeline_pre_clicked.png", "Selected pre-eruption");
		ImageIcon tPreIconActive = createImageIcon("assets/timeline_pre_active.png", "Playing pre-eruption");
		simulationButton btnPreStage = new simulationButton(tPreIconIdle, tPreIconClicked, tPreIconActive,
				KeyEvent.VK_CONTROL, new Rectangle((timelineOffset + 168), 525, 197, 200));
		addClickListeners(btnPreStage);
		contentPane.add(btnPreStage);

		ImageIcon tStage1IconIdle = createImageIcon("assets/timeline_stage1_idle.png", "Start fissure eruption");
		ImageIcon tStage1IconClicked = createImageIcon("assets/timeline_stage1_clicked.png", "Selected fissure eruption");
		ImageIcon tStage1IconActive = createImageIcon("assets/timeline_stage1_active.png", "Playing fissure eruption");
		simulationButton btnStage1 = new simulationButton(tStage1IconIdle, tStage1IconClicked, tStage1IconActive,
				KeyEvent.VK_ALT, new Rectangle((timelineOffset + 365), 525, 188, 200));
		addClickListeners(btnStage1);
		contentPane.add(btnStage1);

		ImageIcon tStage2IconIdle = createImageIcon("assets/timeline_stage2_idle.png", "Start cone building");
		ImageIcon tStage2IconClicked = createImageIcon("assets/timeline_stage2_clicked.png", "Selected cone building");
		ImageIcon tStage2IconActive = createImageIcon("assets/timeline_stage2_active.png", "Playing cone building");
		simulationButton btnStage2 = new simulationButton(tStage2IconIdle, tStage2IconClicked, tStage2IconActive,
				KeyEvent.VK_SPACE, new Rectangle((timelineOffset + 553), 525, 197, 200));
		addClickListeners(btnStage2);
		contentPane.add(btnStage2);

		ImageIcon tStage3IconIdle = createImageIcon("assets/timeline_stage3_idle.png", "Start lava flows");
		ImageIcon tStage3IconClicked = createImageIcon("assets/timeline_stage3_clicked.png", "Selected lava flows");
		ImageIcon tStage3IconActive = createImageIcon("assets/timeline_stage3_active.png", "Playing lava flows");
		simulationButton btnStage3 = new simulationButton(tStage3IconIdle, tStage3IconClicked, tStage3IconActive,
				KeyEvent.VK_SHIFT, new Rectangle((timelineOffset + 750), 525, 188, 200));
		addClickListeners(btnStage3);
		contentPane.add(btnStage3);

		startTheSimulation();
	}

	/**
	 * Handles creation of the separate execution
	 * environment for the closed-source simulation.
	 */
	public void startTheSimulation() {
		try {
			Runtime runTime = Runtime.getRuntime();
			runTime.exec(simulationPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param button  the button to attach listeners
	 * All of the buttons on the interface have a
	 * nearly-identical set of listener requirements.
	 * This function unifies the setup process for
	 * those buttons. It is part of the overlay, not
	 * the button, because the listeners must interact
	 * with each other (EG for radio button behavior).
	 * Four states:
	 *   Mouse clicks
	 *   Mouse releases an existing click
	 *   Mouse leaves button during a click
	 *   Mouse enters button while clicked
	 */
	protected void addClickListeners(simulationButton button) {
		if(button.isMomentary) {
			button.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e1) { clickHandler(button, false); }
				@Override
				public void mouseReleased(MouseEvent e1) { clickHandler(button, true); }
				@Override
				public void mouseEntered(MouseEvent e1) {
					if ((e1.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
						clickHandler(button, false);
					}
				}
				@Override
				public void mouseExited(MouseEvent e1) {
					if ((e1.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
						clickHandler(button, true);
					}
				}
			});
		} else {
			button.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e1) { clickHandler(button, false); }
				@Override
				public void mouseReleased(MouseEvent e1) { clickHandler(button, true); }
				@Override
				public void mouseEntered(MouseEvent e1) {
					if ((e1.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
						clickHandler(button, false);
					}
				}
				@Override
				public void mouseExited(MouseEvent e1) {
					if ((e1.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
						clickHandler(button, true);
					}
				}
			});
		}
	}
	
	/**
	 * @param button    the button that was interacted with
	 * @param released  whether this was the end of the interaction
	 * Launch the application.
	 */
	protected void clickHandler(simulationButton button, boolean released) {
		ifaceTimer.restart();
		// Handle the button that was clicked before
		if(clickedButton != null){
			if(keyTimer.isRunning()) { keyTimer.stop(); }
			liftKey(clickedButton.key);
			clickedButton.setState(simulationButton.IDLE);
			clickedButton = null;
		}
		// Handle the previously-selected radio button
		if(activeButton != null) {
			if(button.isMomentary && !released){
				// Newly-clicked button is a different radio button
				activeButton.setState(simulationButton.IDLE);
			} else {
				// Active radio button isn't clicked anymore
				activeButton.setState(simulationButton.ACTIVE);
			}
		}
		if(!released) {
			button.setState(simulationButton.CLICKED);
			clickedButton = button;
			pressKey(button.key);
			if(button.isMomentary){
				activeButton = button;
				keyTimer.restart();
			}
		}
		contentPane.repaint();
	}

	/**
	 * @param key  the keyboard key to press
	 * Creates a Robot that sends a simulated
	 * key-down keyboard event to the application
	 * that has OS-level focus. Also makes sure
	 * that the application with focus is the
	 * simulation.
	 */
	protected void pressKey(int key){
		checkForeground();
		Robot typer;
		try {
			typer = new Robot();
			typer.keyPress(key);
		} catch (AWTException e2) {
			e2.printStackTrace();
		}
	}
	
	/**
	 * @param key  the keyboard key to release
	 * Creates a Robot that sends a simulated
	 * key-up keyboard event to the application
	 * that has OS-level focus. Also makes sure
	 * that the application with focus is the
	 * simulation.
	 */
	protected void liftKey(int key){
		checkForeground();
		Robot typer;
		try {
			typer = new Robot();
			typer.keyRelease(key);
		} catch (AWTException e2) {
			e2.printStackTrace();
		}
	}

	/**
	 * Makes sure none of the keys that the
	 * overlay ever presses are currently down,
	 * from the simulation's point of view.
	 */
	protected void allKeysUp(){
		for (int key : pressableKeys) {
			liftKey(key);
		}
	}

	/**
	 * @param path         path to the asset
	 * @param description  text-based representation
	 * @return the asset requested, in ImageIcon format
	 * Function for collecting needed assets.
	 */
	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	/**
	 * Makes sure the simulation is the
	 * OS-level application with focus.
	 */
	protected void checkForeground() {
		setForegroundWindowByName(simulationName, true);
	}

	/**
	 * @author Stack Overflow
	 * @version 1.0
	 * Provides OS-level access to application/window
	 * names, and to set the application that has focus.
	 */
	public interface User32 extends StdCallLibrary {
		User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);
		interface WNDENUMPROC extends StdCallCallback {
			boolean callback(Pointer hWnd, Pointer arg);
		}
		boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer arg);
		int GetWindowTextA(Pointer hWnd, byte[] lpString, int nMaxCount);
		int SetForegroundWindow(Pointer hWnd);
		Pointer GetForegroundWindow();
	}

	/**
	 * @param windowName  the OS-level window name
	 * @param starting    actual name must match windowName from start
	 * @return match result
	 */
	public static boolean setForegroundWindowByName(final String windowName, final boolean starting) {
		final User32 user32 = User32.INSTANCE;
		return user32.EnumWindows(new User32.WNDENUMPROC() {
			@Override
			public boolean callback(Pointer hWnd, Pointer arg) {
				byte[] windowText = new byte[512];
				user32.GetWindowTextA(hWnd, windowText, 512);
				String wText = Native.toString(windowText);
				//System.out.println(wText);
				if (starting) {
					if (wText.startsWith(windowName)) {
						user32.SetForegroundWindow(hWnd);
						return false;
					}
				} else {
					if (wText.contains(windowName)) {
						user32.SetForegroundWindow(hWnd);
						return false;
					}
				}
				return true;
			}
		}, null);
	}

}
