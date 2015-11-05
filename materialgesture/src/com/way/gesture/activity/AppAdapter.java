package com.way.gesture.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.way.gesture.R;
import com.way.gesture.activity.SelectAppActivity.AppRow;
import com.way.pinnedheaderlistview.SearchablePinnedHeaderListViewAdapter;
import com.way.pinnedheaderlistview.StringArrayAlphabetIndexer;
import com.way.selectcontact.util.PrefixHighlighter;

public class AppAdapter extends SearchablePinnedHeaderListViewAdapter<AppRow> {
	private LayoutInflater mInflater;
	private ArrayList<AppRow> mAppRows = new ArrayList<AppRow>();
	/**
	 * Highlights the query
	 */
	private final PrefixHighlighter mHighlighter;
	/**
	 * The prefix that's highlighted
	 */
	private char[] mPrefix;

	@Override
	public CharSequence getSectionTitle(int sectionIndex) {
		return ((StringArrayAlphabetIndexer.AlphaBetSection) getSections()[sectionIndex]).getName();
	}

	public AppAdapter(Context context) {
		mInflater = LayoutInflater.from(context);
		mHighlighter = new PrefixHighlighter(context);
	}

	public void setData(final ArrayList<AppRow> approws) {
		this.mAppRows = approws;
		final String[] generatedContactNames = generateContactNames(approws);
		if (generatedContactNames.length > 0)
			setSectionIndexer(new StringArrayAlphabetIndexer(generatedContactNames, true));
		notifyDataSetChanged();
	}

	private String[] generateContactNames(final List<AppRow> approws) {
		final ArrayList<String> contactNames = new ArrayList<String>();
		if (approws != null)
			for (final AppRow contactEntity : approws)
				contactNames.add(contactEntity.section);
		return contactNames.toArray(new String[contactNames.size()]);
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
	public boolean doFilter(AppRow item, CharSequence constraint) {
		if (TextUtils.isEmpty(constraint))
			return true;
		final String displayName = item.label.toString();
		return !TextUtils.isEmpty(displayName) && displayName.toLowerCase(Locale.getDefault())
				.contains(constraint.toString().toLowerCase(Locale.getDefault()));
	}

	@Override
	public ArrayList<AppRow> getOriginalList() {
		return mAppRows;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.app_item, parent, false);
			holder.headerView = (TextView) convertView.findViewById(R.id.header_text);
			holder.iconView = (ImageView) convertView.findViewById(R.id.icon);
			holder.titleView = (TextView) convertView.findViewById(R.id.title);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		final AppRow appRow = getItem(position);
		holder.iconView.setImageDrawable(appRow.icon);
		// holder.titleView.setText(appRow.label);
		setText(holder.titleView, appRow.label.toString());
		bindSectionHeader(holder.headerView, null, position);
		return convertView;
	}

	private static class ViewHolder {
		ImageView iconView;
		TextView titleView;
		TextView headerView;
	}

}
