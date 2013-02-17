package com.parworks.arviewer;

import android.os.Handler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ARWebViewClient extends WebViewClient {
	// Handler for read polygons
	Handler mHandler = new Handler();
	WebView currView;
	
	@Override
	public void onPageFinished(WebView view, String url) {
		if (url.equalsIgnoreCase("file:///android_asset/overlay.html")) {
			currView = view;
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					currView.loadUrl("javascript:readPolygons()");
					currView.loadUrl("javascript:updatePolygons()");
				}
			});
		}
	}
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		view.loadUrl(url);
//		return super.shouldOverrideUrlLoading(view, url);
		return true;
	}
}
