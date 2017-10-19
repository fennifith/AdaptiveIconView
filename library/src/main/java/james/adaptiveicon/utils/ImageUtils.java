package james.adaptiveicon.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ImageUtils {

    /**
     * Converts a drawable to a bitmap
     *
     * @param drawable a drawable
     * @return a bitmap
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null)
            return null;

        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null)
                return bitmapDrawable.getBitmap();
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0)
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        else
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Iterates through each pixel in a Bitmap and determines
     * whether it has any transparent parts.
     *
     * @param bitmap a bitmap
     * @return whether the bitmap has transparency
     */
    public static boolean hasTransparency(Bitmap bitmap) {
        for (int y = 0; y < bitmap.getWidth(); y++) {
            for (int x = 0; x < bitmap.getHeight(); x++) {
                if (Color.alpha(bitmap.getPixel(x, y)) < 255)
                    return true;
            }
        }

        return false;
    }

}
