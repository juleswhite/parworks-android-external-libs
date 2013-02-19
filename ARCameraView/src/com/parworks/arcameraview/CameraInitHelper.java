package com.parworks.arcameraview;

import java.util.StringTokenizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class CameraInitHelper {

	private static Context context;
	// Shared preferences
	private static SharedPreferences mSharedPreferences;
	
	public static void initCamera(Context context) {
		CameraInitHelper.context = context;
		initCameraPreferences();
	}
	
	private static void initCameraPreferences() {

		// Read preferences
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		// If this application is newly installed, read hardware information of the device
		if (!mSharedPreferences.getBoolean("initialized", false)) {
			Camera camera = Camera.open();
			if (camera != null) {
				Camera.Parameters params = camera.getParameters();
				createCameraPreferences(params);
				camera.release();
			} else {
				Toast.makeText(context, R.string.errNoCamera, Toast.LENGTH_LONG).show();
			}
		}
	}

	private static void createCameraPreferences(Camera.Parameters params) {

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putBoolean("initialized", true);

		StringTokenizer strParams = new StringTokenizer(params.flatten(), ";");
		while (strParams.hasMoreTokens()) {
			StringTokenizer entries = new StringTokenizer(strParams.nextToken(), "=");
			String key, values;
			if (entries.countTokens() == 2) {
				key = entries.nextToken();
				values = entries.nextToken();
			} else {
				key = entries.nextToken();
				values = "";
			}

			for (int i = 0; i < CameraParameters.SUPPORT_PARAMS.length; i++) {
				if (key.compareTo(CameraParameters.SUPPORT_PARAMS[i]) == 0) {
					editor.putString(key, values);
				}
			}
		}

		editor.commit();

		// For JPEG Thumbnail size, it requires additional task
		String thumbnailSize = mSharedPreferences.getString("jpeg-thumbnail-width", "") + "x" + mSharedPreferences.getString("jpeg-thumbnail-height", "");
		editor.putString("thumbnail-size", thumbnailSize);
		editor.commit();
	}
}
