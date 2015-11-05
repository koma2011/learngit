package com.way.gesture.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.way.gesture.GestureCommond;
import com.way.gesture.R;
import com.way.gesture.util.MyLog;
import com.way.gesture.util.PinYin;
import com.way.pinnedheaderlistview.PinnedHeaderListView;
import com.way.ui.swipeback.SwipeBackActivity;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SelectAppActivity extends SwipeBackActivity
        implements OnItemClickListener, OnQueryTextListener, OnTouchListener {

    private static final String TAG = "SelectAppActivity";
    private static final boolean DEBUG = false;

    private static final String SECTION_BEFORE_A = "#";
    private static final String SECTION_AFTER_Z = "#";
    private static final Intent APP_LAUNCHER_CATEGORY_INTENT = new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER);
    private String[] mExcludePackageNames;// 需要过滤的应用
    private final Handler mHandler = new Handler();
    private final ArrayList<AppRow> mSortedRows = new ArrayList<AppRow>();

    private Context mContext;
    private LayoutInflater mInflater;
    private AppAdapter mAdapter;
    private Parcelable mListViewState;

    private PackageManager mPM;
    private PinnedHeaderListView mListView;
    private SupportMenuItem mSearchMenuItem;
    private SearchView mSearchView;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppRow info = mAdapter.getItem(position);
        Intent intent = new Intent();
        intent.setClass(this, CreateGestureActivity.class);
        intent.putExtra("AppName", info.label);
        intent.putExtra("packageName", info.pkg);
        intent.putExtra("activityName", info.cln);
        MyLog.d("packageInfo:", info.label + "  : " + info.cln);
        intent.putExtra("commond", 0);
        setResult(GestureCommond.GestureSucess, intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_app_layout);
        mContext = this;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mExcludePackageNames = getResources().getStringArray(R.array.exclude_package_names);
        mListView = (PinnedHeaderListView) findViewById(android.R.id.list);
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPM = mContext.getPackageManager();
        mListView.setEmptyView(findViewById(R.id.progressBar));
        mListView.setOnItemClickListener(this);

        // PinnedHeaderListView config start
        mAdapter = new AppAdapter(this);
        int pinnedHeaderBackgroundColor = getResources()
                .getColor(getResIdFromAttribute(this, android.R.attr.colorBackground));
        mAdapter.setPinnedHeaderBackgroundColor(pinnedHeaderBackgroundColor);
        int pinnedHeaderTextColor = getResources().getColor(getResIdFromAttribute(this, R.attr.colorAccent));
        mAdapter.setPinnedHeaderTextColor(pinnedHeaderTextColor);
        mListView.setPinnedHeaderView(mInflater.inflate(R.layout.app_item_section, mListView, false));
        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(mAdapter);
        mListView.setOnTouchListener(this);
        mListView.setEnableHeaderTransparencyChanges(true);
        // PinnedHeaderListView config end
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
        // Search view
        getMenuInflater().inflate(R.menu.search, menu);
        // Filter the list the user is looking it via SearchView
        mSearchMenuItem = (SupportMenuItem) menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
        if (mSearchMenuItem == null || mSearchView == null) {
            return false;
        }
        // mSearchMenuItem.setSupportOnActionExpandListener(this);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQueryHint(getString(R.string.searchHint));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAppsList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG)
            MyLog.d(TAG, "Saving listView state");
        mListViewState = mListView.onSaveInstanceState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mListViewState = null; // you're dead to me
    }

    private void loadAppsList() {
        AsyncTask.execute(mCollectAppsRunnable);
    }

    private String getSection(CharSequence label) {
        if (label == null || label.length() == 0)
            return SECTION_BEFORE_A;
        char c = Character.toUpperCase(label.charAt(0));
        String locale = getResources().getConfiguration().locale.toString();
        MyLog.i("way", "locale = " + locale);
        if (TextUtils.equals(locale, Locale.CHINA.toString())) {// 中文
            String pinyin = PinYin.getPinYin(String.valueOf(c));
            c = TextUtils.isEmpty(pinyin) ? c : pinyin.charAt(0);
        }
        if (c < 'A')
            return SECTION_BEFORE_A;
        if (c > 'Z')
            return SECTION_AFTER_Z;
        return Character.toString(c);
    }

    public static class Row {
        public String section;
    }

    public static class AppRow extends Row {
        public String pkg;
        public String cln;
        public int uid;
        public Drawable icon;
        public CharSequence label;
        public boolean first; // first app in section
    }

    private static final Comparator<AppRow> mRowComparator = new Comparator<AppRow>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(AppRow lhs, AppRow rhs) {
            return sCollator.compare(lhs.section, rhs.section);
        }
    };

    public AppRow loadAppRow(PackageManager pm, ResolveInfo app) {
        final AppRow row = new AppRow();
        row.pkg = app.activityInfo.applicationInfo.packageName;
        row.cln = app.activityInfo.name;
        row.uid = app.activityInfo.applicationInfo.uid;
        try {
            row.label = app.loadLabel(pm);
        } catch (Throwable t) {
            MyLog.e(TAG, "Error loading application label for " + row.pkg, t);
            row.label = row.pkg;
        }
        row.section = getSection(row.label);
        row.icon = app.loadIcon(pm);
        return row;
    }

    public static List<ResolveInfo> queryNotificationConfigActivities(PackageManager pm) {
        if (DEBUG)
            MyLog.d(TAG, "APP_NOTIFICATION_PREFS_CATEGORY_INTENT is " + APP_LAUNCHER_CATEGORY_INTENT);
        final List<ResolveInfo> resolveInfos = pm.queryIntentActivities(APP_LAUNCHER_CATEGORY_INTENT, 0 // PackageManager.MATCH_DEFAULT_ONLY
        );
        return resolveInfos;
    }

    private final Runnable mCollectAppsRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mSortedRows) {
                final long start = SystemClock.uptimeMillis();
                if (DEBUG)
                    MyLog.d(TAG, "Collecting apps...");
                mSortedRows.clear();

                final List<ResolveInfo> resolvedConfigActivities = queryNotificationConfigActivities(mPM);
                if (DEBUG)
                    MyLog.d(TAG, "  config activities:");
                // 过滤部分应用
                boolean isExclude = false;
                for (ResolveInfo info : resolvedConfigActivities) {
                    final String pkg = info.activityInfo.applicationInfo.packageName;
                    isExclude = false;
                    int count = mExcludePackageNames.length;
                    for (int i = 0; i < count; i++) {
                        if (pkg.startsWith(mExcludePackageNames[i])) {
                            if (DEBUG)
                                MyLog.d(TAG, "exclude packageName=" + pkg);
                            isExclude = true;
                            break;
                        }
                    }
                    // 如果有需要过滤的应用，结束此次循环
                    if (isExclude)
                        continue;
                    final AppRow row = loadAppRow(mPM, info);
                    mSortedRows.add(row);
                }
                Collections.sort(mSortedRows, mRowComparator);

                mHandler.post(mRefreshAppsListRunnable);
                final long elapsed = SystemClock.uptimeMillis() - start;
                if (DEBUG)
                    MyLog.d(TAG, "Collected " + mSortedRows.size() + " apps in " + elapsed + "ms");
            }
        }
    };

    public static int getResIdFromAttribute(final Activity activity, final int attr) {
        if (attr == 0)
            return 0;
        final TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.resourceId;
    }

    private void refreshDisplayedItems() {
        if (DEBUG)
            MyLog.d(TAG, "Refreshing apps...");
        mAdapter.setData(mSortedRows);
        if (mListViewState != null) {
            if (DEBUG)
                MyLog.d(TAG, "Restoring listView state");
            mListView.onRestoreInstanceState(mListViewState);
            mListViewState = null;
        }
        mListView.setEmptyView(findViewById(android.R.id.empty));
        if (DEBUG)
            MyLog.d(TAG, "Refreshed " + mSortedRows.size() + " displayed items");
    }

    private final Runnable mRefreshAppsListRunnable = new Runnable() {
        @Override
        public void run() {
            refreshDisplayedItems();
        }
    };

    @Override
    public boolean onQueryTextChange(String query) {
        if (mAdapter != null) {
            mAdapter.getFilter().filter(query);
            mAdapter.setPrefix(query);
            return true;
        }
        return false;
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
            inputMethodManager.hideSoftInputFromWindow(mListView.getWindowToken(), 0);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (view == mListView) {
            hideSoftKeyboard();
        }
        return false;
    }

}