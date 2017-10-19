package james.adaptiveicon;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import james.adaptiveicon.utils.ImageUtils;
import james.adaptiveicon.utils.ResourceUtils;

public class AdaptiveIcon {

    private Drawable fgDrawable;
    private Drawable bgDrawable;
    private Bitmap fgBitmap;
    private Bitmap bgBitmap;
    private double scale;

    /**
     * @param fgDrawable the foreground drawable
     * @param bgDrawable the background drawable, or null if the foreground drawable should not be clipped
     * @param scale      the scale to apply to the clipped drawables (assuming that bgDrawable is not null)
     */
    public AdaptiveIcon(Drawable fgDrawable, Drawable bgDrawable, double scale) {
        this.fgDrawable = fgDrawable;
        this.bgDrawable = bgDrawable;
        this.scale = scale;
    }

    /**
     * @param fgBitmap the foreground bitmap
     * @param bgBitmap the background bitmap, or null if the foreground bitmap should not be clipped
     * @param scale    the scale to apply to the clipped bitmaps (assuming that bgBitmap is not null)
     */
    public AdaptiveIcon(Bitmap fgBitmap, Bitmap bgBitmap, double scale) {
        this.fgBitmap = fgBitmap;
        this.bgBitmap = bgBitmap;
        this.scale = scale;
    }

    /**
     * @param context an active context
     * @return the foreground drawable of the icon
     */
    public Drawable getFgDrawable(Context context) {
        if (fgDrawable == null)
            fgDrawable = new BitmapDrawable(context.getResources(), fgBitmap);
        return fgDrawable;
    }

    /**
     * @param context an active context
     * @return the background drawable of the icon, or null if the foreground should not be clipped
     */
    @Nullable
    public Drawable getBgDrawable(Context context) {
        if (bgDrawable == null)
            bgDrawable = new BitmapDrawable(context.getResources(), bgBitmap);
        return bgDrawable;
    }

    /**
     * @return the foreground bitmap of the icon
     */
    public Bitmap getFgBitmap() {
        if (fgBitmap == null)
            fgBitmap = ImageUtils.drawableToBitmap(fgDrawable);
        return fgBitmap;
    }

    /**
     * @return the background bitmap of the icon, or null if the foreground should not be clipped
     */
    @Nullable
    public Bitmap getBgBitmap() {
        if (bgBitmap == null)
            bgBitmap = ImageUtils.drawableToBitmap(bgDrawable);
        return bgBitmap;
    }

    public double getScale() {
        return scale;
    }

    /**
     * Recycles the bitmaps used in this icon
     */
    public void recycle() {
        fgBitmap.recycle();
        bgBitmap.recycle();
        fgBitmap = null;
        bgBitmap = null;
    }

    public static class Loader {

        private Context context;
        private Fallback fallback;

        /**
         * @param context the active context for the loader to use
         * @return the loader, for method chaining
         */
        public Loader with(Context context) {
            this.context = context;
            return this;
        }

        /**
         * Sets a fallback for the loader to use if an adaptive icon
         * cannot be obtained.
         *
         * @param fallback the fallback to use
         * @return the loader, for method chaining
         */
        public Loader fallback(Fallback fallback) {
            this.fallback = fallback;
            return this;
        }

        /**
         * Loads an adaptive icon.
         *
         * @param info the app to load the icon for
         * @return the adaptive icon, or null if it cannot be obtained
         */
        @Nullable
        public AdaptiveIcon load(ResolveInfo info) {
            if (context == null)
                throw new IllegalStateException("Loader.with(Context) must be called before loading an icon.");

            PackageManager packageManager = context.getPackageManager();
            Drawable background = null, foreground = null;

            try {
                Resources resources = packageManager.getResourcesForApplication(info.activityInfo.packageName);
                Resources.Theme theme = resources.newTheme();
                ResourceUtils.setFakeConfig(resources, Build.VERSION_CODES.O);
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
            }

            if (foreground != null && background != null)
                return new AdaptiveIcon(foreground, background, 0.666);
            else if (fallback != null)
                return fallback.load(context, info);
            else return null;
        }

        //TODO: add RoundIconFallback and IconPackFallback

        public static class LegacyIconFallback extends Fallback {

            private Drawable background;
            @Nullable
            private Boolean shouldClip;
            @Nullable
            private Integer scale;

            public LegacyIconFallback() {
                background = new ColorDrawable(Color.WHITE);
            }

            /**
             * @param backgroundColor the color of the background, as a color int
             * @return the current LegacyIconFallback, for method chaining
             */
            public LegacyIconFallback withBackgroundColor(@ColorInt int backgroundColor) {
                background = new ColorDrawable(backgroundColor);
                return this;
            }

            /**
             * @param background the drawable to use as the background
             * @return the current LegacyIconFallback, for method chaining
             */
            public LegacyIconFallback withBackground(Drawable background) {
                this.background = background;
                return this;
            }

            /**
             * @param shouldClip whether legacy icons should be clipped by the adaptive icon shape, or null to determine this automatically
             * @return the current LegacyIconFallback, for method chaining
             */
            public LegacyIconFallback shouldClip(@Nullable Boolean shouldClip) {
                this.shouldClip = shouldClip;
                return this;
            }

            /**
             * @param scale the scale of the legacy icon, preferably between 1 and 2, but other values greater than 0 work also
             * @return the current LegacyIconFallback, for method chaining
             */
            public LegacyIconFallback withScale(@Nullable Integer scale) {
                this.scale = scale;
                return this;
            }

            @Override
            public AdaptiveIcon load(Context context, ResolveInfo info) {
                Drawable foreground;
                try {
                    foreground = info.loadIcon(context.getPackageManager());
                } catch (Exception e) {
                    if (getFallback() != null)
                        return getFallback().load(context, info);
                    else {
                        if (BuildConfig.DEBUG)
                            Log.e("AdaptiveIcon", "LegacyIconFallback threw \n"
                                    + e.getClass().getName() + ": " + e.getMessage()
                                    + "\n without a fallback, returning null");
                        return null;
                    }
                }

                return new AdaptiveIcon(foreground, shouldClip != null && !shouldClip ? null : background, scale != null ? scale : (ImageUtils.hasTransparency(ImageUtils.drawableToBitmap(foreground)) ? 1.5 : 1));
            }
        }

        public abstract static class Fallback {

            private Fallback fallback;

            /**
             * This method sets a fallback for this fallback to use if it
             * fails to obtain an acceptable AdaptiveIcon.
             *
             * @param fallback the fallback to use if this one fails
             * @return the current Fallback, for method chaining
             */
            public final Fallback withFallback(Fallback fallback) {
                this.fallback = fallback;
                return this;
            }

            final Fallback getFallback() {
                return fallback;
            }

            /**
             * Loads the adaptive icon for this fallback, or null if it cannot
             * be obtained by this fallback or the fallback specified by the
             * 'withFallback' function.
             *
             * @param context the active context
             * @param info    the application to get the icon from
             * @return the adaptive icon for this fallback, or null if it cannot be obtained
             */
            @Nullable
            public abstract AdaptiveIcon load(Context context, ResolveInfo info);

        }

    }

}
