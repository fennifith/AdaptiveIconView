package james.adaptiveicon.utils;

import android.content.res.Resources;

public class ConversionUtils {

    public static int dpToPx(float dp) {
        return (int) (Resources.getSystem().getDisplayMetrics().density * dp);
    }

    public static float pxToDp(int pixels) {
        return pixels / Resources.getSystem().getDisplayMetrics().density;
    }
}
