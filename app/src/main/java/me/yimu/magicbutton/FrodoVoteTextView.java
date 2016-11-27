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
public class FrodoVoteTextView extends View implements View.OnClickListener {

    static final String TAG = "FrodoVoteTextView";

    private final int MAIN_GRAY = Color.parseColor("#cccccc");
    private final int TEXT_GRAY = Color.parseColor("#bcbcbc");

    private final float POINTS_MAX_RADIUS = dip2px(12);
    private final float POINTS_MIN_RADIUS = dip2px(4);
    private final float POINT_RADIUS = dip2px(1);
    private final float TEXT_MARGIN_LEFT = dip2px(5);

    private final Bitmap ICON_VOTE = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_vote);
    private final Bitmap ICON_VOTED = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_voted);

    private Paint mPaint;
    private Status mStatus = Status.UNVOTE;

    private AnimatorSet mPointsAnimatorSet;
    private AnimatorSet mIconVoteAnimatorSet;

    private AnimatorSet mIconUnVoteAnimatorSet;

    private float mPointsRadius;
    private int mPointsAlpha;

    private Bitmap mIcon = ICON_VOTE;
    private float mIconRotation;
    private float mIconScale;

    private int mVotedCount;
    private int mTextSize = UIUtils.sp2px(getContext(), 12);
    private int mTextColor = TEXT_GRAY;
    private Paint mTextPaint;
    private int mAscent;

    private OnVoteListener mListener;
    private boolean mVotable = true;

    public interface OnVoteListener {
        /**
         * 点赞
         */
        void onVote();

        /**
         * 取消赞
         */
        void onCancelVote();
    }

    private enum Status {
        UNVOTE, // 未点赞
        VOTING, // 点赞中
        VOTED, // 已点赞
        UNVOTING, // 取消赞中
    }

    public FrodoVoteTextView(Context context) {
        this(context, null);
    }

    public FrodoVoteTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrodoVoteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
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
     * set final status VOTED,UNVOTE
     *
     * @param status
     */
    public void setStatus(Status status) {
        mStatus = status;
        if (mStatus == Status.UNVOTE) {
            mIcon = ICON_VOTE;
            resetIcon();
        } else if (mStatus == Status.VOTED) {
            mIcon = ICON_VOTED;
            resetIcon();
        }
    }

    public void setOnVoteListener(OnVoteListener listener) {
        mListener = listener;
    }

    public void setVotedCount(int count) {
        mVotedCount = count;
        invalidate();
    }

    /**
     * 是否可以投票
     *
     * @param votable
     */
    public void setVotable(boolean votable) {
        mVotable = votable;
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
                    setStatus(Status.VOTED);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isCancel = true;
                setStatus(Status.UNVOTE);
                mVotedCount -= 1;
                requestLayout();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    protected void initIconVoteAnimatorSet() {
        mIconVoteAnimatorSet = new AnimatorSet();
        // 1: 角度从0-20，时间50ms，数字+1
        ValueAnimator step1 = ValueAnimator.ofFloat(0, 20f);
        step1.setInterpolator(new LinearInterpolator());
        step1.setDuration(50);
        step1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mIconRotation = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        // 2: 角度从20到-20，大小从1.0到1.3，时间200ms，减速运动
        ValueAnimator step2 = ValueAnimator.ofFloat(0, 1.0f);
        step2.setInterpolator(new DecelerateInterpolator());
        step2.setDuration(200);
        step2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mIconRotation = 20f - value * 40f;
                mIconScale = 1.0f + value * 0.3f;
                postInvalidate();
            }
        });

        // 3: 角度从-20到0，大小从1.3到1.0，时间100ms
        ValueAnimator step3 = ValueAnimator.ofFloat(0, 1.0f);
        step3.setInterpolator(new LinearInterpolator());
        step3.setDuration(100);
        step3.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mIconRotation = -20f + value * 20f;
                mIconScale = 1.3f - value * 0.3f;
                postInvalidate();
            }
        });

        // 4： 大小从1.0到1.1，时间100ms
        ValueAnimator step4 = ValueAnimator.ofFloat(1.0f, 1.1f);
        step4.setInterpolator(new LinearInterpolator());
        step4.setDuration(100);
        step4.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mIconScale = value;
                postInvalidate();
            }
        });

        // 5： 大小从1.1到1.0，时间100ms
        ValueAnimator step5 = ValueAnimator.ofFloat(1.1f, 1.0f);
        step5.setInterpolator(new LinearInterpolator());
        step5.setDuration(100);
        step5.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mIconScale = value;
                postInvalidate();
            }
        });
        mIconVoteAnimatorSet.playSequentially(step1, step2, step3, step4, step5);
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
                    setStatus(Status.UNVOTE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isCancel = true;
                setStatus(Status.VOTED);
                mVotedCount += 1;
                requestLayout();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        if (!mVotable) {
            return;
        }
        if (mStatus == Status.UNVOTE) {
            mStatus = Status.VOTING;
            mVotedCount += 1;
            mIcon = ICON_VOTED;
            startVoteAnimation();
            if (mListener != null) {
                mListener.onVote();
            }
        } else if (mStatus == Status.VOTED) {
            mStatus = Status.UNVOTING;
            mVotedCount -= 1;
            mIcon = ICON_VOTE;
            startUnVoteAnimation();
            if (mListener != null) {
                mListener.onCancelVote();
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
        if (mStatus == Status.VOTING) {
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
        if (mVotedCount == 0) {
            return;
        }
        String text = String.valueOf(mVotedCount);
        Rect textBounds = new Rect();
        mTextPaint.setColor(mTextColor);
        mTextPaint.getTextBounds(String.valueOf(text), 0, text.length(), textBounds);
        canvas.drawText(text, (POINTS_MAX_RADIUS + POINT_RADIUS) * 2,
                getMeasuredHeight() / 2 - textBounds.exactCenterY(), mTextPaint);
    }

    protected void drawEmitPoints(Canvas canvas) {
        canvas.save();
        canvas.translate(POINTS_MAX_RADIUS + POINT_RADIUS, POINTS_MAX_RADIUS + POINT_RADIUS);
        mPaint.reset();
        mPaint.setColor(MAIN_GRAY);
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
            int textWidth = mVotedCount > 0 ? (int) mTextPaint.measureText(
                    String.valueOf(mVotedCount)) : 0;
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
