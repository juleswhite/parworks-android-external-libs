/* 
**
** Copyright 2012, Jules White
**
** 
*/
package com.parworks.arviewer;

import android.app.Activity;
import android.app.Dialog;

import com.parworks.androidlibrary.response.ImageOverlayInfo;

public interface OverlayDialogViewCreator {

	public boolean createView(Activity parent, Dialog d, ImageOverlayInfo overlay);
	
}
