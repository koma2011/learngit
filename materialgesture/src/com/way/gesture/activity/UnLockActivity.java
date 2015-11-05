package com.way.gesture.activity;

import com.way.gesture.util.MyLog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class UnLockActivity extends Activity {
	public static final String EXTRA_MODE = "extra_mode";
	public static final int MODE_LAUNCHER_APP = 0;
	public static final int MODE_MMS = 1;
	public static final int MODE_CALL = 2;
	public static final int MODE_EDIT_GESTURE = 4;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		setTransparent4L();
		overridePendingTransition(0, 0);
		MyLog.i("way", "UnLockActivity onCreate...");
	}

	@Override
	protected void onResume() {
		super.onResume();
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				Intent intent = getIntent();
				MyLog.i("way", "UnLockActivity onResume... intent = " + intent);
				if (intent == null)
					finish();
				int type = intent.getIntExtra(EXTRA_MODE, -1);
				String phoneNumber = intent.getStringExtra("phoneNumber");
				String packageName = intent.getStringExtra("packageName");
				String className = intent.getStringExtra("className");
				MyLog.i("way", "type = " + type + ", phoneNumber = " + phoneNumber + ", packageName = " + packageName
						+ ", className = " + className);
				switch (type) {
				case MODE_LAUNCHER_APP:
					ComponentName componentName = new ComponentName(packageName, className);
					Intent intentApp = new Intent().setComponent(componentName);
					intentApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intentApp.setAction("android.intent.action.MAIN");
					intentApp.addCategory("android.intent.action.LAUNCHER");
					try {
						startActivity(intentApp);
					} catch (Exception e) {
					}
					break;
				case MODE_MMS:
					Intent intentMms = new Intent("android.intent.action.CALL", Uri.parse("tel:" + phoneNumber));
					intentMms.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					try {
						startActivity(intentMms);
					} catch (Exception e) {
					}
					break;
				case MODE_CALL:
					Intent intentCall = new Intent("android.intent.action.SENDTO", Uri.parse("smsto:" + phoneNumber));
					intentCall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					try {
						startActivity(intentCall);
					} catch (Exception e) {
					}
					break;
				case MODE_EDIT_GESTURE:
					Intent intentEditGesture = new Intent(UnLockActivity.this, AddTaskActivity.class);
					intentEditGesture.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intentEditGesture.putExtra("mode", "Edit");
					intentEditGesture.putExtra("recorderID", intent.getIntExtra("recorderID", -1));
					startActivity(intentEditGesture);
					break;
				default:
					Intent intentMain = new Intent(UnLockActivity.this, MainActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intentMain);
					break;
				}
				finish();
			}
		}, 100);

	}

	@SuppressLint("NewApi")
	private void setTransparent4L() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
					| WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
			window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(Color.TRANSPARENT);
			window.setNavigationBarColor(Color.TRANSPARENT);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		overridePendingTransition(0, 0);
	}
}
