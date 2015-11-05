/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.way.selectcontact.data;

import java.util.ArrayList;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;

public class RecipientsListLoader extends
		AsyncTaskLoader<RecipientsListLoader.Result> {
	private Result mResults;
	private boolean mMobileOnly;

	public static class Result {
		public List<PhoneNumber> phoneNumbers;
	}

	public RecipientsListLoader(Context context, boolean mobileOnly) {
		super(context);
		mMobileOnly = mobileOnly;
	}

	@Override
	public Result loadInBackground() {
		final Context context = getContext();
		ArrayList<PhoneNumber> phoneNumbers = PhoneNumber.getPhoneNumbers(
				context, mMobileOnly);
		if (phoneNumbers == null) {
			return new Result();
		}
		Result result = new Result();
		result.phoneNumbers = phoneNumbers;

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
