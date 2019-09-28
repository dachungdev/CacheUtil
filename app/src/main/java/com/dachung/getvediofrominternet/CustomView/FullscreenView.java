package com.dachung.getvediofrominternet.CustomView;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

public class FullscreenView extends VideoView {
    public FullscreenView(Context context) {
        super(context);
    }

    public FullscreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FullscreenView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 屏幕适配宽度
        int width = getDefaultSize(0, widthMeasureSpec);
        // 屏幕适配高度
        int height = getDefaultSize(0, heightMeasureSpec);
        // 设置宽高
        setMeasuredDimension(width, height);
    }
}
