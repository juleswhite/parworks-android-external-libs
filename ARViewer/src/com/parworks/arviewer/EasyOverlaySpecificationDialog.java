package com.parworks.arviewer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.parworks.androidlibrary.response.OverlayBoundary;
import com.parworks.androidlibrary.response.OverlayBoundary.OverlayBoundaryType;
import com.parworks.androidlibrary.response.OverlayConfiguration;
import com.parworks.androidlibrary.response.OverlayContent;
import com.parworks.androidlibrary.response.OverlayContent.OverlayContentType;
import com.parworks.androidlibrary.response.OverlayContent.OverlaySize;
import com.parworks.androidlibrary.response.OverlayCover;
import com.parworks.androidlibrary.response.OverlayCover.OverlayCoverType;
import com.parworks.arviewer.EasyOverlayAttributes.EasyOverlayColors;

public class EasyOverlaySpecificationDialog extends AlertDialog.Builder {
	
	public interface OnPositiveClickListener {
		public void OnPositiveClick(OverlayConfiguration overlayConfiguration, String name);
	}
	public interface OnNegativeClickListener {
		public void OnNegativeClick();
	}
	
	public static final String TAG = EasyOverlaySpecificationDialog.class.getName();

	private Context mContext;
	private View mDialogLayout;
	
	private OnPositiveClickListener mPositiveClickListener;
	private OnNegativeClickListener mNegativeClickListener;
	
	//widgets
	private Spinner mBoundaryColorSpinner;
	private Spinner mBoundaryTypeSpinner;
	private Spinner mCoverTypeSpinner;
	private Spinner mCoverColorSpinner;
	private Spinner mContentTypeSpinner;
	private Spinner mContentSizeSpinner;
	
	private EditText mTitleEditText;
	private SeekBar mCoverTransparencySeekBar;
	private EditText mCoverProviderEditText;
	private EditText mContentProviderEditText;
	
	private CheckBox mDisplayNameCheckBox;
	
	
	public EasyOverlaySpecificationDialog(Context context, OnPositiveClickListener positiveClickListener, OnNegativeClickListener negativeClickListener, OverlayConfiguration overlayConfiguration, String name) {
		super(context);
		mPositiveClickListener = positiveClickListener;
		mNegativeClickListener = negativeClickListener;
		mContext = context;
		
		
		
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		mDialogLayout = inflater.inflate(R.layout.easy_overlay_specification, null);
		setView(mDialogLayout);
		setTitle(R.string.addOverlayDialogRegisterTitle);
		setNegativeButton(R.string.addOverlayDialogRegisterNegativeButton,new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onNegativeClick();
				
			}
		});
		setPositiveButton(R.string.addOverlayDialogRegisterPositiveButton,new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onPositiveClick();
				
			}
		});
		mCoverTransparencySeekBar = (SeekBar) mDialogLayout.findViewById(R.id.seekBarAddOverlayCoverTransparency);
		mCoverProviderEditText = (EditText) mDialogLayout.findViewById(R.id.editTextAddOverlayCoverProvider);
		mContentProviderEditText = (EditText) mDialogLayout.findViewById(R.id.editTextAddOverlayContentProvider);
		mTitleEditText = (EditText) mDialogLayout.findViewById(R.id.editTextAddOverlayTitle);
		
		mBoundaryColorSpinner = (Spinner) mDialogLayout.findViewById(R.id.spinnerAddOverlayBoundaryColor);
		mBoundaryTypeSpinner = (Spinner) mDialogLayout.findViewById(R.id.spinnerAddOverlayBoundaryType);
		mContentSizeSpinner = (Spinner) mDialogLayout.findViewById(R.id.spinnerAddOverlayContentSize);
		mContentTypeSpinner = (Spinner) mDialogLayout.findViewById(R.id.spinnerAddOverlayContentType);
		mCoverColorSpinner = (Spinner) mDialogLayout.findViewById(R.id.spinnerAddOverlayCoverColor);
		mCoverTypeSpinner = (Spinner) mDialogLayout.findViewById(R.id.spinnerAddOverlayCoverType);
		
		mDisplayNameCheckBox = (CheckBox) mDialogLayout.findViewById(R.id.checkBoxDisplayName);
		if(overlayConfiguration != null) {
			mCoverTransparencySeekBar.setProgress(overlayConfiguration.getCover().getTransparency());
			mCoverProviderEditText.setText(overlayConfiguration.getCover().getProvider());
			mContentProviderEditText.setText(overlayConfiguration.getContent().getProvider());
			if(name != null) {
				mTitleEditText.setText(name);
			} else {
				mTitleEditText.setText(overlayConfiguration.getTitle());
			}
			
			fillSpinner(mBoundaryColorSpinner,EasyOverlayAttributes.EasyOverlayColors.values(),overlayConfiguration.getBoundary().getColor());
			fillSpinner(mCoverTypeSpinner,OverlayCoverType.values(),overlayConfiguration.getCover().getType());
			fillSpinner(mBoundaryTypeSpinner,OverlayBoundaryType.values(),overlayConfiguration.getBoundary().getType());
			fillSpinner(mCoverColorSpinner,EasyOverlayAttributes.EasyOverlayColors.values(),overlayConfiguration.getCover().getColor());
			fillSpinner(mContentTypeSpinner,OverlayContentType.values(),overlayConfiguration.getContent().getType());
			fillSpinner(mContentSizeSpinner,OverlayContent.OverlaySize.values(),overlayConfiguration.getContent().getSize());
			
			if (overlayConfiguration.getTitle() == null || TextUtils.isEmpty(overlayConfiguration.getTitle())) {
				mDisplayNameCheckBox.setChecked(false);
			} else {
				mDisplayNameCheckBox.setChecked(true);
			}

		} else {
			fillSpinner(mBoundaryColorSpinner,EasyOverlayAttributes.EasyOverlayColors.values());
			fillSpinner(mCoverTypeSpinner,OverlayCoverType.values());
			fillSpinner(mBoundaryTypeSpinner,OverlayBoundaryType.values());
			fillSpinner(mCoverColorSpinner,EasyOverlayAttributes.EasyOverlayColors.values());
			fillSpinner(mContentTypeSpinner,OverlayContentType.values());
			fillSpinner(mContentSizeSpinner,OverlayContent.OverlaySize.values());
			setAllDefaultValues();
		}
		setCoverTypeListener();

	}
	public EasyOverlaySpecificationDialog(Context context, OnPositiveClickListener positiveClickListener, OnNegativeClickListener negativeClickListener) {
		this(context,positiveClickListener,negativeClickListener,null, null);
	}
	public void setContentProvider(String contentProvider) {
		mContentProviderEditText.setText(contentProvider);
	}
	
	private void onPositiveClick() {
		OverlayBoundary overlayBoundary = new OverlayBoundary();
		overlayBoundary.setColor(getSelectedString(mBoundaryColorSpinner,EasyOverlayColors.class));
		overlayBoundary.setType(getSelectedString(mBoundaryTypeSpinner,OverlayBoundaryType.class));
		
		OverlayCover overlayCover = new OverlayCover();
		overlayCover.setColor(getSelectedString(mCoverColorSpinner, EasyOverlayColors.class));
		overlayCover.setProvider(mCoverProviderEditText.getText().toString());
		overlayCover.setTransparency(mCoverTransparencySeekBar.getProgress());
		overlayCover.setType(getSelectedString(mCoverTypeSpinner, OverlayCoverType.class));
		
		OverlayContent overlayContent = new OverlayContent();
		overlayContent.setProvider(mContentProviderEditText.getText().toString());
		overlayContent.setSize(getSelectedString(mContentSizeSpinner, OverlaySize.class));
		overlayContent.setType(getSelectedString(mContentTypeSpinner, OverlayContentType.class));
		
		OverlayConfiguration overlayConfiguration = new OverlayConfiguration();
		overlayConfiguration.setBoundary(overlayBoundary);
		overlayConfiguration.setContent(overlayContent);
		overlayConfiguration.setCover(overlayCover);
		
		
		if(mDisplayNameCheckBox.isChecked()) {
			overlayConfiguration.setTitle(mTitleEditText.getText().toString());
		} else {
			overlayConfiguration.setTitle(null);
		}
		
		mPositiveClickListener.OnPositiveClick(overlayConfiguration, mTitleEditText.getText().toString());
		
	}
	private <T extends Enum<T>> String getSelectedString(Spinner spinner, Class<T> type) {
		return (String) spinner.getSelectedItem();	
	}
	private void onNegativeClick() {
		mNegativeClickListener.OnNegativeClick();
	}
	
	private void setCoverTypeListener() {
		mCoverTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adapter, View arg1,
					int position, long arg3) {
				OverlayCoverType coverType = OverlayCoverType.valueOf((String)adapter.getItemAtPosition(position));
					if (coverType == OverlayCoverType.IMAGE) {
					mDialogLayout.findViewById(R.id.textViewAddOverlayCoverColor).setVisibility(View.INVISIBLE);
					mDialogLayout.findViewById(R.id.spinnerAddOverlayCoverColor).setVisibility(View.INVISIBLE);
					mDialogLayout.findViewById(R.id.textViewAddOverlayCoverProvider).setVisibility(View.VISIBLE);
					mDialogLayout.findViewById(R.id.editTextAddOverlayCoverProvider).setVisibility(View.VISIBLE);
				} else if (coverType == OverlayCoverType.HIDE) {
					mDialogLayout.findViewById(R.id.textViewAddOverlayCoverColor).setVisibility(View.INVISIBLE);
					mDialogLayout.findViewById(R.id.spinnerAddOverlayCoverColor).setVisibility(View.INVISIBLE);
					mDialogLayout.findViewById(R.id.textViewAddOverlayCoverProvider).setVisibility(View.INVISIBLE);
					mDialogLayout.findViewById(R.id.editTextAddOverlayCoverProvider).setVisibility(View.INVISIBLE);
				} else if (coverType == OverlayCoverType.REGULAR) {
					mDialogLayout.findViewById(R.id.textViewAddOverlayCoverColor).setVisibility(View.VISIBLE);
					mDialogLayout.findViewById(R.id.spinnerAddOverlayCoverColor).setVisibility(View.VISIBLE);
					mDialogLayout.findViewById(R.id.textViewAddOverlayCoverProvider).setVisibility(View.INVISIBLE);
					mDialogLayout.findViewById(R.id.editTextAddOverlayCoverProvider).setVisibility(View.INVISIBLE);
				}
				else {
					Log.e(TAG, "Cover type was a type other than the ones specified. Go to EasyOverlaySpecificationDialog.setCoverTypeListener() and specify visibility for the new CoverType");
					mDialogLayout.findViewById(R.id.textViewAddOverlayCoverColor).setVisibility(View.INVISIBLE);
					mDialogLayout.findViewById(R.id.spinnerAddOverlayCoverColor).setVisibility(View.INVISIBLE);
					mDialogLayout.findViewById(R.id.textViewAddOverlayCoverProvider).setVisibility(View.INVISIBLE);
					mDialogLayout.findViewById(R.id.editTextAddOverlayCoverProvider).setVisibility(View.INVISIBLE);
				}
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
		
	}
	
	private <T extends Enum<T>>void fillSpinner(Spinner spinner, T[] values) {
		fillSpinner(spinner,values,null);
	}
	private <T extends Enum<T>>void fillSpinner(Spinner spinner, T[] values,String selection) {
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(mContext, R.layout.spinner_text_view_black);
		for(String str : enumNameToStringArray(values)) {
			spinnerAdapter.add(str);
		}
		spinner.setAdapter(spinnerAdapter);
		if(selection !=null) {
			spinner.setSelection(spinnerAdapter.getPosition(selection));
		}
	}
	private <T extends Enum<T>> String[] enumNameToStringArray(T[] values) {
		int i = 0;
		String[] result = new String[values.length];
		for (T value: values) {
			result[i++] = value.name();
		}
		return result;
	}
	private void setAllDefaultValues() {
		setDefaultValue(mContentTypeSpinner,OverlayContentType.TEXT, OverlayContentType.class);
		setDefaultValue(mContentSizeSpinner, OverlaySize.LARGE, OverlaySize.class);
		
		setDefaultValue(mCoverColorSpinner,EasyOverlayAttributes.EasyOverlayColors.WHITE,EasyOverlayColors.class);
		setDefaultValue(mCoverTypeSpinner,OverlayCoverType.REGULAR,OverlayCoverType.class);
		
		setDefaultValue(mBoundaryTypeSpinner,OverlayBoundaryType.DASHED,OverlayBoundaryType.class);
		setDefaultValue(mBoundaryColorSpinner,EasyOverlayColors.RED,EasyOverlayColors.class);
	}
	private <T extends Enum<T>> void setDefaultValue(Spinner spinner, T defaultValue, Class<T> enumClass) {
		SpinnerAdapter spinnerAdapter = spinner.getAdapter();
		
		int positionOfDefaultValue = 0;
		
		int itemTotal = spinnerAdapter.getCount();
		for(int index = 0; index < itemTotal; ++index) {
			String objectAtPosition = (String) spinnerAdapter.getItem(index);
			T enumObject = Enum.valueOf(enumClass, objectAtPosition);
			if(enumObject == defaultValue) {
				positionOfDefaultValue = index;
				break;
			}
		}
		spinner.setSelection(positionOfDefaultValue);
	}

}
