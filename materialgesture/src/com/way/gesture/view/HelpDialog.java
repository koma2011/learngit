package com.way.gesture.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.way.gesture.R;

public class HelpDialog implements DialogInterface.OnClickListener {
	private Context mContext;
	private CheckBox mCheckBox;
	private Dialog mDialog;

	public void show(Context context, boolean notShow) {
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("not_show_gesture_help", false)
				&& notShow)
			return;
		mContext = context;
		if (mDialog == null)
			mDialog = onCreateDialog(context);
		mDialog.show();

	}

	public void dismiss() {
		if (mDialog != null && mDialog.isShowing())
			mDialog.dismiss();
	}

	public Dialog onCreateDialog(Context context) {
		View rootView = LayoutInflater.from(context).inflate(R.layout.help_dialog_layout, null);
		GifMovieView gifMovieView = (GifMovieView) rootView.findViewById(R.id.gif_view);
		gifMovieView.setMovieResource(R.drawable.help);
		mCheckBox = ((CheckBox) rootView.findViewById(R.id.check_box));
		boolean isCheck = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("not_show_gesture_help",
				false);
		mCheckBox.setChecked(isCheck);
		rootView.findViewById(R.id.checkLayout).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mCheckBox.setChecked(!mCheckBox.isChecked());
			}
		});
		return new AlertDialog.Builder(context).setTitle(R.string.getsure_help_menu).setCancelable(true)
				.setView(rootView).setPositiveButton(android.R.string.ok, this).create();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		boolean isChecked = mCheckBox.isChecked();
		PreferenceManager.getDefaultSharedPreferences(mContext).edit().putBoolean("not_show_gesture_help", isChecked)
				.apply();
		dialog.dismiss();
	}

}
