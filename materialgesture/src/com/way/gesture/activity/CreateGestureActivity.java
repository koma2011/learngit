package com.way.gesture.activity;

import com.way.fab.FloatingActionMenu;
import com.way.gesture.GestureCommond;
import com.way.gesture.R;
import com.way.gesture.bean.GestureLevel;
import com.way.gesture.bean.GestureObject;
import com.way.gesture.util.AppUtil;
import com.way.gesture.util.GestureDataManager;
import com.way.gesture.view.GestureOverlayView;
import com.way.ui.swipeback.SwipeBackActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.gesture.Gesture;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CreateGestureActivity extends SwipeBackActivity
		implements OnClickListener, GestureOverlayView.OnGestureListener {
	private static final int MODE_CREATE = 0;
	private static final int MODE_EDIT = 1;
	private String mActivityName;
	private String mAppName;
	private int mCommond;
	private GestureOverlayView mGestureView;
	private GestureDataManager mGestureDataManager;
	private int mCurrentMode = 0;
	private String mPackageName;
	private String mPhoneNumber;
	private int mRecorderID;
	private String mUserName;
	private View mCreateMenuView;
	private FloatingActionMenu mEditMenuView;
	private AppCompatButton mCreateLeftBtn;
	private AppCompatButton mCreateRightBtn;
	private GestureObject mCreateGestureObject;

	private void onDeleteMenuItem() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.gestureDelete);
		builder.setMessage(R.string.gestureDeleteMessage);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int position) {
				mGestureDataManager.delete(mGestureView.getGestureObject());
				dialogInterface.dismiss();
				setResult(GestureCommond.GestureSucess);
				finish();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.create().show();
	}

	private void onEditGestureMenuItem() {
		Intent intent = new Intent();
		intent.setClass(this, CreateGestureActivity.class);
		intent.putExtra("AppName", mAppName);
		intent.putExtra("mode", "");
		intent.putExtra("packageName", mPackageName);
		intent.putExtra("userName", mUserName);
		intent.putExtra("phoneNumber", mPhoneNumber);
		intent.putExtra("commond", mCommond);
		intent.putExtra("activityName", mActivityName);
		intent.putExtra("recorderID", mRecorderID);
		startActivityForResult(intent, GestureCommond.GestureEdit);
	}

	private void onEditJobMenuItem() {
		Intent intent = new Intent();
		intent.setClass(this, AddTaskActivity.class);
		intent.putExtra("AppName", mAppName);
		intent.putExtra("mode", "Edit");
		intent.putExtra("packageName", mPackageName);
		intent.putExtra("userName", mUserName);
		intent.putExtra("phoneNumber", mPhoneNumber);
		intent.putExtra("commond", mCommond);
		intent.putExtra("activityName", mActivityName);
		intent.putExtra("recorderID", mRecorderID);
		startActivityForResult(intent, GestureCommond.GestureEditJob);
	}

	private void onOkMenuItem() {
		mCreateGestureObject.gestureType = 0;
		mCreateGestureObject.appName = mAppName;
		mCreateGestureObject.className = mActivityName;
		mCreateGestureObject.packageName = mPackageName;
		mCreateGestureObject.gestureType = mCommond;
		mCreateGestureObject.userName = mUserName;
		mCreateGestureObject.phoneNumber = mPhoneNumber;
		mGestureDataManager.insertGestureObject(mCreateGestureObject);
		setResult(GestureCommond.GestureSucess);
		finish();
	}

	private void onRedoMenuItem() {
		reDraw();
		mCreateRightBtn.setEnabled(false);
	}

	private void reDraw() {
		if (mGestureView != null) {
			mGestureView.clear(false);
			mGestureView.modeChangedTo(GestureOverlayView.MODE_NORMAL);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case GestureCommond.GestureEdit:// 104
			if (resultCode == GestureCommond.GestureSucess) {
				mGestureDataManager.delete(mGestureView.getGestureObject());
			}
			setResult(GestureCommond.GestureSucess);
			finish();
			break;
		case GestureCommond.GestureEditJob:// 105
			setResult(GestureCommond.GestureSucess);
			finish();
			break;
		default:
			break;
		}
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_create_getsture_layout);
		mCreateMenuView = findViewById(R.id.menu_create);
		mEditMenuView = (FloatingActionMenu) findViewById(R.id.menu_edit);
		mGestureView = (GestureOverlayView) findViewById(R.id.gesture_overlay_view);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mGestureDataManager = GestureDataManager.defaultManager(this);

		int width = AppUtil.getDisplayWidth(this);
		int height = AppUtil.getDisplayHeight(this);
		final int gestureViewWidth = 9 * Math.min(width, height) / 10;
		FrameLayout.LayoutParams frameLayoutParams = new FrameLayout.LayoutParams(gestureViewWidth, gestureViewWidth,
				Gravity.CENTER);
		mGestureView.setLayoutParams(frameLayoutParams);

		Intent intent = getIntent();
		String mode = intent.getStringExtra("mode");
		mRecorderID = intent.getIntExtra("recorderID", -1);
		mAppName = intent.getStringExtra("AppName");
		mActivityName = intent.getStringExtra("activityName");
		mPackageName = intent.getStringExtra("packageName");
		mUserName = intent.getStringExtra("userName");
		mPhoneNumber = intent.getStringExtra("phoneNumber");
		mCommond = intent.getIntExtra("commond", 0);
		TextView titleTextView = (TextView) findViewById(R.id.textview1);
		if ((mode != null) && (mode.equalsIgnoreCase("Edit"))) {
			mCurrentMode = MODE_EDIT;
			mCreateMenuView.setVisibility(View.GONE);
			mEditMenuView.setVisibility(View.VISIBLE);
			final GestureObject gestureObject = mGestureDataManager.getGestureObject(mRecorderID);
			mGestureView.setEnabled(false);
			mGestureView.setCardBackgroundColor(AppUtil.pickColor(gestureObject));
			new Handler().postDelayed(new Runnable() {

				@Override
				public void run() {
					mGestureView.setGestureObject(gestureObject);
				}
			}, 500);
			mAppName = gestureObject.appName;
			mActivityName = gestureObject.className;
			mPackageName = gestureObject.packageName;
			mUserName = gestureObject.userName;
			mPhoneNumber = gestureObject.phoneNumber;
			mCommond = gestureObject.gestureType;
		} else {

			mCurrentMode = MODE_CREATE;
			mCreateMenuView.setVisibility(View.VISIBLE);
			mEditMenuView.setVisibility(View.GONE);
			mCreateGestureObject = new GestureObject();
			mCreateGestureObject.appName = mAppName;
			mCreateGestureObject.gestureType = mCommond;
			mCreateGestureObject.packageName = mPackageName;
			mCreateGestureObject.className = mActivityName;
			mCreateGestureObject.phoneNumber = mPhoneNumber;

			mGestureView.setEnabled(true);
			mGestureView.setCardBackgroundColor(AppUtil.pickColor(mCreateGestureObject));
			mGestureView.setOnGestureListener(this);
		}

		if (mCommond == 0) {
			String startApp = getString(R.string.startapp);
			titleTextView.setText(startApp + AppUtil.getAppName(this, mPackageName));
		} else if (mCommond == 1) {
			String callto = getString(R.string.gesturelistcallto);
			titleTextView.setText(callto + mUserName);
		} else {
			String callto = getString(R.string.gesturelistsendsms);
			titleTextView.setText(callto + mUserName);
		}
		addFooterBarMenu();
	}

	private void addFooterBarMenu() {
		if (mCurrentMode == MODE_EDIT) {
			mEditMenuView.setClosedOnTouchOutside(true);
			findViewById(R.id.menu_delete_item).setOnClickListener(this);
			findViewById(R.id.menu_edit_gesture_item).setOnClickListener(this);
			findViewById(R.id.menu_edit_job_item).setOnClickListener(this);
		} else {
			mCreateLeftBtn = (AppCompatButton) findViewById(R.id.menu_redo_item);
			mCreateRightBtn = (AppCompatButton) findViewById(R.id.menu_ok_item);
			mCreateLeftBtn.setOnClickListener(this);
			mCreateRightBtn.setOnClickListener(this);
		}
	}

	@Override
	public void onBackPressed() {
		if (mEditMenuView != null && mEditMenuView.isOpened()) {
			mEditMenuView.close(true);
			return;
		}
		super.onBackPressed();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.menu_redo_item:
			onRedoMenuItem();
			break;
		case R.id.menu_ok_item:
			onOkMenuItem();
			break;

		case R.id.menu_delete_item:
			onDeleteMenuItem();
			break;
		case R.id.menu_edit_gesture_item:
			onEditGestureMenuItem();
			break;
		case R.id.menu_edit_job_item:
			onEditJobMenuItem();
			break;
		default:
			break;
		}
	}

	@Override
	public void processGesture(GestureOverlayView overlay, Gesture gesture) {
		if (gesture == null || gesture.getLength() < GestureLevel.MIN_LENGTH) {
			Toast.makeText(this, R.string.gestureMessage3, Toast.LENGTH_SHORT).show();
			overlay.modeChangedTo(GestureOverlayView.MODE_EDIT);
			return;
		}

		GestureObject gestureObject = GestureDataManager.defaultManager(this).searchGesture(gesture);
		// 没有存过的手势
		if (gestureObject == null || gestureObject.gestureType > 2) {
			mCreateGestureObject.gesture = gesture;
			mCreateGestureObject.allGesturePoints = mCreateGestureObject.gesture2Points(gesture);
			mCreateRightBtn.setEnabled(true);
			overlay.modeChangedTo(GestureOverlayView.MODE_EDIT);
			return;
		}

		String message = "";
		switch (gestureObject.gestureType) {
		case GestureObject.TYPE_LAUNCHER_APP:// 启动应用
			message = (getString(R.string.gestureMessageStart) + getString(R.string.startapp)
					+ AppUtil.getAppName(this, gestureObject.packageName) + getString(R.string.gestureMessageEnd));
			break;
		case GestureObject.TYPE_CALL_TO:// 拨号
			message = (getString(R.string.gestureMessageStart) + getString(R.string.gesturelistcallto)
					+ gestureObject.userName + getString(R.string.gestureMessageEnd));
			break;
		case GestureObject.TYPE_MMS_TO:// 发短信
			message = (getString(R.string.gestureMessageStart) + getString(R.string.gesturelistsendsms)
					+ gestureObject.userName + getString(R.string.gestureMessageEnd));
			break;

		default:
			break;
		}
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
		overlay.modeChangedTo(GestureOverlayView.MODE_EDIT);
	}

}