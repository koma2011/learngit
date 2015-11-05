package com.way.gesture;

import com.way.floatwindow.ATouchFloatView;
import com.way.floatwindow.GestureFloatView;
import com.way.gesture.activity.SettingsFragment;
import com.way.gesture.util.MyLog;
import com.way.gesture.view.GestureDialog;
import com.way.gesture.view.GestureToast;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;

public class GestureService extends Service implements GestureController {
    private static final int NOTIFICATION_ID = 99118899;
    private GestureDialog mDialog;
    private ScreenLockController mLockController;
    private SharedPreferences mSharedPreferences;

    @Override
    public void hideOverView() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.cancel();
        }
    }

    @Override
    public boolean isOnLockScreen() {
        return mLockController.isOnLockScreen();
    }

    @Override
    public IBinder onBind(Intent paramIntent) {
        return null;
    }


    private GestureFloatView mGestureFloatView;
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
    private ATouchFloatView mATouchFloatView;
    ATouchFloatView.Callbacks mATouchCallback = new ATouchFloatView.Callbacks() {

        @Override
        public void onClick() {
            showDialog();
        }

        @Override
        public void onLongClick() {

        }
    };

    private void showDialog() {
        if (!mLockController.canShowDialog()) {
            MyLog.d("gesture", "onStartConmand  KeyguardManager need password");
            GestureToast.showToast(this,
                    getResources().getString(R.string.gesture_secure_toast));
            return;
        }
        MyLog.d("gesture", "show dialog");
        if (mDialog != null && mDialog.isShowing())
            return;
		if (mDialog != null && !mLockController.isOnLockScreen()) {
			mDialog.show();
			return;
		}
        //mDialog = new GestureDialog(GestureService.this, GestureService.this);
        //mDialog.show();
        //mDialog = new NewGestureDialog(this, GestureService.this);
        mDialog.show();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLockController = new ScreenLockController();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mDialog = new GestureDialog(this, GestureService.this);
        mSettingsObserver.setListening(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLockController.unRegisterReceiverLock();
        MyLog.d("gesture", "onDestory");
        if (mGestureFloatView != null) {
            mGestureFloatView.removeFromWindow();
            mGestureFloatView = null;
        }
        if (mATouchFloatView != null) {
            mATouchFloatView.removeFromWindow();
            mATouchFloatView = null;
        }
        stopForeground(true);
        mSettingsObserver.setListening(false);
    }
    
    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver(mHandler);
    private final class SettingsObserver extends ContentObserver {
        private final Uri SUPER_LOW_POWER_MODE_URI
                = Settings.Global.getUriFor("super_low_power");

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (SUPER_LOW_POWER_MODE_URI.equals(uri)) {
            	if (mDialog != null && mDialog.isShowing())
            		mDialog.cancel();
            }
        }

        public void setListening(boolean listening) {
            final ContentResolver cr = getContentResolver();
            if (listening) {
                cr.registerContentObserver(SUPER_LOW_POWER_MODE_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }
    }
    private void startForeground(){
    	if(!mSharedPreferences.getBoolean(SettingsFragment.SERVICE_FOREGROUND_KEY, true)){
    		return;
    	}
        Context context = getApplicationContext();
        String title = context.getString(R.string.app_name);
        String subtitle = context.getString(R.string.service_foreground_summary);
        Notification notification = new Notification.Builder(context) //
                .setContentTitle(title)
                .setContentText(subtitle)
                .setSmallIcon(R.drawable.ic_stat_notify_gestures)
                //.setColor(context.getResources().getColor(R.color.primary))
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MIN).build();

        MyLog.d("way", "Gesture service into the foreground with notification.");
        startForeground(NOTIFICATION_ID, notification);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Settings.System.getInt(getContentResolver(), "way_gesture_switch",
                1) == 0) {
            MyLog.d("gesture", " gesture is disabled now ");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (mGestureFloatView == null && mSharedPreferences.getBoolean(SettingsFragment.SWIPE_KEY, false)) {
            mGestureFloatView = new GestureFloatView(getApplicationContext(), mCallbacks);
            MyLog.d("gesture", "display  GestureFloatView...");
        } else if (mGestureFloatView != null && !mSharedPreferences.getBoolean(SettingsFragment.SWIPE_KEY, false)) {
            mGestureFloatView.removeFromWindow();
            mGestureFloatView = null;
            MyLog.d("gesture", "removeFromWindow  GestureFloatView...");
        }
        if (mATouchFloatView == null && mSharedPreferences.getBoolean(SettingsFragment.ATOUCH_KEY, false)) {
            mATouchFloatView = new ATouchFloatView(getApplicationContext(), mATouchCallback);
            MyLog.d("gesture", "display  ATouchFloatView...");
        } else if (mATouchFloatView != null && !mSharedPreferences.getBoolean(SettingsFragment.ATOUCH_KEY, false)) {
            mATouchFloatView.removeFromWindow();
            mATouchFloatView = null;
            MyLog.d("gesture", "removeFromWindow  ATouchFloatView...");
        }

        if (intent == null) {
            return START_STICKY;
        }
        long intentTime = intent.getLongExtra("time", 0L);
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - intentTime) > 2000L) {
            MyLog.d("gesture", "onStartConmand time out:" + currentTime
                    + " ," + intentTime);
            return START_STICKY;
        }
        MyLog.d("gesture", "onStartConmand    difftime :" + currentTime + " ,"
                + intentTime);
        showDialog();
        return START_STICKY;
    }

    @Override
    public void showOverView() {
    }

    @Override
    public void tryUnLock() {
        //mLockController.unLock();
    }

    private class ScreenLockController {
    	private static final String ACTION_HALL_CHANGED = "android.intent.action.HALL_CHANGED";
        //private int mCount = 0;
        private KeyguardManager mKeyguardManager = null;
        //private boolean mDoUnlocked = false;
        //private KeyguardManager.KeyguardLock mLock = null;
        //private String mTag = "ScreenLockController";
        private BroadcastReceiver mLockReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
            	if(intent == null || TextUtils.isEmpty(intent.getAction()))
            		return;
            	String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)
                		|| ACTION_HALL_CHANGED.equals(action)) {
//					MyLog.d("gesture",
//							"receive action_screen_off current_count:" + mCount);
                    hideOverView();
//					try {
//						if (mDoUnlocked) {
//							mLock.reenableKeyguard();
//							mDoUnlocked = false;
//							MyLog.d("gesture",
//									"receive action_screen_off   and do renablekeyguard ");
//						}
//					} catch (Exception e) {
//						MyLog.d("gesture", "lock  exception  ", e);
//					}
                }
            }
        };

        public ScreenLockController() {
            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

            // 声明键盘锁并初始化键盘锁用于锁定或解开键盘锁
//			mLock = mKeyguardManager.newKeyguardLock(mTag);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            intentFilter.addAction(ACTION_HALL_CHANGED);
            registerReceiver(mLockReceiver, intentFilter);

        }

        public void unRegisterReceiverLock() {
            try {
                GestureService.this.unregisterReceiver(mLockReceiver);
            } catch (Exception e) {

            }
        }

        private boolean isSecure() {
            // LockPatternUtils mLockPatternUtils = new LockPatternUtils(this);
            // // 图案 true
            // mLockPatternUtils.isLockPatternEnabled();
            // mLockPatternUtils.savedPatternExists();
            // // 密码 true
            // mLockPatternUtils.isLockPasswordEnabled();
            // // 无 true
            // mLockPatternUtils.isLockScreenDiseabled();
            // 以上全false就是滑动

            boolean isSecured = mKeyguardManager.isKeyguardSecure();
            // switch (mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
            // // 图案解锁
            // case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
            // // 以下是数字密码
            // case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            // case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            // case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            // case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            // if (!mLockPatternUtils.isLockPatternEnabled()
            // || !mLockPatternUtils.savedPatternExists()) {
            // isSecured = false;
            // } else {
            // isSecured = true;
            // }
            // }
            return isSecured;
        }

        public boolean canShowDialog() {
            boolean isLocked = mKeyguardManager.isKeyguardLocked();
            boolean isSecure = isSecure();
            return !isLocked || !isSecure;
        }

        public boolean isOnLockScreen() {
            return mKeyguardManager.isKeyguardLocked();
        }

//		public void unLock() {
//			if (mKeyguardManager.isKeyguardLocked() && !isSecure()) {
//				if (!mDoUnlocked)
//					try {
//						mLock.disableKeyguard();
//						mDoUnlocked = true;
//						mCount++;
//						MyLog.d("gesture", "unLock  done ");
//					} catch (Exception e) {
//						MyLog.d("gesture", "unLock  exception  ", e);
//					}
//			}
//			MyLog.d("gesture", " can not unLock");
//		}
    }
}
