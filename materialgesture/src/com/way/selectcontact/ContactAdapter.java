package com.way.selectcontact;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;

import com.way.gesture.R;
import com.way.pinnedheaderlistview.SearchablePinnedHeaderListViewAdapter;
import com.way.pinnedheaderlistview.StringArrayAlphabetIndexer;
import com.way.selectcontact.data.PhoneNumber;
import com.way.selectcontact.ui.SelectRecipientsListItem;
import com.way.selectcontact.util.PrefixHighlighter;

public class ContactAdapter extends
		SearchablePinnedHeaderListViewAdapter<PhoneNumber> implements
		AbsListView.RecyclerListener {
	private LayoutInflater mInflater;
	private ArrayList<PhoneNumber> mAppRows = new ArrayList<PhoneNumber>();
    /**
     * Highlights the query
     */
    private final PrefixHighlighter mHighlighter;
    /**
     * The prefix that's highlighted
     */
    private char[] mPrefix;

	public ContactAdapter(Context context) {
		mInflater = LayoutInflater.from(context);
		mHighlighter = new PrefixHighlighter(context);
	}
	public void clearData(){
		mAppRows.clear();
		notifyDataSetChanged();
	}
	public void setData(final List<PhoneNumber> approws) {
		mAppRows.clear();
		mAppRows.addAll(approws);
		final String[] generatedContactNames = generateContactNames(approws);
		if (generatedContactNames.length > 0)
			setSectionIndexer(new StringArrayAlphabetIndexer(
					generatedContactNames, true));
		notifyDataSetChanged();
	}

	private String[] generateContactNames(final List<PhoneNumber> approws) {
		final ArrayList<String> contactNames = new ArrayList<String>();
		if (approws != null)
			for (final PhoneNumber contactEntity : approws)
				contactNames.add(contactEntity.getSectionIndex());
		return contactNames.toArray(new String[contactNames.size()]);
	}
    /**
     * @param prefix The query to filter.
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
	public CharSequence getSectionTitle(int sectionIndex) {
		return ((StringArrayAlphabetIndexer.AlphaBetSection) getSections()[sectionIndex])
				.getName();
	}

	@Override
	public boolean doFilter(PhoneNumber item, CharSequence constraint) {
		if (TextUtils.isEmpty(constraint))
			return true;
		final String displayName = item.getName();
		final String phoneNumber = item.getNumber();
		return (!TextUtils.isEmpty(displayName) && displayName.toLowerCase(
				Locale.getDefault()).contains(
				constraint.toString().toLowerCase(Locale.getDefault())))
				|| (!TextUtils.isEmpty(phoneNumber) && phoneNumber.toLowerCase(
						Locale.getDefault()).contains(
						constraint.toString().toLowerCase(Locale.getDefault())));
	}

	@Override
	public ArrayList<PhoneNumber> getOriginalList() {
		return mAppRows;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SelectRecipientsListItem view;

		if (convertView == null) {
			view = (SelectRecipientsListItem) mInflater.inflate(
					R.layout.select_recipients_list_item, parent, false);
		} else {
			view = (SelectRecipientsListItem) convertView;
		}

		bindView(position, view);
		return view;
	}

	private void bindView(int position, SelectRecipientsListItem view) {
		final PhoneNumber phoneNumber = getItem(position);
		PhoneNumber lastNumber = position != 0 ? getItem(position - 1) : null;

		long contactId = phoneNumber.getContactId();
		long lastContactId = lastNumber != null ? lastNumber.getContactId()
				: -1;
		boolean isFirst = contactId != lastContactId;

		view.bind(this, phoneNumber, position, isFirst);
		bindSectionHeader(view.getSectionHeader(), null, position);
	}

	@Override
	public void onMovedToScrapHeap(View view) {
		unbindView(view);
	}

	public void unbindView(View view) {
		if (view instanceof SelectRecipientsListItem) {
			SelectRecipientsListItem srli = (SelectRecipientsListItem) view;
			srli.unbind();
		}
	}
}
