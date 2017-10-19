package james.adaptiveicon;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.DisplayMetrics;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.InvocationTargetException;

import james.adaptiveicon.utils.ImageUtils;

public class AdaptiveIcon {

    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_SQUIRCLE = 1;
    public static final int SHAPE_ROUNDED_SQUARE = 2;
    public static final int SHAPE_SQUARE = 3;
    public static final int SHAPE_TEARDROP = 4;
    public static final int SHAPE_ANY = 5;

    private Drawable fgDrawable;
    private Drawable bgDrawable;
    private Bitmap fgBitmap;
    private Bitmap bgBitmap;

    public AdaptiveIcon(Drawable fgDrawable, Drawable bgDrawable) {
        this.fgDrawable = fgDrawable;
        this.bgDrawable = bgDrawable;
    }

    public AdaptiveIcon(Bitmap fgBitmap, Bitmap bgBitmap) {
        this.fgBitmap = fgBitmap;
        this.bgBitmap = bgBitmap;
    }

    public Drawable getFgDrawable(Context context) {
        if (fgDrawable == null)
            fgDrawable = new BitmapDrawable(context.getResources(), fgBitmap);
        return fgDrawable;
    }

    public Drawable getBgDrawable(Context context) {
        if (bgDrawable == null)
            bgDrawable = new BitmapDrawable(context.getResources(), bgBitmap);
        return bgDrawable;
    }

    public Bitmap getFgBitmap() {
        if (fgBitmap == null)
            fgBitmap = ImageUtils.drawableToBitmap(fgDrawable);
        return fgBitmap;
    }

    public Bitmap getBgBitmap() {
        if (bgBitmap == null)
            bgBitmap = ImageUtils.drawableToBitmap(bgDrawable);
        return bgBitmap;
    }

    @Nullable
    public static AdaptiveIcon load(Context context, ResolveInfo info) {
        PackageManager packageManager = context.getPackageManager();
        Drawable background = null, foreground = null;

        try {
            Resources resources = packageManager.getResourcesForApplication(info.activityInfo.packageName);
            Resources.Theme theme = resources.newTheme();
            setFakeConfig(resources, Build.VERSION_CODES.O);
            AssetManager assetManager = resources.getAssets();

            XmlResourceParser parser = null;
            for (String type : new String[]{"mipmap", "drawable"}) {
                for (String config : new String[]{"-anydpi-v26", "-v26", ""}) {
                    try {
                        parser = assetManager.openXmlResourceParser("res/" + type + config + "/ic_launcher.xml");
                    } catch (Exception e) {
                        continue;
                    }

                    if (parser != null)
                        break;
                }
            }

            int backgroundRes = -1, foregroundRes = -1;
            if (parser != null) {
                int event;
                while ((event = parser.getEventType()) != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        switch (parser.getName()) {
                            case "background":
                                try {
                                    backgroundRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "drawable", 0);
                                } catch (Exception e) {
                                    try {
                                        backgroundRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "mipmap", 0);
                                    } catch (Exception e1) {
                                        try {
                                            backgroundRes = resources.getIdentifier(info.activityInfo.packageName + ":" + parser.getAttributeValue(null, "android:drawable").substring(1), null, null);
                                        } catch (Exception e2) {
                                        }
                                    }
                                }
                                break;
                            case "foreground":
                                try {
                                    foregroundRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "drawable", 0);
                                } catch (Exception e) {
                                    try {
                                        foregroundRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "mipmap", 0);
                                    } catch (Exception e1) {
                                        try {
                                            foregroundRes = resources.getIdentifier(info.activityInfo.packageName + ":" + parser.getAttributeValue(null, "android:drawable").substring(1), null, null);
                                        } catch (Exception e2) {
                                        }
                                    }
                                }
                                break;
                        }
                    }
                    parser.next();
                }

                parser.close();
            }

            if (background == null && backgroundRes != 0) {
                try {
                    background = ResourcesCompat.getDrawable(resources, backgroundRes, theme);
                } catch (Resources.NotFoundException e) {
                    try {
                        background = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_launcher_background", "mipmap", info.activityInfo.packageName), theme);
                    } catch (Resources.NotFoundException e1) {
                        try {
                            background = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_launcher_background", "drawable", info.activityInfo.packageName), theme);
                        } catch (Resources.NotFoundException e2) {
                            background = new ColorDrawable(Color.WHITE);
                        }
                    }
                }
            }

            if (foreground == null) {
                try {
                    foreground = ResourcesCompat.getDrawable(resources, foregroundRes, theme);
                } catch (Resources.NotFoundException e) {
                    try {
                        foreground = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_launcher_foreground", "mipmap", info.activityInfo.packageName), theme);
                    } catch (Resources.NotFoundException e1) {
                        try {
                            foreground = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_launcher_foreground", "drawable", info.activityInfo.packageName), theme);
                        } catch (Resources.NotFoundException e2) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //return null;
        }

        /*if (foreground == null || background == null) {
            background = new ColorDrawable(Color.WHITE);
            foreground = info.loadIcon(context.getPackageManager());
        }*/

        if (foreground != null && background != null)
            return new AdaptiveIcon(foreground, background);
        else return null;
    }

    private static void setFakeConfig(Resources resources, int sdk) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        int width, height;
        DisplayMetrics metrics = resources.getDisplayMetrics();
        if (metrics.widthPixels >= metrics.heightPixels) {
            width = metrics.widthPixels;
            height = metrics.heightPixels;
        } else {
            width = metrics.heightPixels;
            height = metrics.widthPixels;
        }

        Configuration configuration = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AssetManager.class.getDeclaredMethod("setConfiguration", int.class, int.class, String.class, int.class, int.class,
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                    int.class, int.class, int.class, int.class)
                    .invoke(resources.getAssets(), configuration.mcc, configuration.mnc,
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? configuration.locale.toLanguageTag() : null,
                            configuration.orientation, configuration.touchscreen, configuration.densityDpi,
                            configuration.keyboard, configuration.keyboardHidden, configuration.navigation,
                            width, height, configuration.smallestScreenWidthDp,
                            configuration.screenWidthDp, configuration.screenHeightDp, configuration.screenLayout,
                            configuration.uiMode, configuration.colorMode, sdk);
        } else {
            AssetManager.class.getDeclaredMethod("setConfiguration", int.class, int.class, String.class, int.class, int.class,
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                    int.class, int.class, int.class)
                    .invoke(resources.getAssets(), configuration.mcc, configuration.mnc,
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? configuration.locale.toLanguageTag() : null,
                            configuration.orientation, configuration.touchscreen, configuration.densityDpi,
                            configuration.keyboard, configuration.keyboardHidden, configuration.navigation,
                            width, height, configuration.smallestScreenWidthDp,
                            configuration.screenWidthDp, configuration.screenHeightDp, configuration.screenLayout,
                            configuration.uiMode, sdk);
        }
    }

}
