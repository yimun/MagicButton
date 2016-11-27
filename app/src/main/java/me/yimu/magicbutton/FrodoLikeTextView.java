package me.yimu.magicbutton;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * Created by linwei on 16-10-23.
 * <p>
 * 不带边框的点赞按钮，用于小组话题回复
 * icon抖动效果和放射点效果
 */
public class FrodoLikeTextView extends View implements View.OnClickListener {

    static final String TAG = "FrodoVoteTextView";

    private final int MAIN_GREEN = Color.parseColor("#42bd56");

    private final float POINTS_MAX_RADIUS = dip2px(16);
    private final float POINTS_MIN_RADIUS = dip2px(6);
    private final float POINT_RADIUS = dip2px(1);
    private final float TEXT_MARGIN_LEFT = dip2px(5);

    private final Bitmap ICON_LIKE = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_menu_like);
    private final Bitmap ICON_LIKED = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_menu_liked);

    private Paint mPaint;
    private Status mStatus = Status.UNLIKE;

    private AnimatorSet mPointsAnimatorSet;
    private AnimatorSet mIconVoteAnimatorSet;

    private AnimatorSet mIconUnVoteAnimatorSet;

    private float mPointsRadius;
    private int mPointsAlpha;

    private Bitmap mIcon = ICON_LIKE;
    private float mIconRotation;
    private float mIconScale;

    private int mLikedCount;
    private int mTextSize = UIUtils.sp2px(getContext(), 9);
    private int mTextColor = MAIN_GREEN;
    private Paint mTextPaint;
    private int mAscent;

    private OnLikeListener mListener;
    private boolean mLikable = true;

    public interface OnLikeListener {
        /**
         * 点赞
         */
        void onLike();

        /**
         * 取消赞
         */
        void onCancelLike();
    }

    private enum Status {
        UNLIKE,
        LIKING,
        LIKED,
        UNLIKING,
    }

    public FrodoLikeTextView(Context context) {
        this(context, null);
    }

    public FrodoLikeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrodoLikeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {
        setOnClickListener(this);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(mTextSize);
        resetIcon();

        initPointsAnimatorSet();
        initIconVoteAnimatorSet();
        initIconUnVoteAnimatorSet();
    }

    private void resetIcon() {
        mIconRotation = 0;
        mIconScale = 1.0f;
        mPointsAlpha = 255;
    }

    public Status getStatus() {
        return mStatus;
    }

    /**
     * set final status LIKED,UNLIKE
     *
     * @param status
     */
    public void setStatus(Status status) {
        mStatus = status;
        if (mStatus == Status.UNLIKE) {
            mIcon = ICON_LIKE;
            resetIcon();
        } else if (mStatus == Status.LIKED) {
            mIcon = ICON_LIKED;
            resetIcon();
        }
    }

    public void setOnLikeListener(OnLikeListener listener) {
        mListener = listener;
    }

    public void setLikedCount(int count) {
        mLikedCount = count;
        requestLayout();
    }

    /**
     * 是否可以喜欢
     *
     * @param likable
     */
    public void setLikable(boolean likable) {
        mLikable = likable;
    }

    /**
     * 取消投票，恢复之前的状态
     */
    public void cancelAll() {
        if (mPointsAnimatorSet.isRunning()) {
            mPointsAnimatorSet.cancel();
        }
        if (mIconVoteAnimatorSet.isRunning()) {
            mIconVoteAnimatorSet.cancel();
        }
        if (mIconUnVoteAnimatorSet.isRunning()) {
            mIconUnVoteAnimatorSet.cancel();
        }
    }

    public void startVoteAnimation() {
        mPointsAnimatorSet.start();
        mIconVoteAnimatorSet.start();
    }

    public void startUnVoteAnimation() {
        mIconUnVoteAnimatorSet.start();
    }

    protected void initPointsAnimatorSet() {
        mPointsAnimatorSet = new AnimatorSet();
        ValueAnimator emitAnimator = ValueAnimator.ofFloat(POINTS_MIN_RADIUS, POINTS_MAX_RADIUS);
        emitAnimator.setInterpolator(new DecelerateInterpolator());
        emitAnimator.setDuration(400);
        emitAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPointsRadius = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        ValueAnimator alphaAnimator = ValueAnimator.ofInt(255, 0);
        alphaAnimator.setDuration(400);
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPointsAlpha = (int) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        mPointsAnimatorSet.playSequentially(emitAnimator, alphaAnimator);
        mPointsAnimatorSet.addListener(new Animator.AnimatorListener() {
            boolean isCancel;

            @Override
            public void onAnimationStart(Animator animation) {
                isCancel = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isCancel) {
                    setStatus(Status.LIKED);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isCancel = true;
                setStatus(Status.UNLIKE);
                mLikedCount -= 1;
                requestLayout();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    protected void initIconVoteAnimatorSet() {
        mIconVoteAnimatorSet = new AnimatorSet();
        // 1: 大小从1.0到1.2，时间200ms
        ValueAnimator step1 = ValueAnimator.ofFloat(1.0f, 1.2f);
        step1.setInterpolator(new LinearInterpolator());
        step1.setDuration(200);
        step1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mIconScale = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });

        // 2: 大小从1.0到1.2，时间100ms
        ValueAnimator step2 = ValueAnimator.ofFloat(1.2f, 1.0f);
        step2.setInterpolator(new LinearInterpolator());
        step2.setDuration(100);
        step2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mIconScale = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });

        // 3: 大小从1.0到1.1，时间100ms
        ValueAnimator step3 = ValueAnimator.ofFloat(1.0f, 1.1f);
        step3.setInterpolator(new LinearInterpolator());
        step3.setDuration(100);
        step3.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mIconScale = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });

        // 4: 大小从1.1到1.0，时间100ms
        ValueAnimator step4 = ValueAnimator.ofFloat(1.1f, 1.0f);
        step4.setInterpolator(new LinearInterpolator());
        step4.setDuration(100);
        step4.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mIconScale = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        mIconVoteAnimatorSet.playSequentially(step1, step2, step3, step4);
    }

    protected void initIconUnVoteAnimatorSet() {
        mIconUnVoteAnimatorSet = new AnimatorSet();
        // 1: 大小从1.0到1.2，时间200ms
        ValueAnimator step1 = ValueAnimator.ofFloat(1.0f, 1.2f);
        step1.setInterpolator(new LinearInterpolator());
        step1.setDuration(200);
        step1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mIconScale = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });

        // 2: 大小从1.0到1.2，时间100ms
        ValueAnimator step2 = ValueAnimator.ofFloat(1.2f, 1.0f);
        step2.setInterpolator(new LinearInterpolator());
        step2.setDuration(100);
        step2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mIconScale = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });

        // 3: 大小从1.0到1.1，时间100ms
        ValueAnimator step3 = ValueAnimator.ofFloat(1.0f, 1.1f);
        step3.setInterpolator(new LinearInterpolator());
        step3.setDuration(100);
        step3.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mIconScale = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });

        // 4: 大小从1.1到1.0，时间100ms
        ValueAnimator step4 = ValueAnimator.ofFloat(1.1f, 1.0f);
        step4.setInterpolator(new LinearInterpolator());
        step4.setDuration(100);
        step4.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mIconScale = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        mIconUnVoteAnimatorSet.playSequentially(step1, step2, step3, step4);
        mIconUnVoteAnimatorSet.addListener(new Animator.AnimatorListener() {
            boolean isCancel;

            @Override
            public void onAnimationStart(Animator animation) {
                isCancel = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isCancel) {
                    setStatus(Status.UNLIKE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isCancel = true;
                setStatus(Status.LIKED);
                mLikedCount += 1;
                requestLayout();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        if (!mLikable) {
            return;
        }
        if (mStatus == Status.UNLIKE) {
            mStatus = Status.LIKING;
            mLikedCount += 1;
            mIcon = ICON_LIKED;
            startVoteAnimation();
            if (mListener != null) {
                mListener.onLike();
            }
        } else if (mStatus == Status.LIKED) {
            mStatus = Status.UNLIKING;
            mLikedCount -= 1;
            mIcon = ICON_LIKE;
            startUnVoteAnimation();
            if (mListener != null) {
                mListener.onCancelLike();
            }
        }
        requestLayout();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        // 绘制图标
        drawIcon(canvas);

        // 绘制发射点
        if (mStatus == Status.LIKING) {
            drawEmitPoints(canvas);
        }

        // 绘制文字
        drawText(canvas);

    }

    protected void drawIcon(Canvas canvas) {
        float iconWidth = mIcon.getWidth();
        float iconHeight = mIcon.getHeight();
        canvas.save();
        canvas.translate(POINTS_MAX_RADIUS + POINT_RADIUS - mIcon.getWidth() / 2, (getMeasuredHeight() - iconHeight) / 2);
        Matrix matrix = new Matrix();
        matrix.postRotate(mIconRotation, iconWidth / 2, iconHeight / 2);
        matrix.postScale(mIconScale, mIconScale, iconWidth / 2, iconHeight / 2);
        canvas.drawBitmap(mIcon, matrix, null);
        canvas.restore();
    }

    protected void drawText(Canvas canvas) {
        if (mLikedCount == 0) {
            return;
        }
        String text = String.valueOf(mLikedCount);
        Rect textBounds = new Rect();
        mTextPaint.setColor(mTextColor);
        mTextPaint.getTextBounds(String.valueOf(text), 0, text.length(), textBounds);
        canvas.drawText(text, (POINTS_MAX_RADIUS + POINT_RADIUS) * 2 - dip2px(6),
                textBounds.height(), mTextPaint);
    }

    protected void drawEmitPoints(Canvas canvas) {
        canvas.save();
        canvas.translate(POINTS_MAX_RADIUS + POINT_RADIUS, POINTS_MAX_RADIUS + POINT_RADIUS);
        mPaint.reset();
        mPaint.setColor(MAIN_GREEN);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAlpha(mPointsAlpha);
        for (int d = 0; d < 360 ; d += 45) {
            canvas.drawCircle(mPointsRadius * (float) Math.cos(d / 180f * Math.PI),
                    mPointsRadius * (float) Math.sin(d / 180f * Math.PI), POINT_RADIUS, mPaint);
        }
        canvas.restore();
    }

    private float dip2px(float dipValue) {
        float m = getContext().getResources()
                .getDisplayMetrics().density;
        return (int) (dipValue * m + 0.5F);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            int textWidth = mLikedCount > 0 ? (int) mTextPaint.measureText(
                    String.valueOf(mLikedCount)) : 0;
            result = mIcon.getWidth() + textWidth + getPaddingLeft() + getPaddingRight();
            if (textWidth > 0) {
                result += TEXT_MARGIN_LEFT;
            }
            result = (int) (POINTS_MAX_RADIUS + POINT_RADIUS) * 2 + textWidth;
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        mAscent = (int) mTextPaint.ascent();
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = (int) (-mAscent + mTextPaint.descent()) + getPaddingTop()
                    + getPaddingBottom();
            result = Math.max(result, (int) (POINT_RADIUS + POINTS_MAX_RADIUS) * 2);
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

}
