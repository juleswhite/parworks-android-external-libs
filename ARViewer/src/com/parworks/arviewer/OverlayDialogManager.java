/* 
 **
 ** Copyright 2012, Jules White
 **
 ** 
 */
package com.parworks.arviewer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.parworks.androidlibrary.response.ImageOverlayInfo;
import com.parworks.androidlibrary.response.OverlayContent.OverlayContentType;
import com.parworks.androidlibrary.response.OverlayContent.OverlaySize;

public class OverlayDialogManager {	

	public abstract class AbstractOverlayViewFactory implements OverlayDialogViewCreator {

		protected void configureDialogSize(Dialog d, OverlaySize size) {
			// get current display size
			Display display = d.getWindow().getWindowManager().getDefaultDisplay();
			int width = display.getWidth();
			int height = display.getHeight();
			if (size == OverlaySize.LARGE) {
				width *= 0.95;
				height *= 0.95;
			} else if (size == OverlaySize.MEDIUM) {
				width *= 0.75;
				height *= 0.75;
			} else {
				width *= 0.5;
				height *= 0.5;
			}
			// update size attributes
			WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			lp.copyFrom(d.getWindow().getAttributes());
			lp.width = width;
			lp.height = height;
			d.getWindow().setAttributes(lp);
		}
	}

	/** Handle URL Content */
	public class OverlayHtmlViewFactory extends AbstractOverlayViewFactory {
		@Override
		public boolean createView(final Activity parent, final Dialog d, ImageOverlayInfo overlay) {

			// get the content uri
			String uri = overlay.getConfiguration().getContent().getProvider();

			// get the content size
			OverlaySize size = overlay.getConfiguration().getContent().getOverlayContentSize();
			if (size == OverlaySize.FULL_SCREEN) {
				// we want to use the native viewer to view the image in full screen
				Intent i = new Intent(Intent.ACTION_VIEW); 
				i.setData(Uri.parse(overlay.getConfiguration().getContent().getProvider()));
				parent.startActivity(i);
				return false;
			} else {
				// init dialog
				d.requestWindowFeature(Window.FEATURE_NO_TITLE);						
				d.setContentView(R.layout.overlay_web_dialog);
				// use WebView for URL shown in a window
				WebView wv = new WebView(parent);
				wv.setWebViewClient(new OverlayWebViewClient()); // prevent redirect to default browser
				wv.loadUrl(uri);
				configureDialogSize(d, size);
				LinearLayout l = (LinearLayout) d.findViewById(R.id.overlayDialogWebContentContainer);			
				LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);					
				l.addView(wv, params);
				return true;
			}
		}
	}

	/** Handle Text Content */
	public class OverlayTextViewFactory extends AbstractOverlayViewFactory {
		@Override
		public boolean createView(Activity context, Dialog d, ImageOverlayInfo overlay) {
			d.requestWindowFeature(Window.FEATURE_NO_TITLE);
			d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			View v = context.getLayoutInflater().inflate(R.layout.overlay_dialog, null);
			TextView tv = (TextView)v.findViewById(R.id.popupText);
			tv.setText(overlay.getConfiguration().getContent().getProvider());
			d.setContentView(v);

			OverlaySize size = overlay.getConfiguration().getContent().getOverlayContentSize();
			configureDialogSize(d, size);
			return true;
		}
	}

	/** Handle Video Content */
	public class VideoViewFactory extends AbstractOverlayViewFactory {
		@Override
		public boolean createView(final Activity parent, final Dialog d,
				ImageOverlayInfo overlay) {

			// get the content uri
			final String uri = overlay.getConfiguration().getContent().getProvider();

			// get the content size
			OverlaySize size = overlay.getConfiguration().getContent().getOverlayContentSize();
			if (size == OverlaySize.FULL_SCREEN) {
				// we want to use the native viewer to view the image in full screen
				Intent i = new Intent(Intent.ACTION_VIEW); 
				i.setData(Uri.parse(uri));
				parent.startActivity(i);
				return false;
			} else {
				// init dialog
				d.requestWindowFeature(Window.FEATURE_NO_TITLE);						
				d.setContentView(R.layout.overlay_web_dialog);

				// use VideoView for video playing in a window
				VideoView vv = new VideoView(parent);
				vv.setVideoURI(Uri.parse(uri));
				vv.requestFocus();
				vv.setOnErrorListener(new OnErrorListener() {					
					@Override
					public boolean onError(MediaPlayer mp, int what, int extra) {
						// when it comes to here, it means the URI provided
						// cannot be played in the VideoView, such as a Youtube link
						// then we will use the intent to hanlde it

						// dismiss the dialog first
						d.dismiss();
						Intent i = new Intent(Intent.ACTION_VIEW); 
						i.setData(Uri.parse(uri));
						parent.startActivity(i);						
						return true;
					}
				});

				configureDialogSize(d, size);
				LinearLayout l = (LinearLayout) d.findViewById(R.id.overlayDialogWebContentContainer);			
				LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);					
				l.addView(vv, params);

				vv.start();
				return true;
			}
		}		
	}


	

	/** Handle Image Content */
	public class ImageViewFactory extends AbstractOverlayViewFactory {
		
		public final String TAG = ImageViewFactory.class.getName();
		@Override
		public boolean createView(Activity parent, Dialog d,
				ImageOverlayInfo overlay) {

			// get the content uri
			String uriString = overlay.getConfiguration().getContent().getProvider();
			Uri uri = Uri.parse(uriString);

			// get the content size
			OverlaySize size = overlay.getConfiguration().getContent().getOverlayContentSize();
			
			
			//if the size is full screen, or if the uri is a local content uri, start gallery viewer
			Log.d(TAG, "Uri scheme is: " + uri.getScheme() + ". Uri is: " + uriString);
			if ( (size == OverlaySize.FULL_SCREEN) || (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) ) {
				// we want to use the native viewer to view the image in full screen
				Intent i = new Intent(Intent.ACTION_VIEW); 
				i.setDataAndType(
						Uri.parse(overlay.getConfiguration().getContent().getProvider()), 
						"image/*");
				parent.startActivity(i);
				return false;
			} else {
				// init dialog
				d.requestWindowFeature(Window.FEATURE_NO_TITLE);						
				d.setContentView(R.layout.overlay_web_dialog);
				// use WebView for image shown in a window
				WebView wv = new WebView(parent);
				wv.loadUrl(uriString);
				configureDialogSize(d, size);
				LinearLayout l = (LinearLayout) d.findViewById(R.id.overlayDialogWebContentContainer);			
				LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);					
				l.addView(wv, params);
				return true;
			}
		}		
	}

	/** Handle Audio Content */
	public class AudioViewFactory extends AbstractOverlayViewFactory {
		@Override
		public boolean createView(final Activity parent, final Dialog d,
				ImageOverlayInfo overlay) {
			// get the content uri
			final String uri = overlay.getConfiguration().getContent().getProvider();

			// TODO Use MediaPlayer to better handle this

			// we want to use the native viewer to view the image in full screen
			Intent i = new Intent(Intent.ACTION_VIEW); 
			i.setDataAndType(Uri.parse(uri), "audio/*");
			parent.startActivity(i);
			return false;		
		}		
	}


	private EnumMap<OverlayContentType, OverlayDialogViewCreator> viewFactories_ = 
			new EnumMap<OverlayContentType, OverlayDialogViewCreator>(OverlayContentType.class);

	private Map<ImageOverlayInfo, Dialog> dialogCache = new HashMap<ImageOverlayInfo, Dialog>();

	public OverlayDialogManager() {
		viewFactories_.put(OverlayContentType.TEXT,	new OverlayTextViewFactory());
		viewFactories_.put(OverlayContentType.URL, new OverlayHtmlViewFactory());
		viewFactories_.put(OverlayContentType.VIDEO, new VideoViewFactory());
		viewFactories_.put(OverlayContentType.IMAGE, new ImageViewFactory());
		viewFactories_.put(OverlayContentType.AUDIO, new AudioViewFactory());
	}

	public void showOverlayDialog(Activity parent, ImageOverlayInfo overlay) {
		Dialog d = dialogCache.get(overlay);

		boolean show = false;
		if (d == null) {
			d = new Dialog(parent,R.style.DialogSlideAnim);
			show = buildContentView(parent, d, overlay);
			if (show) {
				dialogCache.put(overlay,d);
			}
		} else {
			show = true;
		}

		if (show) {
			d.show();
		}
	}

	private boolean buildContentView(Activity parent, Dialog d, ImageOverlayInfo overlay) {
		// retrieve the type of the content
		OverlayContentType type = overlay.getConfiguration().getContent().getOverlayContentType();
		OverlayDialogViewCreator factory = viewFactories_.get(type);
		// create the view for the content
		boolean show = false;
		if (factory != null) {
			show = factory.createView(parent, d, overlay);
		}		
		return show;
	}	
}
