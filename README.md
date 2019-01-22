AdaptiveIconView is a library built to allow an easy implementation of pre-Oreo adaptive icon support in Android launchers. Special thanks to [Lawnchair](https://github.com/LawnchairLauncher/Lawnchair) and its developers for providing the method used to access -v26 assets on pre-Oreo devices. 

For demonstration purposes, sample APKs can be downloaded [here](../../releases).

[![](https://jitpack.io/v/me.jfenn/AdaptiveIconView.svg)](https://jitpack.io/#me.jfenn/AdaptiveIconView)
[![Build Status](https://travis-ci.com/fennifith/AdaptiveIconView.svg)](https://travis-ci.com/fennifith/AdaptiveIconView)
[![Discord](https://img.shields.io/discord/514625116706177035.svg?logo=discord&colorB=7289da)](https://discord.gg/sVtzgbr)

## Screenshots

| Circles | Squircles | Teardrops |
|---------|-----------|-----------|
| ![img](./.github/images/circle.png?raw=true) | ![img](./.github/images/squircle.png?raw=true) | ![img](./.github/images/teardrop.png?raw=true) |

## Usage

### Setup

This project is published on [JitPack](https://jitpack.io/), which you can add to your project by copying the following to your root build.gradle at the end of "repositories".

```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

To add the dependency, copy this line into your app module's build.gradle file.

```gradle
compile 'me.jfenn:AdaptiveIconView:0.0.1'
```

##### Support Libraries

The Android support libraries have been refactored from `android.support.*` to `androidx.*` as detailed [here](https://developer.android.com/topic/libraries/support-library/androidx-overview). As such, AdaptiveIconView only uses the new dependencies. If your project still uses the older support libraries for some reason, you must compile your own version of the library (either refactoring it yourself or using a version from before the change).


### Loading an Icon

The [`AdaptiveIcon`](https://github.com/TheAndroidMaster/AdaptiveIconView/blob/master/library/src/main/java/james/adaptiveicon/AdaptiveIcon.java) class contains multiple ways to load and store the assets required to pass to an [`AdaptiveIconView`](https://github.com/TheAndroidMaster/AdaptiveIconView/blob/master/library/src/main/java/james/adaptiveicon/AdaptiveIconView.java). Most methods involve the `AdaptiveIcon.Loader` subclass, which contains many customization options and several 'fallback' classes for applications passed that do not have an adaptive icon.

#### Method 1: Simple, Easy, and Just About The Worst Thing That You Could Possibly Do

This method will create an asynchronous thread to load the icon for you, but you will need to properly manage it yourself to prevent memory leaks. Unless you don't care about memory leaks and just want to quickly implement something that works, in which case this is perfect for you. Coincidentally, this is the loading method that is used in the sample application.

```java
new AdaptiveIcon.Loader()
  .with(this) //always pass a context before calling any other methods
  .fallback(new AdaptiveIcon.Loader.LegacyIconFallback()) //(optional) specify a fallback to use if there is no adaptive icon or if it is inaccessible to the app
  .loadAsync(resolveInfo, new AdaptiveIcon.Loader.AsyncCallback() { //specify a ResolveInfo of the app to load the icon of
    @Override
    public void onResult(ResolveInfo info, AdaptiveIcon icon) {
      //pass the loaded icon to an AdaptiveIconView
    }
  });
```

However, if you actually care about your end users at all, it would be far better to use the `load(ResolveInfo)` method instead of `loadAsync` and handle the multithreading spaghetti yourself. Please also note that, while it may not make a huge impact on performance, you can call `load` multiple times on the same `Loader` with different `ResolveInfo`s instead of creating a new `Loader` for every single icon that you need to load.

#### Method 2: Do It All Yourself

Ignoring the variety of different loading and fallback methods, all that the `AdaptiveIcon` class really does is store drawables, bitmaps, and a scale for the adaptive icon. With this in mind, you can easily create one yourself.

```java
AdaptiveIcon icon = new AdaptiveIcon(foregroundDrawable, backgroundDrawable, 1.0);
```

One thing to keep in mind here is that the scale (the third parameter passed to the constructor), for some bizarre reason, is actually reversed. Smaller numbers will make the icon larger, and larger numbers will make it smaller. Keep in mind that the scale does not affect the size of the shape of the icon, only what is displayed inside of it.

### Creating a View

The [AdaptiveIconView](https://github.com/TheAndroidMaster/AdaptiveIconView/blob/master/library/src/main/java/james/adaptiveicon/AdaptiveIconView.java) class should be pretty simple to use. The `setIcon` and `getIcon` methods set and get the `AdaptiveIcon` class it uses, updating the view automatically. `setPath(Rect, Path)`, or `setPath(String)` (which accepts a path string within a 100x100 rect) can be used to specify a shape for the icon to clip to, but you may find it easier to use `setPath(int pathType)` instead (provided types are `PATH_CIRCLE`, `PATH_SQUIRCLE`, `PATH_ROUNDED_SQUARE`, `PATH_SQUARE`, and the infamous `PATH_TEARDROP`). `setOffset(x, y)` can be used to quickly offset the drawables inside the icon for fancy movement animations.

By default, the view will set its own touch listener to animate the icon scale when it is clicked. This can be disabled by setting another touch listener (or just passing null). There are currently no methods to scale the icon yourself, but they should be added soon.

### More Fallback Options

In Method 1, the `LegacyIconFallback` is used to tell the loader to create an `AdaptiveIcon` using the legacy icon if there is no adaptive icon for the specified `ResolveInfo`. In addition to this, fallbacks can be chained together - you can create another fallback as a fallback for another fallback that is the fallback for the original loader - and there are many more options that have not yet been covered. Instead of going over all of the options in detail, I will just create a table for each of the fallbacks with short descriptions of each.

#### Fallback (abstract class)

|Method Name|Parameters|Description|
|-----|-----|-----|
|withFallback|Fallback|Sets a fallback to use if there is no icon for this fallback.|
|getFallback||Returns the current fallback, or null if there isn't one.|
|load|Context, ResolveInfo|Loads the icon for this fallback, or this fallback's fallback if it fails.|

#### LegacyIconFallback

|Method Name|Parameters|Description|
|-----|-----|-----|
|withBackgroundColor|@ColorInt int|Sets the background to a new ColorDrawable of the passed color int (defaults to the dominant color of the legacy icon).|
|withBackground|Drawable|Sets the background to the passed Drawable.|
|shouldClip|@Nullable Boolean|Whether legacy icons should be clipped by the shape, or null (the default value) to determine automatically.|
|withScale|@Nullable Double|Specify a custom scale for legacy icons, or null (default) to determine automatically.|
|shouldRemoveShadow|boolean|Whether the shadow and other transparent parts should be removed from the icon - may sometimes result in choppy edges (defaults to false).|

#### RoundIconFallback

|Method Name|Parameters|Description|
|-----|-----|-----|
|withBackgroundColor|@ColorInt int|Sets the background to a new ColorDrawable of the passed color int (defaults to the dominant color of the round icon).|
|withBackground|Drawable|Sets the background to the passed Drawable.|
|withScale|@Nullable Double|Specify a custom scale for round icons, or null (default) to determine automatically.|
|shouldRemoveShadow|boolean|Whether the shadow and other transparent parts should be removed from the icon - may sometimes result in choppy edges (defaults to false).|

#### IconPackFallback

This fallback has not yet been created (see [issue #1](../../issues/1)), but will allow you to pass the package name of an icon pack for the loader to use.
