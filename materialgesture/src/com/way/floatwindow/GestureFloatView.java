//package com.android.systemui.statusbar;
package com.way.floatwindow;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.way.gesture.R;
import com.way.gesture.util.AppUtil;
import com.way.gesture.util.MyLog;

import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

/**
 * we can move this class to SystemUI, change the LayoutParams.type =
 * LayoutParams.TYPE_STATUS_BAR_PANEL, then we can get the better touch
 * 
 * @author way
 *
 */
public class GestureFloatView extends View {
	private static final String TAG = "GestureFloatView";
	private static final boolean DEBUG = false;
	private static final long SWIPE_TIMEOUT_MS = 500;
	private static final int MAX_TRACKED_POINTERS = 32; // max per input system
	private static final int UNTRACKED_POINTER = -1;

	private static final int SWIPE_NONE = 0;
	private static final int SWIPE_FROM_TOP = 1;
	private static final int SWIPE_FROM_BOTTOM = 2;
	private static final int SWIPE_FROM_RIGHT = 3;

	private final int mSwipeStartThreshold;
	private final int mSwipeDistanceThreshold;
	private final Callbacks mCallbacks;
	private final int[] mDownPointerId = new int[MAX_TRACKED_POINTERS];
	private final float[] mDownX = new float[MAX_TRACKED_POINTERS];
	private final float[] mDownY = new float[MAX_TRACKED_POINTERS];
	private final long[] mDownTime = new long[MAX_TRACKED_POINTERS];

	private int mScreenHeight;
	private int mScreenWidth;
	private int mDownPointers;
	private boolean mSwipeFireable;
	private boolean mDebugFireable;
	private static final String GESTURE_SWITCH_KEY = "way_gesture_switch";
	private final Object mLock = new Object();
	private Context mContext;
	private final Handler mHandler = new Handler();
	private final GestureSwitchObserver mSettingsObserver = new GestureSwitchObserver(
			mHandler);
	private WindowManager mWindowManager;
	private LayoutParams mLayoutParams;

	private final class GestureSwitchObserver extends ContentObserver {
		private final Uri WAY_GESTURE_SWITCH_URI = Settings.System
				.getUriFor(GESTURE_SWITCH_KEY);

		public GestureSwitchObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange, Uri uri) {
			if (WAY_GESTURE_SWITCH_URI.equals(uri)) {
				synchronized (mLock) {
					updateViewFlags();
				}
			}
		}

		public void setListening(boolean listening) {
			final ContentResolver cr = mContext.getContentResolver();
			if (listening) {
				cr.registerContentObserver(WAY_GESTURE_SWITCH_URI, false, this);
			} else {
				cr.unregisterContentObserver(this);
			}
		}
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if(DEBUG)
		MyLog.i(TAG, "configuration changed: "
				+ mContext.getResources().getConfiguration());
	}

	private void updateViewFlags() {
		if(DEBUG)
		MyLog.d(TAG, "switch is change");
		if (Settings.System.getInt(getContext().getContentResolver(),
				GESTURE_SWITCH_KEY, 0) == 0) {
			mLayoutParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
					| LayoutParams.FLAG_NOT_FOCUSABLE
					| FLAG_LAYOUT_IN_SCREEN
					| LayoutParams.FLAG_NOT_TOUCHABLE;
		} else {
			mLayoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE
									| FLAG_LAYOUT_IN_SCREEN;
		}
		mWindowManager.updateViewLayout(GestureFloatView.this, mLayoutParams);
	}

	/**
	 * 获取手机屏幕高度
	 * 
	 * @return screen height
	 */
	public int getDisplayHeight() {
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		mWindowManager.getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	/**
	 * 获取手机屏幕宽度
	 * 
	 * @return screen width
	 */
	public int getDisplayWidth() {
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		mWindowManager.getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}

	public GestureFloatView(Context context, Callbacks callbacks) {
		super(context);
		mContext = context;
		mCallbacks = checkNull("callbacks", callbacks);
		mWindowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		mScreenHeight = getDisplayHeight();
		mScreenWidth = getDisplayWidth();
		int statusBarHeight = AppUtil.getStatusBarSize(context.getResources());
		mSwipeStartThreshold = checkNull("context", statusBarHeight);
		mSwipeDistanceThreshold = mSwipeStartThreshold;
		if (DEBUG)
			MyLog.d(TAG, "mSwipeStartThreshold=" + mSwipeStartThreshold
					+ " mSwipeDistanceThreshold=" + mSwipeDistanceThreshold);

		mLayoutParams = new LayoutParams();
		mLayoutParams.width = getResources().getDimensionPixelOffset(
				R.dimen.float_touch_view_width);
		mLayoutParams.height = statusBarHeight;
		mLayoutParams.format = PixelFormat.RGBA_8888;

		// mLayoutParams.type = LayoutParams.TYPE_STATUS_BAR_PANEL;
		mLayoutParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
		if (Settings.System.getInt(getContext().getContentResolver(),
				GESTURE_SWITCH_KEY, 0) == 0) {
			mLayoutParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
					| LayoutParams.FLAG_NOT_FOCUSABLE
					| FLAG_LAYOUT_INSET_DECOR
					| FLAG_LAYOUT_IN_SCREEN
					| LayoutParams.FLAG_NOT_TOUCHABLE;
		} else {
			mLayoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE
					| FLAG_LAYOUT_IN_SCREEN;
		}
		mLayoutParams.gravity = Gravity.START | Gravity.TOP;
		//setBackgroundColor(Color.BLUE);
		//mWindowManager.addView(this, createLayoutParams(getContext()));
		mWindowManager.addView(this,mLayoutParams);

	}
	static WindowManager.LayoutParams createLayoutParams(Context context) {
		Resources res = context.getResources();
		int width = res.getDimensionPixelSize(R.dimen.float_touch_view_width);
		int height = AppUtil.getStatusBarSize(res);

		final WindowManager.LayoutParams params =
				new WindowManager.LayoutParams(width, height, TYPE_SYSTEM_ERROR, FLAG_NOT_FOCUSABLE
						| FLAG_NOT_TOUCH_MODAL
						| FLAG_LAYOUT_NO_LIMITS
						| FLAG_LAYOUT_INSET_DECOR
						| FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
		params.gravity = Gravity.TOP | Gravity.START;

		return params;
	}
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mSettingsObserver.setListening(true);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mSettingsObserver.setListening(false);
	}

	public void removeFromWindow() {
		if (mWindowManager != null) {
			mWindowManager.removeView(this);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			mSwipeFireable = true;
			mDebugFireable = true;
			mDownPointers = 0;
			captureDown(event, 0);
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			captureDown(event, event.getActionIndex());
			if (mDebugFireable) {
				mDebugFireable = event.getPointerCount() < 5;
				if (!mDebugFireable) {
					if (DEBUG)
						MyLog.d(TAG, "Firing debug");
					mCallbacks.onDebug();
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mSwipeFireable) {
				final int swipe = detectSwipe(event);
				mSwipeFireable = swipe == SWIPE_NONE;
				if (swipe == SWIPE_FROM_TOP) {
					if (DEBUG)
						MyLog.d(TAG, "Firing onSwipeFromTop");
					mCallbacks.onSwipeFromTop();
				} else if (swipe == SWIPE_FROM_BOTTOM) {
					if (DEBUG)
						MyLog.d(TAG, "Firing onSwipeFromBottom");
					mCallbacks.onSwipeFromBottom();
				} else if (swipe == SWIPE_FROM_RIGHT) {
					if (DEBUG)
						MyLog.d(TAG, "Firing onSwipeFromRight");
					mCallbacks.onSwipeFromRight();
				}
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			mSwipeFireable = false;
			mDebugFireable = false;
			break;
		default:
			if (DEBUG)
				MyLog.d(TAG, "Ignoring " + event);
		}
		return true;
	}

	private static <T> T checkNull(String name, T arg) {
		if (arg == null) {
			throw new IllegalArgumentException(name + " must not be null");
		}
		return arg;
	}

	private void captureDown(MotionEvent event, int pointerIndex) {
		final int pointerId = event.getPointerId(pointerIndex);
		final int i = findIndex(pointerId);
		if (DEBUG)
			MyLog.d(TAG, "pointer " + pointerId + " down pointerIndex="
					+ pointerIndex + " trackingIndex=" + i);
		if (i != UNTRACKED_POINTER) {
			mDownX[i] = event.getX(pointerIndex);
			mDownY[i] = event.getY(pointerIndex);
			mDownTime[i] = event.getEventTime();
			if (DEBUG)
				MyLog.d(TAG, "pointer " + pointerId + " down x=" + mDownX[i]
						+ " y=" + mDownY[i]);
		}
	}

	private int findIndex(int pointerId) {
		for (int i = 0; i < mDownPointers; i++) {
			if (mDownPointerId[i] == pointerId) {
				return i;
			}
		}
		if (mDownPointers == MAX_TRACKED_POINTERS
				|| pointerId == MotionEvent.INVALID_POINTER_ID) {
			return UNTRACKED_POINTER;
		}
		mDownPointerId[mDownPointers++] = pointerId;
		return mDownPointers - 1;
	}

	private int detectSwipe(MotionEvent move) {
		final int historySize = move.getHistorySize();
		final int pointerCount = move.getPointerCount();
		for (int p = 0; p < pointerCount; p++) {
			final int pointerId = move.getPointerId(p);
			final int i = findIndex(pointerId);
			if (i != UNTRACKED_POINTER) {
				for (int h = 0; h < historySize; h++) {
					final long time = move.getHistoricalEventTime(h);
					final float x = move.getHistoricalX(p, h);
					final float y = move.getHistoricalY(p, h);
					final int swipe = detectSwipe(i, time, x, y);
					if (swipe != SWIPE_NONE) {
						return swipe;
					}
				}
				final int swipe = detectSwipe(i, move.getEventTime(),
						move.getX(p), move.getY(p));
				if (swipe != SWIPE_NONE) {
					return swipe;
				}
			}
		}
		return SWIPE_NONE;
	}

	private int detectSwipe(int i, long time, float x, float y) {
		final float fromX = mDownX[i];
		final float fromY = mDownY[i];
		final long elapsed = time - mDownTime[i];
		if (DEBUG)
			MyLog.d(TAG, "pointer " + mDownPointerId[i] + " moved (" + fromX
					+ "->" + x + "," + fromY + "->" + y + ") in " + elapsed);
		if (fromY <= mSwipeStartThreshold
				&& y > fromY + mSwipeDistanceThreshold
				&& elapsed < SWIPE_TIMEOUT_MS) {
			return SWIPE_FROM_TOP;
		}
		if (fromY >= mScreenHeight - mSwipeStartThreshold
				&& y < fromY - mSwipeDistanceThreshold
				&& elapsed < SWIPE_TIMEOUT_MS) {
			return SWIPE_FROM_BOTTOM;
		}
		if (fromX >= mScreenWidth - mSwipeStartThreshold
				&& x < fromX - mSwipeDistanceThreshold
				&& elapsed < SWIPE_TIMEOUT_MS) {
			return SWIPE_FROM_RIGHT;
		}
		return SWIPE_NONE;
	}

	public interface Callbacks {
		public void onSwipeFromTop();

		public void onSwipeFromBottom();

		public void onSwipeFromRight();

		public void onDebug();
	}
}
