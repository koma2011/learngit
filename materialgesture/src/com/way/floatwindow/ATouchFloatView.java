package com.way.floatwindow;

import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

import com.way.gesture.R;
import com.way.gesture.util.MyLog;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

public class ATouchFloatView extends ImageView {
	private final Callbacks mCallbacks;
	private WindowManager mWindowManager;
	private LayoutParams mLayoutParams;
	private int baseX;
	private int baseY;
	private int changeX = 0;
	private int changeY = 0;
	private Handler mHandler = new Handler();
	private boolean hasMoved = false;
	public boolean isLongClick = false;

	public ATouchFloatView(Context context, Callbacks callbacks) {
		super(context);
		mCallbacks = callbacks;
		mWindowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);

		int screenWidth = getDisplayWidth();
		int screenHeight = getDisplayHeight();
		baseX = screenWidth - getViewWidth();
		baseY = screenHeight / 2  ;
		mLayoutParams = new LayoutParams();
		mLayoutParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
		mLayoutParams.format = PixelFormat.RGBA_8888;
		mLayoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE
							|FLAG_LAYOUT_IN_SCREEN;
		mLayoutParams.gravity = Gravity.START | Gravity.TOP;
		mLayoutParams.width = mLayoutParams.height =
				//WindowManager.LayoutParams.WRAP_CONTENT;
				getResources().getDimensionPixelOffset(R.dimen.atouch_view_width);
		mLayoutParams.x = screenWidth - getViewWidth();
		mLayoutParams.y = screenHeight / 2;

		setImageResource(R.drawable.btn_assistivetouch);
		setAlpha(0.4f);
		mWindowManager.addView(this, mLayoutParams);

	}
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
			MyLog.i("way", "configuration changed: "
					+ getResources().getConfiguration());
		updateViewFinalPosition();
	}

	private void updateViewFinalPosition() {
		if(baseX > (getDisplayWidth() - getViewWidth())/2){
			baseX = getDisplayWidth() - getViewWidth();
			updatePosition(baseX,baseY);
		}
		if(baseY > getDisplayHeight()) {
			baseY = getDisplayHeight();
			updatePosition(baseX,baseY);
		}
	}

	private Runnable runnable4LongClick = new Runnable() {
		@Override
		public void run() {
			isLongClick = true;
			onLongClick();
		}
	};
	private Runnable runnable4Shrink = new Runnable() {
		@Override
		public void run() {
			shrinking();
		}
	};
	private Runnable runnable4Transparent = new Runnable() {
		@Override
		public void run() {
			transparenting();
		}
	};
	private void onLongClick() {
		mCallbacks.onLongClick();
	}

	private void onClick() {
		mCallbacks.onClick();
	}
	public void moved() {
		hasMoved = true;
		mHandler.removeCallbacks(runnable4LongClick);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int rawX = (int) event.getRawX();
		// 手指按下时记录必要数据,纵坐标的值都需要减去状态栏高度
		int rawY = (int) event.getRawY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mHandler.removeCallbacks(runnable4Transparent);
			mHandler.removeCallbacks(runnable4Shrink);
			setImageResource(R.drawable.btn_assistivetouch_pressed);
			setAlpha(1.0f);
			mCurrentAlpha = MAX_ALPHA;
			this.changeX = rawX;
			this.changeY = rawY;
			mHandler.postDelayed(runnable4LongClick, 1000L);

			break;
		case MotionEvent.ACTION_MOVE:
			int offsetX = rawX - changeX;
			int offsetY = rawY - changeY;

			if ((Math.abs(offsetX) > 3) || (Math.abs(offsetY) > 3)) {
				baseX = offsetX + baseX;
				baseY = offsetY + baseY;
				updatePosition(baseX, baseY);
				changeX = rawX;
				changeY = rawY;
				moved();
			}
			break;
		case MotionEvent.ACTION_UP:
			setImageResource(R.drawable.btn_assistivetouch);
			this.mHandler.removeCallbacks(runnable4LongClick);
			if (!hasMoved) {
				if (!isLongClick) {
					onClick();
				}
			}
			hasMoved = false;
			isLongClick = false;
			shrinking();

			break;
		default:
			break;
		}
		return false;
	}

	private void updatePosition(int x, int y) {
		mLayoutParams.x = x;
		mLayoutParams.y = y;
		mWindowManager.updateViewLayout(this, mLayoutParams);
	}


	public void removeFromWindow() {
		if (mWindowManager != null) {
			mWindowManager.removeView(this);
		}
	}

	private int getViewWidth(){
		return getResources().getDimensionPixelOffset(R.dimen.atouch_view_width);
	}

	/**
	 * 获取手机屏幕高度
	 * 
	 * @return
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
	 * @return
	 */
	public int getDisplayWidth() {
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		mWindowManager.getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}
	private void shrinking() {
		final int width = getDisplayWidth() - getViewWidth();
		int speed = 15;
		if (baseX < width / 2) {
			speed = -speed;
		}
		baseX = speed + baseX;
		updatePosition(baseX, baseY);
		if (baseX >= width) {
			baseX = width;
			updatePosition(baseX, baseY);
			mHandler.removeCallbacks(runnable4Shrink);
			transparenting();
			return;
		}
		if (baseX <= 1) {
			baseX = 0;
			updatePosition(baseX, baseY);
			mHandler.removeCallbacks(runnable4Shrink);
			transparenting();
			return;
		}
		mHandler.postDelayed(runnable4Shrink, 10L);
	}
	private static final int MAX_ALPHA = 255;
	private static final int MIN_ALPHA = 100;
	private int mCurrentAlpha = MAX_ALPHA;
	private void transparenting() {
		if (mCurrentAlpha <= MIN_ALPHA) {
			return;
		}
		mCurrentAlpha = mCurrentAlpha - 1;
		setAlpha(mCurrentAlpha / 255.0f);
		mHandler.postDelayed(runnable4Transparent, 10L);
	}

	public interface Callbacks {
		public void onClick();
		public void onLongClick();
	}
}
