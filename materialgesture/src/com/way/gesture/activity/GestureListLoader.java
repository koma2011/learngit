package com.way.gesture.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;

public class GestureListLoader extends AsyncTaskLoader<GestureListLoader.Result> {
	private Result mResults;

	public static class Result {
		public List<GestureMode> gestureModes;
	}

	public GestureListLoader(Context context) {
		super(context);
	}

	@Override
	public Result loadInBackground() {
		ArrayList<GestureMode> gestureModes = GestureMode.getGestureModes(getContext());
		if (gestureModes == null) {
			return new Result();
		}
		Result result = new Result();
		result.gestureModes = gestureModes;
		return result;
	}

	// Called when there is new data to deliver to the client. The
	// super class will take care of delivering it; the implementation
	// here just adds a little more logic.
	@Override
	public void deliverResult(Result result) {
		mResults = result;

		if (isStarted()) {
			// If the Loader is started, immediately deliver its results.
			super.deliverResult(result);
		}
	}

	@Override
	protected void onStartLoading() {
		if (mResults != null) {
			// If we currently have a result available, deliver it immediately.
			deliverResult(mResults);
		}

		if (takeContentChanged() || mResults == null) {
			// If the data has changed since the last time it was loaded
			// or is not currently available, start a load.
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		// Attempt to cancel the current load task if possible.
		cancelLoad();
	}

	@Override
	protected void onReset() {
		super.onReset();

		// Ensure the loader is stopped
		onStopLoading();

		// At this point we can release the resources associated if needed.
		mResults = null;
	}
}
