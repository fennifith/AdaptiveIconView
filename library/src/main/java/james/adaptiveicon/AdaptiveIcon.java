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

    private static final String ANDROID_SCHEMA = "http://schemas.android.com/apk/res/android";
    private static final String[] IC_DIRS = new String[]{"mipmap", "drawable"};
    private static final String[] IC_CONFIGS = new String[]{"-anydpi-v26", "-v26", ""};

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

                XmlResourceParser manifestParser = null;
                String iconName = null;
                try {
                    manifestParser = assetManager.openXmlResourceParser("AndroidManifest.xml");
                } catch (Exception e) {
                }

                if (manifestParser != null) {
                    int event;
                    while ((event = manifestParser.getEventType()) != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG && manifestParser.getName().equals("application")) {
                            iconName = resources.getResourceName(manifestParser.getAttributeResourceValue(ANDROID_SCHEMA, "icon", 0));
                            if (iconName.contains("/"))
                                iconName = iconName.split("/")[1];
                            break;
                        }

                        manifestParser.next();
                    }

                    manifestParser.close();
                }

                XmlResourceParser parser = null;
                for (int dir = 0; dir < IC_DIRS.length && parser == null; dir++) {
                    for (int config = 0; config < IC_CONFIGS.length && parser == null; config++) {
                        for (String name : iconName != null && !iconName.equals("ic_launcher") ? new String[]{iconName, "ic_launcher"} : new String[]{"ic_launcher"}) {
                            try {
                                parser = assetManager.openXmlResourceParser("res/" + IC_DIRS[dir] + IC_CONFIGS[config] + "/" + name + ".xml");
                            } catch (Exception e) {
                                continue;
                            }

                            if (parser != null)
                                break;
                        }
                    }
                }

                int backgroundRes = -1, foregroundRes = -1;
                if (parser != null) {
                    int event;
                    while ((event = parser.getEventType()) != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG) {
                            switch (parser.getName()) {
                                case "background":
                                    for (int dir = 0; dir < IC_DIRS.length; dir++) {

                                    }
                                    try {
                                        backgroundRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "drawable", 0);
                                    } catch (Exception e) {
                                        try {
                                            backgroundRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "mipmap", 0);
                                        } catch (Exception e1) {
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
                return new AdaptiveIcon(foreground, background, 0.5);
            else if (fallback != null)
                return fallback.load(context, info);
            else return null;
        }

        //TODO: add IconPackFallback

        public static class RoundIconFallback extends Fallback {

            private Drawable background;
            @Nullable
            private Integer scale;
            private boolean removeShadow = true;

            /**
             * @param backgroundColor the color of the background, as a color int
             * @return the current RoundIconFallback, for method chaining
             */
            public RoundIconFallback withBackgroundColor(@ColorInt int backgroundColor) {
                background = new ColorDrawable(backgroundColor);
                return this;
            }

            /**
             * @param background the drawable to use as the background, or null to find a color automatically
             * @return the current RoundIconFallback, for method chaining
             */
            public RoundIconFallback withBackground(@Nullable Drawable background) {
                this.background = background;
                return this;
            }

            /**
             * @param scale the scale of the legacy icon, preferably between 1 and 2, but other values greater than 0 work also
             * @return the current RoundIconFallback, for method chaining
             */
            public RoundIconFallback withScale(@Nullable Integer scale) {
                this.scale = scale;
                return this;
            }

            /**
             * @param removeShadow whether the shadow (or any other transparent parts) should be removed from the icon
             * @return the current RoundIconFallback, for method chaining
             */
            public RoundIconFallback shouldRemoveShadow(boolean removeShadow) {
                this.removeShadow = removeShadow;
                return this;
            }

            @Nullable
            @Override
            public AdaptiveIcon load(Context context, ResolveInfo info) {
                PackageManager packageManager = context.getPackageManager();
                Drawable roundIcon;

                try {
                    Resources resources = packageManager.getResourcesForApplication(info.activityInfo.packageName);
                    Resources.Theme theme = resources.newTheme();
                    ResourceUtils.setFakeConfig(resources, Build.VERSION_CODES.O);
                    AssetManager assetManager = resources.getAssets();

                    XmlResourceParser manifestParser = null;
                    String iconName = null;
                    try {
                        manifestParser = assetManager.openXmlResourceParser("AndroidManifest.xml");
                    } catch (Exception e) {
                    }

                    if (manifestParser != null) {
                        int event;
                        while ((event = manifestParser.getEventType()) != XmlPullParser.END_DOCUMENT) {
                            if (event == XmlPullParser.START_TAG && manifestParser.getName().equals("application")) {
                                iconName = resources.getResourceName(manifestParser.getAttributeResourceValue(ANDROID_SCHEMA, "roundIcon", 0));
                                if (iconName.contains("/"))
                                    iconName = iconName.split("/")[1];
                                break;
                            }

                            manifestParser.next();
                        }

                        manifestParser.close();
                    }

                    if (iconName != null)
                        Log.d("AdaptiveIcon", "Found a round icon for " + info.activityInfo.packageName + "! " + iconName);

                    try {
                        roundIcon = ResourcesCompat.getDrawable(resources, resources.getIdentifier(iconName, "mipmap", info.activityInfo.packageName), theme);
                    } catch (Resources.NotFoundException e1) {
                        try {
                            roundIcon = ResourcesCompat.getDrawable(resources, resources.getIdentifier(iconName, "drawable", info.activityInfo.packageName), theme);
                        } catch (Resources.NotFoundException e2) {
                            try {
                                roundIcon = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_launcher_round", "mipmap", info.activityInfo.packageName), theme);
                            } catch (Resources.NotFoundException e3) {
                                roundIcon = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_launcher_round", "drawable", info.activityInfo.packageName), theme);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (getFallback() != null)
                        return getFallback().load(context, info);
                    else {
                        if (BuildConfig.DEBUG)
                            Log.e("AdaptiveIcon", "RoundIconFallback threw \n"
                                    + e.getClass().getName() + ": " + e.getMessage()
                                    + "\n without a fallback, returning null");
                        return null;
                    }
                }

                Bitmap fgBitmap = ImageUtils.drawableToBitmap(roundIcon);
                if (removeShadow)
                    fgBitmap = ImageUtils.removeShadow(fgBitmap);

                return new AdaptiveIcon(
                        fgBitmap,
                        ImageUtils.drawableToBitmap(background != null ? background : new ColorDrawable(ImageUtils.getDominantColor(fgBitmap))),
                        scale != null ? scale : 0.8
                );
            }
        }

        public static class LegacyIconFallback extends Fallback {

            private Drawable background;
            @Nullable
            private Boolean shouldClip;
            @Nullable
            private Integer scale;
            private boolean removeShadow = true;

            /**
             * @param backgroundColor the color of the background, as a color int
             * @return the current LegacyIconFallback, for method chaining
             */
            public LegacyIconFallback withBackgroundColor(@ColorInt int backgroundColor) {
                background = new ColorDrawable(backgroundColor);
                return this;
            }

            /**
             * @param background the drawable to use as the background, or null to find a color automatically
             * @return the current LegacyIconFallback, for method chaining
             */
            public LegacyIconFallback withBackground(@Nullable Drawable background) {
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

            /**
             * @param removeShadow whether the shadow (or any other transparent parts) should be removed from the icon
             * @return the current LegacyIconFallback, for method chaining
             */
            public LegacyIconFallback shouldRemoveShadow(boolean removeShadow) {
                this.removeShadow = removeShadow;
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

                Bitmap fgBitmap = ImageUtils.drawableToBitmap(foreground);
                if (removeShadow)
                    fgBitmap = ImageUtils.removeShadow(fgBitmap);

                return new AdaptiveIcon(
                        fgBitmap,
                        shouldClip != null && !shouldClip ? null : ImageUtils.drawableToBitmap(background != null ? background : new ColorDrawable(ImageUtils.getDominantColor(fgBitmap))),
                        scale != null ? scale : (ImageUtils.hasTransparency(fgBitmap) ? 1.25 : 1)
                );
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
