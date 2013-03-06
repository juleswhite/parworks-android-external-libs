package com.parworks.arcameraview;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

/**
 * Standard camera view implemented migrated from Joon's client side code
 * 
 * @author yusun
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
	
	public static final String TAG = CameraView.class.getName();

	private SurfaceHolder mSurfaceHolder;
	private Camera mCamera;
	private CameraParameters mCameraParameters;

	private Context mContext;

	int screenWidth;
	int screenHeight;
	int viewHeight;
	int viewWidth;

	boolean needsLayout;

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		

		mContext = context;

		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		int rotation = display.getRotation();
		Log.d(TAG,"Rotation is: " + rotation);

		// If vertical, we fill 2/3 the height and all the width. If horizontal,
		// fill the entire height and 2/3 the width
		if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
			Log.d(TAG,"rotation must be either rotatation_0 or rotation_180");
			screenWidth = display.getWidth();
			screenHeight = display.getHeight();
			Log.d(TAG,"screenWidth is: " + screenWidth);
			Log.d(TAG,"screenHeight is: " + screenHeight);
			viewHeight = 2 * (screenHeight / 3);
			Log.d(TAG,"viewHeight is: " + viewHeight);
			viewWidth = screenWidth;
		} else {
			Log.d(TAG,"rotation was something other than 0 or 180");
			screenWidth = display.getWidth();
			screenHeight = display.getHeight();
			Log.d(TAG,"screenWidth is: " + screenWidth);
			Log.d(TAG,"screenHeight is: " + screenHeight);
			viewWidth = 2 * (screenWidth / 3);
			Log.d(TAG,"viewWidth is: " + viewWidth);
			viewHeight = screenHeight;
		}
//		viewWidth = screenWidth;
//		viewHeight = screenHeight;

		viewWidth = Utilities.getDensityPixels(viewWidth, mContext);
		viewHeight = Utilities.getDensityPixels(viewHeight, mContext);
		Log.d(TAG,"viewHeight is: " + viewHeight);
		Log.d(TAG,"viewWidth is: " + viewWidth);
		
		mSurfaceHolder = getHolder();
		mSurfaceHolder.addCallback(this);

		needsLayout = true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
		mCamera.setDisplayOrientation(90);
		if (mCamera != null) {
			try {
				mCamera.setPreviewDisplay(holder);
				Log.d(TAG,"ZOOM LEVEL IS: " + mCamera.getParameters().getZoom());
			} catch (Exception e) {
				mCamera.release();
				mCamera = null;
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (mCamera != null) {
			updateParameter();
			mCamera.startPreview();
		}
	}

	public void updateParameter() {
		Log.d(TAG,"1: UpdateParameter() flash is: " + mCamera.getParameters().getFlashMode());
		Point pictureSize = mCameraParameters.getPictureSize();
		int jpegQuality = mCameraParameters.getJpegQuality();
		String focusMode = mCameraParameters.getFocusMode();
		String flashMode = mCameraParameters.getFlashMode();
		String whiteBalance = mCameraParameters.getWhiteBalance();
		Point previewSize = mCameraParameters.getPreviewSize();
		Point thumbnailSize = mCameraParameters.getThumbnailSize();

		Camera.Parameters params = mCamera.getParameters();
		if (pictureSize != null)
			params.setPictureSize(pictureSize.x, pictureSize.y);
		if (focusMode != null)
			params.setFocusMode(focusMode);
		if (flashMode != null)
			params.setFlashMode(flashMode);
		if (whiteBalance != null)
			params.setWhiteBalance(whiteBalance);
		if (previewSize != null)
			params.setPreviewSize(previewSize.x, previewSize.y);
		if (thumbnailSize != null)
			params.setJpegThumbnailSize(thumbnailSize.x, thumbnailSize.y);
		if (jpegQuality > 0) {
			params.setJpegQuality(jpegQuality);
		}
		
		mCamera.setParameters(params);
		Log.d(TAG,"2: UpdateParameter() flash is: " + mCamera.getParameters().getFlashMode());
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	public void setCameraParameters(CameraParameters cParams) {
		mCameraParameters = cParams;
	}

	public void setJpegImageQuality(int quality) {
		mCameraParameters.setJpegQuality(quality);

		Point maxPictureSize = mCameraParameters.getMaxPictureSize();

		// The following code could cause Galaxy Nuxes to break
		// It could be due to the un-supported picture size

		// if (maxPictureSize != null) {
		// mCameraParameters.setPictureSize(new Point(
		// (int) (maxPictureSize.x * factor), (int) (maxPictureSize.y *
		// factor)));
		// }

		updateParameter();
	}

	public void setFlashMode(String flashMode) {
		mCameraParameters.setFlashMode(flashMode);
		updateParameter();
	}

	public String getFlashMode() {
		return mCameraParameters.getFlashMode();
	}

	public void setPictureSizeImageQuality(Point size) {
		mCameraParameters.setPictureSize(size);
		updateParameter();
	}

	public void autoFocus(Camera.AutoFocusCallback afcb) {
		if (mCamera != null) {
			mCamera.autoFocus(afcb);
		}
	}

	public void takePicture(Camera.PictureCallback pcb, Location location) {
		if (mCamera != null) {
			Camera.Parameters params = mCamera.getParameters();
			if (location != null && location.getProvider() != null) {
				params.setGpsProcessingMethod(location.getProvider());
				params.setGpsLatitude(location.getLatitude());
				params.setGpsLongitude(location.getLongitude());
				params.setGpsAltitude(location.getAltitude());
			} else {
				params.removeGpsData();
			}

			mCamera.setParameters(params);
			mCamera.takePicture(null, null, pcb);
		}
	}

	@Override
	public void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		if (changed) {
			(this).layout(0, 0, viewWidth, viewHeight);
			needsLayout = false;
		}
	}

}