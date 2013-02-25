package com.parworks.arviewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.parworks.androidlibrary.ar.AugmentedData;
import com.parworks.androidlibrary.ar.Overlay;
import com.parworks.androidlibrary.ar.Vertex;
import com.parworks.androidlibrary.response.AugmentImageResultResponse;
import com.parworks.androidlibrary.response.ImageOverlayInfo;
import com.parworks.androidlibrary.response.OverlayPoint;
import com.parworks.arviewer.utils.AugmentedDataUtils;
import com.parworks.arviewer.utils.JsonMapper;

public class MiniARViewer extends RelativeLayout {
	
	private static final String TAG = "MiniARViewer";
			
	private Context context;
	private ImageView imageView;
	
	private OverlayView overlayView;
	private OverlayViewFactory overlayViewFactory;
	
	/** The size of the image view used to show the image */
	private int width;
	private int height;
	
	/** The original width/height of the augmented image */
	private int originalWidth;
	private int originalHeight;
	
	/** The scale factor */
	private float xscale;
	private float yscale;
	
	private List<ImageOverlayInfo> overlays;

	public MiniARViewer(Context context) {
		super(context);
		init(context);
	}

	public MiniARViewer(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {		
		View v = LayoutInflater.from(context).inflate(R.layout.mini_arviewer, this);
		imageView = (ImageView) v.findViewById(R.id.miniARViewImage);
		overlayView = (OverlayView) v.findViewById(R.id.miniAROverlayView);
		overlayViewFactory = new OverlayViewFactory();
		this.context = context;
	}	
	
	public void setImageBitmap(Bitmap bitmap) {
		this.imageView.setImageBitmap(bitmap);
	}
	
	public void setSize(int width, int height) {		
		imageView.getLayoutParams().width = width;
		imageView.getLayoutParams().height = height;
		overlayView.getLayoutParams().width = width;
		overlayView.getLayoutParams().height = height;
		this.width = width;
		this.height = height;
	}
	
	public void setOriginalSize(int width, int height) {
		this.originalWidth = width;
		this.originalHeight = height;
		initScale();
	}
	
	private void initScale() {
		// scale factor
		xscale = ((float) width) / ((float) originalWidth);
		yscale = ((float) height) / ((float) originalHeight);			
	}
	
	public void setAugmentedData(String augmentedDataStr) {		
		if (augmentedDataStr != null) {
			try {
				AugmentImageResultResponse data = JsonMapper.get()
						.readValue(augmentedDataStr, AugmentImageResultResponse.class);
				AugmentedData augmentedData = AugmentedDataUtils.convertAugmentResultResponse(null, data);
				
				overlays = new ArrayList<ImageOverlayInfo>();
				for (Overlay o : augmentedData.getOverlays()) {
					ImageOverlayInfo info = new ImageOverlayInfo();
					info.setContent(o.getDescription());
					info.setName(o.getName());
					List<OverlayPoint> points = new ArrayList<OverlayPoint>();
					for (Vertex v : o.getVertices()) {
						OverlayPoint p = new OverlayPoint();
						p.setX(v.getxCoord());
						p.setY(v.getyCoord());
						points.add(p);
					}
					info.setPoints(points);
					overlays.add(info);
				}
				
				// set the overlays 
				initOverlayView();
			} catch (IOException e) {
				Log.e(TAG, "Failed to parse the augmented data", e);
			}
		}
	}
	
	private void initOverlayView() {
		for (ImageOverlayInfo ov : overlays) {
			overlayViewFactory.createOverlayView((Activity) context, overlayView, ov, 
					xscale, yscale, null, false, false); // Let's see if we need to enable the fade animation
		}		
	}
}
