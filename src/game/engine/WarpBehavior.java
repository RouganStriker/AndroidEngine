/** 
 * WarpBehavior class - derived from Animation 
 * Requires game.engine.Engine to build.
 */
package game.engine;
import android.graphics.*;

public class WarpBehavior extends Animation {
    private RectF p_bounds;
    private Float2 p_velocity;
    private Point p_size;
    private boolean p_adjustWarpPosition;	//Whether or not to account for overstepping the boundary
    
    public WarpBehavior(RectF bounds, int w, int h, Float2 velocity, boolean adjustWarpPosition) {
        this(bounds, new Point(w,h), velocity, adjustWarpPosition);
    }
    
    public WarpBehavior(RectF bounds, Point size, Float2 velocity, boolean adjustWarpPosition) {
        animating = true;
        p_bounds = bounds;
        p_velocity = velocity;
        p_size = size;
        p_adjustWarpPosition = adjustWarpPosition;
    }
    
    @Override
    public Float2 adjustPosition(Float2 original) {
        Float2 modified = original;
        modified.x += p_velocity.x;
        modified.y += p_velocity.y;

        if (modified.x < p_bounds.left)
        {
            modified.x = p_bounds.right - p_size.x - ((p_adjustWarpPosition) ? (p_bounds.left - modified.x) : 0);
        }
        else if (modified.x > p_bounds.right - p_size.x)
        {
            modified.x = p_bounds.left + ((p_adjustWarpPosition) ? (modified.x + p_size.x - p_bounds.right) : 0);
        }
        if (modified.y < p_bounds.top)
        {
            modified.y = p_bounds.bottom - p_size.y + ((p_adjustWarpPosition) ? (p_bounds.top - modified.y) : 0);
        }
        else if (modified.y > p_bounds.bottom - p_size.y)
        {
            modified.y = p_bounds.top + ((p_adjustWarpPosition) ? (modified.y + p_size.y - p_bounds.bottom) : 0);
        }

        return modified;
    }
}
