/* 
 **
 ** Copyright 2013, Jules White
 **
 ** 
 */
package com.parworks.arviewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.parworks.androidlibrary.response.ImageOverlayInfo;

public class OverlayGroups {

	private List<String> mGroupNames;
	private Map<String, List<ImageOverlayInfo>> mOverlays;
	
	public OverlayGroups(List<ImageOverlayInfo> overlays){
		refresh(overlays);
	}
	
	public void refresh(List<ImageOverlayInfo> overlays){
		mGroupNames = new ArrayList<String>();
		mOverlays = new HashMap<String, List<ImageOverlayInfo>>();
		
		for(ImageOverlayInfo overlay : overlays){
			String group = getGroupName(overlay);
			if(group != null){
				if( mOverlays.get(group) == null){
					mGroupNames.add(group);
				}
				getOverlaysForGroup(group).add(overlay);
			}
		}
		
		Collections.sort(mGroupNames);
		
		mGroupNames.add("All");
		getOverlaysForGroup("All").addAll(overlays);
	}
	
	public String getGroupName(ImageOverlayInfo overlay){
		return overlay.getName();
	}
	
	public List<String> getGroupNames() {
		return mGroupNames;
	}

	public void setGroupNames(List<String> groupNames) {
		mGroupNames = groupNames;
	}

	public List<ImageOverlayInfo> getOverlaysForGroup(String group) {
		List<ImageOverlayInfo> overlays = mOverlays.get(group);
		if(overlays == null){
			overlays = new ArrayList<ImageOverlayInfo>();
			mOverlays.put(group, overlays);
		}
		
		return overlays;
	}

}
