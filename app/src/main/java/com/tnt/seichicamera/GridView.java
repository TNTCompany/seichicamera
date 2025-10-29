package com.tnt.seichicamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class GridView extends View {

    private final Paint paint = new Paint();

    public GridView(Context context) {
        super(context);
        init();
    }

    public GridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(0x80FFFFFF); // 半透明白色
        paint.setStrokeWidth(1.5f);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 画 2 条竖线
        canvas.drawLine(width / 3f, 0, width / 3f, height, paint);
        canvas.drawLine(width * 2 / 3f, 0, width * 2 / 3f, height, paint);

        // 画 2 条横线
        canvas.drawLine(0, height / 3f, width, height / 3f, paint);
        canvas.drawLine(0, height * 2 / 3f, width, height * 2 / 3f, paint);
    }
}