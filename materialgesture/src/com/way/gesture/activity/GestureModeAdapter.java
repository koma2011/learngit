package com.way.gesture.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.way.gesture.GestureCommond;
import com.way.gesture.R;
import com.way.gesture.bean.GestureObject;
import com.way.gesture.util.AppUtil;
import com.way.gesture.util.GestureDataManager;
import com.way.gesture.view.GestureOverlayView;
import com.way.pinnedheaderlistview.SearchablePinnedHeaderListViewAdapter;
import com.way.pinnedheaderlistview.StringArrayAlphabetIndexer;
import com.way.selectcontact.util.PrefixHighlighter;
import com.way.widget.SlidingLeftViewGroup;

import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.TextView;

public class GestureModeAdapter extends SearchablePinnedHeaderListViewAdapter<GestureMode>
		implements OnClickListener, SlidingLeftViewGroup.OnSlideListener, AbsListView.RecyclerListener {
	private MainActivity mContext;
	private LayoutInflater mInflater;
	private SlidingLeftViewGroup mSlidingItem;
	private ArrayList<GestureMode> mGestureModes = new ArrayList<GestureMode>();
	/**
	 * Highlights the query
	 */
	private final PrefixHighlighter mHighlighter;
	/**
	 * The prefix that's highlighted
	 */
	private char[] mPrefix;

	public GestureModeAdapter(MainActivity context) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mHighlighter = new PrefixHighlighter(context);
	}

	public void clearData() {
		mGestureModes.clear();
		notifyDataSetChanged();
	}

	public void setData(final List<GestureMode> gestureModes) {
		mGestureModes.clear();
		mGestureModes.addAll(gestureModes);
		final String[] sections = generateSections(gestureModes);
		if (sections.length > 0)
			setSectionIndexer(new StringArrayAlphabetIndexer(sections, true));
		notifyDataSetChanged();
	}

	private String[] generateSections(final List<GestureMode> gestureModes) {
		final ArrayList<String> sections = new ArrayList<String>();
		if (gestureModes != null)
			for (final GestureMode gestureMode : gestureModes)
				sections.add(gestureMode.section);
		return sections.toArray(new String[sections.size()]);
	}

	/**
	 * @param prefix
	 *            The query to filter.
	 */
	public void setPrefix(final CharSequence prefix) {
		if (!TextUtils.isEmpty(prefix)) {
			mPrefix = prefix.toString().toUpperCase(Locale.getDefault()).toCharArray();
		} else {
			mPrefix = null;
			notifyDataSetChanged();
		}
	}

	/**
	 * Sets the text onto the textview with highlighting if a prefix is defined
	 * 
	 * @param textView
	 * @param text
	 */
	public void setText(final TextView textView, final String text) {
		if (mPrefix == null) {
			textView.setText(text);
		} else {
			mHighlighter.setText(textView, text, mPrefix);
		}
	}

	@Override
	public boolean doFilter(GestureMode item, CharSequence constraint) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ArrayList<GestureMode> getOriginalList() {
		return mGestureModes;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.gesture_mode_item, parent, false);
			holder = new ViewHolder();
			holder.headerView = (TextView) convertView.findViewById(R.id.section_header);
			holder.mListItem = (SlidingLeftViewGroup) convertView.findViewById(R.id.mymultiViewGroup);
			holder.mLL = convertView.findViewById(R.id.item_ll);
			holder.mName = (TextView) convertView.findViewById(R.id.textview);
			holder.mGestureImageView = (GestureOverlayView) convertView.findViewById(R.id.gestureImageView);
			holder.mDel = (Button) convertView.findViewById(R.id.gestures_delete);
			holder.mEditName = (Button) convertView.findViewById(R.id.gestures_edit_name);
			holder.mEditGesture = (Button) convertView.findViewById(R.id.gestures_edit_gesture);

			holder.mListItem.setSlidingListener(this);
			holder.mLL.setOnClickListener(this);
			holder.mDel.setOnClickListener(this);
			holder.mEditName.setOnClickListener(this);
			holder.mEditGesture.setOnClickListener(this);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		final GestureMode gestureMode = getItem(position);
		holder.mGestureImageView.setEnabled(false);
		holder.mGestureImageView.clear(false);
		holder.mGestureImageView.setCardBackgroundColor(AppUtil.pickColor(gestureMode));
		final GestureOverlayView gestureImageView = holder.mGestureImageView;
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				gestureImageView.setGestureObject(gestureMode);
			}
		}, 500);
		holder.mName.setText(gestureMode.realName);
		holder.mLL.setTag(gestureMode);
		holder.mDel.setTag(gestureMode);
		holder.mEditName.setTag(gestureMode);
		holder.mEditGesture.setTag(gestureMode);
		bindSectionHeader(holder.headerView, null, position);
		return convertView;
	}

	@Override
	public void onClick(View v) {
		if (mSlidingItem != null) {
			mSlidingItem.MoveBack(true);
		}
		final GestureMode gestureMode = (GestureMode) v.getTag();
		if (gestureMode == null)
			return;
		switch (v.getId()) {
		case R.id.item_ll:
			Intent intent = new Intent();
			intent.setClass(mContext, CreateGestureActivity.class);
			intent.putExtra("recorderID", gestureMode.gestureId);
			intent.putExtra("mode", "Edit");
			mContext.startActivityForResult(intent, GestureCommond.GestureEdit);
			break;
		case R.id.gestures_delete:
			onDeleteMenuItem(gestureMode);
			break;
		case R.id.gestures_edit_name:
			startEditName(gestureMode);
			break;
		case R.id.gestures_edit_gesture:
			startEditGesture(gestureMode);
			break;
		default:
			break;
		}
	}

	private void startEditGesture(GestureObject gestureObject) {
		mContext.mLastDeleteGestureObject = gestureObject;
		Intent intent = new Intent();
		intent.setClass(mContext, CreateGestureActivity.class);
		intent.putExtra("AppName", gestureObject.appName);
		intent.putExtra("mode", "");
		intent.putExtra("packageName", gestureObject.packageName);
		intent.putExtra("userName", gestureObject.userName);
		intent.putExtra("phoneNumber", gestureObject.phoneNumber);
		intent.putExtra("commond", gestureObject.gestureType);
		intent.putExtra("activityName", gestureObject.className);
		intent.putExtra("recorderID", gestureObject.gestureId);
		mContext.startActivityForResult(intent, GestureCommond.GestureEdit);
	}

	private void startEditName(GestureObject gestureObject) {
		Intent intent1 = new Intent();
		intent1.setClass(mContext, AddTaskActivity.class);
		intent1.putExtra("AppName", gestureObject.appName);
		intent1.putExtra("mode", "Edit");
		intent1.putExtra("packageName", gestureObject.packageName);
		intent1.putExtra("userName", gestureObject.userName);
		intent1.putExtra("phoneNumber", gestureObject.phoneNumber);
		intent1.putExtra("commond", gestureObject.gestureType);
		intent1.putExtra("activityName", gestureObject.className);
		intent1.putExtra("recorderID", gestureObject.gestureId);
		mContext.startActivityForResult(intent1, GestureCommond.GestureEditJob);
	}

	private void onDeleteMenuItem(final GestureObject gestureObject) {
		GestureDataManager.defaultManager(mContext).delete(gestureObject);
		mContext.getLoaderManager().restartLoader(0, null, mContext);
		mGestureModes.remove(gestureObject);
		setData(mGestureModes);
		notifyDataSetChanged();
	}

	@Override
	public void onSlideToLeft(SlidingLeftViewGroup item) {
		mSlidingItem = item;
	}

	@Override
	public void onSlideBack() {
		mSlidingItem = null;
	}

	@Override
	public void onSlidingStart(SlidingLeftViewGroup item) {
		if (mSlidingItem != null && item != null && mSlidingItem != item) {
			mSlidingItem.MoveBack(false);
		}
	}

	@Override
	public void onMovedToScrapHeap(View view) {
		// TODO Auto-generated method stub

	}

	private static class ViewHolder {
		private TextView headerView;
		private SlidingLeftViewGroup mListItem;
		private View mLL;
		private TextView mName;
		private GestureOverlayView mGestureImageView;
		private Button mDel;
		private Button mEditName;
		private Button mEditGesture;
	}

}
