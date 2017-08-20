package james.adaptiveicon;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;

import java.lang.ref.WeakReference;

public class AdaptiveIconView extends View implements ViewTreeObserver.OnGlobalLayoutListener, View.OnTouchListener {

    public static final int PATH_CIRCLE = 0;
    public static final int PATH_SQUARE = 1;
    public static final int PATH_SQUIRCLE = 2;

    private Bitmap bgBitmap, fgBitmap;
    private Path path;
    private Rect pathSize;

    private Bitmap scaledBgBitmap, scaledFgBitmap;
    private Path scaledPath;

    private int width, height;
    private float x, y;
    private boolean hasChanged;
    private boolean isAdaptive;

    private float bgScale, fgScale;
    private float bgOffsetX, bgOffsetY;
    private float fgOffsetX, fgOffsetY;

    private Paint paint;

    private UpdateThread thread;
    private ValueAnimator animator;

    public AdaptiveIconView(Context context) {
        this(context, null, 0);
    }

    public AdaptiveIconView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AdaptiveIconView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.LTGRAY);

        thread = new UpdateThread(this);
        thread.start();

        getViewTreeObserver().addOnGlobalLayoutListener(this);
        setAdaptive(true);
        setPath(PATH_CIRCLE);

        bgScale = 1;
        fgScale = 1;

        setOnTouchListener(this);
    }

    public void setBitmap(Bitmap bitmap) {
        setBitmap(bitmap, null);
    }

    public void setBitmap(Bitmap bgBitmap, Bitmap fgBitmap) {
        this.bgBitmap = bgBitmap;
        this.fgBitmap = fgBitmap;
        scaledBgBitmap = null;
        scaledFgBitmap = null;
        invalidate();
    }

    public void setDrawable(Drawable drawable) {
        setBitmap(ImageUtils.drawableToBitmap(drawable), null);
    }

    public void setDrawable(Drawable bgDrawable, Drawable fgDrawable) {
        setBitmap(
                ImageUtils.drawableToBitmap(bgDrawable),
                ImageUtils.drawableToBitmap(fgDrawable)
        );
    }

    public Bitmap getBgBitmap() {
        return bgBitmap;
    }

    public Bitmap getFgBitmap() {
        return fgBitmap;
    }

    public void setPath(Rect size, Path path) {
        this.path = path;
        scaledPath = null;
        pathSize = size;
        invalidate();
    }

    public void setPath(int pathType) {
        path = new Path();
        pathSize = new Rect(0, 0, 50, 50);
        switch (pathType) {
            case PATH_CIRCLE:
                path.arcTo(new RectF(pathSize), 0, 359);
                path.close();
                break;
            case PATH_SQUARE:
                break;
            case PATH_SQUIRCLE:
                break;
        }

        invalidate();
    }

    public Path getPath() {
        return path;
    }

    public void setAdaptive(boolean isAdaptive) {
        this.isAdaptive = isAdaptive;
        invalidate();
    }

    public boolean isAdaptive() {
        return isAdaptive;
    }

    private boolean isPrepared() {
        return bgBitmap != null && path != null && pathSize != null;
    }

    private boolean isScaled(int width, int height) {
        return scaledBgBitmap != null && (fgBitmap == null || scaledFgBitmap != null) && scaledPath != null && this.width == width && this.height == height;
    }

    private Path getScaledPath(Path origPath, Rect origRect, int width, int height) {
        Rect newRect = new Rect(0, 0, width, height);
        int origWidth = origRect.right - origRect.left;
        int origHeight = origRect.bottom - origRect.top;

        Matrix matrix = new Matrix();
        matrix.postScale((float) (newRect.right - newRect.left) / origWidth, (float) (newRect.bottom - newRect.top) / origHeight);

        Path newPath = new Path();
        origPath.transform(matrix, newPath);
        return newPath;
    }

    private Bitmap getScaledBitmap(Bitmap bitmap, int width, int height) {
        int x = bitmap.getWidth() / 6;
        int y = bitmap.getHeight() / 6;
        int bmpWidth = (int) (0.666 * bitmap.getWidth());
        int bmpHeight = (int) (0.666 * bitmap.getHeight());

        if (bmpWidth > 0 && bmpHeight > 0)
            bitmap = Bitmap.createBitmap(bitmap, x, y, bmpWidth, bmpHeight);
        return ThumbnailUtils.extractThumbnail(bitmap, width, height);
    }

    private Matrix getBgMatrix(int width, int height) {
        Matrix matrix = new Matrix();
        matrix.postScale(bgScale, bgScale, ((float) width / 2), ((float) height / 2));
        return matrix;
    }

    private Matrix getFgMatrix(int width, int height) {
        Matrix matrix = new Matrix();
        matrix.postScale(fgScale, fgScale, ((float) width / 2), ((float) height / 2));
        return matrix;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isPrepared()) {
            if (!isScaled(canvas.getWidth(), canvas.getHeight())) {
                width = canvas.getWidth();
                height = canvas.getHeight();
                scaledPath = getScaledPath(path, pathSize, width, height);
                scaledBgBitmap = getScaledBitmap(bgBitmap, width, height);
                if (fgBitmap != null)
                    scaledFgBitmap = getScaledBitmap(fgBitmap, width, height);
            }

            if (isAdaptive) {
                canvas.drawPath(scaledPath, paint);
                canvas.clipPath(scaledPath);
                if (scaledBgBitmap != null)
                    canvas.drawBitmap(scaledBgBitmap, getBgMatrix(scaledBgBitmap.getWidth(), scaledBgBitmap.getHeight()), paint);
                if (scaledFgBitmap != null)
                    canvas.drawBitmap(scaledFgBitmap, getFgMatrix(scaledFgBitmap.getWidth(), scaledFgBitmap.getHeight()), paint);
            } else if (bgBitmap != null) {
                canvas.drawBitmap(ThumbnailUtils.extractThumbnail(bgBitmap, width, height), 0, 0, paint);
            }
        }
    }

    @Override
    public void onGlobalLayout() {
        float x = getX();
        float y = getY();
    }

    private float startMoveX;
    private float startMoveY;

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (animator != null && animator.isStarted())
                    animator.cancel();

                animator = ValueAnimator.ofFloat(fgScale, 1.2f);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        float value = (float) valueAnimator.getAnimatedValue();
                        bgScale = ((value - 1) / 2) + 1;
                        fgScale = value;
                        invalidate();
                    }
                });
                animator.start();

                startMoveX = event.getX();
                startMoveY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                fgOffsetX = (startMoveX - event.getX()) / 4;
                bgOffsetX = fgOffsetX / 2;
                fgOffsetY = (startMoveY - event.getY()) / 4;
                bgOffsetY = fgOffsetY / 2;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (animator != null && animator.isStarted())
                    animator.cancel();

                animator = ValueAnimator.ofFloat(fgScale, 1);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        float value = (float) valueAnimator.getAnimatedValue();
                        bgScale = ((value - 1) / 2) + 1;
                        fgScale = value;
                        invalidate();
                    }
                });
                animator.start();

                performClick();
                return false;
        }
        return true;
    }

    private static class UpdateThread extends Thread {

        private WeakReference<AdaptiveIconView> viewReference;

        private UpdateThread(AdaptiveIconView view) {
            viewReference = new WeakReference<>(view);
        }

        @Override
        public void run() {
            while (true) {
                AdaptiveIconView view = viewReference.get();
                if (view != null) {
                    if (view.hasChanged)
                        view.invalidate();
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        return;
                    }
                } else return;
            }
        }
    }
}
