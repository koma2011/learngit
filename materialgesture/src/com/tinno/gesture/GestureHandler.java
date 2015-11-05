package com.tinno.gesture;

import android.os.Handler;
import android.os.Message;

import com.way.util.MyLog;

import java.util.ArrayList;

/**
 * Created by way on 15/8/9.
 */
public class GestureHandler extends Handler {
	private static final int STEP_DRAW = 0x001;
	private static final int STEP_TIME = 30;
	private static GestureHandler sGestureHandler = null;
	private boolean mIsStarted = false;
	private ArrayList<GestureOverlayView> mGestureViewLists = new ArrayList<GestureOverlayView>();

	private GestureHandler() {
		sGestureHandler = this;
	}

	public static GestureHandler getInstance() {
		if (sGestureHandler == null)
			new GestureHandler();
		return sGestureHandler;
	}

	public void addGestureView(GestureOverlayView gestureView) {
		mGestureViewLists.add(gestureView);
		if (!mIsStarted) {
			mIsStarted = true;
			sendEmptyMessageDelayed(STEP_DRAW, STEP_TIME);
		}
	}

	@Override
	public void handleMessage(Message message) {
		super.handleMessage(message);
		switch (message.what) {
		case STEP_DRAW:
			//MyLog.i("Gesture", "handleMessage mGestureImageViewLists.size() = " + mGestureViewLists.size());
			if (mGestureViewLists.isEmpty()) {
				mIsStarted = false;
				return;
			}
			if (mGestureViewLists.get(0).stepDraw())
				mGestureViewLists.remove(0);
			sendEmptyMessageDelayed(STEP_DRAW, STEP_TIME);

			break;

		default:
			break;
		}
	}

	public void removeAll() {
		mGestureViewLists.clear();
	}
}
