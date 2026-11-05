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
	private SpriteFont inventory,inventoryOpen,moveInventory,closeInventory,lineInventory,wasdLable, arrowLable,arrowUpLable, arrowDownLable, enterLabel,linePlayer,player;

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

		pauseLabel = new SpriteFont("PAUSE (Press \"P\" to unpause)", 280, 400, "Arial", 18, Color.white);
		pauseLabel.setOutlineColor(Color.black);
		pauseLabel.setOutlineThickness(2.0f);

		controlLable = new SpriteFont("GAME CONTROLS", 280, 100, "Arial", 30, Color.white);

		player = new SpriteFont("Player Controls",185,150,"Arial", 14, Color.white);
		linePlayer = new SpriteFont("-----------------------------------------", 185, 160, "Arial", 14, Color.white);
		wasdLable = new SpriteFont("WASD            Move", 185, 180, "Arial", 14, Color.white);
		arrowLable = new SpriteFont("Arrows           Move", 185, 200, "Arial", 14, Color.white);
		enterLabel = new SpriteFont("Enter             Select/Continue", 185, 220, "Arial", 14, Color.white);
		arrowUpLable = new SpriteFont("Up Arrow        Dialogue Up", 185, 240, "Arial", 14, Color.white);
		arrowDownLable = new SpriteFont("Down Arrow   Dialogue Down", 185, 260, "Arial", 14, Color.white);


		inventory = new SpriteFont("Inventory Controls", 185, 310, "Arial", 14, Color.white);
		lineInventory = new SpriteFont("-----------------------------------------", 185, 320, "Arial", 14, Color.white);
		inventoryOpen = new SpriteFont("I Key                   Open", 185, 340, "Arial", 14, Color.white);
		moveInventory =  new SpriteFont("Move Selection   Arrows", 185, 360, "Arial", 14, Color.white);
		closeInventory = new SpriteFont("ESC Key			             Close",  185, 380, "Arial", 14, Color.white);

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
			graphicsHandler.drawFilledRectangle(180, 135, 450, 300, Color.white);
			graphicsHandler.drawFilledRectangle(0, 0, ScreenManager.getScreenWidth(), ScreenManager.getScreenHeight(), new Color(0, 19, 0, 100));
			pauseLabel.draw(graphicsHandler);
			controlLable.draw(graphicsHandler);

			wasdLable.draw(graphicsHandler);
			arrowLable.draw(graphicsHandler);
			enterLabel.draw(graphicsHandler);
			arrowUpLable.draw(graphicsHandler);
			arrowDownLable.draw(graphicsHandler);
			inventory.draw(graphicsHandler);
			moveInventory.draw(graphicsHandler);
			linePlayer.draw(graphicsHandler);
			player.draw(graphicsHandler);
			lineInventory.draw(graphicsHandler);
			inventoryOpen.draw(graphicsHandler);
			closeInventory.draw(graphicsHandler);
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
