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

## Diagnostics dump folder

The debug-bug button in an app toolbar offloads the current diagnostic log when
tapped. Long-press it to open Diagnostics, where **Choose or change debug dump
folder** sets a writable, persistent Android document-tree destination for that
app. The picker writes a small test log before saving the destination. Folder
access is app-specific; if the document provider cannot grant persistent write
access, Diagnostics reports the error and does not save that destination.

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

## Skin image formats and animation

Logical Winamp sprite roles are independent of their encoded format. Canvas decodes
the artwork, including indexed IFF cycling and static SVG/SVGZ. A skin
may map a role explicitly using `theme.json`:

```json
{"name":"My skin","images":{"main":"faceplate.iff","video":["background.png","title.svg"]}}
```

Layer arrays contain one to four images with identical canvas dimensions. Without
a mapping, `role.bmp` is preferred, then another matching role filename in stable
order. Import limits are 512 entries, 8 MiB per entry and 24 MiB total expanded
archive data. Decoded skin images are bounded to 2 million pixels each and an
8 million pixel cache budget including composite/layer buffers. Invalid mappings
are rejected; unavailable codecs retain the existing visual fallback.

Hosts register `ThemeManager.registerSkinAnimationHost { invalidateArtwork() }`
while visible, and close the returned handle when paused or destroyed. One shared
worker renders skin animations; the last host closing pauses rendering. Theme
changes close old controllers and discard queued results. The common
`ImageAnimationPlayback` scheduler lives in ui-android so skins do not depend on
the optional image-viewer screen. Applications may set `defaultAudioPlayerSkin`
before their first theme lookup; explicit user selections take precedence.

The optional manifest name supplies the displayed skin name. The configured default also supplies fallback artwork when a saved bundled archive is no longer installed, avoiding a dependency on a particular classic skin asset.

SVG/SVGZ artwork renders as still images; custom SVG cycling metadata is ignored. IFF and other supported image animations retain lifecycle-aware playback. The shared scheduler includes rendering/publication time in its 33 ms frame deadline. Palette extraction uses a still-only decode that preserves the original first-frame pixels.

When a user saves a type-wide external-app choice, extension defaults for that same MIME type are replaced. This prevents automatically seeded companion preferences from overriding the new choice. File-specific rules for other files and unrelated MIME types remain intact. These preferences apply inside the host app; Android's system defaults remain separate.

Audio extension identity remains stable when bounded content probing finds no
signature. Concrete content results still take precedence, and an explicit
generic-binary result remains binary. This lets a host route supported audio
formats to the separately installed Infinimedia decoder without relabeling an
inconclusive probe as a failed parse.

## Image thumbnails and displayed navigation order

Explicit Canvas thumbnail MIME aliases, including application/postscript and application/x-krita, are recognized as image presentations. Browser hosts can therefore keep their thumbnails ahead of stale generic/binary icon settings after content identity validation.

ImageNavigationOrder records a browser's already sorted/filtered image rows in an app-private cache snapshot. Only an opaque token is passed to the local image Activity, including Open with's internal-image route. The viewer reads that exact order for next/previous/slideshow; it does not relist or resort a valid snapshot. Snapshots are capped at 8 MiB/100,000 rows and 32 files, preserve opaque URIs and survive Activity recreation. Missing/corrupt snapshots fall back to a stable name-sorted folder list. The snapshot grants no additional URI permissions and is not a cross-application queue protocol.
