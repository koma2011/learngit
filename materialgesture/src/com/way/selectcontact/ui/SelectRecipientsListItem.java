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

package com.way.selectcontact.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.way.gesture.R;
import com.way.selectcontact.ContactAdapter;
import com.way.selectcontact.data.ContactPhotoManager;
import com.way.selectcontact.data.ContactPhotoManager.DefaultImageRequest;
import com.way.selectcontact.data.PhoneNumber;
import com.way.selectcontact.widget.CheckableQuickContactBadge;

public class SelectRecipientsListItem extends LinearLayout {

	private TextView mSectionHeader;
	private TextView mNameView;
	private TextView mNumberView;
	private TextView mLabelView;
	private CheckableQuickContactBadge mAvatarView;
	private static ContactPhotoManager sContactPhotoManager;

	public SelectRecipientsListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		sContactPhotoManager = ContactPhotoManager.getInstance(context);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		mSectionHeader = (TextView) findViewById(R.id.section_header);
		mNameView = (TextView) findViewById(R.id.name);
		mNumberView = (TextView) findViewById(R.id.number);
		mLabelView = (TextView) findViewById(R.id.label);
		mAvatarView = (CheckableQuickContactBadge) findViewById(R.id.avatar);

		//if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP)
		//	mAvatarView.setOverlay(null);
	}

	public TextView getSectionHeader() {
		return mSectionHeader;
	}

	private void updateAvatarView(PhoneNumber phoneNumber) {
		if (phoneNumber == null) {
			// we were unbound in the meantime
			return;
		}
		String name = phoneNumber.getName();
		String number = phoneNumber.getNumber();
		long photoId = phoneNumber.getPhotoId();
		Log.i("liweiping", "photoId = " + photoId);
		sContactPhotoManager.loadThumbnail(mAvatarView, photoId, false, true,
				new DefaultImageRequest(name, number, true));
		mAvatarView.setVisibility(View.VISIBLE);
		// if (mContact.existsInDatabase()) {
		// mAvatarView.assignContactUri(mContact.getUri());
		// } else {
		//mAvatarView.assignContactFromPhone(phoneNumber.getNumber(), true);
		// }
		//
	}

	public final void bind(ContactAdapter contactAdapter,
			PhoneNumber phoneNumber, int position, boolean isFirst) {
		if (isFirst) {
			mNameView.setVisibility(View.VISIBLE);
			updateAvatarView(phoneNumber);
		} else {
			mAvatarView.setImageDrawable(new ColorDrawable(
					android.R.color.transparent));
			mNameView.setVisibility(View.GONE);
		}

		String lastNumber = (String) mAvatarView.getTag();
		String newNumber = phoneNumber.getNumber();
		boolean sameItem = lastNumber != null && lastNumber.equals(newNumber);

		mAvatarView.setChecked(phoneNumber.isChecked(), sameItem);
		mAvatarView.setTag(newNumber);
		mAvatarView.setVisibility(View.VISIBLE);

		// mNumberView.setText(newNumber);
		// mNameView.setText(phoneNumber.getName());
		contactAdapter.setText(mNumberView, newNumber);
		contactAdapter.setText(mNameView, phoneNumber.getName());
		mLabelView.setText(Phone.getTypeLabel(getResources(),
				phoneNumber.getType(), phoneNumber.getLabel()));
		mLabelView.setVisibility(View.VISIBLE);
	}

	public void unbind() {
		// Contact.removeListener(this);
		// mContact = null;
	}

}
