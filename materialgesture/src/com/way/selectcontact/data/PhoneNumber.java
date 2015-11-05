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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeSet;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.way.selectcontact.util.PinYin;

/**
 * An interface for finding information about phone numbers
 */
public class PhoneNumber implements Comparable<PhoneNumber>, Parcelable {

	private static final String TAG = "Mms/PhoneNumber";

	public static final String[] PROJECTION = new String[] { 
			Phone._ID,//0
			Phone.NUMBER, //1
			Phone.TYPE,//2
			Phone.LABEL,//3
			Phone.DISPLAY_NAME_PRIMARY,//4
			Phone.IS_SUPER_PRIMARY,//5
			Phone.CONTACT_ID, //6
			Phone.LOOKUP_KEY, //7
			Phone.PHOTO_ID //8
			};

	private static final String[] PROJECTION_ALT = new String[] { 
			Phone._ID,//0
			Phone.NUMBER,//1
			Phone.TYPE,//2
			Phone.LABEL,//3
			Phone.DISPLAY_NAME_ALTERNATIVE, //4
			Phone.IS_SUPER_PRIMARY,//5
			Phone.CONTACT_ID,//6
			Phone.LOOKUP_KEY,//7
			Phone.PHOTO_ID//8
			};

	private static final String SELECTION = Phone.NUMBER + " NOT NULL";
	private static final String SELECTION_MOBILE_ONLY = SELECTION + " AND "
			+ Phone.TYPE + "=" + Phone.TYPE_MOBILE;

	private static final int COLUMN_ID = 0;
	private static final int COLUMN_NUMBER = 1;
	private static final int COLUMN_TYPE = 2;
	private static final int COLUMN_LABEL = 3;
	private static final int COLUMN_DISPLAY_NAME = 4;
	private static final int COLUMN_IS_SUPER_PRIMARY = 5;
	private static final int COLUMN_CONTACT_ID = 6;
	private static final int COLUMN_LOOKUP_KEY = 7;
	private static final int CONTACT_PHOTO_ID_COLUMN = 8;

	private long mId;
	private String mNumber;
	private int mType;
	private String mLabel;
	private String mName;
	private boolean mIsDefault;
	private long mContactId;
	private boolean mIsChecked;
	private String mSectionIndex;
	private String mLookupKey;
	private long mPhotoId;

	public PhoneNumber(String stranger, boolean checked) {
		mNumber = stranger;
		mName = stranger;
		mIsChecked = checked;
	}

	public PhoneNumber() {
	}
	
	public PhoneNumber(Cursor c) {
		mId = c.getLong(COLUMN_ID);
		mNumber = c.getString(COLUMN_NUMBER);
		mType = c.getInt(COLUMN_TYPE);
		mLabel = c.getString(COLUMN_LABEL);
		mName = c.getString(COLUMN_DISPLAY_NAME);
		mContactId = c.getLong(COLUMN_CONTACT_ID);
		mIsDefault = c.getInt(COLUMN_IS_SUPER_PRIMARY) != 0;
		mLookupKey = c.getString(COLUMN_LOOKUP_KEY);
		mPhotoId = c.getLong(CONTACT_PHOTO_ID_COLUMN);
	}

	private PhoneNumber(Context context, Cursor c, String sectionIndex) {
		mId = c.getLong(COLUMN_ID);
		mNumber = c.getString(COLUMN_NUMBER);
		mType = c.getInt(COLUMN_TYPE);
		mLabel = c.getString(COLUMN_LABEL);
		mName = c.getString(COLUMN_DISPLAY_NAME);
		mContactId = c.getLong(COLUMN_CONTACT_ID);
		mIsDefault = c.getInt(COLUMN_IS_SUPER_PRIMARY) != 0;
		mLookupKey = c.getString(COLUMN_LOOKUP_KEY);
		mPhotoId = c.getLong(CONTACT_PHOTO_ID_COLUMN);
		if(sectionIndex != null){
			mSectionIndex = sectionIndex;
		}else{
			mSectionIndex = getSection(context, mName);
		}

		if (Log.isLoggable(TAG, Log.VERBOSE)) {
			Log.d(TAG, "Create phone number: recipient=" + mName
					+ ", recipientId=" + mId + ", recipientNumber=" + mNumber);
		}
	}

	private static final String SECTION_UNKNOWN = "#";
	private String getSection(Context context, CharSequence label) {
		if (label == null || label.length() == 0)
			return SECTION_UNKNOWN;
		char c = Character.toUpperCase(label.charAt(0));
		String locale = context.getResources().getConfiguration().locale.toString();
		if (TextUtils.equals(locale, Locale.CHINA.toString())) {// 中文
			String pinyin = PinYin.getPinYin(String.valueOf(c));
			c = TextUtils.isEmpty(pinyin) ? c : pinyin.charAt(0);
		}
		if (c < 'A' || c > 'Z')
			return SECTION_UNKNOWN;
		return Character.toString(c);
	}
	public long getId() {
		return mId;
	}

	public String getNumber() {
		return mNumber;
	}

	public int getType() {
		return mType;
	}

	public String getLabel() {
		return mLabel;
	}

	public String getSectionIndex() {
		return mSectionIndex;
	}

	public String getName() {
		return mName;
	}

	public boolean isDefault() {
		return mIsDefault;
	}

	public long getContactId() {
		return mContactId;
	}

	public String getLookupKey() {
		return mLookupKey;
	}

	public long getPhotoId() {
		return mPhotoId;
	}

	public void setPhotoId(long photoId) {
		this.mPhotoId = photoId;
	}

	/**
	 * Returns true if this phone number is selected for a multi-operation.
	 */
	public boolean isChecked() {
		return mIsChecked;
	}

	public void setChecked(boolean checked) {
		mIsChecked = checked;
	}

	/**
	 * The primary key of a recipient is its number
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PhoneNumber) {
			PhoneNumber other = (PhoneNumber) obj;
			return mContactId == other.mContactId
					&& PhoneNumberUtils.compare(mNumber, other.mNumber);
		} else if (obj instanceof String) {
			return PhoneNumberUtils.compare(mNumber, (String) obj);
		}
		return false;
	}

	@Override
	public int compareTo(PhoneNumber other) {
		int result = mName.compareTo(other.mName);
		if (result != 0) {
			return result;
		}
		if (mIsDefault != other.mIsDefault) {
			return mIsDefault ? -1 : 1;
		}
		result = mNumber.compareTo(other.mNumber);
		if (result != 0) {
			return result;
		}
		if (mContactId != other.mContactId) {
			return mContactId < other.mContactId ? -1 : 1;
		}
		return 0;
	}

	/**
	 * Get all possible recipients (groups and contacts with phone number(s)
	 * only)
	 * 
	 * @param context
	 * @return all possible recipients
	 */
	public static ArrayList<PhoneNumber> getPhoneNumbers(Context context,
			boolean mobileOnly) {
		final ContentResolver resolver = context.getContentResolver();
		final Uri uri = Phone.CONTENT_URI
				.buildUpon()
				.appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
				.build();
		// final SharedPreferences prefs =
		// PreferenceManager.getDefaultSharedPreferences(context);
		// boolean mobileOnly =
		// prefs.getBoolean(SelectRecipientsList.PREF_MOBILE_NUMBERS_ONLY,
		// true);
		final Cursor cursor = resolver.query(uri, PROJECTION,
				mobileOnly ? SELECTION_MOBILE_ONLY : SELECTION, null,
				Phone.SORT_KEY_PRIMARY);

		if (cursor == null) {
			return null;
		}

		if (cursor.getCount() == 0) {
			cursor.close();
			return null;
		}

		Bundle bundle = cursor.getExtras();
		String[] sections = null;
		int[] counts = null;

		if (bundle.containsKey(Contacts.EXTRA_ADDRESS_BOOK_INDEX_TITLES)) {
			sections = bundle
					.getStringArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_TITLES);
			counts = bundle
					.getIntArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS);
		}

		// As we can't sort by super primary state when using the index extra
		// query parameter,
		// we have to group by contact in a first pass and sort by primary state
		// in a second pass
		ArrayList<Long> contactIdOrder = new ArrayList<Long>();
		HashMap<Long, TreeSet<PhoneNumber>> numbers = new HashMap<Long, TreeSet<PhoneNumber>>();
		int section = 0, sectionPosition = 0;
		long lastContactId = -1;

		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			String sectionIndex = null;
			if (sections != null && (section < sections.length)) {
				sectionIndex = sections[section];
				sectionPosition++;
				if (sectionPosition >= counts[section]) {
					section++;
					sectionPosition = 0;
				}
			}

			PhoneNumber number = new PhoneNumber(context, cursor, sectionIndex);
			if (!contactIdOrder.contains(number.mContactId)) {
				contactIdOrder.add(number.mContactId);
			}
			TreeSet<PhoneNumber> numbersByContact = numbers
					.get(number.mContactId);
			if (numbersByContact == null) {
				numbersByContact = new TreeSet<PhoneNumber>();
				numbers.put(number.mContactId, numbersByContact);
			}
			numbersByContact.add(number);
		}
		cursor.close();

		// Construct the final list
		ArrayList<PhoneNumber> phoneNumbers = new ArrayList<PhoneNumber>();

		for (Long contactId : contactIdOrder) {
			TreeSet<PhoneNumber> numbersByContact = numbers.get(contactId);
			// In case there wasn't a primary number, we declare the first item
			// to by default
			numbersByContact.first().mIsDefault = true;
			for (PhoneNumber number : numbersByContact) {
				phoneNumbers.add(number);
			}
		}

		return phoneNumbers;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(mContactId);
		dest.writeString(mName);
		dest.writeString(mNumber);
	}
	public static final Creator<PhoneNumber> CREATOR = new Creator<PhoneNumber>() {

		@Override
		public PhoneNumber createFromParcel(Parcel source) {
			PhoneNumber phoneNumber = new PhoneNumber();
			phoneNumber.mContactId = source.readLong();
			phoneNumber.mName = source.readString();
			phoneNumber.mNumber = source.readString();
			return phoneNumber;
		}

		@Override
		public PhoneNumber[] newArray(int size) {
			return new PhoneNumber[size];
		}

	};
}
