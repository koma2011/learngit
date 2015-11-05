package com.way.gesture.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.way.gesture.R;
import com.way.gesture.bean.GestureObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.gesture.Gesture;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class GestureDataManager {
    private static final String GESTURE_SYSTEM_DIR = "/system/etc/gesture/";
    private static final String GESTURE_DB_FILE = "gesture.db";
    public static final String GESTURE_LIBRARYS_FILE = "gesture_librarys";
    private static final String GESTURE_TABLE_NAME = "gesture_object";
    private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS " + GESTURE_TABLE_NAME
            + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, type INTEGER NOT NULL, appName TEXT,"
            + " className TEXT, package TEXT, modelData DATA, pointData DATA NOT NULL,"
            + " phoneNumber TEXT, userName TEXT, phoneType INTEGER);";
    private static GestureDataManager mGestureDataManager = null;
    private SQLiteDatabase mSQLiteDatabase;
    private GestureLibraryManager mGestureLibrary;
    private Context mContext;

    private GestureDataManager(Context context) {
        mContext = context;
        boolean isRestoreFile = restoreFiles(context);
        mSQLiteDatabase = context.openOrCreateDatabase(GESTURE_DB_FILE, 0, null);
        mGestureLibrary = GestureLibraryManager.defaultManager(context);
        if (!isRestoreFile)
            mSQLiteDatabase.execSQL(DATABASE_CREATE);
        recheckDBApp(context);
    }
    public GestureLibraryManager getGestureLibraryManager(){
        return mGestureLibrary;
    }

    private void recheckDBApp(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "gesture_recheck", false)) {
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("gesture_recheck", true).apply();
        Cursor cursor = mSQLiteDatabase
                .query(GESTURE_TABLE_NAME, new String[]{"type", "appName",
                                "package", "pointData", "phoneNumber", "userName",
                                "phoneType", "_id", "className"}, "type=0", null, null,
                        null, null);
        if ((cursor != null) && (cursor.getCount() != 0)) {
            while (cursor.moveToNext()) {
                GestureObject gestureObject = new GestureObject();
                int gestureType = cursor.getInt(0);
                String appName = cursor.getString(1);
                String packageName = cursor.getString(2);
                String phoneNumber = cursor.getString(4);
                byte[] pointDataBytes = cursor.getBlob(3);
                String userName = cursor.getString(5);
                String phoneType = cursor.getString(6);
                int recorderId = cursor.getInt(7);
                String className = cursor.getString(8);
                gestureObject.gestureType = gestureType;
                gestureObject.packageName = packageName;
                gestureObject.appName = appName;
                gestureObject.className = className;
                gestureObject.phoneNumber = phoneNumber;
                gestureObject.phoneType = phoneType;
                gestureObject.userName = userName;
                gestureObject.bytes2Points(pointDataBytes);
                gestureObject.gesture = mGestureLibrary.getGesture(String.valueOf(recorderId));
                gestureObject.gestureId = recorderId;
                if (!AppUtil.isAppExists(context, packageName) && TextUtils.equals("org.codeaurora.snapcam", packageName)) {
                    gestureObject.packageName = "com.myos.camera";
                    gestureObject.className = "com.myos.camera.CameraLauncher";
                    updateGesture(gestureObject);
                }
            }
            cursor.close();
        }
    }


    private static void copyFile(InputStream inputStream,
                                 OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[4 * 1024];
        int len = -1;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
    }

    public static GestureDataManager defaultManager(Context context) {
        if (mGestureDataManager == null) {
            mGestureDataManager = new GestureDataManager(context);
        }
        return mGestureDataManager;
    }

    /**
     * 如果有用户需要内置手势，可以实现此函数
     *
     * @param context
     */
    private static boolean restoreFiles(Context context) {
        File dbDir = new File("/data/data/com.way.gesture/databases");
        if (!dbDir.exists()) {
            dbDir.mkdir();
        }
        if (!new File(GESTURE_SYSTEM_DIR, GESTURE_DB_FILE).exists()) {
            MyLog.d("gesture",
                    "/system/etc/gesture/gesture.db not exists ,no need restore");
        }
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "way_gesture_restore", false)) {
            return true;
        }

        try {
            // copy gesture.db
            File dbInputFile = new File(GESTURE_SYSTEM_DIR, GESTURE_DB_FILE);
            InputStream dbInputStream = null;
            if (dbInputFile.exists())
                dbInputStream = new FileInputStream(dbInputFile);
            else
                dbInputStream = context.getResources().openRawResource(R.raw.gesture);
            //context.getAssets().open("gesture.db");
            File dbOutputFile = context.getDatabasePath(GESTURE_DB_FILE);
            //new File(
            //"/data/data/com.way.gesture/databases/gesture.db");
            if (dbOutputFile.exists()) {
                MyLog.d("gesture", "gesture.db exists delete it now ");
                dbOutputFile.delete();
            }
            MyLog.d("gesture", "gesture.db not exists need restore");
            dbOutputFile.createNewFile();
            FileOutputStream dbOutputStream = new FileOutputStream(dbOutputFile);
            copyFile(dbInputStream, dbOutputStream);
            dbInputStream.close();
            dbOutputStream.close();

            // copy gesture_librarys
            File gestureInputFile = new File(GESTURE_SYSTEM_DIR, GESTURE_LIBRARYS_FILE);
            InputStream gestureInputStream = null;
            if (gestureInputFile.exists())
                gestureInputStream = new FileInputStream(gestureInputFile);
            else
                gestureInputStream = context.getResources().openRawResource(R.raw.gesture_librarys);
            //context.getAssets().open("gesture_librarys");
            File gestureFile = new File(context.getFilesDir(), GESTURE_LIBRARYS_FILE);
            //new File(
            //"/data/data/com.way.gesture/files/gesture_librarys");
            if (gestureFile.exists()) {
                MyLog.d("gesture", "gesture_librarys exists delete it now ");
                gestureFile.delete();
            }
            MyLog.d("gesture", "gesture_librarys not exists need restore");
            FileOutputStream gestureFileOutputStream = context.openFileOutput(
                    "gesture_librarys", Context.MODE_PRIVATE);
            copyFile(gestureInputStream, gestureFileOutputStream);
            gestureInputStream.close();
            gestureFileOutputStream.close();
            MyLog.d("gesture",
                    "gesture restore file from /system/etc/gesture  sucess ");
            return true;

        } catch (IOException e) {
            MyLog.d("gesture", "gesture restore error  ", e);
            try {
                File dbFile = context.getDatabasePath(GESTURE_DB_FILE);
                if (dbFile.exists()) {
                    dbFile.delete();
                }
                File gestureFile = new File(context.getFilesDir(), GESTURE_LIBRARYS_FILE);
                if (gestureFile.exists()) {
                    gestureFile.delete();
                }
            } finally {
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putBoolean("way_gesture_restore", true).apply();
            }

        } finally {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean("way_gesture_restore", true).apply();
        }
        return false;
    }

    public void delete(GestureObject gestureObject) {
        int id = mSQLiteDatabase.delete("gesture_object", "_id="
                + gestureObject.gestureId, null);
        MyLog.w("liweiping", "delete :" + id);
        mGestureLibrary.removeGesture(gestureObject);
    }

    public Cursor getAllGestureList() {
        return mSQLiteDatabase
                .query(GESTURE_TABLE_NAME, new String[]{"type", "appName",
                                "package", "pointData", "phoneNumber", "userName",
                                "phoneType", "_id", "className"}, null, null, null,
                        null, null);
    }

    public ArrayList<GestureObject> getGestureArrayList() {
        ArrayList<GestureObject> gestureObjects = new ArrayList<GestureObject>();
        Cursor cursor = getAllGestureList();
        if ((cursor != null) && (cursor.getCount() != 0)) {
            cursor.moveToFirst();
            do {
                GestureObject gestureObject = new GestureObject();
                int gestureType = cursor.getInt(0);
                String appName = cursor.getString(1);
                String packageName = cursor.getString(2);
                String phoneNumber = cursor.getString(4);
                byte[] pointDataBytes = cursor.getBlob(3);
                String userName = cursor.getString(5);
                String phoneType = cursor.getString(6);
                int recorderId = cursor.getInt(7);
                String className = cursor.getString(8);
                gestureObject.gestureType = gestureType;
                gestureObject.packageName = packageName;
                gestureObject.appName = appName;
                gestureObject.className = className;
                gestureObject.phoneNumber = phoneNumber;
                gestureObject.phoneType = phoneType;
                gestureObject.userName = userName;
                gestureObject.allGesturePoints = gestureObject.bytes2Points(pointDataBytes);
                gestureObject.gestureId = recorderId;
                gestureObject.gesture = mGestureLibrary.getGesture(String.valueOf(recorderId));
                if (gestureObject.gestureType >= 3)
                    continue;
                if (gestureObject.gestureType == 0 && !AppUtil.isAppExists(mContext, gestureObject.packageName)) {
                    delete(gestureObject);
                    continue;
                }
                gestureObjects.add(gestureObject);
            } while (cursor.moveToNext());
        }
        if (cursor != null)
            cursor.close();
        return gestureObjects;
    }

    public int getGestureCount() {
        return getGestureArrayList().size();
    }

    public GestureObject getGestureObject(int id) {
        Cursor cursor = mSQLiteDatabase.query(GESTURE_TABLE_NAME, new String[]{
                        "type", "appName", "package", "className", "pointData",
                        "phoneNumber", "userName", "phoneType", "_id"}, "_id=" + id,
                null, null, null, null);
        if ((cursor == null) || (cursor.getCount() != 1)) {
            if (cursor != null)
                cursor.close();
            return null;
        }
        cursor.moveToFirst();
        GestureObject gestureObject = new GestureObject();
        int gestureType = cursor.getInt(0);
        String appName = cursor.getString(1);
        String packageName = cursor.getString(2);
        String className = cursor.getString(3);
        String phoneNumber = cursor.getString(5);
        byte[] pointDataBytes = cursor.getBlob(4);
        String userName = cursor.getString(6);
        String phoneType = cursor.getString(7);
        int recorderID = cursor.getInt(8);
        gestureObject.gestureType = gestureType;
        gestureObject.packageName = packageName;
        gestureObject.appName = appName;
        gestureObject.className = className;
        gestureObject.phoneNumber = phoneNumber;
        gestureObject.phoneType = phoneType;
        gestureObject.userName = userName;
        gestureObject.allGesturePoints = gestureObject.bytes2Points(pointDataBytes);
        gestureObject.gestureId = recorderID;
        gestureObject.gesture = mGestureLibrary.getGesture(String.valueOf(id));
        cursor.close();
        return gestureObject;
    }

    public void insertGestureObject(GestureObject gestureObject) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("type", Integer.valueOf(gestureObject.gestureType));
        if (gestureObject.gestureType == 0) {
            contentValues.put("package", gestureObject.packageName);
            contentValues.put("appName", gestureObject.appName);
            contentValues.put("className", gestureObject.className);
        } else {
            contentValues.put("phoneNumber", gestureObject.phoneNumber);
            contentValues.put("userName", gestureObject.userName);
            contentValues.put("phoneType", gestureObject.phoneType);
        }
        contentValues.put("pointData", gestureObject.points2Bytes());
        long id = mSQLiteDatabase.insert("gesture_object", null, contentValues);
        gestureObject.gestureId = (int) id;
        mGestureLibrary.addOrUpdateGesture(String.valueOf(gestureObject.gestureId), gestureObject.gesture);
        MyLog.w("liweiping", "insert :" + id);
    }

    public GestureObject searchGesture(Gesture gesture) {
        //return searchGesture(gestureObject, 0);
        for (int i = 0; i < 3; i++) {
            String recorderIDStr = mGestureLibrary.searchGesture(gesture);
            if (TextUtils.isEmpty(recorderIDStr)) {
                break;
            }
            GestureObject searchGestureObject = getGestureObject(Integer
                    .valueOf(recorderIDStr).intValue());
            if (searchGestureObject == null) {
                mGestureLibrary.removeGesture(recorderIDStr);
                continue;
            }
            return searchGestureObject;
        }
        return null;
    }

    public GestureObject searchGesture(GestureObject gestureObject) {
        return searchGesture(gestureObject, 0);
    }

    public GestureObject searchGesture(GestureObject gestureObject, int type) {
        //MyLog.i("liweiping", "gestureObject.modelData.length = "
        //		+ gestureObject.mModelDatas.length);
//		if (gestureObject.mModelDatas.length == 4) {
//			ArrayList<GestureObject> gestureObjects = getGestureArrayList();
//			if ((gestureObjects != null) && (gestureObjects.size() > 0))
//				// for (int i = 0; i < gestureObjects.size(); i++)
//				for (GestureObject item : gestureObjects)
//					if (gestureObject.isSameTo(item))
//						return item;
//			return null;
//		}
        int i = 0;
        while (i < 5) {// 最多尝试5次查询
            i++;
            String recorderIDStr = mGestureLibrary.searchGesture(gestureObject.gesture);
            if (TextUtils.isEmpty(recorderIDStr)) {
                break;
            }
            MyLog.d("liweiping", "searchGesture id :" + recorderIDStr);
            GestureObject searchGestureObject = getGestureObject(Integer
                    .valueOf(recorderIDStr).intValue());
            if (searchGestureObject == null) {
                mGestureLibrary.removeGesture(recorderIDStr);
                continue;
                // return null;
            }
//			MyLog.d("liweiping", "ges mode data len:"
//					+ gestureObject.mModelDatas.length);
            MyLog.d("liweiping",
                    "found gesture :" + searchGestureObject.toString());

//            if (!searchGestureObject.isValide()) {
//                MyLog.d("liweiping", "found gesture but is invalide");
//                delete(searchGestureObject);
//                continue;
//                // return null;
//            }
            return searchGestureObject;
        }
        MyLog.d("liweiping", "searchGesture null 2");
        return null;
    }

    public void updateGesture(GestureObject gestureObject) {
        byte[] pointDataBytes = gestureObject.points2Bytes();
        ContentValues contentValues = new ContentValues();
        contentValues.put("type", Integer.valueOf(gestureObject.gestureType));
        if (gestureObject.gestureType == 0) {
            contentValues.put("package", gestureObject.packageName);
            contentValues.put("appName", gestureObject.appName);
            contentValues.put("className", gestureObject.className);
        } else {
            contentValues.put("phoneNumber", gestureObject.phoneNumber);
            contentValues.put("userName", gestureObject.userName);
            contentValues.put("phoneType", gestureObject.phoneType);
        }
        if ((pointDataBytes != null) && (pointDataBytes.length > 0))
            contentValues.put("pointData", pointDataBytes);
        mSQLiteDatabase.update(GESTURE_TABLE_NAME, contentValues, "_id="
                + gestureObject.gestureId, null);
        mGestureLibrary.addOrUpdateGesture(String.valueOf(gestureObject.gestureId), gestureObject.gesture);
    }
}
