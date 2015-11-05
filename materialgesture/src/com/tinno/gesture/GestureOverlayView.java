/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tinno.gesture;

import java.util.ArrayList;

import com.way.gesture.R;
import com.way.gesture.bean.GestureObject;
import com.way.gesture.view.GestureToast;
import com.way.util.MyLog;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;

/**
 * A transparent overlay for gesture input that can be placed on top of other
 * widgets or contain other widgets.
 */
public class GestureOverlayView extends CardView {
	public static final int MODE_NORMAL = 0;// 普通模式
	public static final int MODE_EDIT = 1;// 编辑模式
	public static final int MODE_PROCESS = 2;// 处理模式
	public static final int MODE_DRAWING = 3;// 正在画手势模式
	protected int mCurrentMode;
	public static final int ORIENTATION_HORIZONTAL = 0;
	public static final int ORIENTATION_VERTICAL = 1;

	private static final int FADE_ANIMATION_RATE = 16;
	private static final boolean GESTURE_RENDERING_ANTIALIAS = true;
	private static final boolean DITHER_FLAG = true;

	private final Paint mGesturePaint = new Paint();

	private long mFadeDuration = 150;
	private long mFadeOffset = 500;
	private long mFadingStart;
	private boolean mFadingHasStarted;
	private boolean mFadeEnabled = true;

	private int mCurrentColor;
	private int mCertainGestureColor = 0xFFFFFF00;
	private int mUncertainGestureColor = 0x48FFFF00;
	private float mGestureStrokeWidth = 12.0f;
	private int mInvalidateExtraBorder = 10;

	private float mGestureStrokeLengthThreshold = 50.0f;
	private float mGestureStrokeSquarenessTreshold = 0.275f;
	private float mGestureStrokeAngleThreshold = 40.0f;

	private int mOrientation = ORIENTATION_VERTICAL;

	private final Rect mInvalidRect = new Rect();
	private final Path mPath = new Path();
	private boolean mGestureVisible = true;

	private float mX;
	private float mY;

	private float mCurveEndX;
	private float mCurveEndY;

	private float mTotalLength;
	private boolean mIsGesturing = false;
	private boolean mPreviousWasGesturing = false;
	private boolean mInterceptEvents = true;
	private boolean mIsListeningForGestures;
	// private boolean mResetGesture;

	// current gesture
	private Gesture mCurrentGesture;
	private final ArrayList<GesturePoint> mStrokeBuffer = new ArrayList<GesturePoint>(100);

	// fading out effect
	private boolean mIsFadingOut = false;
	private float mFadingAlpha = 1.0f;
	private final AccelerateDecelerateInterpolator mInterpolator = new AccelerateDecelerateInterpolator();

	private final FadeOutRunnable mFadingOut = new FadeOutRunnable();

	public GestureOverlayView(Context context) {
		super(context);
		init();
	}

	public GestureOverlayView(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.gestureOverlayViewStyle);
	}

	public GestureOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GestureOverlayView, defStyleAttr,
				R.style.GestureOverlayView);

		mGestureStrokeWidth = a.getFloat(R.styleable.GestureOverlayView_gestureStrokeWidth, mGestureStrokeWidth);
		mInvalidateExtraBorder = Math.max(1, ((int) mGestureStrokeWidth) - 1);
		mCertainGestureColor = a.getColor(R.styleable.GestureOverlayView_gestureColor, mCertainGestureColor);
		mUncertainGestureColor = a.getColor(R.styleable.GestureOverlayView_uncertainGestureColor,
				mUncertainGestureColor);
		mFadeDuration = a.getInt(R.styleable.GestureOverlayView_fadeDuration, (int) mFadeDuration);
		mFadeOffset = a.getInt(R.styleable.GestureOverlayView_fadeOffset, (int) mFadeOffset);
		mGestureStrokeLengthThreshold = a.getFloat(R.styleable.GestureOverlayView_gestureStrokeLengthThreshold,
				mGestureStrokeLengthThreshold);
		mGestureStrokeAngleThreshold = a.getFloat(R.styleable.GestureOverlayView_gestureStrokeAngleThreshold,
				mGestureStrokeAngleThreshold);
		mGestureStrokeSquarenessTreshold = a.getFloat(R.styleable.GestureOverlayView_gestureStrokeSquarenessThreshold,
				mGestureStrokeSquarenessTreshold);
		mInterceptEvents = a.getBoolean(R.styleable.GestureOverlayView_eventsInterceptionEnabled, mInterceptEvents);
		mFadeEnabled = a.getBoolean(R.styleable.GestureOverlayView_fadeEnabled, mFadeEnabled);
		mOrientation = a.getInt(R.styleable.GestureOverlayView_orientation, mOrientation);

		a.recycle();

		init();
	}

	private void init() {
		setWillNotDraw(false);

		final Paint gesturePaint = mGesturePaint;
		gesturePaint.setAntiAlias(GESTURE_RENDERING_ANTIALIAS);
		gesturePaint.setColor(mCertainGestureColor);
		gesturePaint.setStyle(Paint.Style.STROKE);
		gesturePaint.setStrokeJoin(Paint.Join.ROUND);
		gesturePaint.setStrokeCap(Paint.Cap.ROUND);
		gesturePaint.setStrokeWidth(mGestureStrokeWidth);
		gesturePaint.setDither(DITHER_FLAG);

		mCurrentColor = mCertainGestureColor;
		setPaintAlpha(255);
		modeChangedTo(MODE_NORMAL);
	}

	public ArrayList<GesturePoint> getCurrentStroke() {
		return mStrokeBuffer;
	}

	public int getOrientation() {
		return mOrientation;
	}

	public void setOrientation(int orientation) {
		mOrientation = orientation;
	}

	public void setGestureColor(int color) {
		mCertainGestureColor = color;
	}

	public void setUncertainGestureColor(int color) {
		mUncertainGestureColor = color;
	}

	public int getUncertainGestureColor() {
		return mUncertainGestureColor;
	}

	public int getGestureColor() {
		return mCertainGestureColor;
	}

	public float getGestureStrokeWidth() {
		return mGestureStrokeWidth;
	}

	public void setGestureStrokeWidth(float gestureStrokeWidth) {
		mGestureStrokeWidth = gestureStrokeWidth;
		mInvalidateExtraBorder = Math.max(1, ((int) gestureStrokeWidth) - 1);
		mGesturePaint.setStrokeWidth(gestureStrokeWidth);
	}

	public float getGestureStrokeLengthThreshold() {
		return mGestureStrokeLengthThreshold;
	}

	public void setGestureStrokeLengthThreshold(float gestureStrokeLengthThreshold) {
		mGestureStrokeLengthThreshold = gestureStrokeLengthThreshold;
	}

	public float getGestureStrokeSquarenessTreshold() {
		return mGestureStrokeSquarenessTreshold;
	}

	public void setGestureStrokeSquarenessTreshold(float gestureStrokeSquarenessTreshold) {
		mGestureStrokeSquarenessTreshold = gestureStrokeSquarenessTreshold;
	}

	public float getGestureStrokeAngleThreshold() {
		return mGestureStrokeAngleThreshold;
	}

	public void setGestureStrokeAngleThreshold(float gestureStrokeAngleThreshold) {
		mGestureStrokeAngleThreshold = gestureStrokeAngleThreshold;
	}

	public boolean isEventsInterceptionEnabled() {
		return mInterceptEvents;
	}

	public void setEventsInterceptionEnabled(boolean enabled) {
		mInterceptEvents = enabled;
	}

	public boolean isFadeEnabled() {
		return mFadeEnabled;
	}

	public void setFadeEnabled(boolean fadeEnabled) {
		mFadeEnabled = fadeEnabled;
	}

	public Gesture getGesture() {
		return mCurrentGesture;
	}

	public void setGesture(Gesture gesture) {
		if (mCurrentGesture != null) {
			clear(false);
		}

		setCurrentColor(mCertainGestureColor);
		mCurrentGesture = gesture;

		final Path path = mCurrentGesture.toPath();

		final RectF bounds = new RectF();
		path.computeBounds(bounds, true);

		// TODO: The path should also be scaled to fit inside this view
		mPath.rewind();
		mPath.addPath(path, -bounds.left + (getWidth() - bounds.width()) / 2.0f,
				-bounds.top + (getHeight() - bounds.height()) / 2.0f);

		// mResetGesture = true;

		invalidate();

	}

	public void setGestureObject(GestureObject gestureObject) {
		if (mCurrentGesture != null) {
			clear(false);
		}

		setCurrentColor(mCertainGestureColor);
		mPath.rewind();

		mCurrentGesture = gestureObject.gesture;
		mGestureObject = gestureObject;
		initParams();
		GestureHandler.getInstance().addGestureView(this);
	}

	public GestureObject getGestureObject() {
		return mGestureObject;
	}

	private GestureObject mGestureObject;
	private int mCurrentDrawIndex;
	boolean isSplit = false;
	float mScale = 1.0f;
	float mAddtionX = 10f;
	float mAddtionY = 10f;
	float mOffsetX = 0f;
	float mOffsetY = 0f;

	private void initParams() {
		if (mCurrentGesture == null || mGestureObject == null)
			return;
		mCurrentDrawIndex = 0;// 初始化起始点
		final ArrayList<GesturePoint> points = mGestureObject.allGesturePoints;
		if (points == null || points.isEmpty())
			return;
		float minX = points.get(0).x;
		float maxX = points.get(0).x;
		float minY = points.get(0).y;
		float maxY = points.get(0).y;

		for (GesturePoint point : points) {
			if(point.x == 0f && point.y == 0f)//如果是分割点,则忽略掉
				continue;
			if (point.x < minX)
				minX = point.x;
			if (point.x > maxX)
				maxX = point.x;
			if (point.y < minY)
				minY = point.y;
			if (point.y > maxY)
				maxY = point.y;
		}
        mGestureStrokeWidth = (Math.min(getWidth(), getHeight()) / 25.0f);//笔迹粗细
        float padding = (Math.min(getWidth(), getHeight()) / 20.0f) - (mGestureStrokeWidth / 2.0f);//边距
		float scaleX = 1.0f;
		float scaleY = 1.0f;
		mAddtionX = padding;
		mAddtionY = padding;
		mOffsetX = minX;
		mOffsetY = minY;

		if (maxX != minX) {
			scaleX = (getWidth() - 2 * padding) / Math.abs(maxX - minX);
		}
		if (maxY != minY) {
			scaleY = (getHeight() - 2 * padding) / Math.abs(maxY - minY);
		}
		mScale = Math.min(scaleX, scaleY);//取最小的缩放值
		if (scaleY >= scaleX) {
			float size = (maxY - minY) * mScale;
			mAddtionY += (getHeight() - size) / 2;//Y轴偏移量
		} else {
			float size = (maxX - minX) * mScale;
			mAddtionX += (getWidth() - size) / 2;//X轴偏移量
		}
		Log.i("Gesture", "mScale = " + mScale 
				+ ", scaleX = " + scaleX 
				+ ", scaleY = " + scaleY 
				+ ", mOffsetX = " + mOffsetX 
				+ ", maxX = " + maxX
				+ ", mOffsetY = " + mOffsetY
				+ ", maxY = " + maxY
				+ ", mAddtionX = " + mAddtionX 
				+ ", mAddtionY = " + mAddtionY
				+ ", getHeight() = " + getHeight()
				+ ", getWidth() = " + getWidth()
				+ ", padding = " + padding
				);

	}

	public boolean stepDraw() {
		if (mGestureObject == null)
			return true;
		ArrayList<GesturePoint> pointArrayList = mGestureObject.allGesturePoints;
		if (pointArrayList.size() == 0)
			return true;
		int i = 0;
		while (i < 3 && mCurrentDrawIndex < pointArrayList.size()) {
			GesturePoint point = pointArrayList.get(mCurrentDrawIndex);
			if (point.x == 0 && point.y == 0) {
				isSplit = true;
				mX = 0;
				mY = 0;
			} else {
				int x = (int) (((point.x  - mOffsetX) * mScale) + mAddtionX);
				int y = (int) (((point.y  - mOffsetY) * mScale) + mAddtionY);
				if (isSplit || mCurrentDrawIndex == 0) {
					isSplit = false;
					mPath.moveTo(x, y);
				} else {
					float dx = Math.abs(x - mX);
					float dy = Math.abs(y - mY);
					if (dx >= GestureStroke.TOUCH_TOLERANCE || dy >= GestureStroke.TOUCH_TOLERANCE) {
						mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
					}
				}
				mX = x;
				mY = y;
			}

			mCurrentDrawIndex++;
			i++;
		}
		invalidate();
		if (mCurrentDrawIndex >= pointArrayList.size()) {
			return true;
		}
		return false;
	}

	public Path getGesturePath() {
		return mPath;
	}

	public Path getGesturePath(Path path) {
		path.set(mPath);
		return path;
	}

	public boolean isGestureVisible() {
		return mGestureVisible;
	}

	public void setGestureVisible(boolean visible) {
		mGestureVisible = visible;
	}

	public long getFadeOffset() {
		return mFadeOffset;
	}

	public void setFadeOffset(long fadeOffset) {
		mFadeOffset = fadeOffset;
	}

	public void setOnGestureListener(OnGestureListener listener) {
		mOnGestureListener = listener;
	}

	public void removeOnGestureListener() {
		mOnGestureListener = null;
	}

	public boolean isGesturing() {
		return mIsGesturing;
	}

	private void setCurrentColor(int color) {
		mCurrentColor = color;
		if (mFadingHasStarted) {
			setPaintAlpha((int) (255 * mFadingAlpha));
		} else {
			setPaintAlpha(255);
		}
		invalidate();
	}

	/**
	 * @hide
	 */
	public Paint getGesturePaint() {
		return mGesturePaint;
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		if (mCurrentGesture != null && mGestureVisible) {
			canvas.drawPath(mPath, mGesturePaint);
		}
	}

	private void setPaintAlpha(int alpha) {
		alpha += alpha >> 7;
		final int baseAlpha = mCurrentColor >>> 24;
		final int useAlpha = baseAlpha * alpha >> 8;
		mGesturePaint.setColor((mCurrentColor << 8 >>> 8) | (useAlpha << 24));
	}

	public void clear(boolean animated) {
		clear(animated, false, true);
	}

	private void clear(boolean animated, boolean fireActionPerformed, boolean immediate) {
		setPaintAlpha(255);
		removeCallbacks(mFadingOut);
		// mResetGesture = false;
		// mFadingOut.resetMultipleStrokes = false;

		if (animated && mCurrentGesture != null) {
			mFadingAlpha = 1.0f;
			mIsFadingOut = true;
			mFadingHasStarted = false;
			mFadingStart = AnimationUtils.currentAnimationTimeMillis() + mFadeOffset;

			postDelayed(mFadingOut, mFadeOffset);
		} else {
			mFadingAlpha = 1.0f;
			mIsFadingOut = false;
			mFadingHasStarted = false;

			if (immediate) {
				mCurrentGesture = null;
				mPath.rewind();
				invalidate();
			} else if (fireActionPerformed) {
				postDelayed(mFadingOut, mFadeOffset);
				// } else if (mGestureStrokeType ==
				// GESTURE_STROKE_TYPE_MULTIPLE) {
				// mFadingOut.resetMultipleStrokes = true;
				// postDelayed(mFadingOut, mFadeOffset);
			} else {
				mCurrentGesture = null;
				mPath.rewind();
				invalidate();
			}
		}
	}

	public void cancelClearAnimation() {
		setPaintAlpha(255);
		mIsFadingOut = false;
		mFadingHasStarted = false;
		removeCallbacks(mFadingOut);
		mPath.rewind();
		mCurrentGesture = null;
	}

	public void cancelGesture() {
		mIsListeningForGestures = false;

		// add the stroke to the current gesture
		mCurrentGesture.addStroke(new GestureStroke(mStrokeBuffer));

		// pass the event to handlers
		final long now = SystemClock.uptimeMillis();
		final MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);

		event.recycle();

		clear(false);
		mIsGesturing = false;
		mPreviousWasGesturing = false;
		mStrokeBuffer.clear();

	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		cancelClearAnimation();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (isEnabled()) {
			Log.i("Gesture", "mCurrentMode = " + mCurrentMode);
			if (mCurrentMode == MODE_EDIT || mCurrentMode == MODE_PROCESS)
				return true;
			final boolean cancelDispatch = (mIsGesturing
					|| (mCurrentGesture != null && mCurrentGesture.getStrokesCount() > 0 && mPreviousWasGesturing))
					&& mInterceptEvents;

			processEvent(event);

			if (cancelDispatch) {
				event.setAction(MotionEvent.ACTION_CANCEL);
			}

			super.dispatchTouchEvent(event);

			return true;
		}

		return super.dispatchTouchEvent(event);
	}

	private boolean processEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			touchDown(event);
			invalidate();
			return true;
		case MotionEvent.ACTION_MOVE:
			if (mIsListeningForGestures) {
				Rect rect = touchMove(event);
				if (rect != null) {
					invalidate(rect);
				}
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mIsListeningForGestures) {
				touchUp(event, false);
				invalidate();
				return true;
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			if (mIsListeningForGestures) {
				touchUp(event, true);
				invalidate();
				return true;
			}
		}

		return false;
	}

	private void touchDown(MotionEvent event) {
		mIsListeningForGestures = true;

		float x = event.getX();
		float y = event.getY();

		mX = x;
		mY = y;

		mTotalLength = 0;
		mIsGesturing = false;

		// if (mResetGesture) {
		// if (mHandleGestureActions)
		// setCurrentColor(mUncertainGestureColor);
		// mResetGesture = false;
		// mCurrentGesture = null;
		// mPath.rewind();
		// } else
		if (mCurrentGesture == null || mCurrentGesture.getStrokesCount() == 0) {
			// if (mHandleGestureActions)
			setCurrentColor(mUncertainGestureColor);
		}

		// if there is fading out going on, stop it.
		if (mFadingHasStarted) {
			cancelClearAnimation();
		} else if (mIsFadingOut) {
			setPaintAlpha(255);
			mIsFadingOut = false;
			mFadingHasStarted = false;
			removeCallbacks(mFadingOut);
		}

		if (mCurrentGesture == null) {
			mCurrentGesture = new Gesture();
		}

		//if (mCurrentMode == MODE_DRAWING) {// 支持多笔，
			removeCallbacks(mDelayProcess);
		//}
		mStrokeBuffer.add(new GesturePoint(x, y, event.getEventTime()));
		mPath.moveTo(x, y);

		final int border = mInvalidateExtraBorder;
		mInvalidRect.set((int) x - border, (int) y - border, (int) x + border, (int) y + border);

		mCurveEndX = x;
		mCurveEndY = y;

	}

	private Rect touchMove(MotionEvent event) {
		Rect areaToRefresh = null;

		final float x = event.getX();
		final float y = event.getY();

		final float previousX = mX;
		final float previousY = mY;

		final float dx = Math.abs(x - previousX);
		final float dy = Math.abs(y - previousY);

		if (dx >= GestureStroke.TOUCH_TOLERANCE || dy >= GestureStroke.TOUCH_TOLERANCE) {
			areaToRefresh = mInvalidRect;

			// start with the curve end
			final int border = mInvalidateExtraBorder;
			areaToRefresh.set((int) mCurveEndX - border, (int) mCurveEndY - border, (int) mCurveEndX + border,
					(int) mCurveEndY + border);

			float cX = mCurveEndX = (x + previousX) / 2;
			float cY = mCurveEndY = (y + previousY) / 2;

			mPath.quadTo(previousX, previousY, cX, cY);

			// union with the control point of the new curve
			areaToRefresh.union((int) previousX - border, (int) previousY - border, (int) previousX + border,
					(int) previousY + border);

			// union with the end point of the new curve
			areaToRefresh.union((int) cX - border, (int) cY - border, (int) cX + border, (int) cY + border);

			mX = x;
			mY = y;

			mStrokeBuffer.add(new GesturePoint(x, y, event.getEventTime()));

			if (!mIsGesturing) {
				mTotalLength += (float) Math.sqrt(dx * dx + dy * dy);

				if (mTotalLength > mGestureStrokeLengthThreshold) {
					final OrientedBoundingBox box = GestureUtils.computeOrientedBoundingBox(mStrokeBuffer);

					float angle = Math.abs(box.orientation);
					if (angle > 90) {
						angle = 180 - angle;
					}

					if (box.squareness > mGestureStrokeSquarenessTreshold || (mOrientation == ORIENTATION_VERTICAL
							? angle < mGestureStrokeAngleThreshold : angle > mGestureStrokeAngleThreshold)) {
						if (mCurrentMode == MODE_NORMAL) {
							modeChangedTo(MODE_DRAWING);
						}
						mIsGesturing = true;
						setCurrentColor(mCertainGestureColor);

					}
				}
			}

		}

		return areaToRefresh;
	}

	private void touchUp(MotionEvent event, boolean cancel) {
		mIsListeningForGestures = false;

		// A gesture wasn't started or was cancelled
		if (mCurrentGesture != null) {
			// add the stroke to the current gesture
			mCurrentGesture.addStroke(new GestureStroke(mStrokeBuffer));

			if (!cancel) {
				removeCallbacks(mDelayProcess);
				postDelayed(mDelayProcess, mFadeOffset);
				// clear(mFadeEnabled, mIsGesturing, false);
			} else {
				cancelGesture(event);

			}
		} else {
			cancelGesture(event);
		}

		mStrokeBuffer.clear();
		mPreviousWasGesturing = mIsGesturing;
		mIsGesturing = false;

	}

	public void modeChangedTo(int mode) {
		if (mCurrentMode != mode)
			mCurrentMode = mode;
	}

	private Runnable mDelayProcess = new Runnable() {
		@Override
		public void run() {
			if (mCurrentMode == MODE_NORMAL) {
				GestureToast.showToast(getContext(), getResources().getString(R.string.gestureMessage3));
				clear(false);
				return;
			}
			MyLog.i("GestureOverlayView", "mDelayProcess mCurrentMode = " + mCurrentMode);
			modeChangedTo(MODE_PROCESS);
			processGesture();

		}
	};

	private void processGesture() {
		if (mOnGestureListener != null)
			mOnGestureListener.processGesture(this, mCurrentGesture);
	}

	private void cancelGesture(MotionEvent event) {

		clear(false);
	}

	private class FadeOutRunnable implements Runnable {
		// boolean resetMultipleStrokes;

		public void run() {
			if (mIsFadingOut) {
				final long now = AnimationUtils.currentAnimationTimeMillis();
				final long duration = now - mFadingStart;

				if (duration > mFadeDuration) {

					mPreviousWasGesturing = false;
					mIsFadingOut = false;
					mFadingHasStarted = false;
					mPath.rewind();
					mCurrentGesture = null;
					setPaintAlpha(255);
				} else {
					mFadingHasStarted = true;
					float interpolatedTime = Math.max(0.0f, Math.min(1.0f, duration / (float) mFadeDuration));
					mFadingAlpha = 1.0f - mInterpolator.getInterpolation(interpolatedTime);
					setPaintAlpha((int) (255 * mFadingAlpha));
					postDelayed(this, FADE_ANIMATION_RATE);
				}
				// } else if (resetMultipleStrokes) {
				// mResetGesture = true;
			} else {

				mFadingHasStarted = false;
				mPath.rewind();
				mCurrentGesture = null;
				mPreviousWasGesturing = false;
				setPaintAlpha(255);
			}

			invalidate();
		}
	}

	private OnGestureListener mOnGestureListener;

	public static interface OnGestureListener {
		void processGesture(GestureOverlayView overlay, Gesture gesture);
	}

}
