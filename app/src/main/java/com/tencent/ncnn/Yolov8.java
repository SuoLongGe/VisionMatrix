package com.tencent.ncnn;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

public class Yolov8 {
    static {
        System.loadLibrary("yolov8ncnn");
    }

    public native int loadModel(AssetManager mgr, String paramPath, String binPath);
    public native DetectionResult[] detect(Bitmap bitmap, float threshold);

    public static class DetectionResult {
        public int classId;
        public String className;
        public float confidence;
        public float x;
        public float y;
        public float width;
        public float height;
    }
}

