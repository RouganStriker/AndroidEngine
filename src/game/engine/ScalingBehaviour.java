/** 
 * ThrobAnimation Class
 */
package game.engine;

public class ScalingBehaviour extends Animation {
    private float p_speed, p_scaleBy;
    private Float2 p_endScale;
   
    /*
     * 
     * @param  scaleBy  value to scale sprite by, > 1 to enlarge, < 1 to shrink
     * @param  speed    speed at which to scale (positive value)
     */
    public ScalingBehaviour(float scaleBy, float speed) { 
        animating = true;
        this.p_scaleBy = scaleBy;
        this.p_endScale = null;
        this.p_speed = (p_scaleBy >= 1) ? speed : speed * -1;
    }
    
    @Override
    public Float2 adjustScale(Float2 original) {
        Float2 modified = original;
        
        if(p_endScale == null){
        	p_endScale = new Float2(original.x * p_scaleBy, original.y * p_scaleBy);
        }
        
        modified.x += p_speed;
        modified.y += p_speed;

        if ((p_speed >= 0 && (modified.x >= p_endScale.x || modified.y >= p_endScale.y)) ||
        	(p_speed < 0 && (modified.x <= p_endScale.x || modified.y <= p_endScale.y))) {
        	animating = false;
        }

        return modified;
    }

}
