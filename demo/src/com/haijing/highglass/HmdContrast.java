package com.haijing.highglass;

/**
 * Created by Administrator on 2016/6/27.
 */
public class HmdContrast {
    public static final int DEFAULT_W = 50;
    public static final int DEFAULT_R = 50;
    public static final int DEFAULT_G = 50;
    public static final int DEFAULT_B = 50;

    public int w;
    public int r;
    public int g;
    public int b;

    public HmdContrast() {
        this.w = DEFAULT_W;
        this.r = DEFAULT_R;
        this.g = DEFAULT_G;
        this.b = DEFAULT_B;
    }

    public HmdContrast(int w, int r, int g, int b) {
        this.w = w;
        this.r = r;
        this.g = g;
        this.b = b;
    }
}
