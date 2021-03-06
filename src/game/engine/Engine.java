/**
 * Android Game Engine Core Class (FINISHED)
 * 
 * THIS IS THE *FINAL* H23 REVISION!
 * 
 * Teach Yourself Android 4.0 Game Programming in 24 Hours
 * Copyright (c)2012 by Jonathan S. Harbour
 */

package game.engine; 
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.ListIterator;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.renderscript.Float3;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;

/**
 * Engine Core Class 
 */
public abstract class Engine extends Activity implements Runnable, OnTouchListener {
	protected static Engine instance;
    private SurfaceView p_view;
    private Canvas p_canvas, p_drawing_canvas;
    private Bitmap p_canvas_ss;
    private Thread p_thread; 
    private boolean p_running, p_paused;
    private boolean p_forceDraw; 	// To force a draw even if the thread is paused
    private int p_pauseCount;
    private Paint p_paintDraw, p_paintFont;
    private Typeface p_typeface;
    private Point[] p_touchPoints;
    private int p_numPoints; 
    private long p_preferredFrameRate, p_sleepTime;
    private Point p_screenSize;
    protected boolean p_collisionIgnoreSameID;
    protected boolean debug_showCollisionBoundaries;
    protected boolean debug_showFPS;
    private LinkedList<Sprite> p_group;
    protected Button button1;
    protected Button button2;
    
    private Bitmap sprAIntersection, sprBIntersection;

    /**
     * Engine constructor
     */
    public Engine() {
        Log.d("Engine","Engine constructor");
        instance = this;
        p_view = null;
        p_canvas = null;
        p_thread = null;
        p_running = false;
        p_paused = false;
        p_paintDraw = null;
        p_paintFont = null;
        p_numPoints = 0;
        p_typeface = null;
        p_preferredFrameRate = 40;
        p_sleepTime = 1000 / p_preferredFrameRate;
        p_pauseCount = 0;
        p_group = new LinkedList<Sprite>();
        p_collisionIgnoreSameID = false;
        debug_showCollisionBoundaries = false;
        debug_showFPS = false;
        p_forceDraw = false;
    }
    
    /**
     * Abstract methods that must be implemented in the sub-class!
     */
    public abstract void init();
    public abstract void load();
    public abstract void draw();
    public abstract void update();
    public abstract void collision(Sprite sprite);

    public static Engine getInstance(){
    	return instance;
    }

    /**
     * Activity.onCreate event method
     */
    @Override 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Engine","Engine.onCreate start");

        //disable the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        //set default screen orientation
        setScreenOrientation(ScreenModes.LANDSCAPE);

        /**
         * Call abstract init method in sub-class
         */
        init();
        
        //create the view object
        setContentView(R.layout.main);
        p_view = (SurfaceView) findViewById(R.id.surfaceView);

        initializeButtons();

        //turn on touch listening
        p_view.setOnTouchListener(this);
        
        //create the points array
        p_touchPoints = new Point[5];
        for (int n=0; n<5; n++) {
            p_touchPoints[n] = new Point(0,0);
        }

        //create Paint object for drawing styles
        p_paintDraw = new Paint();
        p_paintDraw.setColor(Color.WHITE);
        
        //create Paint object for font settings
        p_paintFont = new Paint();
        p_paintFont.setColor(Color.WHITE);
        p_paintFont.setTextSize(24);
        
        //get the screen dimensions
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        p_screenSize = new Point(dm.widthPixels,dm.heightPixels);
        
        p_canvas_ss = Bitmap.createBitmap(p_screenSize.x, p_screenSize.y, Config.RGB_565);
        p_drawing_canvas = new Canvas();
        p_drawing_canvas.setBitmap(p_canvas_ss);
        
        // Call abstract load method in sub-class!
        load();
        
        //launch the thread
        p_running = true;
        p_thread = new Thread(this);
        p_thread.start();        
        
        Log.d("Engine","Engine.onCreate end");
    }

	protected void initializeButtons() {
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
	}

    /**
     * Runnable.run thread method (MAIN LOOP)
     */
    @Override 
    public void run() {
        Log.d("Engine","Engine.run start");
        
        ListIterator<Sprite> iter=null;    
        
        Timer frameTimer = new Timer();
        int frameCount=0;
        int frameRate=0;
        long startTime=0;
        long timeDiff=0;
        
        while (p_running) {
            // Process frame only if not paused
            if (p_paused) {
            	if(p_forceDraw) {
            		if(p_view.getHolder().getSurface().isValid()) {
            			drawCanvas(frameRate);
            			p_forceDraw = false;
            		}
            	}
            	continue;
            }
            
            // Calculate frame rate
            frameCount++;
            startTime = frameTimer.getElapsed();
            if (frameTimer.stopwatch(1000)) {
                frameRate = frameCount;
                frameCount = 0;
                
                //reset touch input count
                p_numPoints = 0;
            }


            // Call abstract update method in sub-class
            update();
            
            
            collisionTest();

            drawCanvas(frameRate);
            
            collisionCleanUp();

            // Calculate frame update time and sleep if necessary
            timeDiff = frameTimer.getElapsed() - startTime;
            long updatePeriod = p_sleepTime - timeDiff;
            if (updatePeriod > 0) {
                try {
                    Thread.sleep( updatePeriod );
                }
                catch(InterruptedException e) {}
            }
            
        }//while
        Log.d("Engine","Engine.run end");
        System.exit(RESULT_OK);
    }

	protected void drawCanvas(int frameRate) {
		ListIterator<Sprite> iter;
		// begin drawing
		if (beginDrawing()) {
      
		    // Call abstract draw method in sub-class
		    draw();
		    
		   
		    /**
		     * Draw the group entities with transforms
		     */
		    iter = p_group.listIterator(); 
		    while (iter.hasNext()) {
		        Sprite spr = (Sprite)iter.next();
		        if (spr.getAlive()) {
		            spr.animate();
		            spr.draw();
		        }
		    }

		    /**
		     * Print some engine debug info.
		     */
		    if(debug_showFPS) {
			    int x = p_drawing_canvas.getWidth()-150;
			    p_drawing_canvas.drawText("ENGINE", x, 20, p_paintFont);
			    p_drawing_canvas.drawText(toString(frameRate) + " FPS", x, 40, p_paintFont);
			    p_drawing_canvas.drawText("Pauses: " + toString(p_pauseCount), x, 60, p_paintFont);
		    }
		    // done drawing
		    endDrawing();
		}
	}

	protected void collisionCleanUp() {
		collisionCleanUp(p_group);
	}

	protected void collisionCleanUp(LinkedList<Sprite> sprites) {
		/*
		 * Do some cleanup: collision notification, removing
		 * 'dead' sprites from the list.
		 */
		ListIterator<Sprite> iter = sprites.listIterator(); 
		Sprite spr = null;
		while (iter.hasNext()) {
		    spr = (Sprite)iter.next();
		    
		    //remove from list if flagged
		    if (!spr.getAlive()) {
		        iter.remove();
		        continue;
		    }
		    
		    //is collision enabled for this sprite?
		    if (spr.getCollidable()) {
		        
		        //has this sprite collided with anything?
		        if (spr.getCollided()) {

		            //is the target a valid object?
		            if (spr.getOffender() != null) {

		                /*
		                 * External func call: notify game of collision
		                 * (with validated offender)
		                 */
		                collision(spr);

		                //reset offender
		                spr.setOffender(null);
		            }

		            //reset collided state
		            spr.setCollided(false);   
		        }
		    }
		}
	}    
    
    /**
     * BEGIN RENDERING
     * Verify that the surface is valid and then lock the canvas. 
     */
    private boolean beginDrawing() {
        if (!p_view.getHolder().getSurface().isValid()) {
            return false;
        }
        p_canvas = p_view.getHolder().lockCanvas();
        return true;
    }
    
    protected void collisionTest(){
    	collisionTest(p_group, p_group);
    }

    protected void collisionTest(LinkedList<Sprite> spritesA, LinkedList<Sprite> spritesB){
        /**
         * Test for collision between sprite group A and sprite group B.
         * Note that this takes place outside of rendering.
         */
    	ListIterator<Sprite> iterA=null, iterB=null;
    	
        iterA = spritesA.listIterator();
        while (iterA.hasNext()) {
            Sprite sprA = (Sprite)iterA.next();
            if (!sprA.getAlive()) continue;
            if (!sprA.getCollidable()) continue;
                
            /*
             * Improvement to prevent double collision testing
             */
            if (sprA.getCollided()) 
                continue; //skip to next iterator
            
            //iterate the list again
            iterB = spritesB.listIterator(); 
            while (iterB.hasNext()) {
                Sprite sprB = (Sprite)iterB.next();
                if (!sprB.getAlive()) continue;
                if (!sprB.getCollidable()) continue;
                
                /*
                 * Improvement to prevent double collision testing
                 */
                if (sprB.getCollided()) 
                    continue; //skip to next iterator

                //do not collide with itself
                if (sprA == sprB) continue;
                
                /*
                 * Ignore sprites with the same ID? This is an important
                 * consideration. Decide if your game requires it or not.
                 */
                if (p_collisionIgnoreSameID && sprA.getIdentifier() == sprB.getIdentifier())
                    continue;
                
                if (collisionCheck(sprA, sprB)) {
                    sprA.setCollided(true);
                    sprA.setOffender(sprB);
                    sprB.setCollided(true);
                    sprB.setOffender(sprA);
                    break; //exit while
                }
            }
        }
    }
    /**
     * END RENDERING
     * Unlock the canvas to free it for future use.
     */
    private void endDrawing() {
    	p_canvas.drawBitmap(p_canvas_ss, 0, 0, null);
        p_view.getHolder().unlockCanvasAndPost(p_canvas);
    }
    
    /**
     * Activity.onResume event method
     */
    @Override 
    public void onResume() {
        Log.d("Engine","Engine.onResume");
        super.onResume();
        onResumeBehaviour();
    }

	protected void onResumeBehaviour() {
		p_paused = false;
	}

    /**
     * Activity.onPause event method
     */
    @Override 
    public void onPause() {
        Log.d("Engine","Engine.onPause");
        super.onPause();
        onPauseBehaviour();
    }

	protected void onPauseBehaviour() {
		p_paused = true;
		p_pauseCount++;
	}
    
    @Override
    protected void onDestroy() {
    	Log.d("Engine","Engine.onDestroy");
        super.onDestroy();
        
        ListIterator<Sprite> iter = p_group.listIterator(); 
        while (iter.hasNext()) {
            Sprite spr = (Sprite)iter.next();
            spr.destroy();
        }
        
        if(sprAIntersection != null)
        	sprAIntersection.recycle();
        if(sprBIntersection != null)
        	sprBIntersection.recycle();
        if(p_canvas_ss != null)
        	p_canvas_ss.recycle();
    }
    
    /**
     * OnTouchListener.onTouch event method
     */
    @Override 
    public boolean onTouch(View v, MotionEvent event) {
        //count the touch inputs
        p_numPoints = event.getPointerCount();
        if (p_numPoints > 5) p_numPoints = 5;
        
        //store the input values
        for (int n=0; n<p_numPoints; n++) {
            p_touchPoints[n].x = (int)event.getX(n);
            p_touchPoints[n].y = (int)event.getY(n);
        }
        return true;
    }
    
    /**
     * Shortcut methods to duplicate existing Android methods.
     */
    public void fatalError(String msg) {
        Log.e("FATAL ERROR", msg);
        System.exit(0);
    }
    
    /**
     * Drawing helpers
     */
    public void drawText(String text, int x, int y) {
        p_canvas.drawText(text, x, y, p_paintFont);
    }
    
    /**
     * Engine helper get/set methods for private properties.
     */
    public Point getSize() {
        return p_screenSize;
    }
    
    public int getScreenWidth() {
        return p_screenSize.x;
    }
    
    public int getScreenHeight() {
        return p_screenSize.y;
    }
    
    public SurfaceView getView() {
        return p_view;
    }

    public Canvas getCanvas() {
        return p_drawing_canvas;
    }
    
    public Bitmap getScreenShot(){
    	return p_canvas_ss;
    }
    
    public void setFrameRate(int rate) {
        p_preferredFrameRate = rate;
        p_sleepTime = 1000 / p_preferredFrameRate;
    }
    
    public int getTouchInputs() {
        return p_numPoints;
    }
    
    public Point getTouchPoint(int index) {
        if (index > p_numPoints) 
            index = p_numPoints;
        return p_touchPoints[index];
    }
    
    public void setDrawColor(int color) {
        p_paintDraw.setColor(color);
    }
    
    public void setTextColor(int color) {
        p_paintFont.setColor(color);
    }
    
    public void setTextSize(int size) {
        p_paintFont.setTextSize((float)size);
    }

    public void setTextSize(float size) {
        p_paintFont.setTextSize(size);
    }

    /**
     * Font style helper
     */
    public enum FontStyles {
        NORMAL (Typeface.NORMAL),
        BOLD (Typeface.BOLD),
        ITALIC (Typeface.ITALIC),
        BOLD_ITALIC (Typeface.BOLD_ITALIC);
        int value;
        FontStyles(int type) {
            this.value = type;
        }
    }
    
    public void setTextStyle(FontStyles style) {
        
        p_typeface = Typeface.create(Typeface.DEFAULT, style.value);
        p_paintFont.setTypeface(p_typeface);
    }
    
    /**
     * Screen mode helper
     */
    public enum ScreenModes { 
        LANDSCAPE (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
        PORTRAIT (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        int value;
        ScreenModes(int mode) {
            this.value = mode;
        }
    }
    public void setScreenOrientation(ScreenModes mode) {
        setRequestedOrientation(mode.value);
    }
    
    /**
     * Round to a default 2 decimal places
     */
    public double round(double value) {
        return round(value,2);
    }
    
    /**
     * Round to any number of decimal places
     */
    public double round(double value, int precision) {
        try {
            BigDecimal bd = new BigDecimal(value);
            BigDecimal rounded = bd.setScale(precision, BigDecimal.
                    ROUND_HALF_UP);
            return rounded.doubleValue();
        }
        catch (Exception e) {
            Log.e("Engine","round: error rounding number");
        }
        return 0;
    }
    
    /**
     * String conversion helpers
     */
    public String toString(int value) {
        return Integer.toString(value);
    }
    
    public String toString(float value) { 
        return Float.toString(value);
    }
    
    public String toString(double value) { 
        return Double.toString(value);
    }
    
    public String toString(Float2 value) {
        String s = "X:" + round(value.x) + "," +
            "Y:" + round(value.y);
        return s;
    }
    
    public String toString(Float3 value) {
        String s = "X:" + round(value.x) + "," +
            "Y:" + round(value.y) + "," +
            "Z:" + round(value.z);
        return s;
    }
    
    public String toString(Point value) {
        Float2 f = new Float2(value.x,value.y);
        return toString((Float2)f);
    }
    
    public String toString(Rect value) { 
        RectF r = new RectF(value.left, value.top, value.right, 
                value.bottom);
        return toString((RectF)r);
    }

    public String toString(RectF value) { 
        String s = "{" + round(value.left) + "," +
            round(value.top) + "," +
            round(value.right) + "," +
            round(value.bottom) + "}";
        return s;
    }
    
    
    
    /**
     * Entity grouping methods
     */
    
    public void addToGroup(Sprite sprite) {
        p_group.add(sprite);
    }
    
    public void removeFromGroup(Sprite sprite) {
        p_group.remove(sprite); 
    }
    
    public void removeFromGroup(int index) {
        p_group.remove(index);
    }
    
    public int getGroupSize() {
        return p_group.size();
    }
    
    /**
     * Collision detection 
     */
    
    public boolean collisionCheck(Sprite A, Sprite B) {
    	RectF sprABounds = A.getBounds();
    	RectF sprBBounds = B.getBounds();
    	RectF intersection = new RectF();
    	
        if(intersection.setIntersect(sprABounds, sprBBounds)) {
        	// Perform pixel check
        	sprAIntersection = Bitmap.createBitmap(A.getTexture().getBitmap(), (int)(intersection.left - sprABounds.left), (int)(intersection.top - sprABounds.top), (int)intersection.width(), (int)intersection.height());
        	int[] sprAIntersectionPixels = new int[(int) (sprAIntersection.getHeight() * sprAIntersection.getWidth())];
        	sprBIntersection = Bitmap.createBitmap(B.getTexture().getBitmap(), (int)(intersection.left - sprBBounds.left), (int)(intersection.top - sprBBounds.top), (int)intersection.width(), (int)intersection.height());
        	int[] sprBIntersectionPixels = new int[(int) (sprBIntersection.getHeight() * sprBIntersection.getWidth())];

        	sprAIntersection.getPixels(sprAIntersectionPixels, 0, sprAIntersection.getWidth(), 0, 0, sprAIntersection.getWidth(), sprAIntersection.getHeight());
        	sprBIntersection.getPixels(sprBIntersectionPixels, 0, sprBIntersection.getWidth(), 0, 0, sprBIntersection.getWidth(), sprBIntersection.getHeight());
        	
        	for(int i = 0; i < sprAIntersectionPixels.length; i++) {
        		int pixelA = sprAIntersectionPixels[i];
        		int pixelB = sprBIntersectionPixels[i];
        		if(pixelA != Color.TRANSPARENT && pixelB != Color.TRANSPARENT) {
        			return true;
        		}
        	}
        	
        	sprAIntersection.eraseColor(Color.TRANSPARENT);
        	sprBIntersection.eraseColor(Color.TRANSPARENT);
        }

        return false;
    }
    
    public static Texture loadTexture(String textureName){
		Texture texture = new Texture(instance);
		
		if(!texture.loadFromAsset(textureName)){
			Log.e("FATAL ERROR", "Failed to load" + textureName);
	        System.exit(0);
		}
		
		return texture;
	}
    
    public static Texture loadTexture(int textureId){
		Texture texture = new Texture(instance);
		
		if(!texture.loadFromDrawable(textureId)){
			Log.e("FATAL ERROR", "Failed to load" + textureId);
	        System.exit(0);
		}
		
		return texture;
	}
	
	public static Sprite createSprite(){
		Sprite spr = new Sprite(instance);
		
		return spr;
	}
	
	public static Sprite createSprite(String textureName){
		Sprite spr = new Sprite(instance);
		
		spr.setTexture(loadTexture(textureName));

		return spr;
	}
	
	public boolean getShowCollisionBoundaries(){
		return debug_showCollisionBoundaries;
	}
	
	protected boolean isPaused(){
		return p_paused;
	}

	protected void setPaused(boolean bPause) {
		p_paused = bPause;
	}
	
	protected void enableForceRedraw() {
		p_forceDraw = true;
	}
}


