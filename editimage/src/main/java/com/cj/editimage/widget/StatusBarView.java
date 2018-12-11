package com.cj.editimage.widget;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by cai.jia 2018/11/6 19:37
 */
public class StatusBarView extends View {

    public StatusBarView(Context context) {
        this(context, null);
    }

    public StatusBarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private static int getStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int statusBarHeight = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            statusBarHeight = getStatusBarHeight(getContext());
        }

        setMeasuredDimension(widthMeasureSpec, resolveSize(0, MeasureSpec.makeMeasureSpec(statusBarHeight, MeasureSpec.EXACTLY)));
    }
}
