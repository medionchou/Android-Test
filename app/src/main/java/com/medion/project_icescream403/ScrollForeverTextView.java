package com.medion.project_icescream403;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by EC-510 on 2015/9/23.
 */
public class ScrollForeverTextView extends TextView {
    public ScrollForeverTextView(Context context) {
        super(context);
    }
    public ScrollForeverTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ScrollForeverTextView(Context context, AttributeSet attrs,int defStyle) {
        super(context, attrs, defStyle);
    }
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if(focused)
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }
    @Override
    public void onWindowFocusChanged(boolean focused) {
        if(focused)
            super.onWindowFocusChanged(focused);
    }
    @Override
    public boolean isFocused() {
        return true;
    }


}
