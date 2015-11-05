package com.way.gesture.activity;

import android.os.Bundle;

import com.way.gesture.R;
import com.way.ui.swipeback.SwipeBackActivity;

public class SettingsActivity extends SwipeBackActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings_layout);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().replace(R.id.settings_fragment, new SettingsFragment()).commit();
		}
	}
}
