package com.parworks.arviewer;

import android.webkit.WebView;
import android.webkit.WebViewClient;

public class OverlayWebViewClient extends WebViewClient {
	
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		view.loadUrl(url);
		return true;
	}	
}
