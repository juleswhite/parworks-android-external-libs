package com.parworks.arcameraview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import android.graphics.Point;

import com.parworks.androidlibrary.response.SiteInfo;
import com.parworks.androidlibrary.utils.Maps;

public class CameraParameters {

	public static enum UserImageQuality {
		HIGH,
		MEDIUM_HIGH,
		MEDIUM,
		MEDIUM_LOW,
		LOW
	}
	
	public static final int DEFAULT_JPEG_QUALITY = 70;

	private static class ImageQuality {
		public int minEdge;
		public int jpegQuality;

		public ImageQuality(int minEdge, int jpegQuality) {
			super();
			this.minEdge = minEdge;
			this.jpegQuality = jpegQuality;
		}
	}

	public static final Map<String, ImageQuality> PROFILE_IMAGE_QUALITY = Maps
			.newMap().with("config-dense", new ImageQuality(-1, 70))
			.with("config-super-dense", new ImageQuality(-1, 70))
			.with("config-accurate", new ImageQuality(1024, 50))
			.with("config-super-accurate", new ImageQuality(1024, 40))
			.with("default", new ImageQuality(1024, 50)).build();  // TEST OUT THIS

	public static final String[] SUPPORT_PARAMS = { "picture-size",
			"picture-size-values", "focus-mode", "focus-mode-values",
			"flash-mode", "flash-mode-values", "whitebalance",
			"whitebalance-values", "preview-size", "preview-size-values",
			"jpeg-thumbnail-width", "jpeg-thumbnail-height",
			"jpeg-thumbnail-size-values", "jpeg-thumbnail-quality" };

	private Point pictureSize;
	private String focusMode;
	private String flashMode;
	private String whiteBalance;
	private Point previewSize;
	private Point thumbnailSize;
	private List<Point> supportedPictureSizes;
	private List<String> supportedFocusModes;
	private List<String> supportedFlashModes;
	private List<String> supportedWhiteBalances;
	private List<Point> supportedPreviewSizes;
	private List<Point> supportedThumbnailSizes;
	private int jpegQuality = DEFAULT_JPEG_QUALITY;

	private Point maxPictureSize;

	public void init(SiteInfo site, boolean augment, Map<String, ?> prefsData) {
		if (augment) {
			initForAugmentation(site, prefsData);
		} else {
			initForBaseImageCapture(site, prefsData);
		}
	}
	
	public Point getMaxPictureSize() {
		return maxPictureSize;
	}

	private void initForAugmentation(SiteInfo site, Map<String, ?> prefsData) {
		updateParameters(prefsData);

		ImageQuality qual = site != null ? 
				PROFILE_IMAGE_QUALITY.get(site.getProcessingProfile()) :
				PROFILE_IMAGE_QUALITY.get("default");

		if (qual.minEdge > -1) {
			Point curr = pictureSize;
			for (Point q : supportedPictureSizes) {
				if (q.x > qual.minEdge && q.y > qual.minEdge && curr.x > q.x
						&& curr.y > q.y) {
					curr = q;
				}
			}
			pictureSize = curr;
		}
		jpegQuality = qual.jpegQuality;
	}

	private void initForBaseImageCapture(SiteInfo site, Map<String, ?> prefsData) {
		updateParameters(prefsData);
		pictureSize = maxPictureSize;
	}

	private void updateParameters(Map<String, ?> prefsData) {

		focusMode = (String) prefsData.get("focus-mode");
		flashMode = (String) prefsData.get("flash-mode");
		whiteBalance = (String) prefsData.get("whitebalance");
		previewSize = parseSize((String) prefsData.get("preview-size"));
		thumbnailSize = parseSize((String) prefsData.get("thumbnail-size"));

		if (supportedPictureSizes == null) {
			supportedPictureSizes = new ArrayList<Point>();
			String value = (String) prefsData.get("picture-size-values");
			if (value != null) {
				StringTokenizer st = new StringTokenizer(value, ",");
				while (st.hasMoreTokens()) {
					Point sz = parseSize(st.nextToken());
					supportedPictureSizes.add(sz);

					if (maxPictureSize == null
							|| (sz.x > maxPictureSize.x && sz.y > maxPictureSize.y)) {
						maxPictureSize = sz;
					}
				}
			}

			supportedFocusModes = new ArrayList<String>();
			value = (String) prefsData.get("focus-mode-values");
			if (value != null) {
				StringTokenizer st = new StringTokenizer(value, ",");
				while (st.hasMoreElements())
					supportedFocusModes.add(st.nextToken());
			}

			supportedFlashModes = new ArrayList<String>();
			value = (String) prefsData.get("flash-mode-values");
			if (value != null) {
				StringTokenizer st = new StringTokenizer(value, ",");
				while (st.hasMoreElements())
					supportedFlashModes.add(st.nextToken());
			}

			supportedWhiteBalances = new ArrayList<String>();
			value = (String) prefsData.get("whitebalance-values");
			if (value != null) {
				StringTokenizer st = new StringTokenizer(value, ",");
				while (st.hasMoreElements())
					supportedWhiteBalances.add(st.nextToken());
			}

			supportedPreviewSizes = new ArrayList<Point>();
			value = (String) prefsData.get("preview-size-values");
			if (value != null) {
				StringTokenizer st = new StringTokenizer(value, ",");
				while (st.hasMoreElements())
					supportedPreviewSizes.add(parseSize(st.nextToken()));
			}

			supportedThumbnailSizes = new ArrayList<Point>();
			value = (String) prefsData.get("jpeg-thumbnail-size-values");
			if (value != null) {
				StringTokenizer st = new StringTokenizer(value, ",");
				while (st.hasMoreElements())
					supportedThumbnailSizes.add(parseSize(st.nextToken()));
			}
		}

		if (maxPictureSize != null) {
			pictureSize = maxPictureSize;
		} else {
			pictureSize = parseSize((String) prefsData.get("picture-size"));
		}
	}

	public List<Point> getSupportedPictureSizes() {
		return supportedPictureSizes;
	}

	public List<String> getSupportedFoucsModes() {
		return supportedFocusModes;
	}

	public List<String> getSupportedFlashModes() {
		return supportedFlashModes;
	}

	public List<String> getSupportedWhiteBalance() {
		return supportedWhiteBalances;
	}

	public List<Point> getSupportedPreviewSizes() {
		return supportedPreviewSizes;
	}

	public List<Point> getSupportedThumbnailSizes() {
		return supportedThumbnailSizes;
	}

	public Point getPictureSize() {
		return pictureSize;
	}

	public void setPictureSize(Point size) {
		pictureSize = size;
	}

	public String getFocusMode() {
		return focusMode;
	}

	public void setFocusMode(String mode) {
		focusMode = mode;
	}

	public String getFlashMode() {
		return flashMode;
	}

	public void setFlashMode(String mode) {
		flashMode = mode;
	}

	public String getWhiteBalance() {
		return whiteBalance;
	}

	public void setWhiteBalance(String wb) {
		whiteBalance = wb;
	}

	public Point getPreviewSize() {
		return previewSize;
	}

	public void setPreviewSize(Point size) {
		previewSize = size;
	}

	public Point getThumbnailSize() {
		return thumbnailSize;
	}

	public void setThumbnailSize(Point size) {
		thumbnailSize = size;
	}

	public int getJpegQuality() {
		return jpegQuality;
	}

	public void setJpegQuality(int jpegQuality) {
		this.jpegQuality = jpegQuality;
	}

	private Point parseSize(String size) {
		if (size != null) {
			StringTokenizer st = new StringTokenizer(size, "x");
			int width = Integer.parseInt(st.nextToken());
			int height = Integer.parseInt(st.nextToken());
			return new Point(width, height);
		} else
			return null;
	}
	
	public String[] getSupportedPicatureSizes() {
		String[] result = new String[this.supportedPictureSizes.size()];
		int index = 0;
		for(Point p : this.getSupportedPictureSizes()) {
			result[index++] = p.x + "x" + p.y;
		}
		return result;
	}
}
