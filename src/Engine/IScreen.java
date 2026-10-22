package Engine;

// The three methods every screen piece needs. OptionScreen implements this (instead of extending Screen).
public interface IScreen {
    void initialize();
    void update();
    void draw(GraphicsHandler graphicsHandler);
}