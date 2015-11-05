package com.way.gesture.util;

import java.util.ArrayList;

import com.way.gesture.bean.GestureLevel;
import com.way.gesture.bean.GestureObject;

import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GesturePoint;
import android.gesture.GestureStore;
import android.gesture.GestureStroke;
import android.gesture.Prediction;

public class GestureLibraryManager {
	private static GestureLibraryManager mGestureLibraryManager = null;
	private GestureLibrary mGestureLibrary;

	private GestureLibraryManager(Context context) {
		mGestureLibrary = GestureLibraries.fromPrivateFile(context, GestureDataManager.GESTURE_LIBRARYS_FILE);
        /*
        public static final int SEQUENCE_INVARIANT = 1;
	    // when SEQUENCE_SENSITIVE is used, only single stroke gestures are currently allowed
	    public static final int SEQUENCE_SENSITIVE = 2;//默认

	    // ORIENTATION_SENSITIVE and ORIENTATION_INVARIANT are only for SEQUENCE_SENSITIVE gestures
	    public static final int ORIENTATION_INVARIANT = 1;
	    // at most 2 directions can be recognized
	    public static final int ORIENTATION_SENSITIVE = 2;//默认两个方向
	    // at most 4 directions can be recognized
	    static final int ORIENTATION_SENSITIVE_4 = 4;
	    // at most 8 directions can be recognized
	    static final int ORIENTATION_SENSITIVE_8 = 8;
	    */
		mGestureLibrary.setOrientationStyle(4);// 8个方向，识别条件会更苛刻一点
		mGestureLibrary.setSequenceType(GestureStore.SEQUENCE_SENSITIVE);
		mGestureLibrary.load();
	}

	private GestureLevel convertGesture(Gesture gesture) {
		GestureLevel gestureLevel = new GestureLevel();
		final int size = gesture.getStrokesCount();
		ArrayList<GestureStroke> strokes = gesture.getStrokes();

		ArrayList<GesturePoint> gesturePoints = new ArrayList<GesturePoint>();
		for (int i = 0; i < size; i++) {
			float[] points = strokes.get(i).points;
			final int count = points.length;
			for (int j = 0; j < count; j += 2) {
				float x = points[j];
				float y = points[j + 1];
				GesturePoint gesturePoint = new GesturePoint(x, y, j);
				gesturePoints.add(gesturePoint);
			}
		}
		gestureLevel.addStroke(new GestureStroke(gesturePoints));
		return gestureLevel;
	}

	public static GestureLibraryManager defaultManager(Context context) {
		if (mGestureLibraryManager == null)
			mGestureLibraryManager = new GestureLibraryManager(context);
		return mGestureLibraryManager;
	}

	public void removeGesture(GestureObject gestureObject) {
		if (gestureObject != null)
			removeGesture(String.valueOf(gestureObject.gestureId));
	}

	public void removeGesture(String gestureId) {
		Gesture gesture = getGesture(gestureId);
		if (gesture == null)
			return;
		mGestureLibrary.removeGesture(gestureId, gesture);
		mGestureLibrary.save();
	}

	public Gesture getGesture(String gestureId) {
		ArrayList<Gesture> gestures = mGestureLibrary.getGestures(gestureId);
		if (gestures == null || gestures.size() == 0)
			return null;
		return gestures.get(0);
	}

	public String searchGesture(Gesture gesture) {
		GestureLevel gestureLevel = convertGesture(gesture);
		float level = gestureLevel.getLevel();
		MyLog.d("gesture", "gesture length = " + gestureLevel.getLength() + ", level = " + level);
		ArrayList<Prediction> predictions = mGestureLibrary.recognize(gestureLevel);
		if (predictions == null || predictions.isEmpty()) {
			return null;
		}

		Prediction bestPrediction = predictions.get(0);
		MyLog.d("gesture", "searchGesture  first score = " + bestPrediction.score + ", name = " + bestPrediction.name);
		if (bestPrediction.score > level) {
			return bestPrediction.name;
		}
		return null;
	}

	public boolean addOrUpdateGesture(String gestureId, Gesture gesture) {
		mGestureLibrary.addGesture(String.valueOf(gestureId), convertGesture(gesture));
		return mGestureLibrary.save();
	}
}