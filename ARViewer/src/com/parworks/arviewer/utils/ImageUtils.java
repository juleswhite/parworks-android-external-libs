package com.parworks.arviewer.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Environment;

import com.parworks.androidlibrary.ar.BaseImage;

public class ImageUtils {

	private static String filename = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/" + "parworks/tmpimage.jpeg";
	
	public static float[] getImageSizeFloat(String filename) {
		String[] sizeStr = getImageSize(filename).split("x");
		float[] size = new float[2];
		if (sizeStr.length == 2) {
			size[0] = Float.parseFloat(sizeStr[0]);
			size[1] = Float.parseFloat(sizeStr[1]);
		}
		return size;
	}

	public static String getImageSize(String filename) {
		String size = "";
		try {
			ExifInterface exif = new ExifInterface(filename);
			size = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) + "x" + exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
		} catch (IOException e) {
			File image = new File(filename);
			BitmapFactory.Options bounds = new BitmapFactory.Options();
			bounds.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(image.getPath(), bounds);
			size = bounds.outWidth + "x" + bounds.outHeight;
		}
		return size;
	}

	public static List<String> getListOfBaseImageIds(List<BaseImage> baseImages) {
		List<String> imageIds = new ArrayList<String>();
		for(BaseImage image : baseImages) {
			imageIds.add(image.getBaseImageId());
		}
		return imageIds;
	}

	public static Bitmap decodeSampledBitmapFromStream(InputStream inputStream,
			int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		//options.inJustDecodeBounds = true;

		//BitmapFactory.decodeStream(inputStream, null, options);

		// Calculate inSampleSize
		//options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		options.inSampleSize = 1;
		
		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		
		return BitmapFactory.decodeStream(inputStream, null, options);
	}
	
	public static String saveBitmapAsFile(Bitmap bitmap, String filePath) {
		String finalPath = (filePath == null) ? filename : filePath;
	    FileOutputStream out;
		try {
			out = new FileOutputStream(finalPath);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
		} catch (FileNotFoundException e) {		
			e.printStackTrace();
		}
		return finalPath;
	}
	
	public static Bitmap decodeSampledBitmapFromFile(String filePath, int sampleSize) throws FileNotFoundException {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = sampleSize;		
		options.inJustDecodeBounds = false;		
		return BitmapFactory.decodeStream(new FileInputStream(new File(filePath)), null, options);		
	}

	public static int calculateInSampleSize(
			int width, int height, int reqWidth, int reqHeight) {
		// Raw height and width of image
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float)height / (float)reqHeight);
			} else {
				inSampleSize = Math.round((float)width / (float)reqWidth);
			}
		}
		return inSampleSize;
	}
}
