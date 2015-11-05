package com.way.gesture.activity;

import com.way.fab.FloatingActionButton;
import com.way.gesture.GestureCommond;
import com.way.gesture.GestureService;
import com.way.gesture.R;
import com.way.gesture.activity.GestureListLoader.Result;
import com.way.gesture.bean.GestureObject;
import com.way.gesture.util.GestureDataManager;
import com.way.gesture.util.MyLog;
import com.way.gesture.view.GestureHandler;
import com.way.gesture.view.HelpDialog;
import com.way.pinnedheaderlistview.PinnedHeaderListView;
import com.way.ui.swipeback.SwipeBackActivity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CompoundButton;
import android.widget.Toast;

public class MainActivity extends SwipeBackActivity implements CompoundButton.OnCheckedChangeListener, OnClickListener,
		LoaderManager.LoaderCallbacks<GestureListLoader.Result> {
	private PinnedHeaderListView mListView;
	private FloatingActionButton mFab;
	private GestureModeAdapter mAdapter;
	private HelpDialog mHelpDialog = null;
	private LayoutInflater mInflater;
	public GestureObject mLastDeleteGestureObject;
	private int mPreviousVisibleItem;

	public static int getResIdFromAttribute(final Activity activity, final int attr) {
		if (attr == 0)
			return 0;
		final TypedValue typedValue = new TypedValue();
		activity.getTheme().resolveAttribute(attr, typedValue, true);
		return typedValue.resourceId;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_layout);
		setSwipeBackEnable(false);
		// getSupportActionBar().setElevation(0);
		mInflater = LayoutInflater.from(this);
		mListView = (PinnedHeaderListView) findViewById(android.R.id.list);
		mListView.setEmptyView(findViewById(android.R.id.empty));
		mFab = (FloatingActionButton) findViewById(R.id.fab);
		mFab.setOnClickListener(this);
		mFab.hide(false);
		Animation inAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.show_from_bottom);
		inAnim.setInterpolator(new DecelerateInterpolator());

		Animation outAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.hide_to_bottom);
		outAnim.setInterpolator(new AccelerateInterpolator());
		mFab.setShowAnimation(inAnim);
		mFab.setHideAnimation(outAnim);

		SwitchCompat actionBarSwitch = new SwitchCompat(this);
		actionBarSwitch.setOnCheckedChangeListener(this);
		actionBarSwitch.setChecked(Settings.System.getInt(getContentResolver(), "way_gesture_switch", 1) == 1);
		actionBarSwitch.setOnCheckedChangeListener(this);
		actionBarSwitch.setPadding(0, 0, 25, 0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		getSupportActionBar().setCustomView(actionBarSwitch,
				new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT,
						Gravity.CENTER_VERTICAL | Gravity.END));
		update();
		mAdapter = new GestureModeAdapter(this);
		int pinnedHeaderBackgroundColor = getResources()
				.getColor(getResIdFromAttribute(this, android.R.attr.colorBackground));
		mAdapter.setPinnedHeaderBackgroundColor(pinnedHeaderBackgroundColor);
		int pinnedHeaderTextColor = getResources().getColor(getResIdFromAttribute(this, R.attr.colorAccent));
		mAdapter.setPinnedHeaderTextColor(pinnedHeaderTextColor);
		mListView.setPinnedHeaderView(mInflater.inflate(R.layout.select_recipients_list_section, mListView, false));
		mListView.setAdapter(mAdapter);
		mListView.setOnScrollListener(mAdapter);
		// mListView.setOnTouchListener(this);
		mListView.setEnableHeaderTransparencyChanges(true);
		mAdapter.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if (firstVisibleItem > mPreviousVisibleItem) {
					mFab.hide(true);
				} else if (firstVisibleItem < mPreviousVisibleItem) {
					mFab.show(true);
				}
				mPreviousVisibleItem = firstVisibleItem;
			}
		});
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		GestureHandler.getInstance().removeAll();
	}

	@Override
	public void onResume() {
		super.onResume();
		MyLog.d("GestureAction", "onResume");
		if (mHelpDialog == null) {
			mHelpDialog = new HelpDialog();
			mHelpDialog.show(MainActivity.this, true);
		}
		//if (!getLoaderManager().getLoader(0).isStarted())
			getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.menu_settings).setVisible(false);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_help:
			if (mHelpDialog == null)
				mHelpDialog = new HelpDialog();
			mHelpDialog.show(MainActivity.this, false);
			break;
		case R.id.menu_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.fab:
			Intent intent = new Intent();
			intent.setClass(this, AddTaskActivity.class);
			startActivityForResult(intent, GestureCommond.GestureCreateNew);
			break;
		default:
			break;
		}
	}

	private void update() {
		boolean enable = Settings.System.getInt(getContentResolver(), "way_gesture_switch", 1) == 1;
		mListView.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
		if (enable)
			mFab.show(true);
		else
			mFab.hide(true);
		findViewById(R.id.disable_toast_view).setVisibility(enable ? View.INVISIBLE : View.VISIBLE);
		findViewById(R.id.main_layout).setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		Settings.System.putInt(getContentResolver(), "way_gesture_switch", isChecked ? 1 : 0);
		update();
		if (isChecked)
			startService(new Intent(MainActivity.this, GestureService.class));
		else
			stopService(new Intent(MainActivity.this, GestureService.class));
		MyLog.d("gesture", "onCheckedChanged :" + isChecked);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != GestureCommond.GestureSucess) {
			return;
		}
		// getLoaderManager().restartLoader(0, null, this);
		switch (requestCode) {
		case GestureCommond.GestureCreateNew:// 103
			Toast.makeText(this, R.string.gesture_edit_succeed, Toast.LENGTH_SHORT).show();
			break;
		case GestureCommond.GestureEdit:// 104
			if (mLastDeleteGestureObject != null) {
				MyLog.i("liweiping", "mLastDeleteGestureObject.id = " + mLastDeleteGestureObject.gestureId);
				GestureDataManager.defaultManager(this).delete(mLastDeleteGestureObject);
				mLastDeleteGestureObject = null;
				Toast.makeText(this, R.string.gesture_edit_succeed, Toast.LENGTH_SHORT).show();
			}
			break;
		case GestureCommond.GestureEditJob:// 105
			Toast.makeText(this, R.string.gesture_edit_succeed, Toast.LENGTH_SHORT).show();
			break;
		default:
			break;
		}
	}

	@Override
	public Loader<Result> onCreateLoader(int id, Bundle bundle) {
		return new GestureListLoader(this);
	}

	@Override
	public void onLoadFinished(Loader<Result> loader, Result data) {
		if (data.gestureModes != null)
			mAdapter.setData(data.gestureModes);
		else
			mAdapter.clearData();
	}

	@Override
	public void onLoaderReset(Loader<Result> loader) {
		mAdapter.clearData();
	}

}
