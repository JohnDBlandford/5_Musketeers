package Engine;

import GameObject.Rectangle;
import SpriteFont.SpriteFont;
import Utils.Colors;
import javax.swing.*;
import java.awt.*;

/*
 * This is where the game loop process and render back buffer is setup
 */
public class GamePanel extends JPanel {
	// loads Screens on to the JPanel
	// each screen has its own update and draw methods defined to handle a "section"
	// of the game.
	private ScreenManager screenManager;

	// used to draw graphics to the panel
	private GraphicsHandler graphicsHandler;

	private boolean isGamePaused = false;

	// Pause menu labels
	private SpriteFont pauseLabel;
	private SpriteFont controlLable;
	private SpriteFont wKeyLabel, sKeyLabel, aKeyLabel, dKeyLabel, enterKeyLabel;
	private SpriteFont wLabel, sLabel, aLabel, dLabel, arrowUpLable, arrowDownLable, enterLabel;
	private SpriteFont arrowKeyLineLabel, arrowKeyCarrotLable;

	private KeyLocker keyLocker = new KeyLocker();
	private final Key pauseKey = Key.P;
	private Thread gameLoopProcess;

	private Key showFPSKey = Key.G;
	private SpriteFont fpsDisplayLabel;
	private boolean showFPS = false;
	private int currentFPS;
	private boolean doPaint;

	// The JPanel and various important class instances are setup here
	public GamePanel() {
		super();
		this.setDoubleBuffered(true);

		// attaches Keyboard class's keyListener to this JPanel
		this.addKeyListener(Keyboard.getKeyListener());

		graphicsHandler = new GraphicsHandler();

		screenManager = new ScreenManager();

		pauseLabel = new SpriteFont("PAUSE (Press \"P\" to unpause)", 365, 50, "Arial", 24, Color.white);
		pauseLabel.setOutlineColor(Color.black);
		pauseLabel.setOutlineThickness(2.0f);

		controlLable = new SpriteFont("GAME CONTROLS", 50, 100, "Arial", 30, Color.white);

		wKeyLabel = new SpriteFont("W", 118, 155, "Arial", 24, Color.white);
		aKeyLabel = new SpriteFont("A", 66, 214, "Arial", 24, Color.white);
		sKeyLabel = new SpriteFont("S", 122, 214, "Arial", 24, Color.white);
		dKeyLabel = new SpriteFont("D", 178, 214, "Arial", 24, Color.white);
		arrowKeyLineLabel = new SpriteFont("--", 310, 199, "Arial", 24, Color.white);
		arrowKeyCarrotLable = new SpriteFont(">", 325, 201, "Arial", 24, Color.white);

		wLabel = new SpriteFont("W = Move Forward", 50, 260, "Arial", 14, Color.white);
		aLabel = new SpriteFont("A = Move Left", 50, 280, "Arial", 14, Color.white);
		sLabel = new SpriteFont("S = Move Down", 50, 300, "Arial", 14, Color.white);
		dLabel = new SpriteFont("D = Move Right", 50, 320, "Arial", 14, Color.white);
		enterLabel = new SpriteFont("Enter = Select", 260, 300, "Arial", 14, Color.white);

		arrowUpLable = new SpriteFont("Up Arrow = Dialogue Up", 260, 260, "Arial", 14, Color.white);
		arrowDownLable = new SpriteFont("Down Arrow = Dialogue Down", 260, 280, "Arial", 14, Color.white);

		fpsDisplayLabel = new SpriteFont("FPS", 4, 3, "Arial", 12, Color.black);

		currentFPS = Config.TARGET_FPS;

		// this game loop code will run in a separate thread from the rest of the
		// program
		// will continually update the game's logic and repaint the game's graphics
		GameLoop gameLoop = new GameLoop(this);
		gameLoopProcess = new Thread(gameLoop.getGameLoopProcess());
	}

	// this is called later after instantiation, and will initialize screenManager
	public void setupGame() {
		setBackground(Colors.CORNFLOWER_BLUE);
		screenManager.initialize(new Rectangle(getX(), getY(), getWidth(), getHeight()));
	}

	// this starts the timer (the game loop is started here)
	public void startGame() {
		gameLoopProcess.start();
	}

	public ScreenManager getScreenManager() {
		return screenManager;
	}

	public void setCurrentFPS(int currentFPS) {
		this.currentFPS = currentFPS;
	}

	public void setDoPaint(boolean doPaint) {
		this.doPaint = doPaint;
	}

	public void update() {
		updatePauseState();
		updateShowFPSState();

		if (!isGamePaused) {
			screenManager.update();
		}
	}

	private void updatePauseState() {
		if (Keyboard.isKeyDown(pauseKey) && !keyLocker.isKeyLocked(pauseKey)) {
			isGamePaused = !isGamePaused;
			keyLocker.lockKey(pauseKey);
		}

		if (Keyboard.isKeyUp(pauseKey)) {
			keyLocker.unlockKey(pauseKey);
		}
	}

	private void updateShowFPSState() {
		if (Keyboard.isKeyDown(showFPSKey) && !keyLocker.isKeyLocked(showFPSKey)) {
			showFPS = !showFPS;
			keyLocker.lockKey(showFPSKey);
		}

		if (Keyboard.isKeyUp(showFPSKey)) {
			keyLocker.unlockKey(showFPSKey);
		}

		fpsDisplayLabel.setText("FPS: " + currentFPS);
	}

	public void draw() {
		// draw current game state
		screenManager.draw(graphicsHandler);

		// if game is paused, draw pause gfx over Screen gfx
		if (isGamePaused) {
			graphicsHandler.drawFilledRectangle(40, 135, 450, 300, Color.white);
			graphicsHandler.drawFilledRectangle(0, 0, ScreenManager.getScreenWidth(), ScreenManager.getScreenHeight(),
					new Color(0, 19, 0, 100));
			pauseLabel.draw(graphicsHandler);
			controlLable.draw(graphicsHandler);
			graphicsHandler.drawFilledRectangle(105, 145, 50, 50, Color.darkGray); // w Key
			graphicsHandler.drawFilledRectangle(50, 200, 50, 50, Color.darkGray); // a key
			graphicsHandler.drawFilledRectangle(105, 200, 50, 50, Color.darkGray); // s Key
			graphicsHandler.drawFilledRectangle(160, 200, 50, 50, Color.darkGray); // d key
			graphicsHandler.drawFilledRectangleWithBorder(260, 190, 50, 30, Color.darkGray, Color.lightGray, 1); // up
																													// Key
			graphicsHandler.drawFilledRectangleWithBorder(260, 220, 50, 30, Color.darkGray, Color.lightGray, 1); // down
																													// Key
			wKeyLabel.draw(graphicsHandler);
			aKeyLabel.draw(graphicsHandler);
			sKeyLabel.draw(graphicsHandler);
			dKeyLabel.draw(graphicsHandler);
			wLabel.draw(graphicsHandler);
			aLabel.draw(graphicsHandler);
			sLabel.draw(graphicsHandler);
			dLabel.draw(graphicsHandler);
			enterLabel.draw(graphicsHandler);
			arrowUpLable.draw(graphicsHandler);
			arrowDownLable.draw(graphicsHandler);

			// Rotates text
			graphicsHandler.getGraphics().rotate(Math.toRadians(-90), 300, 230);
			arrowKeyLineLabel.draw(graphicsHandler);
			arrowKeyCarrotLable.draw(graphicsHandler);
			graphicsHandler.getGraphics().rotate(Math.toRadians(-180), 310, 220 - 5.5);
			arrowKeyLineLabel.draw(graphicsHandler);
			arrowKeyCarrotLable.draw(graphicsHandler);

		}

		if (showFPS) {
			fpsDisplayLabel.draw(graphicsHandler);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (doPaint) {
			Graphics2D g2 = (Graphics2D) g.create();
			int logicalW = ScreenManager.getScreenWidth();
			int logicalH = ScreenManager.getScreenHeight();
			if (logicalW > 0 && logicalH > 0) {
				g2.setColor(Color.BLACK);
				g2.fillRect(0, 0, getWidth(), getHeight());

				double scale = Math.min(getWidth() / (double) logicalW, getHeight() / (double) logicalH);
				int offsetX = (int) Math.round((getWidth() - logicalW * scale) / 2);
				int offsetY = (int) Math.round((getHeight() - logicalH * scale) / 2);

				g2.translate(offsetX, offsetY);
				g2.scale(scale, scale);
				g2.clipRect(0, 0, logicalW, logicalH);
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			}
			graphicsHandler.setGraphics(g2);
			draw();
			g2.dispose();
		}
	}
}
