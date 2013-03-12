package com.parworks.arviewer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.taptwo.android.widget.TitleFlowIndicator;
import org.taptwo.android.widget.ViewFlow;
import org.taptwo.android.widget.ViewFlow.ViewSwitchListener;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.parworks.androidlibrary.ar.AugmentedData;
import com.parworks.androidlibrary.ar.BaseImage;
import com.parworks.androidlibrary.ar.Overlay;
import com.parworks.androidlibrary.ar.Vertex;
import com.parworks.androidlibrary.response.ImageOverlayInfo;
import com.parworks.androidlibrary.response.OverlayPoint;
import com.parworks.arviewer.utils.ImageUtils;

public class ARViewerActivity extends SherlockActivity implements View.OnClickListener,
		View.OnTouchListener {

	private static final String TAG = ARViewerActivity.class.getName();

	private static final int MODE_NONE = 0;
	private static final int MODE_DRAG = 1;
	private static final int MODE_ZOOM = 2;
	private static final int MODE_ALPHA = 3;
	private static final float BOUNDARY_MARGIN_FACTOR = 1 / 5f;
	private static final float BOUNDARY_EFFECT_DURATION = 200; // millisecond
	private static final float FRAME_RATE = 64; // frames per second

	// Detectors for pan/drag and pinch-zoom
	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;
	private PointF initTouchPoint = new PointF();
	private PointF initFocusPoint = new PointF();

	// Parameters for drag, pinch-zoom and transparency
	private int mode;
	private float scaleFactor = 1f;
	private PointF translate = new PointF();
	private float prevScaleFactor = 1f;
	private PointF prevTranslate = new PointF();

	// Parameters for animation effect
	private boolean animation;
	private List<Float> scaleSequence = new ArrayList<Float>();
	private List<PointF> transSequence = new ArrayList<PointF>();
	private int frameNumber = 0;
	private int frameSize = 0;

	// View components
	private Point displaySize = new Point();
	private ARImageView mARImageView;

	private ImageButton mShareImage;
	private MenuItem mShareButton;

	private Point imageSize = new Point();
	private Bitmap imageBitmap;

	private BitmapFactory.Options imageBitmapOptions;
	private float imageBitmapScaleFactor;
	private Point imageBitmapSize = new Point();
	private Point originalAugmentSize = new Point();

	// Projected Polygon Model
	private PolygonModel polygonModel = new PolygonModel();

	private String siteId;
	private String imageId;
	private String imagePath;
	private AugmentedData augmentedData;

	private OverlayView mOverlayView;
	private OverlayDialogManager mOverlayDialogManager = new OverlayDialogManager();

	private ImageMetrics mImageMetrics;
	private ViewFlow viewFlow;

	private List<ImageOverlayInfo> mActiveOverlays = null;
	private String mActiveOverlayGroup;
	private OverlayGroups mOverlayGroups;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_arviewer);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// hide action bar
		this.getSupportActionBar().hide();
		// Get and parse arguments
		Bundle args = getIntent().getExtras();

		siteId = args.getString("site-id");
		imageId = args.getString("image-id");
		imagePath = args.getString("file-path");
		augmentedData = (AugmentedData) args.get("augmented-data");

		String originalAugmentationSize = args.getString("original-size");
		if (originalAugmentationSize != null
				&& !TextUtils.isEmpty(originalAugmentationSize)) {
			String[] size = originalAugmentationSize.split("x");
			originalAugmentSize.x = Integer.parseInt(size[0]);
			originalAugmentSize.y = Integer.parseInt(size[1]);
		}

		Log.d(TAG, "SiteId: " + siteId + " ImageId: " + imageId);
		Log.d(TAG, "ImagePath: " + imagePath);
		Log.d(TAG, "AugmentedData: " + augmentedData);
		Log.d(TAG, "OriginalSize: " + originalAugmentationSize);

		// Initialize gesture detectors and animation task
		mGestureDetector = new GestureDetector(getApplicationContext(),
				new GestureListener());
		mScaleGestureDetector = new ScaleGestureDetector(
				getApplicationContext(), new ScaleListener());

		// init display size
		Display display = getWindowManager().getDefaultDisplay();
		displaySize.x = display.getWidth();
		displaySize.y = display.getHeight();

		// Initialize view components
		mARImageView = (ARImageView) findViewById(R.id.ARImageView);		

		// overlay view
		mOverlayView = (OverlayView) findViewById(R.id.AROverlayViewOld);

		// share image button
		mShareImage = (ImageButton) findViewById(R.id.shareImageButton);
		mShareImage.setOnClickListener(this);

		// Load image bitmap
		Bitmap src = null; 
		try {
			src = ImageUtils.decodeSampledBitmapFromFile(imagePath,
					ImageUtils.calculateInSampleSize(originalAugmentSize.x,
							originalAugmentSize.y, displaySize.x, displaySize.y));
		} catch(FileNotFoundException e) {
			Toast.makeText(this, 
					"Failed to load the image.", Toast.LENGTH_LONG).show();
			// quit the activity 
			finish();
		}

		// make sure the image has been loaded correctly
		if (src == null) {
			Log.w(TAG, "Failed to open the image for " + siteId + " " + imageId);
			Toast.makeText(this, 
					"Failed to load the image.", Toast.LENGTH_LONG).show();
			this.finish();
			return;
		}

		imageSize.x = src.getWidth();
		imageSize.y = src.getHeight();

		computeBitmapSize();
		imageBitmapOptions = new BitmapFactory.Options();
		imageBitmapOptions.inSampleSize = (int) (1 / imageBitmapScaleFactor);

		mImageMetrics = new ImageMetrics(this, imageSize);

		// rescale bitmap
		imageBitmap = Bitmap.createScaledBitmap(src, imageBitmapSize.x,
				imageBitmapSize.y, true);

		// setup views
		mARImageView.setBitmap(imageBitmap);

		// check the augmented data first
		if (augmentedData != null) {
			polygonModel = PolygonModel.readAugmentedData(augmentedData,
					imageSize, originalAugmentSize);				
		}		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		setProgressBarIndeterminateVisibility(false);
		getSupportMenuInflater().inflate(R.menu.activity_arviewer, menu);

		mShareButton = menu.findItem(R.id.SHARE_AUGMENTED_IMAGE);
		mShareButton.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Log.d(TAG, "Capturing the augmented image");
				Bitmap bitmap = takeScreenShot();
				String filePath = Environment.getExternalStorageDirectory()
						.getPath() + "/parworks/sharingimage.jpeg";
				savePic(bitmap, filePath);

				// start the sharing intent
				Log.d(TAG, "Sharing the image");
				Intent sharingIntent = new Intent(Intent.ACTION_SEND);
				sharingIntent.setType("image/jpeg");
				sharingIntent.putExtra(Intent.EXTRA_STREAM,
						Uri.parse("file://" + filePath));
				startActivity(Intent
						.createChooser(sharingIntent, "Share Image"));

				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Take the screenshot for the image area used to be shared
	 */
	private Bitmap takeScreenShot() {
		View view = this.getWindow().getDecorView();
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();
		Bitmap b1 = view.getDrawingCache();
		Rect frame = new Rect();
		this.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
		int width = this.getWindowManager().getDefaultDisplay().getWidth();
		int height = this.getWindowManager().getDefaultDisplay().getHeight();
		int statusBarHeight = frame.top;

		Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height
				- statusBarHeight);

		view.destroyDrawingCache();
		return b;
	}

	private void savePic(Bitmap b, String strFileName) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(strFileName);
			if (null != fos) {
				b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
				fos.flush();
				fos.close();
			}
		} catch (IOException e) {
			Log.w(TAG, "Failed to save the picture");
		}
	}

	private void updateOverlayView() {

		mOverlayView.clearOverlays();

		BaseImage image = new BaseImage(imageId);

		if (augmentedData != null) {
			image.setWidth(originalAugmentSize.x);
			image.setHeight(originalAugmentSize.y);
		} else {
			return;
		}

		float xscale = ((float) imageBitmapSize.x)
				/ ((float) image.getWidth());
		float yscale = ((float) imageBitmapSize.y)
				/ ((float) image.getHeight());
		float scale = Math.min(xscale, yscale);

		List<ImageOverlayInfo> overlays = null;

		if (augmentedData != null) {
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
		}

		mOverlayGroups = new OverlayGroups(overlays);
		viewFlow = (ViewFlow) findViewById(R.id.viewflow);

		viewFlow.setOnViewSwitchListener(new ViewSwitchListener() {
			public void onSwitched(View v, int position) {
				mActiveOverlayGroup = mOverlayGroups.getGroupNames().get(position);
				mActiveOverlays = mOverlayGroups.getOverlaysForGroup(mActiveOverlayGroup);
			}
		});

		TitleFlowIndicator indicator = (TitleFlowIndicator) findViewById(R.id.viewflowindic);
		OverlayViewAdapter adapter = new OverlayViewAdapter(mOverlayGroups, this,
				mImageMetrics, scale, 0);
		viewFlow.setAdapter(adapter);

		indicator.setTitleProvider(adapter);
		viewFlow.setFlowIndicator(indicator);			
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Set translation values for each views
		checkBoundaries(true);
		mARImageView.setTranslation(scaleFactor, translate);

		updateOverlayView();
	}

	@Override
	protected void onPause() {
		super.onPause();
		prevScaleFactor = scaleFactor;
		prevTranslate.set(translate);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.shareImageButton) {				
			Log.d(TAG, "Capturing the augmented image");
			Bitmap bitmap = takeScreenShot();
			String filePath = Environment.getExternalStorageDirectory()
					.getPath() + "/parworks/sharingimage.jpeg";
			savePic(bitmap, filePath);

			// start the sharing intent
			Log.d(TAG, "Sharing the image");
			Intent sharingIntent = new Intent(Intent.ACTION_SEND);
			sharingIntent.setType("image/jpeg");
			sharingIntent.putExtra(Intent.EXTRA_STREAM,
					Uri.parse("file://" + filePath));
			startActivity(Intent
					.createChooser(sharingIntent, "Share Image"));
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		if ((v instanceof OverlayView)) {
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				initDragMode(event.getX(), event.getY());
				break;

			case MotionEvent.ACTION_POINTER_DOWN:
				int pointerIndex = event.getActionIndex();
				int numPointers = event.getPointerCount();
				float focusX = 0f,
						focusY = 0f;

				for (int i = 0; i < numPointers; i++) {
					focusX += event.getX(i);
					focusY += event.getY(i);
				}
				focusX /= numPointers;
				focusY /= numPointers;

				if (pointerIndex == 1)
					initZoomMode(focusX, focusY);

				else if (pointerIndex == 2)
					initAlphaMode(focusX, focusY);

				else
					mode = MODE_NONE;
				break;

			case MotionEvent.ACTION_MOVE:
				if (mode == MODE_ALPHA) {
					pointerIndex = event.getActionIndex();
					numPointers = event.getPointerCount();

					focusY = 0f;
					for (int i = 0; i < numPointers; i++) {
						focusY += event.getY(i);
					}
					focusY /= numPointers;
				}
				break;

			case MotionEvent.ACTION_POINTER_UP:
				pointerIndex = event.getActionIndex();
				int remainNumPointer = event.getPointerCount() - 1;

				if (remainNumPointer == 1) {
					prevScaleFactor = scaleFactor;
					initDragMode(event.getX(1 - pointerIndex),
							event.getY(1 - pointerIndex));
				} else if (remainNumPointer == 2) {
					focusX = focusY = 0f;
					for (int i = 0; i < remainNumPointer + 1; i++) {
						if (i != pointerIndex) {
							focusX += event.getX(i);
							focusY += event.getY(i);
						}
					}
					focusX /= remainNumPointer;
					focusY /= remainNumPointer;
					initZoomMode(focusX, focusY);
				}
				break;

			case MotionEvent.ACTION_UP:
				PointF src = new PointF(translate.x, translate.y);
				float srcScale = scaleFactor;

				checkBoundaries(true);
				PointF dst = new PointF(translate.x, translate.y);
				float dstScale = scaleFactor;

				boundaryEffect(src, srcScale, dst, dstScale);
				mode = MODE_NONE;
				prevTranslate.set(translate);

				break;
			}

			mGestureDetector.onTouchEvent(event);
			mScaleGestureDetector.onTouchEvent(event);

			// Draw image
			if (mode != MODE_NONE) {
				checkBoundaries(false);
				mARImageView.invalidate();
			}

			return true;
		}
		return false;
	}

	private void computeBitmapSize() {
		PointF scale = new PointF();
		scale.x = (float) displaySize.x / imageSize.x;
		scale.y = (float) displaySize.y / imageSize.y;

		imageBitmapScaleFactor = Math.min(scale.x, scale.y);
		imageBitmapSize.x = (int) (imageBitmapScaleFactor * imageSize.x);
		imageBitmapSize.y = (int) (imageBitmapScaleFactor * imageSize.y);
	}

	private void initDragMode(float x, float y) {
		mode = MODE_DRAG;
		animation = false;
		initTouchPoint.set(x, y);
		prevTranslate.set(translate);
	}

	private void initZoomMode(float focusX, float focusY) {
		mode = MODE_ZOOM;
		animation = false;
		initFocusPoint.set(focusX, focusY);
		prevScaleFactor = scaleFactor;
		prevTranslate.set(translate);
	}

	private void initAlphaMode(float focusX, float focusY) {
		mode = MODE_ALPHA;
		animation = false;
		initTouchPoint.set(focusX, focusY);
	}

	private void checkBoundaries(boolean strict) {
		float boundaryMargin = 0f;
		if (!strict)
			boundaryMargin = Math.min(displaySize.x, displaySize.y)
			* BOUNDARY_MARGIN_FACTOR;
		if (strict && scaleFactor < 1f)
			scaleFactor = 1f;

		PointF displayDiff = new PointF();
		displayDiff.x = displaySize.x - scaleFactor * imageBitmapSize.x;
		displayDiff.y = displaySize.y - scaleFactor * imageBitmapSize.y;

		if (displayDiff.x >= 0f && strict)
			translate.x = displayDiff.x / 2f;
		else if (displayDiff.x < boundaryMargin) {
			if (translate.x < displayDiff.x - boundaryMargin) // left bound
				translate.x = displayDiff.x - boundaryMargin;
			else if (translate.x > boundaryMargin) // right bound
				translate.x = boundaryMargin;
		} else if (displayDiff.x >= boundaryMargin) {
			if (translate.x < -boundaryMargin) // left bound
				translate.x = -boundaryMargin;
			else if (translate.x > displayDiff.x + boundaryMargin) // right
				// bound
				translate.x = displayDiff.x + boundaryMargin;
		}

		if (displayDiff.y >= 0f && strict)
			translate.y = displayDiff.y / 2f;
		else if (displayDiff.y < boundaryMargin) {
			if (translate.y < displayDiff.y - boundaryMargin) // top bound
				translate.y = displayDiff.y - boundaryMargin;
			else if (translate.y > boundaryMargin) // bottom bound
				translate.y = boundaryMargin;
		} else if (displayDiff.y >= boundaryMargin) {
			if (translate.y < -boundaryMargin) // top bound
				translate.y = -boundaryMargin;
			else if (translate.y > displayDiff.y + boundaryMargin) // bottom
				// bound
				translate.y = displayDiff.y + boundaryMargin;
		}
	}

	private void boundaryEffect(PointF src, float srcScale, PointF dst,
			float dstScale) {
		scaleSequence.clear();
		scaleSequence.add(Float.valueOf(srcScale));
		transSequence.clear();
		transSequence.add(new PointF(src.x, src.y));

		PointF distance = new PointF();
		distance.x = dst.x - src.x;
		distance.y = dst.y - src.y;

		PointF velocity = new PointF();
		velocity.x = distance.x / (BOUNDARY_EFFECT_DURATION * 0.001f);
		velocity.y = distance.y / (BOUNDARY_EFFECT_DURATION * 0.001f);

		PointF pixelStep = new PointF();
		pixelStep.x = velocity.x * 1 / FRAME_RATE;
		pixelStep.y = velocity.y * 1 / FRAME_RATE;

		int numSteps = 0;
		if (pixelStep.x != 0.0f && pixelStep.y != 0.0f)
			numSteps = (int) Math.max(Math.abs(distance.x / pixelStep.x),
					Math.abs(distance.y / pixelStep.y));
		else if (pixelStep.x == 0.0f && pixelStep.y != 0.0f)
			numSteps = (int) Math.abs(distance.y / pixelStep.y);
		else if (pixelStep.x != 0.0f && pixelStep.y == 0.0f)
			numSteps = (int) Math.abs(distance.x / pixelStep.x);

		for (int i = 0; i < numSteps; i++) {
			if (srcScale < 1f)
				srcScale += 2f * pixelStep.length()
				/ PointF.length(imageBitmapSize.x, imageBitmapSize.y);

			src.x += pixelStep.x;
			src.y += pixelStep.y;
			scaleSequence.add(Float.valueOf(srcScale));
			transSequence.add(new PointF(src.x, src.y));
		}

		src.set(dst);
		srcScale = dstScale;
		scaleSequence.add(Float.valueOf(srcScale));
		transSequence.add(new PointF(src.x, src.y));

		animation = true;
		new AnimationTask().execute();
	}

	private class AnimationTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			long timeStep = (long) (1f / FRAME_RATE * 1000f);
			frameNumber = 0;
			frameSize = scaleSequence.size();

			while (animation && frameNumber < frameSize) {
				scaleFactor = scaleSequence.get(frameNumber);
				translate = transSequence.get(frameNumber);

				mARImageView.setTranslation(scaleFactor, translate);
				mARImageView.postInvalidate();

				frameNumber++;
				SystemClock.sleep(timeStep);
			}

			prevTranslate.set(translate);
			prevScaleFactor = scaleFactor;
			animation = false;
			return null;
		}
	}

	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onSingleTapUp(MotionEvent e) {

			float Px = (e.getX() - translate.x) / scaleFactor
					/ imageBitmapScaleFactor;
			float Py = (e.getY() - translate.y) / scaleFactor
					/ imageBitmapScaleFactor;

			int index = polygonModel.getClickedPolygonIndex(Px, Py);
			if (index >= 0) {
				ImageOverlayInfo overlay = null;
				if (augmentedData != null) {
					overlay = new ImageOverlayInfo();
					overlay.setContent(polygonModel
							.getPolygonDescription(index));
					overlay.setName(polygonModel.getPolygonName(index));
				}

				if(mActiveOverlays == null || (mActiveOverlayGroup == null || mActiveOverlayGroup.equalsIgnoreCase("All")) || mActiveOverlayGroup.equals(overlay.getName())){
					mOverlayDialogManager.showOverlayDialog(ARViewerActivity.this,
							overlay);
				}
			}
			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			if (mode == MODE_DRAG) {
				translate.x = e2.getX() - initTouchPoint.x + prevTranslate.x;
				translate.y = e2.getY() - initTouchPoint.y + prevTranslate.y;
			}
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			return false;
		}
	}

	private class ScaleListener extends	ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			if (mode == MODE_ZOOM) {
			}
			return false;
		}
	}

	@Override
	public void finish() {
		super.finish();
		if (this.imageBitmap != null) {
			Log.d(TAG, "recycling the bitmap");
			//this.imageBitmap.recycle();
		}
	}	
}