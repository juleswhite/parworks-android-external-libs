package com.parworks.arcameraview;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.parworks.androidlibrary.ar.ARErrorListener;
import com.parworks.androidlibrary.ar.ARListener;
import com.parworks.androidlibrary.ar.ARSite;
import com.parworks.androidlibrary.ar.AugmentedData;
import com.parworks.arviewer.ARViewerActivity;
import com.parworks.arviewer.utils.ImageUtils;

/**
 * This activity captures an image and handle the saved image for different
 * purposes.
 * <p>
 * If the given intent attribute "isAugment" is true, it will use the saved
 * image to do image augmentation; otherwise, it will use the saved image as a
 * base image for the site.
 * 
 * @author yusun
 */
public class CaptureImageActivity extends SherlockActivity implements
View.OnClickListener, View.OnTouchListener, Camera.AutoFocusCallback,
Camera.PictureCallback {

	private static final String TAG = CaptureImageActivity.class.getName();

	private Context mContext;

	// The attribute name in the intent extra to specify whether to
	// do augmentation or not
	public static final String IS_AUGMENT_ATTR = "isAugment";

	private static final String IMAGE_FOLDER = "parworks";

	// Whether to use this activity to do image augmentation.
	// If not, use it to add base image
	private boolean isAugment = true;
	// Augmented Image Id
	private String augmentedImageId;

	// View components
	private Point displaySize = new Point();

	/** Camera related UI */
	private CameraView mCameraView;
	private ImageButton mCameraImageButton;
	private ImageButton mCameraExitButton;
	private ImageButton mCameraFlashButton;
	private ImageButton mCameraImageJpegSettingButton;
	private ImageButton mCameraImagePicSizeSettingButton;
	//	private ImageButton mCameraSettingButton;

	/** Camera parameters */
	private Camera mCamera;
	private CameraParameters mCameraParameters = new CameraParameters();
	private Point prevPictureSize = new Point();

	// Alert Dialog
	private AlertDialog alertDialog;	

	// Flags
	private boolean isCameraViewClicked = false;
	private boolean isCameraImageButtonClicked = false;

	// Shared preferences
	private SharedPreferences mSharedPreferences;
	private SharedPreferences.Editor mEditor;

	// progress bar used for augmentation
	private ProgressDialog progressBar;

	// Filename parameters
	// TODO: move this to a centralized place
	private String storagePath;

	/** The current site/image/path the activity is serving */
	private String currentSiteId;
	private String currentImageId;
	private String currentImagePath;

	/** Multiple sites can be provided for augmentation */
	private List<String> currentSiteList;

	//private SiteInfo site;

	public static final String SITE_ID_KEY = "site-id";
	public static final String SITE_LIST = "site-list";

	/** Called when the activity is first created. */
	@SuppressWarnings({ "deprecation", "unchecked" })
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;

		// get the target site(s)
		currentSiteId = getIntent().getExtras().getString(SITE_ID_KEY);
		currentSiteList = (List<String>) getIntent().getExtras().get(SITE_LIST);

		// init storage for the captured image
		if (storagePath == null) {
			storagePath = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/" + IMAGE_FOLDER;
		}

		// we can handle the augmentation from other app by sharing action
		if (Intent.ACTION_SEND.equals(getIntent().getAction())
				&& getIntent().hasExtra(Intent.EXTRA_STREAM)) {
			currentImageId = UUID.randomUUID().toString();
			augmentImageFromShare();
		} else {

			// Get the augment usage boolean; be default, we use it for augmentation
			isAugment = getIntent().getExtras().getBoolean(IS_AUGMENT_ATTR, true);

			// requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);

			setContentView(R.layout.activity_capture_image);

			// Get display dimension
			Display display = getWindowManager().getDefaultDisplay();
			displaySize.x = display.getWidth();
			displaySize.y = display.getHeight();

			// Initialize view components
			mCameraView = (CameraView) findViewById(R.id.cameraView);
			mCameraView.setOnClickListener(this);

			mCameraImageButton = (ImageButton) findViewById(R.id.imageButtonCamera);
			mCameraImageButton.setOnClickListener(this);			

			mCameraImageJpegSettingButton = (ImageButton) findViewById(R.id.imageButtonJpeg);
			mCameraImageJpegSettingButton.setOnClickListener(this);

			mCameraImagePicSizeSettingButton = (ImageButton) findViewById(R.id.imageButtonSize);
			mCameraImagePicSizeSettingButton.setOnClickListener(this);			

			mCameraExitButton = (ImageButton) findViewById(R.id.imageButtonExit);
			mCameraExitButton.setOnClickListener(this);

			mCameraFlashButton = (ImageButton) findViewById(R.id.imageButtonFlash);		
			mCameraFlashButton.setImageResource(R.drawable.camera_flash_auto_selector);			
			mCameraFlashButton.setOnClickListener(this);

			mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);

			// Initialize camera parameters
			mCameraView.setCameraParameters(mCameraParameters);
			cameraInit();
			
			// hide action bar
			this.getSupportActionBar().hide();									
		}

		// Initialize alert dialog used when augmenting
		AugmentationAlertDialogListener alertDialogListener = new AugmentationAlertDialogListener();
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		alertDialog = adb
				.setTitle(R.string.alertDialogRegisterTitle)
				.setMessage(R.string.alertDialogRegisterMessage)
				.setNegativeButton(R.string.alertDialogRegisterNegativeButton,
						alertDialogListener)
						.setPositiveButton(R.string.alertDialogRegisterPositiveButton,
								alertDialogListener).create();

		// Initialize the augment progress bar
		progressBar = new ProgressDialog(this);
		progressBar.setCancelable(true);
		progressBar.setMessage("Augmenting ...");
		progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	}

	public void augmentImageFromShare() {
		try {
			// First, we need to figure out who
			// called us.
			Intent intent = getIntent();

			// Now, we make sure that someone passed us
			// a stream to send to the server.
			if (Intent.ACTION_SEND.equals(intent.getAction())
					&& intent.hasExtra(Intent.EXTRA_STREAM)) {

				// Grab the stream for the image, video, or
				// other shareable object...
				String type = intent.getType();
				Uri stream = (Uri) intent
						.getParcelableExtra(Intent.EXTRA_STREAM);
				if (stream != null && type != null) {
					Log.i(TAG, "Got a stream for an image...");

					// The content resolver allows us to grab the data
					// that the URI refers to. We can also use it to
					// read some metadata about the file.
					ContentResolver contentResolver = getContentResolver();

					// This call tries to guess what type of stream
					// we have been passed.
					String contentType = contentResolver.getType(stream);

					String name = null;
					int size = -1;
					// Now, we index into the metadata for the stream and
					// figure out what we are dealing with...size/name.
					Cursor metadataCursor = contentResolver.query(stream,
							new String[] { OpenableColumns.DISPLAY_NAME,
							OpenableColumns.SIZE }, null, null, null);
					if (metadataCursor != null) {
						try {
							if (metadataCursor.moveToFirst()) {
								name = metadataCursor.getString(0);
								size = metadataCursor.getInt(1);

							}
						} finally {
							metadataCursor.close();
						}
					}

					// If for some reason we couldn't get a name,
					// we just use the last path segment as the name.
					if (name == null)
						name = stream.getLastPathSegment();

					// Now, we try to resolve the URI to an actual InputStream
					// that we can read.
					InputStream in = contentResolver.openInputStream(stream);

					// Finally, we pipe the stream to the network.
					String uuid = UUID.randomUUID().toString();
					String imageFilename = storagePath + "/" + uuid + ".jpg";
					currentImageId = uuid;
					currentImagePath = imageFilename;

					saveImage(in, imageFilename);
					augmentImage();
				} else {
					Log.i("VShare", "Stream was null");
				}
			} else {
				Log.i("VShare", "Invoked outside of send....");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void triggerJpegQualitySelection() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				CaptureImageActivity.this);
		builder.setTitle("Choose JPEG Quality");

		class QualitySelector {
			int selected = mCameraParameters.getJpegQuality();
		}

		final QualitySelector qualitySelected = new QualitySelector();

		final String[] jpegValues = new String[] { "70", "60", "50", "40", "30", "20" };
		int initSelection = 0;
		// make default selection
		for (int i = 0; i < jpegValues.length; i++) {
			if (jpegValues[i].equals(Integer.toString(mCameraParameters.getJpegQuality()))) {
				initSelection = i;
			}
		}

		builder.setSingleChoiceItems(jpegValues, initSelection,
				new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				qualitySelected.selected = Integer
						.parseInt(jpegValues[arg1]);
			}
		});

		builder.setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mCameraView.setJpegImageQuality(qualitySelected.selected);
			}
		});

		AlertDialog qualityChooserDialog = builder.create();
		qualityChooserDialog.show();
	}

	private void triggerPictureSizeSelection() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				CaptureImageActivity.this);
		builder.setTitle("Choose Picture Size");

		class QualitySelector {
			Point selected = mCameraParameters.getPictureSize();
		}

		final QualitySelector qualitySelected = new QualitySelector();

		final String[] supportedPictureSizes = mCameraParameters
				.getSupportedPicatureSizes();
		// make default selection
		int initSelection = 0;
		for (int i = 0; i < supportedPictureSizes.length; i++) {
			if (supportedPictureSizes[i].equals(mCameraParameters
					.getPictureSize().x
					+ "x"
					+ mCameraParameters.getPictureSize().y)) {
				initSelection = i;
			}
		}

		builder.setSingleChoiceItems(supportedPictureSizes, initSelection,
				new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				String[] points = supportedPictureSizes[arg1]
						.split("x");
				qualitySelected.selected = new Point(Integer
						.parseInt(points[0]), Integer
						.parseInt(points[1]));
			}
		});

		builder.setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mCameraView
				.setPictureSizeImageQuality(qualitySelected.selected);
			}
		});

		AlertDialog qualityChooserDialog = builder.create();
		qualityChooserDialog.show();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (Intent.ACTION_SEND.equals(getIntent().getAction())
				&& getIntent().hasExtra(Intent.EXTRA_STREAM)) {
			currentImageId = UUID.randomUUID().toString();
			augmentImageFromShare();
		} else {
			// Enable all the buttons
			mCameraView = (CameraView) findViewById(R.id.cameraView);
			mCameraView.setEnabled(true);			
			mCameraImageButton.setEnabled(true);
			isCameraViewClicked = false;
			isCameraImageButtonClicked = false;			
		}
	}

	private void cameraInit() {
		// Read preferences
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this.getApplicationContext());
		mEditor = mSharedPreferences.edit();

		// Initialize filename parameters
		storagePath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/" + IMAGE_FOLDER;

		// Update camera parameters
		mCameraParameters.init(null, isAugment, mSharedPreferences.getAll());

		// Change hardware preview size and cameraView size according to
		// camera parameters
		Point currPictureSize = mCameraParameters.getPictureSize();
		if (!prevPictureSize.equals(currPictureSize.x, currPictureSize.y)) {
			changeCameraViewSize();
			prevPictureSize.set(currPictureSize.x, currPictureSize.y);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mEditor.commit();
	}

	private void handleFlashButtonChange() {
		if (mCameraView.getFlashMode().equals(Camera.Parameters.FLASH_MODE_AUTO)){
			mCameraView.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			mCameraFlashButton.setImageResource(R.drawable.camera_flash_off_selector);
		} else if(mCameraView.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)){
			mCameraView.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
			mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
			mCameraFlashButton.setImageResource(R.drawable.camera_flash_on_selector);			
		} else {
			mCameraView.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			mCameraFlashButton.setImageResource(R.drawable.camera_flash_auto_selector);
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.cameraView) {
			mCameraView.setEnabled(false);
			mCameraImageButton.setEnabled(false);
			isCameraViewClicked = true;

			if (mCameraParameters.getFocusMode().compareTo(
					Camera.Parameters.FOCUS_MODE_INFINITY) != 0)
				mCameraView.autoFocus(this);
		} 
		else if (v.getId() == R.id.imageButtonExit) {
			onBackPressed();
		} 
		else if (v.getId() == R.id.imageButtonFlash) {
			Log.d(TAG, "ImageButtonFlash on click flash mode is: " + mCameraView.getFlashMode());
			handleFlashButtonChange();
		} 
		else if (v.getId() == R.id.imageButtonJpeg) {
			triggerJpegQualitySelection();
		}
		else if (v.getId() == R.id.imageButtonSize) {
			triggerPictureSizeSelection();
		} 
		else if (v.getId() == R.id.imageButtonCamera) {
			mCameraView.setEnabled(false);
			mCameraImageButton.setEnabled(false);
			isCameraImageButtonClicked = true;

			if (mCameraParameters.getFocusMode() != null && 
					mCameraParameters.getFocusMode().compareTo(Camera.Parameters.FOCUS_MODE_INFINITY) != 0) {
				mCameraView.autoFocus(this); // this will automatically call camera.takePicture
			} else {
				mCameraView.takePicture(this, null);
			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			((ImageButton) v).setColorFilter(0xAA00FFFF, Mode.SRC_OVER);
			break;

		case MotionEvent.ACTION_UP:
			((ImageButton) v).setColorFilter(0x00000000, Mode.SRC_OVER);
			break;
		}

		return false;
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if (success) {
			if (isCameraViewClicked) {
				mCameraView.setEnabled(true);
				mCameraImageButton.setEnabled(true);
				isCameraViewClicked = false;

			} else if (isCameraImageButtonClicked) {
				Camera.Parameters params = camera.getParameters();
				params.removeGpsData();
				camera.setParameters(params);
				camera.takePicture(null, null, this);
			}
		} else {
			camera.cancelAutoFocus();
			mCameraView.setEnabled(true);
			mCameraImageButton.setEnabled(true);
			isCameraViewClicked = false;
			isCameraImageButtonClicked = false;
			Toast.makeText(this, "Unable to auto-focus, try again",
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		String uuid = UUID.randomUUID().toString();
		String imageFilename = storagePath + "/" + uuid + ".jpg";
		currentImageId = uuid;
		currentImagePath = imageFilename;

		try {
			saveImage(data, imageFilename);
			Log.d(TAG, "A photo was taken successfully and saved at "
					+ imageFilename);
			if (isAugment) {
				augmentedImageId = uuid;
				// AugmentImagesProvider.get().addBitmapToCache(uuid,
				// imageFilename);
			}

			mCamera = camera;

			// confirm augmentation when needed
			if (isAugment) {
				alertDialog.show();
			} else {				
				resetCamera();
			}
		} catch (IOException e) {
			Toast.makeText(this, "Unable to record a photo", Toast.LENGTH_SHORT)
			.show();
		}
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub

		Log.d(TAG, "onBackPressed in CaptureImageActivity");
		Intent returnIntent = new Intent();
		returnIntent.putExtra(SITE_ID_KEY, currentSiteId);
		setResult(RESULT_OK, returnIntent);
		finish();
		super.onBackPressed();
	}

	/**
	 * Handles saving the image.
	 * 
	 * Currently, it saves the image to disk. It can be changed to save it to
	 * database as well.
	 */
	private void saveImage(byte[] data, String imageFilename)
			throws IOException {
		saveImage(new ByteArrayInputStream(data), imageFilename);
	}

	public void selectSiteThenAugment() {
		// ARClient.getARSites().getUserSites(new ARListener<List<ARSite>>() {
		//
		// @Override
		// public void handleResponse(List<ARSite> resp) {
		AlertDialog.Builder about = new AlertDialog.Builder(
				CaptureImageActivity.this);

		about.setTitle("Select Site");

		View view = getLayoutInflater().inflate(R.layout.select_site_dialog,
				null);
		about.setView(view);

		final Spinner spinner = (Spinner) view
				.findViewById(R.id.siteSelectorSpinner);
		// Create an ArrayAdapter using the string array and a default spinner
		// layout


		// List<String> sites = SitesProvider.get().getCachedSiteIds();

		//		List<String> sites = new ArrayList<String>();
		//		 for(ARSite s : resp){
		//		 sites.add(s.getSiteId());
		//		 }
		//sites.add("AK0");
		//		sites.add("AK1");
		//		sites.add("AK2");
		//		sites.add("AK3");

		//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
		//				CaptureImageActivity.this,
		//				android.R.layout.simple_spinner_dropdown_item, sites);
		//		// Specify the layout to use when the list of choices appears
		//		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		//		// Apply the adapter to the spinner
		//		spinner.setAdapter(adapter);

		about.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				currentSiteId = "" + spinner.getSelectedItem();
				dialog.dismiss();
				augmentImage();
			}
		});
		about.show();
	}

	/**
	 * Handles saving the image.
	 * 
	 * Currently, it saves the image to disk. It can be changed to save it to
	 * database as well.
	 */
	private void saveImage(InputStream data, String imageFilename)
			throws IOException {

		// Make a directory, if not exists
		File f = new File(storagePath);
		if (!f.exists())
			f.mkdirs();

		// Write image file
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(imageFilename, false);
			IOUtils.copy(data, fos);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	private void changeCameraViewSize() {
		if (mCameraView != null) {
			getOptimalCameraPreviewSize();
			PointF cameraViewMargin = getCameraViewMargin();
			int cameraViewWidth = displaySize.x - (int) cameraViewMargin.x;
			int cameraViewHeight = displaySize.y - (int) cameraViewMargin.y;
			RelativeLayout.LayoutParams lParams = new RelativeLayout.LayoutParams(
					cameraViewWidth, cameraViewHeight);
			lParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
					RelativeLayout.TRUE);
			lParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			mCameraView.setLayoutParams(lParams);
		}
	}

	private void getOptimalCameraPreviewSize() {
		Point pictureSize = mCameraParameters.getPictureSize();
		float pictureAspect = (float) pictureSize.x / pictureSize.y;
		float minDiffAspect = 0.01f;

		List<Point> candidates = new ArrayList<Point>();
		for (Point size : mCameraParameters.getSupportedPreviewSizes()) {
			float previewAspect = (float) size.x / size.y;
			if (Math.abs(pictureAspect - previewAspect) < minDiffAspect)
				candidates.add(size);
		}

		int minDiffSize = pictureSize.y;
		Point previewSize = new Point();
		for (int i = 0; i < candidates.size(); i++) {
			Point size = candidates.get(i);
			int diff = Math.abs(pictureSize.y - size.y);
			if (diff < minDiffSize) {
				previewSize.set(size.x, size.y);
				minDiffSize = diff;
			}
		}

		mCameraParameters.setPreviewSize(previewSize);
		mEditor.putString("preview-size", previewSize.x + "x" + previewSize.y);
	}

	private PointF getCameraViewMargin() {
		Point previewSize = mCameraParameters.getPreviewSize();
		float previewAspect = (float) previewSize.x / previewSize.y;
		float displayAspect = (float) displaySize.x / displaySize.y;

		if (displayAspect >= previewAspect) {
			float wm = displaySize.x
					- ((float) displaySize.y / previewSize.y * previewSize.x);
			return new PointF(wm, 0);
		} else {
			float hm = displaySize.y
					- ((float) displaySize.x / previewSize.x * previewSize.y);
			return new PointF(0, hm);
		}
	}

	/** Reset camera and start preview again */
	private void resetCamera() {
		if (mCamera != null) {
			mCamera.cancelAutoFocus();
			mCamera.startPreview();
			mCameraView.setEnabled(true);
			mCameraImageButton.setEnabled(true);
			isCameraImageButtonClicked = false;
		}
	}

	private void augmentImage() {
		if (currentSiteId == null) {
			selectSiteThenAugment();
		} else {
			try {
				augmentImage(new FileInputStream(currentImagePath));
			} catch (IOException e) {
				Log.w(TAG, "Failed to read the file: " + currentImagePath);
				progressBar.dismiss();
				resetCamera();
			}
		}
	}

	private void augmentImage(final InputStream img) {
		// call to augment

		ARClient.getARSites().getExisting(currentSiteId,
				new ARListener<ARSite>() {
			@Override
			public void handleResponse(ARSite resp) {

				Log.i(TAG, "Augmenting image for " + currentSiteId);
				progressBar.show();
				resp.augmentImage(img, new ARListener<AugmentedData>() {
					@Override
					public void handleResponse(final AugmentedData resp) {
						Log.i(TAG, "Localization: "	+ resp.isLocalization());

						progressBar.dismiss();
						if (resp.isLocalization()) {
							Intent intent = new Intent(
									CaptureImageActivity.this,
									ARViewerActivity.class);
							intent.putExtra("image-id",	augmentedImageId);
							intent.putExtra("augmented-data", resp);
							intent.putExtra("show-overlay", true);
							intent.putExtra("file-path", currentImagePath);
							intent.putExtra("original-size", ImageUtils
									.getImageSize(currentImagePath));
							startActivity(intent);
						} else {
							Toast.makeText(
									CaptureImageActivity.this,
									"Nothing to augment for this image. Try again.",
									Toast.LENGTH_LONG).show();
							resetCamera();
						}
					}
				}, new ARErrorListener() {
					@Override
					public void handleError(Exception error) {
						Log.w(TAG, "Failed to augment the image: "
								+ error.getMessage());
						progressBar.dismiss();
						resetCamera();
					}
				});
			}
		}, new ARErrorListener() {
			@Override
			public void handleError(Exception error) {
				Log.w(TAG, "Failed to get existing site: "
						+ currentSiteId);
				resetCamera();
			}
		});

	}

	/** Augment towards multiple sites */
	private void augmentMultiple() {
		try {
			Log.i(TAG, "Augmenting image for " + currentSiteList);
			progressBar.show();
			ARClient.getARSites().augmentImageGroup(currentSiteList,
					new FileInputStream(currentImagePath),
					new ARListener<AugmentedData>() {
				@Override
				public void handleResponse(AugmentedData resp) {
					Log.i(TAG, "Localization: " + resp.isLocalization());
					Log.i(TAG, "Augmented overlays: "
							+ resp.getOverlays().size());

					progressBar.dismiss();
					if (resp.isLocalization()) {
						Intent intent = new Intent(
								CaptureImageActivity.this,
								ARViewerActivity.class);
						intent.putExtra("image-id", augmentedImageId);
						intent.putExtra("augmented-data", resp);
						intent.putExtra("show-overlay", true);
						intent.putExtra("file-path", currentImagePath);
						intent.putExtra("original-size", ImageUtils
								.getImageSize(currentImagePath));
						startActivity(intent);
					} else {
						Toast.makeText(
								CaptureImageActivity.this,
								"Nothing to augment for this image. Please try again.",
								Toast.LENGTH_LONG).show();
						resetCamera();
					}
				}
			}, new ARErrorListener() {
				@Override
				public void handleError(Exception error) {
					Log.w(TAG,
							"Failed to augment the image: " + error);
					Toast.makeText(
							CaptureImageActivity.this,
							"Nothing to augment for this image. Please try again.",
							Toast.LENGTH_LONG).show();
					progressBar.dismiss();
					resetCamera();
				}
			});
		} catch (Exception e) {
			Log.w(TAG, "Failed to augment the image: " + e.getMessage());
			progressBar.dismiss();
			resetCamera();
		}
	}

	private class AugmentationAlertDialogListener implements DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				onAlertDialogPositiveClick();
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				onAlertDialogNegativeClick();
				break;
			}
		}

		public void onAlertDialogNegativeClick() {
			resetCamera();
		}

		public void onAlertDialogPositiveClick() {
			if (currentSiteList != null && currentSiteList.size() > 0) {
				augmentMultiple();
			} else {
				augmentImage();
			}
		}
	}
}
