package com.way.gesture.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.way.gesture.GestureCommond;
import com.way.gesture.R;
import com.way.gesture.bean.GestureObject;
import com.way.gesture.util.GestureDataManager;
import com.way.gesture.util.MyLog;
import com.way.selectcontact.RecipientsListActivity;
import com.way.selectcontact.data.PhoneNumber;
import com.way.ui.swipeback.SwipeBackActivity;

public class AddTaskActivity extends SwipeBackActivity implements OnItemClickListener {
	private int mCommond;
	private int mMode;
	private int mRecorderID;

	private void pickContacts() {
		Intent intent = new Intent(this, RecipientsListActivity.class);
		intent.putExtra(RecipientsListActivity.EXTRA_SELECT_MODE, RecipientsListActivity.MODE_SINGLE);
		intent.putExtra(RecipientsListActivity.EXTRA_MOBILE_NUMBERS_ONLY, false);
		startActivityForResult(intent, GestureCommond.GestureSelectPhone);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case GestureCommond.GestureLaunchApp:// 101
			if (data == null)
				return;
			if (mMode == 0) {
				data.putExtra("mode", "");
				startActivityForResult(data, GestureCommond.GestureCreateNew);
			} else {
				GestureDataManager gestureDataManager = GestureDataManager.defaultManager(this);
				GestureObject gestureObject = gestureDataManager.getGestureObject(mRecorderID);
				if (gestureObject != null) {
					gestureObject.appName = data.getStringExtra("AppName");
					gestureObject.packageName = data.getStringExtra("packageName");
					gestureObject.className = data.getStringExtra("activityName");
					MyLog.d("add option", " appName:" + gestureObject.appName + " packageName:"
							+ gestureObject.packageName + "   " + gestureObject.className);
					gestureObject.gestureType = 0;
					gestureDataManager.updateGesture(gestureObject);
					setResult(GestureCommond.GestureSucess);
					Toast.makeText(this, R.string.gesture_edit_succeed, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, R.string.gesture_edit_failed, Toast.LENGTH_SHORT).show();
				}
				finish();
			}
			break;
		case GestureCommond.GestureSelectPhone:// 102
			if (data == null)
				return;
			// parseData(data);
			PhoneNumber phoneNumber = (PhoneNumber) data.getParcelableExtra(RecipientsListActivity.EXTRA_RECIPIENTS);
			if (phoneNumber == null)
				return;

			String name = phoneNumber.getName();
			String number = phoneNumber.getNumber();
			if (mMode == 0) {
				Intent intent = new Intent();
				intent.setClass(this, CreateGestureActivity.class);
				intent.putExtra("commond", mCommond);
				intent.putExtra("userName", name);
				intent.putExtra("phoneNumber", number);
				startActivityForResult(intent, GestureCommond.GestureCreateNew);
			} else {
				GestureDataManager gestureDataManager = GestureDataManager.defaultManager(this);
				GestureObject gestureObject = gestureDataManager.getGestureObject(mRecorderID);
				if (gestureObject != null) {
					gestureObject.userName = name;
					gestureObject.phoneNumber = number;
					gestureObject.gestureType = mCommond;
					gestureDataManager.updateGesture(gestureObject);
					setResult(GestureCommond.GestureSucess);
					Toast.makeText(this, R.string.gesture_edit_succeed, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, R.string.gesture_edit_failed, Toast.LENGTH_SHORT).show();
				}
				finish();
			}
			break;
		case GestureCommond.GestureCreateNew:// 103
			if (resultCode == GestureCommond.GestureSucess) {
				MyLog.d("add new gesture", "add new gesture");
				setResult(GestureCommond.GestureSucess);
			}
			finish();
			break;

		default:
			break;
		}
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_add_task_layout);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		ListView listview = ((ListView) findViewById(android.R.id.list));
		SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.app_list_item,
				new String[] { "title", "img" }, new int[] { R.id.title, R.id.img });
		listview.setAdapter(adapter);
		listview.setOnItemClickListener(this);
		mCommond = 0;
		mMode = 0;
		Intent intent = getIntent();
		String mode = intent.getStringExtra("mode");
		if ((mode != null) && (mode.equalsIgnoreCase("Edit"))) {
			mMode = 1;
			mRecorderID = intent.getIntExtra("recorderID", 0);
		}
	}

	private List<Map<String, Object>> getData() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("title", getString(R.string.launchApp));
		map.put("img", R.drawable.ic_adb_black);
		list.add(map);

		map = new HashMap<String, Object>();
		map.put("title", getString(R.string.callother));
		map.put("img", R.drawable.ic_call_black);
		list.add(map);

		map = new HashMap<String, Object>();
		map.put("title", getString(R.string.smstoother));
		map.put("img", R.drawable.ic_message_black);
		list.add(map);

		return list;
	}

	@Override
	public void onResume() {
		super.onResume();
		MyLog.d("gesture", "ActivityAddOption onResume");
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (position == 0) {
			Intent intent = new Intent();
			intent.setClass(this, SelectAppActivity.class);
			startActivityForResult(intent, GestureCommond.GestureLaunchApp);
			return;
		}
		mCommond = position;
		pickContacts();
	}
}