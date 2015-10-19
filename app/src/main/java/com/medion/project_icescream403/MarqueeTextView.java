package com.medion.project_icescream403;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by Medion on 2015/10/19.
 */
public class MarqueeTextView extends TextView {

    private float textLength = 0f;

    private float viewWidth = 0f;

    private float x = 0f;

    private float y = 0f;

    private float speed = 1.5f;

    private float start_point = 0.0f;

    private float moving_length = 0.0f;

    private Paint paint = null;

    private String text = "";

    public MarqueeTextView(Context context) {
        super(context);
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setSpeed(float speed)
    {
        this.speed = speed;
    }

    private void init()
    {
        paint = getPaint();
        paint.setColor(Color.GREEN);
        text = getText().toString();
        textLength = paint.measureText(text);
        viewWidth = getWidth();
        x = textLength;
        start_point = viewWidth + textLength;
        moving_length = viewWidth + textLength * 2;
        y = getTextSize() + getPaddingTop();

    }

    public void setNewText(String text) {
        this.text = text;
        x += (paint.measureText(text) - textLength);
        textLength = paint.measureText(text);
        start_point = viewWidth + textLength;
        moving_length = viewWidth + textLength * 2;
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        if (viewWidth == 0) {
            init();
        }
        canvas.drawText(text, start_point - x, y, paint);
        x += speed;
        if (x > moving_length)
            x = textLength;
        invalidate();
    }
}
