package com.parworks.arcameraview;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mSurfaceHolder = getHolder();
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // This is required for Android below 3.0
		mSurfaceHolder.addCallback(this);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated");
		mCamera = Camera.open();
		if (mCamera != null) {
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (Exception e) {
				mCamera.release();
				mCamera = null;
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG,"surfaceChanged");
		if (mCamera != null) {
			updateParameter();
			mCamera.startPreview();
		}
	}

	public void updateParameter() {
		if (mCamera == null) return;
		
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

}