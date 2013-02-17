/* 
**
** Copyright 2012, Jules White
**
** 
*/
package com.parworks.arviewer;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.Display;

public class ImageMetrics {

	private Point displaySize = new Point();
	private Point imageSize = new Point();
	private Point imageBitmapSize = new Point();
	private float imageBitmapScaleFactor;
	private int[] imageOffset;
	private int[] additionalOffset = {0,0};
	private Activity context;
	
	public ImageMetrics(Activity ctx, Point imagesz){
		imageSize = imagesz;
		context = ctx;
		computeBitmapSize();
	}
	
	private void computeBitmapSize() {
		Display display = context.getWindowManager().getDefaultDisplay();
		displaySize.x = display.getWidth();
		displaySize.y = display.getHeight();
		
		PointF scale = new PointF();
		scale.x = (float) displaySize.x / imageSize.x;
		scale.y = (float) displaySize.y / imageSize.y;

		imageBitmapScaleFactor = Math.min(scale.x, scale.y);
		imageBitmapSize.x = (int) (imageBitmapScaleFactor * imageSize.x);
		imageBitmapSize.y = (int) (imageBitmapScaleFactor * imageSize.y);
		
		imageOffset = new int[]{
					   (displaySize.x - imageBitmapSize.x) + additionalOffset[0],
					   (displaySize.y - imageBitmapSize.y) + additionalOffset[1]};
	}
	
	public void addAdditionalXOffset(int xoff){
		additionalOffset[0] = xoff;
	}
	
	public void addAdditionalYOffset(int yoff){
		additionalOffset[1] = yoff;
	}
	
	public Point getDisplaySize() {
		return displaySize;
	}

	public void setDisplaySize(Point displaySize) {
		this.displaySize = displaySize;
	}

	public Point getImageSize() {
		return imageSize;
	}

	public void setImageSize(Point imageSize) {
		this.imageSize = imageSize;
	}

	public Point getImageBitmapSize() {
		return imageBitmapSize;
	}

	public void setImageBitmapSize(Point imageBitmapSize) {
		this.imageBitmapSize = imageBitmapSize;
	}

	public float getImageBitmapScaleFactor() {
		return imageBitmapScaleFactor;
	}

	public void setImageBitmapScaleFactor(float imageBitmapScaleFactor) {
		this.imageBitmapScaleFactor = imageBitmapScaleFactor;
	}

	public int[] getFullSizeImageOffset(){
		computeBitmapSize();
		return imageOffset;
	}
}
