/* 
 **
 ** Copyright 2012, Jules White
 **
 ** 
 */
package com.parworks.arviewer;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

import com.parworks.androidlibrary.response.ImageOverlayInfo;
import com.parworks.androidlibrary.response.OverlayContent;
import com.parworks.androidlibrary.response.OverlayCover;
import com.parworks.androidlibrary.response.OverlayPoint;
import com.parworks.androidlibrary.response.OverlayCover.OverlayCoverType;

public class OverlayView extends RelativeLayout {

	private static final String TAG = OverlayView.class.getName();
	
	public static class OverlayPopupAnimation extends Animation {

		public OverlayPopupAnimation() {
			super();
		}

		public OverlayPopupAnimation(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
	}

	public class TransformationAnimation extends Animation {

		private View child_;

		public TransformationAnimation(View child) {
			super();
			child_ = child;
			setRepeatMode(Animation.INFINITE);
		}

		@Override
		public boolean getTransformation(long currentTime,
				Transformation outTransformation) {
			outTransformation.set(OverlayView.this.getTransformation(child_));
			return true;
		}
	}

	public class CentroidTransformationAnimation extends Animation {

		private View child_;

		public CentroidTransformationAnimation(View child) {
			super();
			child_ = child;
			setRepeatMode(Animation.INFINITE);
		}

		@Override
		public boolean getTransformation(long currentTime,
				Transformation outTransformation) {
			outTransformation.set(OverlayView.this.getTransformation(child_));
			return true;
		}
	}

	public interface OverlayTransform {
		public float[] getDestination();
	}

	public class OverlayCentroidTransform extends Transformation implements
			OverlayTransform {

		private Matrix mMatrix;

		private float[] mDestination;

		public OverlayCentroidTransform(ImageOverlayInfo overlay, float xscale,
				float yscale, int w, int h, int iw, int ih) {

			OverlayPoint p1 = overlay.getPoints().get(0);
			OverlayPoint p2 = overlay.getPoints().get(1);
			OverlayPoint p3 = overlay.getPoints().get(2);

			int[] offs = getOverlayImageOffset(overlay);
			// TODO: The current transformation does not
			// work well for Overlays with 3 points or
			// more than 4 points.
			// The following code only avoids NullPointerExcpetion
			// when there are only 3 overlay points, but does not
			// solve the actual display problem
			OverlayPoint p4 = new OverlayPoint();
			if (overlay.getPoints().size() > 3) {
				p4 = overlay.getPoints().get(3);
			}
			
			float scale = getOverlayImageScale(overlay);
			w = (int)Math.rint(scale * w);
			h = (int)Math.rint(scale * h);

			float x = (p1.getX() + p2.getX() + p3.getX() + p4.getX()) / 4;
			float y = (p1.getY() + p2.getY() + p3.getY() + p4.getY()) / 4;
			x = x * xscale;
			y = y * yscale;

			float[] topleft = { x - (w / 2) + (iw / 2) + offs[0],
					y - (h / 2) + (ih / 2) + offs[1] };

			float[] dst2 = { topleft[0], topleft[1], topleft[0] + w,
					topleft[1], topleft[0] + w, topleft[1] + h, topleft[0],
					topleft[1] + h };

			Matrix matrix2 = new Matrix();
			float[] src2 = new float[] { 0, 0, w, 0, w, h, 0, h };

			mDestination = dst2;
			matrix2.setPolyToPoly(src2, 0, dst2, 0, src2.length >> 1);
			mMatrix = matrix2;
		}

		private float getOverlayImageScale(ImageOverlayInfo overlay) {
			String scalestr = getProviderParam(overlay, "scale");

			float scale = 1.0f;

			try {
				if (scalestr != null) {
					scale = Float.parseFloat(scalestr);
				}
			} catch (Exception e) {
				Log.e(TAG, "Malformed cover image scale spec", e);
			}
			return scale;
		}

		private int[] getOverlayImageOffset(ImageOverlayInfo overlay) {
			int[] offs = new int[2];

			String offstr = getProviderParam(overlay, "offset");
			try {
				if (offstr != null) {
					String[] off = offstr.split(",");
					offs[0] = Integer.parseInt(off[0]);
					offs[1] = Integer.parseInt(off[1]);
				}
			} catch (Exception e) {
				Log.e(TAG, "Malformed cover image offset spec", e);
			}
			return offs;
		}

		public float[] getDestination() {
			return mDestination;
		}

		@Override
		public Matrix getMatrix() {
			return mMatrix;
		}

	}

	public class OverlaySkewTransform extends Transformation implements
			OverlayTransform {

		private Matrix mMatrix;

		private float[] mDestination;

		public OverlaySkewTransform(ImageOverlayInfo overlay, float xscale,
				float yscale, int w, int h, int iw, int ih) {

			OverlayPoint p1 = overlay.getPoints().get(0);
			OverlayPoint p2 = overlay.getPoints().get(1);
			OverlayPoint p3 = overlay.getPoints().get(2);

			// TODO: The current transformation does not
			// work well for Overlays with 3 points or
			// more than 4 points.
			// The following code only avoids NullPointerExcpetion
			// when there are only 3 overlay points, but does not
			// solve the actual display problem
			OverlayPoint p4 = new OverlayPoint();
			if (overlay.getPoints().size() > 3) {
				p4 = overlay.getPoints().get(3);
			}

			int ydelta = ih / 2;
			int xdelta = iw / 2;
			Matrix matrix2 = new Matrix();
			float[] src2 = new float[] { 0, 0, w, 0, w, h, 0, h };
			float[] dst2 = new float[] { xdelta + p1.getX() * xscale,
					ydelta + p1.getY() * yscale, xdelta + p2.getX() * xscale,
					ydelta + p2.getY() * yscale, xdelta + p3.getX() * xscale,
					ydelta + p3.getY() * yscale, xdelta + p4.getX() * xscale,
					ydelta + p4.getY() * yscale };

			mDestination = dst2;
			matrix2.setPolyToPoly(src2, 0, dst2, 0, src2.length >> 1);
			mMatrix = matrix2;
		}

		public float[] getDestination() {
			return mDestination;
		}

		@Override
		public Matrix getMatrix() {
			return mMatrix;
		}

	}

	private float mXScale;
	private float mYScale;

	private ImageMetrics mImageMetrics;

	private Map<View, View> mPopups = new HashMap<View, View>();
	private Map<View, Transformation> mTransforms = new HashMap<View, Transformation>();
	private Map<View, ImageOverlayInfo> mViewOverlays = new HashMap<View, ImageOverlayInfo>();

	public OverlayView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public RelativeLayout.LayoutParams getPopupPosition(View popup,
			float[] overlaypos) {

		float lx = Float.MAX_VALUE;
		float rx = 0;
		float ty = Float.MAX_VALUE;

		for (int i = 0; i < overlaypos.length / 2; i++) {
			float x = overlaypos[i * 2];
			float y = overlaypos[i * 2 + 1];
			lx = (x < lx) ? x : lx;
			rx = (x > rx) ? x : rx;
			ty = (y < ty) ? y : ty;
		}

		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.leftMargin = (int) (lx + ((rx - lx) / 2) - 180);
		params.topMargin = (int) ty - popup.getMeasuredHeight();
		params.width = 360;
		// params.height = 400;

		return params;
	}

	public float getOverlayScale() {
		return mXScale;
	}

	public void setOverlayScale(float xscale, float yscale) {
		mXScale = xscale;
		mYScale = yscale;
	}

	public ImageMetrics getImageMetrics() {
		return mImageMetrics;
	}

	public void setImageMetrics(ImageMetrics imageMetrics) {
		mImageMetrics = imageMetrics;
	}

	public OverlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// setStaticTransformationsEnabled(true);
	}

	public OverlayView(Context context) {
		super(context);
		// setStaticTransformationsEnabled(true);
	}

	public void addOverlay(ImageOverlayInfo overlay, final View v,
			RelativeLayout.LayoutParams params) {
		mViewOverlays.put(v, overlay);

		Animation a = new TransformationAnimation(v);

		Animation existing = v.getAnimation();
		if (existing != null) {
			if (existing instanceof AnimationSet) {
				((AnimationSet) existing).addAnimation(a);
			} else {
				AnimationSet set = new AnimationSet(false);
				set.addAnimation(a);
				a = set;
			}
		} else {
			v.startAnimation(a);
		}
		addView(v, params);
	}

	public void addOverlay(ImageOverlayInfo overlay, View v,
			RelativeLayout.LayoutParams params, float xscale, float yscale,
			ImageMetrics metrics) {
		mXScale = xscale;
		mYScale = yscale;
		mImageMetrics = metrics;
		addOverlay(overlay, v, params);
	}

	public void setPopup(ImageOverlayInfo overlay, View v, View popup) {
		mPopups.put(v, popup);
		addView(popup,
				getPopupPosition(popup, new float[] { 0, 0, 100, 0, 100, 100,
						0, 100 }));
	}

	public void clearOverlays() {
		removeAllViews();
		mTransforms.clear();
		mViewOverlays.clear();
	}

	protected Transformation getTransformation(View child) {
		Transformation vt = mTransforms.get(child);

		if (vt == null) {
			ImageOverlayInfo i = mViewOverlays.get(child);
			if (i != null) {

				int[] offs = new int[] { 0, 0 };
				if (mImageMetrics != null) {
					offs = mImageMetrics.getFullSizeImageOffset();
				}

				OverlayCover content = i.getConfiguration().getCover();
				if (content.getType().equalsIgnoreCase("IMAGE")
						&& content.getProvider().endsWith("#no-scale")) {
					vt = new OverlayCentroidTransform(i, mXScale, mYScale,
							child.getWidth(), child.getHeight(), offs[0],
							offs[1]);
				} else {
					vt = new OverlaySkewTransform(i, mXScale, mYScale,
							child.getWidth(), child.getHeight(), offs[0],
							offs[1]);
				}

				mTransforms.put(child, vt);

				View pop = mPopups.get(child);
				if (pop != null) {
					float[] dst = ((OverlayTransform) vt).getDestination();
					LayoutParams params = getPopupPosition(pop, dst);
					pop.setLayoutParams(params);
					pop.invalidate();
				}
			}
		}

		return vt;
	}
	
	public static String removeProviderParam(ImageOverlayInfo overlay, String param) {
		String offstr = overlay.getConfiguration().getCover().getProvider();

		String value = null;
		String start = "#" + param + "[";
		String end = "]";
		if (offstr.indexOf(start) > -1) {
			int s = offstr.indexOf(start);
			int e = offstr.indexOf(end, s);
			value = offstr.substring(0,s) + offstr.substring(e+1);
		}
		return value;
	}
	
	public static String getProviderParam(ImageOverlayInfo overlay, String param) {
		String offstr = overlay.getConfiguration().getCover().getProvider();

		String value = null;
		String start = "#" + param + "[";
		String end = "]";
		if (offstr.indexOf(start) > -1) {
			int s = offstr.indexOf(start);
			int e = offstr.indexOf(end, s);
			value = offstr.substring(s + start.length(), e);
		}
		return value;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
	}
}
