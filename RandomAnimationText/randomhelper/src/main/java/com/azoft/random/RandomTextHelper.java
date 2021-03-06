package com.azoft.random;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 10.06.16
 * Time: 13:25
 *
 * @author Artem Zalevskiy
 */
public class RandomTextHelper {

    private static final float HALF = 0.5f;
    private static final int DEFAULT_DELAY = 70;
    private static final Pattern COMPILE = Pattern.compile("%s", Pattern.LITERAL);

    private Paint mPaint;
    private String mTemplate;
    private String mDrawText;
    private boolean mIsFinish;
    private long mDelayMillis;
    private TextView mTextView;
    private final Handler mHandler = new Handler();
    private final Collection<RandomString> mRandomStrings = new ArrayList<>();

    public final void init(final TextView view, final TypedArray typedArray) {
        mTextView = view;
        if (null == mTextView) {
            throw new IllegalArgumentException("TextView should be not null!!!");
        }
        if (null != typedArray) {
            final String rules = typedArray.getString(R.styleable.randomText_rules);
            final String template = typedArray.getString(R.styleable.randomText_template);
            final Boolean autoStart = typedArray.getBoolean(R.styleable.randomText_autoStart, false);

            if (!TextUtils.isEmpty(template) && !TextUtils.isEmpty(rules)) {
                setTemplate(template, rules);
                if (autoStart) {
                    startTextAnimation();
                }
            }
        }

        mTextView.setEnabled(mIsFinish);
        setDelayMillis(DEFAULT_DELAY);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mTextView.getTextSize());
        mPaint.setTypeface(mTextView.getTypeface());
        mPaint.setTextAlign(Paint.Align.LEFT);
        mPaint.setColor(mTextView.getTextColors().getDefaultColor());
    }

    public void setTypeface(final Typeface tf) {
        if (null != mPaint) {
            mPaint.setTypeface(tf);
        }
    }

    public void setDelayMillis(final long delayMillis) {
        mDelayMillis = delayMillis;
    }

    public final void setTemplate(@NonNull final String template, @NonNull final String rules) {
        mTemplate = template;
        mRandomStrings.clear();
        mRandomStrings.addAll(RandomString.parseParams(rules));
    }

    public boolean startTextAnimation(@NonNull final String template, @NonNull final String rules) {
        setTemplate(template, rules);
        return startTextAnimation();
    }

    public final boolean startTextAnimation() {
        if (TextUtils.isEmpty(mTemplate) || mRandomStrings.isEmpty()) {
            return false;
        }
        mIsFinish = false;
        mTextView.setEnabled(false);
        mDrawText = getDrawTextRandom();
        mTextView.requestLayout();

        mHandler.removeCallbacks(mCharacterChange);
        mHandler.postDelayed(mCharacterChange, mDelayMillis);
        return true;
    }

    public void stopAnimation() {
        mIsFinish = true;
        mTextView.setEnabled(true);
        mDrawText = mTextView.getText().toString();
        mTextView.requestLayout();
    }

    public boolean isFinish() {
        return mIsFinish;
    }

    private String getDrawTextRandom() {
        String temp = mTemplate;
        for (final RandomString randomString : mRandomStrings) {
            String s = null;
            switch (randomString.getTypeRandomEnum()) {
                case ALPHA:
                    s = RandomStringUtils.randomAlphabetic(randomString.getLength());
                    break;
                case NUMERIC:
                    s = RandomStringUtils.randomNumeric(randomString.getLength());
                    break;
                case ALPHA_NUMERIC:
                    s = RandomStringUtils.randomAlphanumeric(randomString.getLength());
                    break;
                default:
                    break;
            }
            if (TextUtils.isEmpty(s)) {
                continue;
            }
            temp = COMPILE.matcher(temp).replaceFirst(Matcher.quoteReplacement(s));
        }

        return temp;
    }

    public int getWidthMeasureDrawText() {
        return mTextView.getPaddingLeft() + Math.round(mPaint.measureText(null == mDrawText ? "" : mDrawText)) + 10 + mTextView.getTotalPaddingRight();
    }

    public int getHeightMeasureDrawText() {
        return mTextView.getMeasuredHeight() + 10;
    }

    private int getDrawX() {
        return mTextView.getPaddingLeft();
    }

    private int getDrawY() {
        final float half = (mTextView.getMeasuredHeight() - mTextView.getPaddingBottom()) * HALF;
        return Math.round(half * HALF + half);
    }

    public void onDraw(@NonNull final Canvas canvas) {
        if (!TextUtils.isEmpty(mDrawText)) {
            canvas.drawText(mDrawText, getDrawX(), getDrawY(), mPaint);
        }
    }

    private final Runnable mCharacterChange = new Runnable() {
        @Override
        public void run() {
            if (isFinish()) {
                mTextView.invalidate();
                mHandler.removeCallbacks(mCharacterChange);
            } else {
                mDrawText = getDrawTextRandom();
                mTextView.invalidate();
                mHandler.postDelayed(mCharacterChange, mDelayMillis);
            }
        }
    };

    public Parcelable onSaveInstanceState(final Parcelable superState) {
        final RandomSavedState savedState = new RandomSavedState(superState);
        savedState.mDrawText = mDrawText;
        savedState.mIsFinish = mIsFinish;
        savedState.mDelayMillis = mDelayMillis;

        mTextView.setEnabled(mIsFinish);

        return savedState;
    }

    public Parcelable onRestoreInstanceState(final Parcelable state) {
        if (!(state instanceof RandomSavedState)) {
            return state;
        }

        final RandomSavedState savedState = (RandomSavedState) state;

        mDrawText = savedState.mDrawText;
        mIsFinish = savedState.mIsFinish;
        mDelayMillis = savedState.mDelayMillis;
        return savedState.getSuperState();
    }

    public void resumeAnimation() {
        mHandler.removeCallbacks(mCharacterChange);
        mHandler.post(mCharacterChange);
    }

    public void pauseAnimation() {
        mHandler.removeCallbacks(mCharacterChange);
    }

    static class RandomSavedState extends View.BaseSavedState {

        private String mDrawText;
        private long mDelayMillis;
        private boolean mIsFinish;

        RandomSavedState(final Parcelable superState) {
            super(superState);
        }

        private RandomSavedState(final Parcel in) {
            super(in);
            mIsFinish = 1 == in.readInt();
            mDelayMillis = in.readLong();
            mDrawText = in.readString();
        }

        @Override
        public void writeToParcel(final Parcel out, final int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mIsFinish ? 1 : 0);
            out.writeLong(mDelayMillis);
            out.writeString(mDrawText);
        }

        public static final Creator<RandomSavedState> CREATOR = new Creator<RandomSavedState>() {
            @Override
            public RandomSavedState createFromParcel(final Parcel source) {
                return new RandomSavedState(source);
            }

            @Override
            public RandomSavedState[] newArray(final int size) {
                return new RandomSavedState[size];
            }
        };
    }
}
