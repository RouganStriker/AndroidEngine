/** 
 * Texture Class for Android Game Engine
 * Teach Yourself Android 4.0 Game Programming in 24 Hours
 * Copyright (c)2012 by Jonathan S. Harbour
 */

package game.engine;

import java.io.IOException;
import java.io.InputStream;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

public class Texture {

    private Context p_context;
    private Bitmap p_bitmap;
    private String p_textureName;
    private Point p_size;

    public Texture(Context context) {
        p_context = context;
        p_bitmap = null;
        p_size = new Point(-1,-1);
    }
    
    public Bitmap getBitmap() {
        return p_bitmap;
    }
    
    public Point getSize(){
    	return p_size;
    }
    
    public String getName(){
    	return p_textureName;
    }
    
    public void clearBitmap() {
        p_bitmap = null;
    }
    
    public boolean loadFromAsset(String filename) {
        InputStream istream=null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            istream = p_context.getAssets().open(filename);
            p_bitmap = BitmapFactory.decodeStream(istream,null,options);
            istream.close();
            p_size.set(p_bitmap.getHeight(), p_bitmap.getWidth());
            p_textureName = filename;
        } catch (IOException e) {
            return false;
        }
        return true;
    }
    
}

