package game.engine;

public class JumpBehavior extends Animation{
	private float p_gravity;
	private final static float DEFAULT_GRAVITY = 1.0f;
	private Float2 p_velocity;
	private float p_originalPositionY;
	private float p_originalVelocityY;
	private int p_time;
	private boolean bSetOriginalPosition;
	
	public JumpBehavior(Float2 velocity) {
		this(velocity, DEFAULT_GRAVITY);
	}
	
	public JumpBehavior(Float2 velocity, float gravity) {
		animating = true;
		p_gravity = gravity;
		p_velocity = velocity;
		p_originalPositionY = 0;
		p_originalVelocityY = velocity.y;
		bSetOriginalPosition = false;
		p_time = 0;
	}
	
	@Override
    public Float2 adjustPosition(Float2 original) {
        Float2 modified = new Float2(original.x, original.y);
        modified.y += p_velocity.y;

		if (!bSetOriginalPosition) {
			p_originalPositionY = original.y;
			bSetOriginalPosition = true;
		}
        
        if (modified.y >= p_originalPositionY) {
        	modified.y = p_originalPositionY;
        	animating = false;
        }
        
        return modified;
    }
	
	public Float2 adjustVelocity(Float2 original) {		
		p_velocity.y = p_originalVelocityY + p_gravity * p_time;
		p_time++;
		
		Float2 modified = new Float2(p_velocity.x, p_velocity.y);
		
		return modified;
	}
}
