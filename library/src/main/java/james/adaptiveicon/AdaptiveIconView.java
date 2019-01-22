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
import android.media.ThumbnailUtils;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import james.adaptiveicon.utils.PathUtils;

public class AdaptiveIconView extends View implements View.OnTouchListener { //TODO: remove arbitrary calculations, improve bitmap handling

    public static final int PATH_CIRCLE = 0;
    public static final int PATH_SQUIRCLE = 1;
    public static final int PATH_ROUNDED_SQUARE = 2;
    public static final int PATH_SQUARE = 3;
    public static final int PATH_TEARDROP = 4;

    private AdaptiveIcon icon;
    private Path path;
    private Rect pathSize;

    private Bitmap scaledBgBitmap, scaledFgBitmap;
    private Path scaledPath;

    private int width, height;

    private float fgScale = 1;
    private float offsetX, offsetY;

    private Paint paint;

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
        paint.setFilterBitmap(true);

        setPath(PATH_CIRCLE);
        setOnTouchListener(this);
    }

    /**
     * Sets the icon for this view to use. It must contain a foreground image,
     * but the background image is optional - without it, the foreground image
     * will not be clipped and any scaling will not be applied.
     *
     * @param icon the icon for this view to use
     */
    public void setIcon(AdaptiveIcon icon) {
        this.icon = icon;
        scaledFgBitmap = null;
        scaledBgBitmap = null;
        postInvalidate();
    }

    /**
     * Returns the current AdaptiveIcon.
     *
     * @return the current AdaptiveIcon
     */
    public AdaptiveIcon getIcon() {
        return icon;
    }

    /**
     * Sets a custom path for this view to use, providing that its AdaptiveIcon has
     * a background image
     *
     * @param size the bounds of the path, used to scale it to fit the size of the view
     * @param path the custom path
     */
    public void setPath(Rect size, Path path) {
        this.path = path;
        scaledPath = null;
        pathSize = size;
        postInvalidate();
    }

    /**
     * Sets a path for this view to use from one of the presets.
     *
     * @param pathType must be either PATH_CIRCLE, PATH_SQUIRCLE, PATH_ROUNDED_SQUARE, PATH_SQUARE, or PATH_TEARDROP
     */
    public void setPath(int pathType) {
        path = new Path();
        pathSize = new Rect(0, 0, 50, 50);
        switch (pathType) {
            case PATH_CIRCLE:
                path.arcTo(new RectF(pathSize), 0, 359);
                path.close();
                break;
            case PATH_SQUIRCLE:
                setPath("M 50,0 C 10,0 0,10 0,50 C 0,90 10,100 50,100 C 90,100 100,90 100,50 C 100,10 90,0 50,0 Z");
                break;
            case PATH_ROUNDED_SQUARE:
                setPath("M 50,0 L 70,0 A 30,30,0,0 1 100,30 L 100,70 A 30,30,0,0 1 70,100 L 30,100 A 30,30,0,0 1 0,70 L 0,30 A 30,30,0,0 1 30,0 z");
                break;
            case PATH_SQUARE:
                path.lineTo(0, 50);
                path.lineTo(50, 50);
                path.lineTo(50, 0);
                path.lineTo(0, 0);
                path.close();
                break;
            case PATH_TEARDROP:
                setPath("M 50,0 A 50,50,0,0 1 100,50 L 100,85 A 15,15,0,0 1 85,100 L 50,100 A 50,50,0,0 1 50,0 z");
                break;
        }

        postInvalidate();
    }

    /**
     * Sets a custom path from string data.
     *
     * @param pathData the path data string to use
     */
    public void setPath(String pathData) {
        path = PathUtils.createPathFromPathData(pathData);
        pathSize = new Rect(0, 0, 100, 100);
    }

    /**
     * Returns the current path this view is using.
     *
     * @return the current path this view is using
     */
    public Path getPath() {
        return path;
    }

    /**
     * Call this method to offset the icon for animating icon movements
     *
     * @param offsetX the amount to move the icon (reversed) horizontally, between 0 and 1
     * @param offsetY the amount to move the icon (reversed) vertically, between 0 and 1
     */
    public void setOffset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        postInvalidate();
    }

    private boolean isPrepared() {
        return icon != null && path != null && pathSize != null;
    }

    private boolean isScaled(int width, int height) {
        return scaledBgBitmap != null && (icon.getFgBitmap() == null || scaledFgBitmap != null) && scaledPath != null && this.width == width && this.height == height;
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
        double scale = icon.getScale();

        if (scale <= 1)
            return ThumbnailUtils.extractThumbnail(bitmap, (int) ((2 - scale) * width), (int) ((2 - scale) * height));
        else if (bitmap.getWidth() > 1 && bitmap.getHeight() > 1) {
            int widthMargin = (int) ((scale - 1) * width);
            int heightMargin = (int) ((scale - 1) * height);

            if (widthMargin > 0 && heightMargin > 0) {
                Bitmap source = ThumbnailUtils.extractThumbnail(bitmap, (int) ((2 - scale) * width), (int) ((2 - scale) * height));
                int dWidth = width + widthMargin;
                int dHeight = height + heightMargin;
                bitmap = Bitmap.createBitmap(dWidth, dHeight, bitmap.getConfig());
                Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(source, (dWidth - source.getWidth()) / 2, (dHeight - source.getHeight()) / 2, new Paint());
                return bitmap;
            }
        } else if (bitmap.getWidth() > 0 && bitmap.getHeight() > 0)
            return ThumbnailUtils.extractThumbnail(bitmap, width, height);

        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isPrepared()) {
            if (!isScaled(canvas.getWidth(), canvas.getHeight())) {
                width = canvas.getWidth();
                height = canvas.getHeight();
                scaledPath = getScaledPath(path, pathSize, width, height);
                if (icon.getBgBitmap() != null) {
                    scaledBgBitmap = getScaledBitmap(icon.getBgBitmap(), width, height);
                    scaledFgBitmap = getScaledBitmap(icon.getFgBitmap(), width, height);
                } else if (icon.getFgBitmap() != null)
                    scaledFgBitmap = ThumbnailUtils.extractThumbnail(icon.getFgBitmap(), width, height);

            }

            if (scaledBgBitmap != null) {
                canvas.drawPath(scaledPath, paint);
                canvas.clipPath(scaledPath);

                float dx = width * offsetX * 0.066f;
                float dy = height * offsetY * 0.066f;
                if (scaledBgBitmap.getWidth() > width && scaledBgBitmap.getHeight() > height)
                    canvas.scale(2 - ((fgScale + 1) / 2), 2 - ((fgScale + 1) / 2), width / 2, height / 2);
                else {
                    dx = 0;
                    dy = 0;
                }

                float marginX = (scaledBgBitmap.getWidth() - width) / 2;
                float marginY = (scaledBgBitmap.getHeight() - height) / 2;
                canvas.drawBitmap(scaledBgBitmap, dx - marginX, dy - marginY, paint);
            }

            if (scaledFgBitmap != null) {
                canvas.scale(2 - fgScale, 2 - fgScale, width / 2, height / 2);
                float dx = ((width - scaledFgBitmap.getWidth()) / 2) + (width * offsetX * 0.188f);
                float dy = ((height - scaledFgBitmap.getHeight()) / 2) + (height * offsetY * 0.188f);
                canvas.drawBitmap(scaledFgBitmap, dx, dy, paint);
                canvas.scale(fgScale + 1, fgScale + 1, width / 2, height / 2);
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (animator != null && animator.isStarted())
                    animator.cancel();

                animator = ValueAnimator.ofFloat(fgScale, 1.2f, 1);
                animator.setDuration(500);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        fgScale = (float) valueAnimator.getAnimatedValue();
                        postInvalidate();
                    }
                });
                animator.start();

                performClick();
                return false;
        }
        return true;
    }
}
