package com.parworks.arviewer;

import java.util.List;

import org.taptwo.android.widget.TitleProvider;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.parworks.androidlibrary.response.ImageOverlayInfo;

public class OverlayViewAdapter extends BaseAdapter implements TitleProvider {

	private OverlayGroups mOverlayGroups;

	private ImageMetrics mImageMetrics;

	private float mScale;

	private int mTitleHeight;
	
	private ARViewerActivity mViewer;

	private OverlayViewFactory mViewFactory = new OverlayViewFactory();

	public OverlayViewAdapter(OverlayGroups overlayGroups, ARViewerActivity viewer,
			ImageMetrics imetrics, float scale, int titleheight) {
		mViewer = viewer;
		mTitleHeight = titleheight;
		mOverlayGroups = overlayGroups;
		mScale = scale;
		mImageMetrics = imetrics;

		mImageMetrics.addAdditionalYOffset(-1 * titleheight);
	}

	@Override
	public int getItemViewType(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public int getCount() {
		return mOverlayGroups.getGroupNames().size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		OverlayView ov = new OverlayView(mViewer);

		String group = mOverlayGroups.getGroupNames().get(position);
		List<ImageOverlayInfo> overlays = mOverlayGroups
				.getOverlaysForGroup(group);
		for (ImageOverlayInfo oi : overlays) {
			mViewFactory.createOverlayView(mViewer, ov, oi, mScale, mScale,
					mImageMetrics, overlays.size() == 1, true);
		}
		
		ov.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mViewer.onTouch(v,event);
			}
		});

		return ov;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.taptwo.android.widget.TitleProvider#getTitle(int)
	 */
	public String getTitle(int position) {
		return mOverlayGroups.getGroupNames().get(position);
	}

}
