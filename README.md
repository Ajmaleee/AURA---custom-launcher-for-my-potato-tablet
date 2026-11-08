# AURA — lightweight iOS-inspired launcher for Android 4.4 (API 19)

Target: L8806 tablet, Android 4.4.2, ARMv7, 1024×600 mdpi, ~507 MB RAM.
Plain Java, no AndroidX, no dependencies, no services, no WebView. The APK contains no native code, so it runs on ARMv7.

**Status: source complete, NOT compiled or tested.** The authoring environment had no Android SDK, `javac` or `adb`.
Expect to fix small compile errors on first build. Nothing here has been run on API 19 or on the L8806.

## Layout
- `app/src/main/java/com/aura/launcher/`
  - `LauncherActivity` — the single HOME activity; home, dock, clock, edit mode, and the overlays (App Library, folder, settings), which are created on demand and removed when closed.
  - `HomeView` — paged icon grid (custom `ViewGroup`): paging, swipe-up, long-press, drag to reorder, drop on an icon to make a folder, drop on the dock.
  - `IconView` — one icon + label drawn straight onto a canvas (no nested views). Used by the home grid, dock, library and folders.
  - `IconCache` — lazy 2 MB LRU of icons pre-rendered onto rounded plates, loaded on one background thread, evicted on memory pressure.
  - `Model` — `Repo` (installed apps, queried once and again on package changes), `Item`, `Store` (home layout and dock as JSON in SharedPreferences).
  - `Prefs` — settings in SharedPreferences.
- `preview/index.html` — the approved HTML design preview (design validation only, not part of the APK).
- `build.sh` — optional Gradle-free build.

## Features
Home grid (4–8 columns, portrait uses fewer), 4–5 icon dock, compact clock/date, long-press edit mode with jiggle and remove badges, drag to reorder, folders (create by dropping an icon on another, tap to open, tap title to rename, long-press an item to take it out), App Library (plain icon + name grid, A–Z order, search, tap to launch, long-press to add to Home), swipe left/right between pages, swipe up for the Library, settings panel (columns, icon size, spacing, labels, dock, clock, light/dark, animations on/off, wallpaper picker, reset layout). New installs are added to Home; uninstalled apps are removed.

## Build
Gradle (JDK 11+, Android SDK with any platform ≥ 19; adjust `compileSdkVersion` in `app/build.gradle` to one you have):
```
gradle wrapper        # once; no wrapper jar is included
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```
Without Gradle: `ANDROID_HOME=/path/to/sdk ./build.sh` → `build/aura-debug.apk`.

## Install
```
adb install -r app-debug.apk
```
Press Home and choose **AURA** as the default home app.

## Design and performance choices
- The wallpaper is the system wallpaper (`Theme.Holo.Wallpaper`): AURA holds no wallpaper bitmap. A flat gradient scrim keeps labels legible.
- The real Android status bar is used for Wi-Fi/Bluetooth/battery; the preview's fake status bar is not reproduced.
- No blur, no shadows beyond text, no live wallpaper, no background services. The only receivers are package changes (while the app process lives) and time ticks (registered only between `onStart` and `onStop`).
- Overlays are not kept alive when closed; the icon cache is cleared on `onTrimMemory`.
- Rotation is handled in place (`configChanges`); pagination is recomputed from one flat list, so layouts survive rotation.

## Known limitations
- Untested (see Status). Memory use and API 19 behaviour are unmeasured.
- Dragging between pages is not implemented; use the Library's long-press or the folder/dock drops.
- Native widget hosting, accelerometer wallpaper movement and weather are not implemented.
- Dock contents change by dragging a home icon onto the dock (replaces the nearest slot).
- No custom icon packs; icons sit on a uniform light plate.
- The Home button while AURA is already default returns to page 1 and closes overlays.
