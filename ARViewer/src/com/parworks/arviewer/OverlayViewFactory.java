/* 
 **
 ** Copyright 2012, Jules White
 **
 ** 
 */
package com.parworks.arviewer;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.graphics.Point;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.parworks.androidlibrary.response.ImageOverlayInfo;
import com.parworks.androidlibrary.response.OverlayPoint;
import com.parworks.androidlibrary.response.OverlayCover.OverlayCoverType;

public class OverlayViewFactory {

	public class RectangleCreator implements OverlayViewCreator {

		@SuppressWarnings("deprecation")
		@Override
		public void createOverlayView(Activity context,
				OverlayView overlayview, ImageOverlayInfo overlay,
				boolean showpopup, boolean showFadeAnimation, float xscale, float yscale) {

			// setup the boundary
			ImageView view = new ImageView(context);
			view.setBackgroundDrawable(new OverlayCoverView(overlay));
			if (showFadeAnimation) {
				Animation myFadeInAnimation = AnimationUtils.loadAnimation(
						context, R.anim.pulsate);
				view.startAnimation(myFadeInAnimation);
			}
			
			// setting this to be invisible helps to hide the animition process
			view.setVisibility(View.INVISIBLE);

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					200, 200);
			overlayview.addOverlay(overlay, view, params);

			// retrieve the overlay title
			String popupcon = overlay.getConfiguration().getTitle();
			// show the title popup only when it is specified
			if (showpopup && popupcon != null && !TextUtils.isEmpty(popupcon)) {
				View pop = context.getLayoutInflater().inflate(R.layout.popup,
						null);
				((TextView) pop.findViewById(R.id.arPopupContent))
						.setText(popupcon);

				// potential format configuration for text here
				// pop.setBackgroundResource(R.drawable.popup);
				// pop.setText(overlay.getContent());
				// pop.setTextSize(24);

				overlayview.setPopup(overlay, view, pop);
			}
		}
	}
	
	public double getArea(ImageOverlayInfo overlay, double scale){
		float a1 = 0;
		float b1 = 0;
		
		List<OverlayPoint> points = overlay.getPoints();
		for(int i = 0; i < points.size() -1; i++){
			OverlayPoint p1 = points.get(i);
			OverlayPoint p2 = points.get(i+1);
			a1 += (p1.getX() * p2.getY() * scale);
		}
		
		for(int i = 0; i < points.size() -1; i++){
			OverlayPoint p1 = points.get(i);
			OverlayPoint p2 = points.get(i+1);
			b1 += (p1.getY() * p2.getX() * scale);
		}
		
		float diff = Math.abs(a1 - b1);
		double area = diff/2;
		
		return area;
	}

	public class ImageCreator implements OverlayViewCreator {

		@SuppressWarnings("deprecation")
		@Override
		public void createOverlayView(Activity context,
				OverlayView overlayview, ImageOverlayInfo overlay,
				boolean showpopup, boolean showFadeAnimation, float xscale, float yscale) {

			ImageView view = new ImageView(context);
			view.setAdjustViewBounds(true);
			view.setScaleType(ScaleType.FIT_XY);
			view.setBackgroundDrawable(new OverlayCoverView(overlay));

			String imgres = overlay.getConfiguration().getCover().getProvider();
			if (imgres.startsWith("http")) {
				UrlImageViewHelper.setUrlDrawable(view, imgres,
						android.R.drawable.spinner_background);
			} else {
				// TODO: To support image file path URI e.g., file:///
				int res = context.getResources().getIdentifier(imgres,
						"drawable", context.getPackageName());
				view.setImageResource(res);
			}

			RelativeLayout.LayoutParams params = null;
			if (imgres.endsWith("no-scale")) {
				params = new RelativeLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				
				double area = getArea(overlay,xscale);
				double radius = Math.sqrt(area);
				//radius = (radius < 20)? 20 : radius;
				
				Display display = context.getWindowManager().getDefaultDisplay();
				int max = Math.max(display.getWidth(),display.getHeight());
				radius = (((float)max)/8);
				
				RelativeLayout.LayoutParams pulseparams = new RelativeLayout.LayoutParams(
						(int)Math.rint(radius), (int)Math.rint(radius));
				
				ImageOverlayInfo pinfo = overlay.clone();
				String provider = pinfo.getConfiguration().getCover().getProvider();
				if(provider.indexOf("#offset[") > -1){
					provider = OverlayView.removeProviderParam(overlay, "offset");
					pinfo.getConfiguration().getCover().setProvider(provider);
				}
				ImageView v = new ImageView(context);
				v.setImageDrawable(context.getResources().getDrawable(R.drawable.pulsar));
				Animation a = AnimationUtils.loadAnimation(context, R.anim.pulsate_animation);
				v.startAnimation(a);
				overlayview.addOverlay(pinfo, v, pulseparams);
			} else {
				params = new RelativeLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			}
			overlayview.addOverlay(overlay, view, params);

			// retrieve the overlay title
			String popupcon = overlay.getConfiguration().getTitle();
			// show the title popup only when it is specified
			if (showpopup && popupcon != null && !TextUtils.isEmpty(popupcon)) {
				View pop = context.getLayoutInflater().inflate(R.layout.popup,
						null);
				((TextView) pop.findViewById(R.id.arPopupContent))
						.setText(popupcon);

				// potential format configuration for text here
				// pop.setBackgroundResource(R.drawable.popup);
				// pop.setText(overlay.getContent());
				// pop.setTextSize(24);

				overlayview.setPopup(overlay, view, pop);
			}
		}
	}

	private Map<OverlayCoverType, OverlayViewCreator> mCreators = new EnumMap<OverlayCoverType, OverlayViewCreator>(
			OverlayCoverType.class);

	public OverlayViewFactory() {
		RectangleCreator rect = new RectangleCreator();
		mCreators.put(OverlayCoverType.REGULAR, rect);
		mCreators.put(OverlayCoverType.HIDE, rect);
		mCreators.put(OverlayCoverType.IMAGE, new ImageCreator());
	}

	public void createOverlayView(Activity context, OverlayView overlayview,
			ImageOverlayInfo overlay, float xscale, float yscale,
			ImageMetrics metrics, boolean showpopup, boolean showFadeAnimation) {

		overlayview.setImageMetrics(metrics);
		overlayview.setOverlayScale(xscale, yscale);

		// initialize the right overlay view based on the type
		OverlayViewCreator creator = mCreators.get(overlay.getConfiguration()
				.getCover().getOverlayCoverType());
		if (creator != null) {
			creator.createOverlayView(context, overlayview, overlay, showpopup,
					showFadeAnimation, xscale, yscale);
		}
	}
}
