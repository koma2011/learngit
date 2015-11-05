package com.way.selectcontact;

import java.util.ArrayList;
import java.util.HashSet;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.way.gesture.R;
import com.way.pinnedheaderlistview.PinnedHeaderListView;
import com.way.selectcontact.data.PhoneNumber;
import com.way.selectcontact.data.RecipientsListLoader;
import com.way.selectcontact.data.RecipientsListLoader.Result;
import com.way.ui.swipeback.SwipeBackActivity;

public class RecipientsListActivity extends SwipeBackActivity implements
		LoaderManager.LoaderCallbacks<Result>,
		OnItemClickListener, OnQueryTextListener, OnTouchListener {
	private static final int MENU_DONE = 0;
	public static final String EXTRA_RECIPIENTS = "recipients";
	public static final String EXTRA_MOBILE_NUMBERS_ONLY = "mobile_numbers_only";
	public static final String EXTRA_SELECT_MODE = "select_mode";
	public static final int MODE_SINGLE = 0;
	public static final int MODE_MULTI = 1;
	private int mMode;
	private int mLastPosition;
	private boolean mMobileOnly;
	private PinnedHeaderListView mListView;
	private ContactAdapter mAdapter;
	private HashSet<PhoneNumber> mCheckedPhoneNumbers;
	private LayoutInflater mInflater;
	private SupportMenuItem mSearchMenuItem;
	private SearchView mSearchView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_recipients_list);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mMode = getIntent().getIntExtra(EXTRA_SELECT_MODE, MODE_SINGLE);// 选择模式，单选还是多选
		mMobileOnly = getIntent().getBooleanExtra(EXTRA_MOBILE_NUMBERS_ONLY,
				false);// 是否只显示手机号码
		if (mMode == MODE_SINGLE)
			mLastPosition = -1;
		mInflater = LayoutInflater.from(this);
		mListView = (PinnedHeaderListView) findViewById(android.R.id.list);
		mListView.setEmptyView(findViewById(android.R.id.empty));
		mListView.setOnItemClickListener(this);
		mAdapter = new ContactAdapter(this);
		int pinnedHeaderBackgroundColor = getResources().getColor(
				getResIdFromAttribute(this, android.R.attr.colorBackground));
		mAdapter.setPinnedHeaderBackgroundColor(pinnedHeaderBackgroundColor);
		int pinnedHeaderTextColor = getResources().getColor(
				getResIdFromAttribute(this, R.attr.colorAccent));
		mAdapter.setPinnedHeaderTextColor(pinnedHeaderTextColor);
		mListView.setPinnedHeaderView(mInflater.inflate(
				R.layout.select_recipients_list_section, mListView, false));
		mListView.setAdapter(mAdapter);
		mListView.setOnScrollListener(mAdapter);
		mListView.setOnTouchListener(this);
		mListView.setEnableHeaderTransparencyChanges(true);
		updateEmptyText();
		// Get things ready
		mCheckedPhoneNumbers = new HashSet<PhoneNumber>();
		getLoaderManager().initLoader(0, null, this);
	}

	public static int getResIdFromAttribute(final Activity activity,
			final int attr) {
		if (attr == 0)
			return 0;
		final TypedValue typedValue = new TypedValue();
		activity.getTheme().resolveAttribute(attr, typedValue, true);
		return typedValue.resourceId;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mAdapter != null)
			unbindListItems();
	}

	@Override
	public void onBackPressed() {
		if (mSearchMenuItem.isActionViewExpanded()) {
			mSearchMenuItem.collapseActionView();
			return;
		}
		super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_DONE, 0, R.string.menu_done)
				.setIcon(R.drawable.ic_menu_done)
				.setShowAsActionFlags(
						MenuItem.SHOW_AS_ACTION_ALWAYS
								| MenuItem.SHOW_AS_ACTION_WITH_TEXT)
				.setVisible(false);
		// Search view
		getMenuInflater().inflate(R.menu.contact_search, menu);
		// Filter the list the user is looking it via SearchView
		mSearchMenuItem = (SupportMenuItem) menu.findItem(R.id.menu_search);
		mSearchView = (SearchView) MenuItemCompat
				.getActionView(mSearchMenuItem);
		if (mSearchMenuItem == null || mSearchView == null) {
			return false;
		}
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setQueryHint(getString(R.string.search_contact_hint));
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean hasSelection = mCheckedPhoneNumbers.size() > 0;
		menu.findItem(MENU_DONE).setVisible(hasSelection);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_DONE:
			Intent intent = new Intent();
			putExtraWithContact(intent);
			setResult(RESULT_OK, intent);
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void putExtraWithContact(Intent intent) {
		if (mMode == MODE_SINGLE) {
			for (PhoneNumber phoneNumber : mCheckedPhoneNumbers) {
				if (phoneNumber.isChecked()) {
					intent.putExtra(EXTRA_RECIPIENTS, phoneNumber);
				}
			}
		} else {
			ArrayList<String> numbers = new ArrayList<String>();
			for (PhoneNumber phoneNumber : mCheckedPhoneNumbers) {
				if (phoneNumber.isChecked()) {
					numbers.add(phoneNumber.getNumber());
				}
			}
			intent.putExtra(EXTRA_RECIPIENTS, numbers);
		}
	}

	private void unbindListItems() {
		final int count = mListView.getChildCount();
		for (int i = 0; i < count; i++) {
			mAdapter.unbindView(mListView.getChildAt(i));
		}
	}

	private void updateEmptyText() {
		TextView emptyView = (TextView) mListView.getEmptyView();
		emptyView.setText(getString(R.string.no_recipients));
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		PhoneNumber number = mAdapter.getItem(position);
		checkPhoneNumber(number, !number.isChecked());
		if (mMode == MODE_SINGLE) {
			if (mLastPosition >= 0 && mLastPosition < mAdapter.getCount())
				checkPhoneNumber(mAdapter.getItem(mLastPosition), false);
			if (mCheckedPhoneNumbers.size() == 0)
				mLastPosition = -1;
			else
				mLastPosition = position;
		}
		mAdapter.notifyDataSetChanged();
		invalidateOptionsMenu();
	}

	private void checkPhoneNumber(PhoneNumber phoneNumber, boolean check) {
		phoneNumber.setChecked(check);
		if (check) {
			mCheckedPhoneNumbers.add(phoneNumber);
		} else {
			mCheckedPhoneNumbers.remove(phoneNumber);
		}
	}

	@Override
	public Loader<Result> onCreateLoader(int id, Bundle args) {
		return new RecipientsListLoader(this, mMobileOnly);
	}

	@Override
	public void onLoadFinished(Loader<Result> loader, Result data) {
		if (data.phoneNumbers != null) {
			mAdapter.setData(data.phoneNumbers);
		}
	}

	@Override
	public void onLoaderReset(Loader<Result> loader) {
		mAdapter.clearData();
	}

	@Override
	public boolean onQueryTextChange(String query) {
		mAdapter.getFilter().filter(query);
		mAdapter.setPrefix(query);
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		if (TextUtils.isEmpty(query)) {
			return false;
		}
		if (mSearchView != null) {
			hideSoftKeyboard();
			mSearchView.clearFocus();
		}
		return true;
	}

	private void hideSoftKeyboard() {
		// Hide soft keyboard, if visible
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null)
			inputMethodManager.hideSoftInputFromWindow(
					mListView.getWindowToken(), 0);
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		if (view == mListView) {
			hideSoftKeyboard();
		}
		return false;
	}
}
