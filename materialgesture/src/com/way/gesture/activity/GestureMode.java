package com.way.gesture.activity;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import com.way.gesture.R;
import com.way.gesture.bean.GestureObject;
import com.way.gesture.util.AppUtil;
import com.way.gesture.util.GestureDataManager;
import com.way.gesture.util.GestureLibraryManager;
import com.way.selectcontact.util.PinYin;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public class GestureMode extends GestureObject {
	public String realName;
	public String section;
	private static final String SECTION_UNKNOWN = "#";

	private String getSection(Context context, CharSequence label) {
		if (label == null || label.length() == 0)
			return SECTION_UNKNOWN;
		char c = Character.toUpperCase(label.charAt(0));
		String locale = context.getResources().getConfiguration().locale
				.toString();
		if (TextUtils.equals(locale, Locale.CHINA.toString())) {// 中文
			String pinyin = PinYin.getPinYin(String.valueOf(c));
			c = TextUtils.isEmpty(pinyin) ? c : pinyin.charAt(0);
		}
		if (c < 'A' || c > 'Z')
			return SECTION_UNKNOWN;
		return Character.toString(c);
	}

	private GestureMode(Context context, GestureLibraryManager gestureLibraryManager, Cursor cursor) {
		int gestureType = cursor.getInt(0);
		//String appName = cursor.getString(1);
		String packageName = cursor.getString(2);
		String phoneNumber = cursor.getString(4);
		byte[] pointDataBytes = cursor.getBlob(3);
		String userName = cursor.getString(5);
		String phoneType = cursor.getString(6);
		int recorderId = cursor.getInt(7);
		String className = cursor.getString(8);

		this.gestureType = gestureType;
		this.packageName = packageName;
		if(!TextUtils.isEmpty(packageName))
			this.appName = AppUtil.getAppName(context, packageName);
		this.className = className;
		this.phoneNumber = phoneNumber;
		this.phoneType = phoneType;
		this.userName = userName;
		this.allGesturePoints = bytes2Points(pointDataBytes);
        this.gestureId = recorderId;
        this.gesture = gestureLibraryManager.getGesture(String.valueOf(gestureId));

		switch (gestureType) {
		case TYPE_LAUNCHER_APP:
			section = getSection(context, this.appName);
			String prefxAppName = context.getString(R.string.startapp);
			realName = prefxAppName + this.appName;
			break;
		case TYPE_CALL_TO:
			section = getSection(context, userName);
			String prefixCallName = context
					.getString(R.string.gesturelistcallto);
			realName = prefixCallName + userName;
			break;
		case TYPE_MMS_TO:
			section = getSection(context, userName);
			String prefixSmsName = context
					.getString(R.string.gesturelistsendsms);
			realName = prefixSmsName + userName;
			break;
		default:
			break;
		}
	}

	/**
	 * Get all possible recipients (groups and contacts with phone number(s)
	 * only)
	 * 
	 * @param context
	 * @return all possible recipients
	 */
	public static ArrayList<GestureMode> getGestureModes(Context context) {
		GestureDataManager manager = GestureDataManager.defaultManager(context);
		final Cursor cursor = manager.getAllGestureList();
		if (cursor == null) {
			return null;
		}

		if (cursor.getCount() == 0) {
			cursor.close();
			return null;
		}
		// Construct the final list
		ArrayList<GestureMode> gestureModes = new ArrayList<GestureMode>();
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			GestureMode gestureMode = new GestureMode(context, manager.getGestureLibraryManager(), cursor);
			if (gestureMode.gestureType >= 3)
				continue;
			if (gestureMode.gestureType == 0
					&& !AppUtil.isAppExists(context, gestureMode.packageName)) {
				manager.delete(gestureMode);
				continue;
			}
			gestureModes.add(gestureMode);
		}
		cursor.close();
		Collections.sort(gestureModes, mComparator);
		return gestureModes;
	}

	private static final Comparator<GestureMode> mComparator = new Comparator<GestureMode>() {
		private final Collator sCollator = Collator.getInstance();

		@Override
		public int compare(GestureMode lhs, GestureMode rhs) {
			return sCollator.compare(lhs.section, rhs.section);
		}
	};

}
