package com.way.gesture.activity;

import com.way.gesture.GestureService;
import com.way.gesture.R;
import com.way.materialpreference.SwitchPreference;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.TextUtils;

public class SettingsFragment extends PreferenceFragment {
	private static final int MSG_UPDATE_VERSION = 0x001;
	public static final String SWIPE_KEY = "swipe_from_top_left";
	public static final String ATOUCH_KEY = "use_atouch";
	public static final String SERVICE_FOREGROUND_KEY = "service_foreground";
	private static final String VERSION_KEY = "version";
	private Activity mContext;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		mContext = getActivity();
		PackageManager packageManager = mContext.getPackageManager();
		String packageName = mContext.getPackageName();
		// Update the version number
		try {
			final PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
			findPreference(VERSION_KEY).setSummary(packageInfo.versionName);
		} catch (final NameNotFoundException e) {
			findPreference(VERSION_KEY).setSummary("?");
		}
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		String key = preference.getKey();
		if (TextUtils.equals(key, SWIPE_KEY) || TextUtils.equals(key, ATOUCH_KEY)
				|| TextUtils.equals(key, SERVICE_FOREGROUND_KEY)) {
			mContext.startService(new Intent(mContext, GestureService.class));
		} else if (TextUtils.equals(key, VERSION_KEY)) {
			mHandler.removeMessages(MSG_UPDATE_VERSION);
			mHandler.sendEmptyMessageDelayed(MSG_UPDATE_VERSION, 300);
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mHandler.removeMessages(MSG_UPDATE_VERSION);
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_VERSION:
				 checkNewVersion();
				break;

			default:
				break;
			}
		}
	};

	private void checkNewVersion() {
	}

}
