package me.yimu.magicbutton;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

/**
 * Created by linwei on 16-11-1.
 */

public class RippleButton extends TextView implements View.OnClickListener {

    // Default Value
    public final int DEFAULT_COLOR = Color.parseColor("#cccccc");
    public final int DEFAULT_BORDER_WIDTH = 1;
    public final int DEFAULT_BORDER_RADIUS = UIUtils.dip2px(getContext(), 3);
    public final int DEFAULT_DURATION = 600;
    public final int DEFAULT_BG_COLOR = Color.WHITE;

    private int mBorderColor = DEFAULT_COLOR;
    private int mBorderWidth = DEFAULT_BORDER_WIDTH;
    private int mBorderRadius = DEFAULT_BORDER_RADIUS;
    private int mRippleColor = DEFAULT_COLOR;
    private long mRippleDuration = DEFAULT_DURATION;
    private int mBgColor = DEFAULT_BG_COLOR;

    private PointF mStartPoint;
    private float mRippleRadius;
    private ValueAnimator mRippleAnimator;
    private Path mBorderPath;
    private Paint mPaint;

    private boolean isRippling = false;
    private OnRippleListener mOnRippleListener = null;

    public interface OnRippleListener {
        void onRippleStart();

        void onRippleCancel();

        void onRippleEnd();
    }

    public RippleButton(Context context) {
        this(context, null);
    }

    public RippleButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RippleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.MagicButton, 0, 0);
        if (attr == null) {
            return;
        }

        try {
            mBorderColor = attr.getColor(R.styleable.MagicButton_mgb_borderColor, DEFAULT_COLOR);
            mBorderWidth = attr.getDimensionPixelSize(R.styleable.MagicButton_mgb_borderWidth,
                    DEFAULT_BORDER_WIDTH);
            mBorderRadius = attr.getDimensionPixelSize(R.styleable.MagicButton_mgb_borderRadius,
                    DEFAULT_BORDER_RADIUS);

            mRippleColor = attr.getColor(R.styleable.MagicButton_mgb_rippleColor, DEFAULT_COLOR);
            mRippleDuration = attr.getInteger(R.styleable.MagicButton_mgb_rippleDuration,
                    DEFAULT_DURATION);
            mBgColor = attr.getColor(R.styleable.MagicButton_mgb_bgColor, DEFAULT_BG_COLOR);
        } finally {
            attr.recycle();
        }
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mStartPoint = new PointF(0, 0);
        mBorderPath = new Path();

        this.setOnClickListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 绘制边框背景
        drawRoundBorder(canvas, mBorderWidth, mBorderRadius, mBorderColor);

        // 绘制水波纹效果
        if (isRippling) {
            drawRipple(canvas);
        }
        super.onDraw(canvas);
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
        if (isActivated()) {
            canvas.drawColor(mRippleColor);
        } else {
            canvas.drawColor(mBgColor);
        }
        canvas.restore();
        canvas.drawRoundRect(rectF, radius, radius, mPaint);
    }

    protected void drawRipple(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mRippleColor);
        canvas.save();
        canvas.clipPath(mBorderPath);
        canvas.drawCircle(mStartPoint.x, mStartPoint.y, mRippleRadius, mPaint);
        canvas.restore();
    }

    protected void initRippleAnimator() {
        // 最大半径为水波纹中心到按钮最边角的距离，由勾股定理算出
        float w = Math.max(mStartPoint.x, getMeasuredWidth() - mStartPoint.x);
        float h = Math.max(mStartPoint.y, getMeasuredHeight() - mStartPoint.y);
        float maxRadius = (float) Math.sqrt(Math.pow(w, 2) + Math.pow(h, 2));

        mRippleAnimator = ValueAnimator.ofFloat(UIUtils.dip2px(getContext(), 5), maxRadius);
        mRippleAnimator.setDuration(mRippleDuration);
        mRippleAnimator.setInterpolator(new DecelerateInterpolator());
        mRippleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRippleRadius = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        mRippleAnimator.addListener(new Animator.AnimatorListener() {

            boolean isCanceled;

            @Override
            public void onAnimationStart(Animator animation) {
                isCanceled = false;
                isRippling = true;
                if (mOnRippleListener != null) {
                    mOnRippleListener.onRippleStart();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isCanceled) {
                    setActivated(true);
                    if (mOnRippleListener != null) {
                        mOnRippleListener.onRippleEnd();
                    }
                }
                isRippling = false;

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isCanceled = true;
                isRippling = false;
                if (mOnRippleListener != null) {
                    mOnRippleListener.onRippleCancel();
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    public void setOnRippleListener(OnRippleListener listener) {
        mOnRippleListener = listener;
    }

    public void setRippleStartPos(float startX, float startY) {
        mStartPoint.set(startX, startY);
    }

    public void startRipple() {
        cancelRipple();
        initRippleAnimator();
        mRippleAnimator.start();
    }

    public void cancelRipple() {
        if (mRippleAnimator != null && mRippleAnimator.isRunning()) {
            mRippleAnimator.cancel();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isActivated()) {
                setActivated(false);
            } else {
                setRippleStartPos(event.getX(), event.getY());
                startRipple();
            }

        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onClick(View v) {
 /*       if (isActivated()) {
            setActivated(false);
        } else {
            cancelRipple();
            startRipple();
        }*/
    }
}
