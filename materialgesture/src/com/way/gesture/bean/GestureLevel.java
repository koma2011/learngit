package com.way.gesture.bean;

import android.gesture.Gesture;

public class GestureLevel extends Gesture {
    float max_length = 1280.0F;
    float max_level = 2.8F;
    float min_length = 500.0F;
    float min_level = 2.0F;
    
    public static final float MAX_LENGTH = 1280.0F;
    public static final float MIN_LENGTH = 50.0F;
    public static final float MAX_LEVEL = 2.8F;
    public static final float MIN_LEVEL = 2.0F;

    public float getLevel() {
        float gestureLength = getLength();
        if (gestureLength > max_length)
            gestureLength = max_length;
        if (gestureLength < min_length)
            gestureLength = min_length;
        float ratio = (gestureLength - min_length) / (max_length - min_length);
        return max_level - ratio * (max_level - min_level);
    }
}