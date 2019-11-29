package com.analysys.visual.viewcrawler;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * 增加摇一摇功能，传感器先感知摇一摇，如果触发回调，则返回； 如果未触发，则该次onSensorChanged后续执行翻转屏幕的操作 add by
 * 2018.6.13
 */
class FlipGesture implements SensorEventListener {

    private final static long INTERVAL_TIME = 1000 * 5;
    private static final String TAG = "FlipGesture";
    private static long TIGGER_TIME = 0L;

    private final OnFlipGestureListener mListener;
    //使用数组记录传感器加速度，超过数组长度后从0开始覆盖记载
    private static final int SIZE = 10;
    private float[] mAccXCache = new float[SIZE];
    private float[] mAccYCache = new float[SIZE];
    private float[] mAccZCache = new float[SIZE];
    //数据分析起点
    private int mStartIdx;
    //数据添加位置
    private int mCurrentDataIdx;
    //数据是否被覆盖
    private boolean mDataOverrided;

    //摇动过程中产生的最大差值
    private static final int SHAKE_THRESHOLD = 30;
    //摇动过程中产生的不同方向最大差值比值
    private static final int SHAKE_DIV_THRESHOLD = 10;
    //翻转检测中xy两个方向最大变动
    private static final int FLIP_THRESHOLD_XY = 5;
    //翻转检测中z方向最小变动
    private static final int FLIP_THRESHOLD_Z = 15;

    public FlipGesture(OnFlipGestureListener listener) {
        mListener = listener;
    }

    private boolean shakeOffOrFlip(float[] values) {
        float x = values[0];
        float y = values[1];
        float z = values[2];
        mAccXCache[mCurrentDataIdx] = x;
        mAccYCache[mCurrentDataIdx] = y;
        mAccZCache[mCurrentDataIdx] = z;

        //检查摇一摇
        for (int i = mCurrentDataIdx; true; i--) {
            if (i < 0) {
                if (mDataOverrided) {
                    i = SIZE - 1;
                } else {
                    break;
                }
            }
            if (i == mStartIdx) {
                break;
            }
            float absX = Math.abs(mAccXCache[i] - x);
            float absY = Math.abs(mAccYCache[i] - y);
            float absZ = Math.abs(mAccZCache[i] - z);
            if (absX > SHAKE_THRESHOLD && (absX / absY > SHAKE_DIV_THRESHOLD || absX / absZ > SHAKE_DIV_THRESHOLD))
            {
                reset();
                return true;
            }
            if (absY > SHAKE_THRESHOLD && (absY / absX > SHAKE_DIV_THRESHOLD || absY / absZ > SHAKE_DIV_THRESHOLD))
            {
                reset();
                return true;
            }
            if (absZ > SHAKE_THRESHOLD && (absZ / absX > SHAKE_DIV_THRESHOLD || absZ / absY > SHAKE_DIV_THRESHOLD))
            {
                reset();
                return true;
            }
        }

        //检查翻转
        for (int i = mCurrentDataIdx; true; i--) {
            if (i < 0) {
                if (mDataOverrided) {
                    i = SIZE - 1;
                } else {
                    break;
                }
            }
            if (i == mStartIdx) {
                break;
            }
            //符号相反
            float curZ = mAccZCache[i];
            if (curZ * z < 0 && Math.abs(curZ - z) > FLIP_THRESHOLD_Z) {
                if (Math.abs(mAccXCache[i] - x) < FLIP_THRESHOLD_XY && Math.abs(mAccYCache[i] - y) < FLIP_THRESHOLD_XY) {
                    reset();
                    return true;
                }
            }
        }

        mCurrentDataIdx++;
        if (mCurrentDataIdx == SIZE) {
            mCurrentDataIdx = 0;
            mDataOverrided = true;
        }
        if (mDataOverrided) {
            mStartIdx = mCurrentDataIdx + 1;
            if (mStartIdx == SIZE) {
                mStartIdx = 0;
            }
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Samples may come in around 4 times per second

        int sensorType = event.sensor.getType();
        // values[0]:X轴，values[1]：Y轴，values[2]：Z轴
        float[] values = event.values;
        // 如果传感器类型为加速度传感器，则判断是否为摇一摇
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            if (shakeOffOrFlip(values) && mListener != null && System.currentTimeMillis() - TIGGER_TIME > INTERVAL_TIME) {
                TIGGER_TIME = System.currentTimeMillis();
                mListener.onFlipGesture();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void reset() {
        mCurrentDataIdx = 0;
        mStartIdx = 0;
        mDataOverrided = false;
    }

    public interface OnFlipGestureListener {
        void onFlipGesture();
    }
}
