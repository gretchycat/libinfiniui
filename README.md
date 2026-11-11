# libinfiniui

Shared Android UI, theme/icon handling, viewer dispatch, source adaptation,
external-editor handoff, diagnostics screens and cache/recovery dialogs for
InfiniSuite. This is the single owner of the former `viewer-common` module.
Application screens and decoder implementations live in their respective apps
and libraries. Existing package names are preserved for compatibility.

`ViewerStorage` defaults to libinfinicore's Android local/SAF adapter. Advanced
hosts may set its `factory` before first use and optionally supply a native
streaming source callback. Infinifolder installs its storage graph; consumer
applications do not construct or include that graph. `MediaSourceAdapter`
consumes Android content references from libinfiniselect and third-party sources.
It never treats a foreign provider's opaque document ID as a local filename.

The icon theme is pinned under `themes/icon-theme`. Its builder packages artwork
once in this library's assets and generates its Kotlin registry. Apps do not
bundle independent copies of the same theme source.

## Standalone build

Install JDK 17 or 21, Python 3, Android SDK platform 36 and build tools 35.0.0.
Set `ANDROID_HOME` or use this checkout's `local.properties`.

```sh
python3 scripts/fetch-dependencies.py
./gradlew :ui-android:assembleDebug :ui-android:testDebugUnitTest
```

The dependency lock pins libinfinicore, libinfinistream, libinfiniselect,
libinfinicanvas and libinfiniscribe. The original UI source is preserved in
Infinifolder's history. Tests move with their implementation. Original-code
licensing remains as documented in LICENSE.md; icon/font and dependency notices
must be preserved when distributing applications.

## Separation audit update

The optional ui-image module owns the shared image viewer used by Infinifolder and Infiniscribe. ui-android owns the common UI, source adapter and external-edit handoff. Advanced storage is an injected host capability, not a build dependency.

The parser is tested with synthetic archive fixtures here. The four original Winamp archives and their real-asset regression test belong to Infinimedia; tests never search parent or sibling applications for fixtures.
