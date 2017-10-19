package james.adaptiveicon.utils;

import android.content.res.Resources;

public class ConversionUtils {

    /**
     * Converts density pixels to regular pixels.
     *
     * @param dp density pixels
     * @return regular pixels
     */
    public static int dpToPx(float dp) {
        return (int) (Resources.getSystem().getDisplayMetrics().density * dp);
    }

    /**
     * Converts regular pixels to density pixels.
     *
     * @param pixels regular pixels
     * @return density pixels
     */
    public static float pxToDp(int pixels) {
        return pixels / Resources.getSystem().getDisplayMetrics().density;
    }
}
