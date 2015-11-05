package com.way.gesture.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.way.gesture.App;
import com.way.gesture.R;
import com.way.gesture.bean.GestureObject;

public class AppUtil {
	private static final String TAG = AppUtil.class.getName();

	public static String getAppName(Context context, String pkg) {
		PackageManager pm = context.getPackageManager();
		String name = pkg;
		try {
			name = pm.getApplicationLabel(
					pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA))
					.toString();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return name;
	}

	public static boolean isAppExists(Context context, String pkg) {
		if (TextUtils.isEmpty(pkg))
			return false;
		try {
			context.getPackageManager().getApplicationInfo(pkg,
					PackageManager.GET_UNINSTALLED_PACKAGES);
			return true;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	// 安全启动应用，避免未找到应用程序时崩溃
	public static boolean startActivitySafely(Activity activity, Intent intent) {
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			activity.startActivity(intent);
			activity.finish();
			return true;
		} catch (ActivityNotFoundException e) {
			Toast.makeText(activity, R.string.getsure_option_info2,
					Toast.LENGTH_SHORT).show();
			MyLog.e(TAG, "Unable to launch intent = " + intent, e);
		} catch (SecurityException e) {
			Toast.makeText(activity, R.string.getsure_option_info2,
					Toast.LENGTH_SHORT).show();
			MyLog.e(TAG, "does not have the permission to launch intent = "
					+ intent, e);
		} catch (Exception e) {
			MyLog.e(TAG, "catch Exception ", e);
		}
		return false;
	}

	/**
	 * Returns a deterministic color based on the provided contact identifier
	 * string.
	 */
	public static int pickColor(final String name) {
		int sDefaultColor = App.getContext().getResources()
				.getColor(R.color.letter_tile_default_color);
		if (TextUtils.isEmpty(name)) {
			return sDefaultColor;
		}
		TypedArray sColors = App.getContext().getResources()
				.obtainTypedArray(R.array.letter_tile_colors);
		final int color = Math.abs(name.hashCode()) % sColors.length();
		return sColors.getColor(color, sDefaultColor);
	}

	public static int pickColor(final GestureObject gestureObject) {
		return pickColor(getIdentifier(gestureObject));
	}

	private static String getIdentifier(GestureObject gestureObject) {
		switch (gestureObject.gestureType) {
		case GestureObject.TYPE_LAUNCHER_APP:
			return gestureObject.appName;
		case GestureObject.TYPE_CALL_TO:
		case GestureObject.TYPE_MMS_TO:
			return TextUtils.isEmpty(gestureObject.phoneNumber) ? gestureObject.userName
					: gestureObject.phoneNumber;
		default:
			return null;
		}

	}

	/**
	 * 获取手机屏幕高度
	 * 
	 * @param context
	 * @return
	 */
	public static int getDisplayHeight(Context context) {
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		wm.getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	/**
	 * 获取手机屏幕宽度
	 * 
	 * @param context
	 * @return
	 */
	public static int getDisplayWidth(Context context) {
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		wm.getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}
	public static int getStatusBarSize(Resources res) {
		int result = 0;
		if (res == null) {
			return result;
		}
		int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = res.getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static void transWindows(Activity activity, int color) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			setTranslucentStatus(activity, true);
			setTranslucentNavigation(activity, true);
		}
	}

	@TargetApi(19)
	private static void setTranslucentStatus(Activity activity, boolean on) {
		Window win = activity.getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
		if (on) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
		}
		win.setAttributes(winParams);
	}

	@TargetApi(19)
	private static void setTranslucentNavigation(Activity activity, boolean on) {
		Window win = activity.getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
		if (on) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
		}
		win.setAttributes(winParams);
	}

	@SuppressLint("NewApi")
	public static void setNavigationBar(AppCompatActivity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			activity.getWindow().setNavigationBarColor(
					Color.parseColor("#ffeeeeee"));
			// activity.getSupportActionBar().setElevation(0);
		}
	}
}
