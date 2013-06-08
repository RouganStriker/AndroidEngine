package game.engine;

public class FadingDeathAnimation extends AlphaAnimation {

	public FadingDeathAnimation(int change) {
		super(0, 255, change);
	}

	public FadingDeathAnimation() {
		super(0, 255, -5);
	}
	
    public boolean adjustAlive(boolean original) {
    	return (!animating ? false : original);
    }
}
