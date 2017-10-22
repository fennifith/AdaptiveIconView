package james.adaptiveicon.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * Removes the shadow (and any other transparent parts)
     * from a bitmap.
     *
     * @param bitmap the original bitmap
     * @return the bitmap with the shadow removed
     */
    public static Bitmap removeShadow(Bitmap bitmap) {
        if (!bitmap.isMutable())
            bitmap = bitmap.copy(bitmap.getConfig(), true);

        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < pixels.length; i++) {
            if (Color.alpha(pixels[i]) < 255)
                pixels[i] = Color.TRANSPARENT;
        }

        bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        return bitmap;
    }

    /**
     * Finds the color with the most occurrences inside of a bitmap.
     *
     * @param bitmap the bitmap to get the dominant color of
     * @return the dominant color
     */
    @ColorInt
    public static int getDominantColor(Bitmap bitmap) {
        Map<Integer, Integer> colors = new HashMap<>();

        for (int y = 0; y < bitmap.getWidth(); y++) {
            for (int x = 0; x < bitmap.getHeight(); x++) {
                if (Color.alpha(bitmap.getPixel(x, y)) == 255) {
                    int color = bitmap.getPixel(x, y);
                    colors.put(color, (colors.containsKey(color) ? colors.get(color) : 0) + 1);
                }
            }
        }

        int color = Color.TRANSPARENT;
        int occurrences = 0;
        for (Integer key : colors.keySet()) {
            if (colors.get(key) > occurrences) {
                occurrences = colors.get(key);
                color = key;
            }
        }

        return color;
    }

}
