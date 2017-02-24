package com.example.vizard.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RemoteViews;

import java.util.TimeZone;
import java.util.Timer;

/**
 * Created by Vizard on 17/2/24.
 */
public class AnalogClock extends View {

    private Time mCalendar;//记录当前时间

    //存放三张图片资源
    private Drawable mHourHand;
    private  Drawable mMinuteHand;
    private Drawable mDial;

    //用来记录表盘图片的宽度和高度
    //以便帮助我们在onMeasure中确定View的大小，毕竟，我们得view中最大的drawable就是它了

    private  int mDialWidth;
    private  int mDialHeight;

    //用来记录View是否被加入到了Window中，我们在View attached到
//Window时注册监听器，监听时间的变更，并根据时间的变更，改变自己
//的绘制，在View从Window中剥离时，解除注册，因为我们不需要再监听
//时间变更了，没人能看得到我们的View了。
    private  boolean mAttached;

    private float mMinutes;
    private float mHour;

    //用来跟踪我们的View 的尺寸的变化，
//当发生尺寸变化时，我们在绘制自己
//时要进行适当的缩放。
    private  boolean mChanged;

    public AnalogClock(Context context){
        this(context,null);

    }

    public  AnalogClock(Context context, AttributeSet attrs){

        this(context,attrs,0);
    }

    public  AnalogClock(Context context,AttributeSet attrs,int defStyleAttr){

        this(context,attrs,defStyleAttr,0);
    }

    public AnalogClock(Context context,  AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        super(context,attrs,defStyleAttr,defStyleRes);
        final Resources r = context.getResources();
        if (mDial == null) {
            mDial = context.getDrawable(R.drawable.clock_dial);
        }
        if (mHourHand == null) {
            mHourHand = context.getDrawable(R.drawable.clock_hand_hour);
        }
        if (mMinuteHand == null) {
            mMinuteHand =
                    context.getDrawable(R.drawable.clock_hand_minute);
        }

        mCalendar = new Time();

        mDialWidth = mDial.getIntrinsicWidth();
        mDialHeight = mDial.getIntrinsicHeight();

    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize =  MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize =  MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
            vScale = (float )heightSize / (float) mDialHeight;
        }

        float scale = Math.min(hScale, vScale);

        setMeasuredDimension(resolveSizeAndState((int) (mDialWidth * scale), widthMeasureSpec, 0),
                resolveSizeAndState((int) (mDialHeight * scale), heightMeasureSpec, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mChanged = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean changed = mChanged;
        if (changed) {
            mChanged = false;
        }

        int availableWidth = super.getRight() - super.getLeft();
        int availableHeight = super.getBottom() - super.getTop();

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        final Drawable dial = mDial;
        int w = dial.getIntrinsicWidth();
        int h = dial.getIntrinsicHeight();

        boolean scaled = false;

        if (availableWidth < w || availableHeight < h) {
            scaled = true;
            float scale = Math.min((float) availableWidth / (float) w,
                    (float) availableHeight / (float) h);
            canvas.save();
            canvas.scale(scale, scale, x, y);
        }

        if (changed) {
            dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        dial.draw(canvas);

        canvas.save();
        canvas.rotate(mHour / 12.0f * 360.0f, x, y);
        final Drawable hourHand = mHourHand;
        if (changed) {
            w = hourHand.getIntrinsicWidth();
            h = hourHand.getIntrinsicHeight();
            hourHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        hourHand.draw(canvas);
        canvas.restore();

        canvas.save();
        canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);

        final Drawable minuteHand = mMinuteHand;
        if (changed) {
            w = minuteHand.getIntrinsicWidth();
            h = minuteHand.getIntrinsicHeight();
            minuteHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        minuteHand.draw(canvas);
        canvas.restore();

        if (scaled) {
            canvas.restore();
        }
    }

    private void onTimeChanged() {
        mCalendar.setToNow();

        int hour = mCalendar.hour;
        int minute = mCalendar.minute;
        int second = mCalendar.second;

        mMinutes = minute + second / 60.0f;
        mHour = hour + mMinutes / 60.0f;
        mChanged = true;


    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }

            onTimeChanged();

            invalidate();
        }
    };

}
