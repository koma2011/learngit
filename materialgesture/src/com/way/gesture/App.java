package com.way.gesture;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import com.way.floatwindow.GestureFloatView;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * This class represents the Application core.
 */
@ReportsCrashes(
        mailTo = "way.ping.li@gmail.com",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.color.transparent,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogOkToast = R.string.crash_dialog_ok_toast)
public class App extends Application {
	private static final String TAG = App.class.getSimpleName();
	private static Context mApplicationContext;

	@Override
	public void onCreate() {
		ACRA.init(this);
		super.onCreate();
		if (Settings.System.getInt(getContentResolver(), "way_gesture_switch", 1) == 1)
			startService(new Intent(this, GestureService.class));
		mApplicationContext = getApplicationContext();
		new GestureFloatView(this, mCallbacks);
	}

	public static Context getContext() {
		return mApplicationContext;
	}
	
    private void showDialog() {
        if (Settings.Global.getInt(getContentResolver(),
                "super_low_power", 0) == 1){
        	Toast.makeText(this, R.string.no_support_option, Toast.LENGTH_SHORT).show();
            return;
        }
		Intent intent = new Intent(this, GestureService.class);
		intent.putExtra("time", System.currentTimeMillis());
		startService(intent);
    }


	GestureFloatView.Callbacks mCallbacks = new GestureFloatView.Callbacks() {

		@Override
		public void onSwipeFromTop() {
			showDialog();
		}

		@Override
		public void onSwipeFromRight() {

		}

		@Override
		public void onSwipeFromBottom() {
			//showDialog();
		}

		@Override
		public void onDebug() {

		}
	};

}
