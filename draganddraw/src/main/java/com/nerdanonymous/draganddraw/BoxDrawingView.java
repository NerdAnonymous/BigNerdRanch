package com.nerdanonymous.draganddraw;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BoxDrawingView extends View {

    private static final String PARENT_STATE_KEY = "parent_state_key";
    private static final String BOXES_STATE_KEY = "boxes_state_key";

    private Box mCurrentBox;
    private List<Box> mBoxes = new ArrayList<>();
    private Paint mBoxPaint;
    private Paint mBackgroundPaint;
    private int pointer1 = -1;
    private int pointer2 = -1;
    private float fx, fy, sx, sy, nfx, nfy, nsx, nsy;

    public BoxDrawingView(Context context) {
        this(context, null);
    }

    public BoxDrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(0x22ff0000);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(0xfff8efe0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        PointF current = new PointF(event.getX(), event.getY());

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mCurrentBox = new Box(current);
                mBoxes.add(mCurrentBox);
                pointer1 = event.getPointerId(event.getActionIndex());
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                pointer2 = event.getPointerId(event.getActionIndex());
                fx = event.getX(event.findPointerIndex(pointer1));
                fy = event.getY(event.findPointerIndex(pointer1));
                sx = event.getX(event.findPointerIndex(pointer2));
                sy = event.getY(event.findPointerIndex(pointer2));
                break;
            case MotionEvent.ACTION_MOVE:
                if (-1 != pointer1 && -1 != pointer2) {
                    nfx = event.getX(event.findPointerIndex(pointer1));
                    nfy = event.getY(event.findPointerIndex(pointer1));
                    nsx = event.getX(event.findPointerIndex(pointer2));
                    nsy = event.getY(event.findPointerIndex(pointer2));
                    mCurrentBox.setAngle(angleBetweenLines(fx, fy, sx, sy, nfx, nfy, nsx, nsy));
                }

                if (null != mCurrentBox && -1 != pointer1 && -1 == pointer2) {
                    mCurrentBox.setCurrent(current);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                int upId = event.getPointerId(event.getActionIndex());
                if (pointer1 == upId) {
                    pointer1 = -1;
                }
                if (pointer2 == upId) {
                    pointer2 = -1;
                }
                mCurrentBox = null;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int pointUpId = event.getPointerId(event.getActionIndex());
                if (pointer1 == pointUpId) {
                    pointer1 = -1;
                }
                if (pointer2 == pointUpId) {
                    pointer2 = -1;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                pointer1 = -1;
                pointer2 = -1;
                mCurrentBox = null;
                break;
        }

        return true;
    }

    private float angleBetweenLines(float fx, float fy, float sx, float sy, float nfx, float nfy, float nsx, float nsy) {
        float angle1 = (float) Math.atan2(sy - fy, sx - fx);
        float angle2 = (float) Math.atan2(nsy - nfy, nsx - nfx);

        float calculatedAngle = (float) Math.toDegrees(angle2 - angle1);
        if (calculatedAngle < 0) {
            calculatedAngle += 360;
        }
        return calculatedAngle;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPaint(mBackgroundPaint);

        for (Box box : mBoxes) {
            float left = Math.min(box.getOrigin().x, box.getCurrent().x);
            float right = Math.max(box.getOrigin().x, box.getCurrent().x);
            float top = Math.min(box.getOrigin().y, box.getCurrent().y);
            float bottom = Math.max(box.getOrigin().y, box.getCurrent().y);
            float angle = box.getAngle();
            float px = (box.getOrigin().x + box.getCurrent().x) / 2;
            float py = (box.getOrigin().y + box.getCurrent().y) / 2;

            canvas.save();
            canvas.rotate(angle, px, py);
            canvas.drawRect(left, top, right, bottom, mBoxPaint);
            canvas.restore();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parentState = super.onSaveInstanceState();

        Bundle bundle = new Bundle();
        bundle.putParcelable(PARENT_STATE_KEY, parentState);
        bundle.putParcelableArray(BOXES_STATE_KEY, mBoxes.toArray(new Box[mBoxes.size()]));

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;

        super.onRestoreInstanceState(bundle.getParcelable(PARENT_STATE_KEY));

        Box[] boxes = (Box[]) bundle.getParcelableArray(BOXES_STATE_KEY);
        mBoxes = new ArrayList<>(Arrays.asList(boxes));
    }
}
