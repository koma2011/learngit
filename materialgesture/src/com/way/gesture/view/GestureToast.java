package com.way.gesture.view;

import com.way.gesture.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.Toast;

public class GestureToast extends Dialog {
	private String mMessage;

	private GestureToast(Context context, String msg) {
		super(context, R.style.toastDialog);
		mMessage = msg;
	}

	public static void showToast(Context ontext, String msg) {
		new GestureToast(ontext, msg).show();
	}

	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
		Toast toast = Toast.makeText(getContext(), mMessage, Toast.LENGTH_SHORT);
		toast.getView();
		setContentView(toast.getView());
	}

	public void show() {
		super.show();
		new Handler().postDelayed(new Runnable() {
			public void run() {
				dismiss();
			}
		}, 1000L);
	}
}