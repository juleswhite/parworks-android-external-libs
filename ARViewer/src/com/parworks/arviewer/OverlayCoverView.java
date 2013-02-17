package com.parworks.arviewer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.util.Log;

import com.parworks.androidlibrary.response.ImageOverlayInfo;
import com.parworks.androidlibrary.response.OverlayBoundary.OverlayBoundaryType;
import com.parworks.androidlibrary.response.OverlayCover.OverlayCoverType;

/**
 * The view to draw both the overlay boundary and the cover.
 * <p>
 * This class replaces the old dashed_line.xml in drawable.
 * 
 * @author yusun
 */
public class OverlayCoverView extends ShapeDrawable {

	private static final int STROKE_WIDTH = 10;
	private static final int DEFAULT_STROKE_COLOR = Color.CYAN;
	private static final int DEFAULT_COVER_COLOR = Color.WHITE;
	private static final int DEFAULT_COVER_TRANSPARENCY = 100; //0 to 255
	
	public static final String TAG = OverlayCoverView.class.getName();
	
	/** Paint used to do the overlay cover */
	private Paint fillpaint;
	/** Paint used to do the overlay boundary */
	private Paint strokepaint;
	 
    public OverlayCoverView(ImageOverlayInfo overlay) {
        super(new RectShape());
        
        fillpaint = new Paint(this.getPaint());
        
        // check if to draw the regular overlay cover
        if (overlay.getConfiguration().getCover() == null
        		|| overlay.getConfiguration().getCover().getOverlayCoverType() == OverlayCoverType.IMAGE
        		|| overlay.getConfiguration().getCover().getOverlayCoverType() == OverlayCoverType.HIDE) {
        	// fillpaint.setColor(Color.TRANSPARENT);
        	fillpaint = null;
        } else {
        	// TODO set the color as specified
        	String coverColorString = overlay.getConfiguration().getCover().getColor();
        	int coverColor;
        	if(coverColorString != null) {
	        	try {
	        		coverColor = Color.parseColor(coverColorString);
	        	} catch(IllegalArgumentException parseColorException) {
	        		Log.e(TAG, "OverlayCoverView illegal argument exception. Couldn't parse the color " + coverColorString + ". " + parseColorException.getMessage());
	        		coverColor = DEFAULT_COVER_COLOR;
	        	}
        	} else {
        		coverColor = DEFAULT_COVER_COLOR;
        	}
        	fillpaint.setColor(coverColor);
        	
	        // TODO: set transparency as specified
        	int transparency = overlay.getConfiguration().getCover().getTransparency();
        	if( (0 <= transparency) && (transparency <= 255)) {
        		fillpaint.setAlpha(transparency);
        	} else {
        		fillpaint.setAlpha(DEFAULT_COVER_TRANSPARENCY);
        		Log.e(TAG, "Cover transparency was not between 0 and 255. Using default value instead.");
        	}
        }
        
        // only process the boundary if it is specified
        if (overlay.getConfiguration().getBoundary() != null
        		&& overlay.getConfiguration().getBoundary().getOverlayBoundaryType() != OverlayBoundaryType.HIDE) {
        	if (fillpaint == null) {
        		strokepaint = new Paint(this.getPaint());
        	} else {
        		strokepaint = new Paint(fillpaint);
        	}        	
	        strokepaint.setStyle(Paint.Style.STROKE);
	        
	        // the width is configurable, but we can keep it constant now
	        strokepaint.setStrokeWidth(STROKE_WIDTH);
	        
	        // TODO configure the color of the boundary
        	String boundaryColorString = overlay.getConfiguration().getBoundary().getColor();
        	int boundaryColor;
        	if(boundaryColorString != null) {
	        	try {
	        		boundaryColor = Color.parseColor(boundaryColorString);
	        	} catch(IllegalArgumentException parseColorException) {
	        		Log.e(TAG, "OverlayCoverView illegal argument exception. Couldn't parse the color " + boundaryColorString + ". " + parseColorException.getMessage());
	        		boundaryColor = DEFAULT_STROKE_COLOR;
	        	}
        	} else {
        		boundaryColor = DEFAULT_STROKE_COLOR;
        	}
        	strokepaint.setColor(boundaryColor);
	        
	        // check if we use dashed line or not
	        // we use dashed line for default and specified type
	        if (overlay.getConfiguration().getBoundary().getOverlayBoundaryType() 
	        		!= OverlayBoundaryType.SOLID) {
	        	strokepaint.setPathEffect(new DashPathEffect(new float[] {10, 10}, 0));
	        }
        }
    }
 
    @Override
    protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
    	if (fillpaint != null) {
    		shape.draw(canvas, fillpaint);
    	}
        if (strokepaint != null) {
        	shape.draw(canvas, strokepaint);
        }
    }
}
