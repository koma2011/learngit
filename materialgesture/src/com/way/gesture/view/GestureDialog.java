package com.way.gesture.view;

import com.way.gesture.GestureController;
import com.way.gesture.R;
import com.way.gesture.activity.AddTaskActivity;
import com.way.gesture.activity.MainActivity;
import com.way.gesture.activity.UnLockActivity;
import com.way.gesture.bean.GestureLevel;
import com.way.gesture.bean.GestureObject;
import com.way.gesture.util.GestureDataManager;
import com.way.gesture.util.MyLog;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.gesture.Gesture;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by way on 15/8/7.
 */
public class GestureDialog implements View.OnClickListener, GestureOverlayView.OnGestureListener {
	private Context mContext;
	private GestureController mGestureController;
	private View mSettingsBtn;
	private View mReDrawBtn;
	private View mAddGestureBtn;
	private TextView mWarnTextView;
	private Dialog mDialog;

	public GestureDialog(final Context context, GestureController gestureController) {
		mContext = context;
		mGestureController = gestureController;
	}

	public void show() {

		if (mDialog == null) {
			mDialog = onCreateDialog(mContext);
		} else {
			reDraw();
		}
		mDialog.getWindow().setType(mGestureController.isOnLockScreen() ? WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
				: WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

		mDialog.getWindow().setWindowAnimations(R.style.dialog_window_anim);
		mDialog.show();

	}

	public void cancel() {
		if (mDialog != null && mDialog.isShowing())
			mDialog.cancel();
	}

	public boolean isShowing() {
		return mDialog != null && mDialog.isShowing();
	}

	public Dialog onCreateDialog(Context context) {
		View rootView = initViews();
		return new AlertDialog.Builder(context.getApplicationContext(),
				android.support.v7.appcompat.R.style.Theme_AppCompat_Light_Dialog)
						// .setTitle(R.string.drawgesture)
						.setCancelable(true).setView(rootView).create();
	}

	private View initViews() {
		View rootView = LayoutInflater.from(mContext).inflate(R.layout.gesture_dialog_layout, null);
		int width = getDisplayWidth();
		int height = getDisplayHeight();
		final int gestureViewWidth = 9 * Math.min(width, height) / 10;
		FrameLayout.LayoutParams frameLayoutParams = new FrameLayout.LayoutParams(gestureViewWidth, gestureViewWidth,
				Gravity.CENTER);
		rootView.findViewById(R.id.gesture_container_layout).setLayoutParams(frameLayoutParams);
		rootView.findViewById(R.id.gesture_dialog_root).setOnClickListener(this);
		mSettingsBtn = rootView.findViewById(R.id.gesture_settings_btn);
		mReDrawBtn = rootView.findViewById(R.id.gesture_redraw_btn);
		mAddGestureBtn = rootView.findViewById(R.id.gesture_add_btn);
		mWarnTextView = (TextView) rootView.findViewById(R.id.gesture_warn_message);
		mSettingsBtn.setOnClickListener(this);
		mReDrawBtn.setOnClickListener(this);
		mAddGestureBtn.setOnClickListener(this);

		mGestureOverlayView = (GestureOverlayView) rootView.findViewById(R.id.gesture_overlay_view);
		mGestureOverlayView.setOnGestureListener(this);
		return rootView;
	}

	/**
	 * 获取手机屏幕高度
	 *
	 * @return
	 */
	public int getDisplayHeight() {
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	/**
	 * 获取手机屏幕宽度
	 *
	 * @return
	 */
	public int getDisplayWidth() {
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.gesture_dialog_root:
			this.cancel();
			break;
		case R.id.gesture_redraw_btn:
			MyLog.d(GestureDialog.class.getName(), "onClick R.id.gesture_redraw_btn");
			reDraw();
			break;
		case R.id.gesture_settings_btn:
			if (mGestureController.isOnLockScreen()) {
				cancel();
				Intent i = new Intent(mContext, UnLockActivity.class);
				i.putExtra(UnLockActivity.EXTRA_MODE, -1);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(i);
				return;
			}
			Intent intent = new Intent(mContext, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(intent);
			cancel();
			break;
		case R.id.gesture_add_btn:
			startEditJobActivity();
			break;
		default:
			break;
		}
	}

	private void reDraw() {
		if (mGestureOverlayView != null) {
			mGestureOverlayView.clear(false);
			mGestureOverlayView.modeChangedTo(GestureOverlayView.MODE_NORMAL);
			mAddGestureBtn.setVisibility(View.GONE);
			mReDrawBtn.setVisibility(View.GONE);
			mWarnTextView.setVisibility(View.GONE);
		}
	}

	private void startEditJobActivity() {
		if (mGestureOverlayView == null)
			return;
		GestureDataManager gestureDataManager = GestureDataManager.defaultManager(mContext);
		GestureObject gestureObject = new GestureObject();
		gestureObject.gesture = mGesture;
		gestureObject.allGesturePoints = gestureObject.gesture2Points(mGesture);
		gestureObject.gestureType = 3;
		gestureDataManager.insertGestureObject(gestureObject);
		if (mGestureController.isOnLockScreen()) {
			Intent intent = new Intent(mContext, UnLockActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(UnLockActivity.EXTRA_MODE, UnLockActivity.MODE_EDIT_GESTURE);
			//intent.putExtra("mode", "Edit");
			intent.putExtra("recorderID", gestureObject.gestureId);
			mContext.startActivity(intent);
		} else {
			Intent intent = new Intent(mContext, AddTaskActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("mode", "Edit");
			intent.putExtra("recorderID", gestureObject.gestureId);
			mContext.startActivity(intent);
		}
		cancel();
		reDraw();
		mGestureOverlayView.modeChangedTo(GestureOverlayView.MODE_NORMAL);
	}

	private void showOptionView(boolean isRedrawOnly, int mode) {
		if (isRedrawOnly) {
			mAddGestureBtn.setVisibility(View.GONE);
			mReDrawBtn.setVisibility(View.VISIBLE);
			mWarnTextView.setVisibility(View.VISIBLE);
			mWarnTextView.setText(R.string.gestureMessage3);
			return;
		}
		mAddGestureBtn.setVisibility(View.VISIBLE);
		mReDrawBtn.setVisibility(View.VISIBLE);
		mWarnTextView.setVisibility(View.VISIBLE);
		mWarnTextView.setText(mode == 0 ? R.string.getsure_option_info : R.string.getsure_option_info2);
	}

	private com.way.gesture.view.GestureOverlayView mGestureOverlayView;
	private Gesture mGesture;

	@Override
	public void processGesture(GestureOverlayView overlay, Gesture gesture) {
		mGesture = gesture;
		if (gesture == null || gesture.getLength() < GestureLevel.MIN_LENGTH) {
			showOptionView(true, 0);
			overlay.modeChangedTo(GestureOverlayView.MODE_EDIT);
			return;
		}
		GestureObject searchGestureObject = GestureDataManager.defaultManager(mContext).searchGesture(gesture);
		// 如果没有查询到匹配的手势
		if ((searchGestureObject == null) || (searchGestureObject.gestureType > 2)) {

			overlay.modeChangedTo(GestureOverlayView.MODE_EDIT);
			showOptionView(false, 0);
			return;
		}
		if (mGestureController.isOnLockScreen()) {
			cancel();
			overlay.modeChangedTo(GestureOverlayView.MODE_NORMAL);
			Intent i = new Intent(mContext, UnLockActivity.class);
			i.putExtra(UnLockActivity.EXTRA_MODE, searchGestureObject.gestureType);
			i.putExtra("packageName", searchGestureObject.packageName);
			i.putExtra("className", searchGestureObject.className);
			i.putExtra("phoneNumber", searchGestureObject.phoneNumber);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(i);
			return;
		}

		switch (searchGestureObject.gestureType) {
		case GestureObject.TYPE_LAUNCHER_APP:
			if (searchGestureObject.packageName == null) {
				GestureDataManager.defaultManager(mContext).delete(searchGestureObject);
				overlay.modeChangedTo(GestureOverlayView.MODE_EDIT);
				showOptionView(false, 0);
				return;
			}
			ComponentName componentName = new ComponentName(searchGestureObject.packageName,
					searchGestureObject.className);
			Intent intent = new Intent();
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setAction("android.intent.action.MAIN");
			intent.addCategory("android.intent.action.LAUNCHER");
			intent.setComponent(componentName);
			MyLog.d("gesture", " packageNmae :" + searchGestureObject.packageName + "   className :"
					+ searchGestureObject.className);
			MyLog.d("gesture", " intent :" + intent.toString());
			try {
				mContext.startActivity(intent);
				cancel();
				overlay.modeChangedTo(GestureOverlayView.MODE_NORMAL);
			} catch (Exception e) {
				MyLog.d("gesture", "activity not exists  ", e);

				GestureDataManager.defaultManager(mContext).delete(searchGestureObject);
				overlay.modeChangedTo(GestureOverlayView.MODE_EDIT);
				showOptionView(false, 1);
			}
			break;
		case GestureObject.TYPE_CALL_TO:
			Intent intent1 = new Intent("android.intent.action.CALL",
					Uri.parse("tel:" + searchGestureObject.phoneNumber));
			intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			MyLog.d("gesture", "" + intent1.toString());
			mContext.startActivity(intent1);
			cancel();
			overlay.modeChangedTo(GestureOverlayView.MODE_NORMAL);
			break;
		case GestureObject.TYPE_MMS_TO:
			Intent intent2 = new Intent("android.intent.action.SENDTO",
					Uri.parse("smsto:" + searchGestureObject.phoneNumber));
			intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			MyLog.d("gesture", "start" + intent2.toString());
			try {
				mContext.startActivity(intent2);
				cancel();
				overlay.modeChangedTo(GestureOverlayView.MODE_NORMAL);
				MyLog.d("gesture", "end" + intent2.toString());
			} catch (Exception e) {
				MyLog.d("gesture", "" + e.toString());
			}
			break;
		default:
			break;
		}
	}
}
