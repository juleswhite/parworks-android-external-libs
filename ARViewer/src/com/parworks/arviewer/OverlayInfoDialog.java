package com.parworks.arviewer;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

public class OverlayInfoDialog extends AlertDialog {

	public OverlayInfoDialog(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.dialog_overlay_input);
	}
	
}
