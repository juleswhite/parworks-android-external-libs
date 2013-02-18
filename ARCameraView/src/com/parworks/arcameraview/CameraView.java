package com.parworks.arcameraview;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.location.Location;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Standard camera view implemented migrated from Joon's client side code
 * 
 * @author yusun
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
	
	private SurfaceHolder mSurfaceHolder;
	private Camera mCamera;
	private CameraParameters mCameraParameters;

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mSurfaceHolder = getHolder();
		mSurfaceHolder.addCallback(this);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
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
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (mCamera != null) {
			updateParameter();
			mCamera.startPreview();
		}
	}
	
	public void updateParameter() {
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
		if (jpegQuality > 0){
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
		
		Point maxPictureSize = mCameraParameters.getMaxPictureSize();
		
		// The following code could cause Galaxy Nuxes to break
		// It could be due to the un-supported picture size
		
//		if (maxPictureSize != null) {
//			mCameraParameters.setPictureSize(new Point(
//					(int) (maxPictureSize.x * factor), (int) (maxPictureSize.y * factor)));
//		}

		updateParameter();
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