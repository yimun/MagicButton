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
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * Created by linwei on 16-10-23.
 * <p>
 * 带边框的点赞按钮，用于话题和广播
 * 水波纹按钮和icon抖动效果
 */
public class FrodoVoteButton extends View implements View.OnClickListener {

    static final String TAG = "FrodoVoteButton";

    private final int MAIN_GRAY = Color.parseColor("#cccccc");
    private final int TEXT_GRAY = Color.parseColor("#bcbcbc");

    private int mBgColor;

    private Paint mPaint;
    private Status mStatus = Status.UNVOTE;

    private ValueAnimator mRippleAnimator;
    private PointF mRippleStartPoint = new PointF();
    private float mRippleRadius = -1;
    private Path mBorderPath = new Path();

    private AnimatorSet mIconVoteAnimatorSet;
    private AnimatorSet mIconUnVoteAnimatorSet;

    private Bitmap mIcon;
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

    public FrodoVoteButton(Context context) {
        this(context, null);
    }

    public FrodoVoteButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrodoVoteButton(Context context, AttributeSet attrs, int defStyleAttr) {
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

        mIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_vote);
        resetIcon();
        initIconVoteAnimatorSet();
        initIconUnVoteAnimatorSet();
    }

    private void resetIcon() {
        mIconRotation = 0;
        mIconScale = 1.0f;
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
            mBgColor = Color.WHITE;
            mTextColor = TEXT_GRAY;
            resetIcon();
        } else if (mStatus == Status.VOTED) {
            mBgColor = MAIN_GRAY;
            mTextColor = Color.WHITE;
            resetIcon();
        }
    }

    public void setOnVoteListener(OnVoteListener listener) {
        mListener = listener;
    }

    public void setVotedCount(int count) {
        mVotedCount = count;
        requestLayout();
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
        if (mRippleAnimator.isRunning()) {
            mRippleAnimator.cancel();
        }
        if (mIconVoteAnimatorSet.isRunning()) {
            mIconVoteAnimatorSet.cancel();
        }
        if (mIconUnVoteAnimatorSet.isRunning()) {
            mIconUnVoteAnimatorSet.cancel();
        }
    }

    public void startVoteAnimation() {
        mTextColor = Color.WHITE;
        // 由于ripple的半径计算依赖于view的宽度，这里先重新measure再开始动画，需要post
        post(new Runnable() {
            @Override
            public void run() {
                mRippleAnimator.start();
            }
        });
        mIconVoteAnimatorSet.start();
    }

    public void startUnVoteAnimation() {
        mBgColor = Color.WHITE;
        mTextColor = TEXT_GRAY;
        mIconUnVoteAnimatorSet.start();
    }

    protected void initRippleAnimator() {
        // 水波纹中心定死在icon的中心位置
        mRippleStartPoint.set(getPaddingLeft() + mIcon.getWidth() / 2, getMeasuredHeight() / 2);
        // 最大半径为水波纹中心到按钮最右上角的距离，由勾股定理算出
        int maxRadius = (int) Math.sqrt(
                Math.pow(getMeasuredWidth() - mRippleStartPoint.x, 2) +
                Math.pow(mRippleStartPoint.y, 2));

        mRippleAnimator = ValueAnimator.ofFloat(dip2px(5f), maxRadius);
        mRippleAnimator.setDuration(400);
        mRippleAnimator.setInterpolator(new DecelerateInterpolator());
        mRippleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRippleRadius = (float) animation.getAnimatedValue();
                postInvalidate();
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
        mIconVoteAnimatorSet.addListener(new Animator.AnimatorListener() {
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
            requestLayout();
            startVoteAnimation();
            if (mListener != null) {
                mListener.onVote();
            }
        } else if (mStatus == Status.VOTED) {
            mStatus = Status.UNVOTING;
            mVotedCount -= 1;
            requestLayout();
            startUnVoteAnimation();
            if (mListener != null) {
                mListener.onCancelVote();
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        // 绘制边框背景
        drawRoundBorder(canvas, 1, dip2px(2), MAIN_GRAY);

        // 绘制水波纹效果
        if (mStatus == Status.VOTING) {
            drawRipple(canvas);
        }
        // 绘制图标
        drawIcon(canvas);

        // 绘制文字
        drawText(canvas);

    }

    protected void drawRoundBorder(Canvas canvas, float width, float radius, int color) {
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(width);
        float padding = width / 2;
        RectF rectF = new RectF(padding, padding, (float) getMeasuredWidth() - padding,
                (float) getMeasuredHeight() - padding);
        mBorderPath.reset();
        mBorderPath.addRoundRect(rectF, radius, radius, Path.Direction.CCW);
        canvas.save();
        canvas.clipPath(mBorderPath);
        canvas.drawColor(mBgColor);
        canvas.restore();

        canvas.drawRoundRect(rectF, radius, radius, mPaint);
    }

    protected void drawRipple(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(MAIN_GRAY);
        canvas.save();
        canvas.clipPath(mBorderPath);
        canvas.drawCircle(mRippleStartPoint.x, mRippleStartPoint.y, mRippleRadius, mPaint);
        canvas.restore();
    }

    protected void drawIcon(Canvas canvas) {
        float iconWidth = mIcon.getWidth();
        float iconHeight = mIcon.getHeight();
        canvas.save();
        canvas.translate(getPaddingLeft(), (getMeasuredHeight() - iconHeight) / 2);
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
        canvas.drawText(text, getPaddingLeft() + mIcon.getWidth() + dip2px(6),
                getMeasuredHeight() / 2 - textBounds.exactCenterY(), mTextPaint);
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
        initRippleAnimator();
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
                int textMarginLeft = (int) dip2px(6);
                result += textMarginLeft;
            }
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
            result = Math.max(result, mIcon.getHeight());
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

}
