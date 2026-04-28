package com.neostride.app.feature.coaching;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * 수평 스와이프를 감지하는 LinearLayout
 * 수평 이동이 수직보다 크면 터치를 가로채서 ScrollView로 안 넘어가게 함
 */
public class SwipeDetector extends LinearLayout {

    private float startX, startY;
    private boolean isHorizontalSwipe = false;
    private SwipeListener listener;

    public interface SwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
    }

    public SwipeDetector(Context context) { super(context); }
    public SwipeDetector(Context context, AttributeSet attrs) { super(context, attrs); }
    public SwipeDetector(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }

    public void setSwipeListener(SwipeListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                isHorizontalSwipe = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - startX);
                float dy = Math.abs(ev.getY() - startY);
                // 수평 이동이 수직보다 크면 가로채기
                if (dx > dy && dx > 20) {
                    isHorizontalSwipe = true;
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isHorizontalSwipe && listener != null) {
                    float diffX = ev.getX() - startX;
                    if (Math.abs(diffX) > 80) {
                        if (diffX > 0) {
                            listener.onSwipeRight();
                        } else {
                            listener.onSwipeLeft();
                        }
                    }
                }
                isHorizontalSwipe = false;
                break;
        }
        return true;
    }
}