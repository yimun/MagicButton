package me.yimu.magicbutton;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Layout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.RatingBar;
import android.widget.TextView;

/**
 * Created by linwei on 16-10-23.
 */

public class UIUtils {
        static final String TEST_TEXT = "朋友已走刚升职的你举杯到凌晨还未够用尽心机拉我手缠在我颈背后说你男友有事忙是借口说到终于饮醉酒情侣会走刚失恋的你哭干眼泪前来自首,寂寞因此牵我手.除下了他手信后,何以你今天竟想找寻伴侣";

    public UIUtils() {
    }

    public static int getDisplayWidth(Context context) {
        if(null == context) {
            return 0;
        } else {
            WindowManager wm = (WindowManager)context.getSystemService("window");
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            return metrics.widthPixels;
        }
    }

    public static int getDisplayHeight(Context context) {
        WindowManager wm = (WindowManager)context.getSystemService("window");
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    public static float getDisplayDensity(Context context) {
        WindowManager wm = (WindowManager)context.getSystemService("window");
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.density;
    }

    public static final int getStatusBarHeight(Activity activity) {
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        return frame.top;
    }

    public static int px2dip(Context context, float pxValue) {
        float m = context.getResources().getDisplayMetrics().density;
        return (int)(pxValue / m + 0.5F);
    }

    public static int px2sp(Context context, float pxValue) {
        float m = context.getResources().getDisplayMetrics().scaledDensity;
        return (int)(pxValue / m + 0.5F);
    }

    public static int dip2px(Context context, float dipValue) {
        float m = context.getResources().getDisplayMetrics().density;
        return (int)(dipValue * m + 0.5F);
    }

    public static int sp2px(Context context, float pxValue) {
        float m = context.getResources().getDisplayMetrics().scaledDensity;
        return (int)(pxValue * m + 0.5F);
    }

    public static final int textCountPerLine(Paint paint, int maxWidth) {
        return paint.breakText("朋友已走刚升职的你举杯到凌晨还未够用尽心机拉我手缠在我颈背后说你男友有事忙是借口说到终于饮醉酒情侣会走刚失恋的你哭干眼泪前来自首,寂寞因此牵我手.除下了他手信后,何以你今天竟想找寻伴侣", 0, "朋友已走刚升职的你举杯到凌晨还未够用尽心机拉我手缠在我颈背后说你男友有事忙是借口说到终于饮醉酒情侣会走刚失恋的你哭干眼泪前来自首,寂寞因此牵我手.除下了他手信后,何以你今天竟想找寻伴侣".length(), true, (float)maxWidth, (float[])null);
    }

    public static final int textCountPerLine(TextView textView, int maxWidth) {
        return textCountPerLine((Paint)textView.getPaint(), maxWidth);
    }

    public static boolean isTextViewEllipsized(TextView textView) {
        Layout layout = textView.getLayout();
        if(layout != null) {
            int lines = layout.getLineCount();
            if(lines > 0) {
                int ellipsisCount = layout.getEllipsisCount(lines - 1);
                if(ellipsisCount > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public static float getTextViewLength(TextView textView, String text) {
        TextPaint paint = textView.getPaint();
        return paint.measureText(text);
    }

    public static int getWordCount(String s) {
        int length = 0;

        for(int i = 0; i < s.length(); ++i) {
            int ascii = Character.codePointAt(s, i);
            if(ascii >= 0 && ascii <= 255) {
                ++length;
            } else {
                length += 2;
            }
        }

        return length;
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int color, int cornerDips, int borderDips, Context context) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        int borderSizePx = (int) TypedValue.applyDimension(1, (float)borderDips, context.getResources().getDisplayMetrics());
        int cornerSizePx = (int)TypedValue.applyDimension(1, (float)cornerDips, context.getResources().getDisplayMetrics());
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        paint.setColor(-1);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, (float)cornerSizePx, (float)cornerSizePx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth((float)borderSizePx);
        canvas.drawRoundRect(rectF, (float)cornerSizePx, (float)cornerSizePx, paint);
        return output;
    }

    public static void setRatingBar(RatingBar ratingBar, int max, int average) {
        ratingBar.setNumStars(5);
        ratingBar.setIsIndicator(true);
        ratingBar.setMax(max);
        ratingBar.setRating((float)(average * ratingBar.getNumStars() / max));
    }

    public static Drawable tintDrawable(Drawable drawable, ColorStateList colors) {
        Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTintList(wrappedDrawable, colors);
        return wrappedDrawable;
    }

}
