/* 
**
** Copyright 2012, Jules White
**
** 
*/
package com.parworks.arviewer;

import android.app.Activity;

import com.parworks.androidlibrary.response.ImageOverlayInfo;

public interface OverlayViewCreator {

	public void createOverlayView(Activity context, OverlayView overlayview,
			ImageOverlayInfo overlay, boolean showpopup, boolean showFadeAnimation, float xscale, float yscale);	
}
