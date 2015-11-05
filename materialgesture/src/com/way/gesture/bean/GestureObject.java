package com.way.gesture.bean;

import java.util.ArrayList;

import android.gesture.Gesture;
import android.gesture.GesturePoint;
import android.gesture.GestureStroke;

/**
 * Created by way on 15/8/9.
 */
public class GestureObject {
    private static final String TAG = "GestureObject";

    public static final int TYPE_LAUNCHER_APP = 0;
    public static final int TYPE_CALL_TO = 1;
    public static final int TYPE_MMS_TO = 2;
    public ArrayList<GesturePoint> allGesturePoints = new ArrayList<GesturePoint>();// 所有的点
    public int gestureType;// 有3种：启动应用、拨号、发短信
    public int gestureId;// 手势数据库中的id
    public int phoneId;//电话号码id
    public String phoneNumber;// 电话号码
    public String phoneType;// 电话类型
    public String userName;// 用户名
    public String appName;// 包名
    public String packageName;// 包名
    public String className;// 类名
    public Gesture gesture;

    /**
     * 序列化，将所有点序列化转化成二进制数据以便存入数据库中
     *
     * @return 二进制数据
     */
    public byte[] points2Bytes() {
        byte[] bytes = new byte[4 * (2 * allGesturePoints.size())];
        int i = 0;
        int j = 0;
        while (j < allGesturePoints.size()) {
            GesturePoint point = allGesturePoints.get(j);
            int x = (int) point.x;
            bytes[i] = (byte) (0xFF & x >> 24);
            bytes[(i + 1)] = (byte) (0xFF & x >> 16);
            bytes[(i + 2)] = (byte) (0xFF & x >> 8);
            bytes[(i + 3)] = (byte) (x & 0xFF);
            
            int y = (int) point.y;
            bytes[(i + 4)] = (byte) (0xFF & y >> 24);
            bytes[(i + 5)] = (byte) (0xFF & y >> 16);
            bytes[(i + 6)] = (byte) (0xFF & y >> 8);
            bytes[(i + 7)] = (byte) (y & 0xFF);
            j++;
            i += 8;
        }
        return bytes;
    }

    /**
     * 反序列化，将数据库中存储的二进制点转换成点
     *
     * @param pointDataBytes
     */
    public ArrayList<GesturePoint> bytes2Points(byte[] pointDataBytes) {
        allGesturePoints.clear();
        for (int i = 0; i < pointDataBytes.length; i += 8) {
            int x = 0 + (0xFF000000 & pointDataBytes[i] << 24)
                    + (0xFF0000 & pointDataBytes[(i + 1)] << 16)
                    + (0xFF00 & pointDataBytes[(i + 2)] << 8)
                    + (0xFF & pointDataBytes[(i + 3)]);
            int y = 0
                    + (0xFF000000 & pointDataBytes[(i + 4)] << 24)
                    + (0xFF0000 & pointDataBytes[(i + 5)] << 16)
                    + (0xFF00 & pointDataBytes[(i + 6)] << 8)
                    + (0xFF & pointDataBytes[(i + 7)]);

            GesturePoint gesturePoint = new GesturePoint(x, y, i);
            allGesturePoints.add(gesturePoint);
        }
        return allGesturePoints;
    }

    public ArrayList<GesturePoint> gesture2Points(Gesture gesture){
        final int strokesCount = gesture.getStrokesCount();
        ArrayList<GestureStroke> strokes = gesture.getStrokes();

        ArrayList<GesturePoint> gesturePoints = new ArrayList<GesturePoint>();
        for (int i = 0; i < strokesCount; i++) {
            float[] points = strokes.get(i).points;
            final int pointCount = points.length;
            for (int j = 0; j < pointCount; j += 2) {
                float x = points[j];
                float y = points[j + 1];
                GesturePoint gesturePoint = new GesturePoint(x,
                        y, j);
                gesturePoints.add(gesturePoint);
            }
            if(i < strokesCount - 1)
            	gesturePoints.add(new GesturePoint(0f, 0f, i));
        }
        return gesturePoints;
    }
    
    @Override
    public String toString() {
        return GestureObject.class.getName() + " ["
                + "gestureType=" + gestureType
                + ", gestureId=" + gestureId
                + ", phoneId=" + phoneId
                + ", phoneNumber=" + phoneNumber
                + ", phoneType=" + phoneType
                + ", userName=" + userName
                + ", packageName=" + packageName
                + ", className=" + className
                + ", gesture=" + gesture
                + "]";
    }
}
