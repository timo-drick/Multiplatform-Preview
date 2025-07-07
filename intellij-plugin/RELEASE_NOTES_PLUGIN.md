### V 0.9.0
- Migrated to IntelliJ 2025.1 (IJP 251 target)
- Still support IntelliJ 2024.3 (IJP 243 target)
- Removed support for IntelliJ 2024.2 (IJP 242 target)

### V 0.8.0
- Added simulation of WindowInsets like status bar, navigation bar and display cutout
- Fixed problem with gutter icon dialog when changing dark mode.

### V 0.7.1
- Updated preview rendering to Compose Multiplatform 1.8.0  
  (In my tests this now works for both 1.7.3 and 1.8.0 but if you have issues please report them to the GitHub repo)

### V 0.7.0
- Added fit content to zoom control. And improved zoom controls.
- Fixed problem with using WindowInsets. Changed Compose Multiplatform version to 1.7.3
- Added support for custom sourceSet name for JVM target.

### V 0.6.1
- Fixed problem on Windows machines with temp file path in a Gradle init file.

### V 0.6.0
- Tried to fix some stability issues. Now compilation and rendering is synchronized.
- Refactoring of architecture. Hope there are not too many new issues.
- Fixed a problem with transitive dependencies. Now classpath is collected from Gradle task.
- Added `HotPreviewParameterProvider` to provide mock data for previews.
- Fixed problem with gutter icon when no parameters were attached to the `@HotPreview` annotation.

### V 0.5.0
- Added gutter icon to `@HotPreview` annotations with a graphical editor.
- Added horizontal scrolling when a single preview is too wide.
- Added gallery preview mode.
- Fixed stability issues when rendering previews.

### V 0.4.0
- Added shortcut key to recompile.
- Added settings to change Gradle compile parameters and recompiling behavior.
- Improved rendering performance.
- Added foldable sections for preview functions.
- Added support for locale.
- Added support for groups.

### V 0.3.1
- Added support for Android Studio Ladybug and IntelliJ 2024.2.5.

### V 0.3.0
- Improved error handling and visualization.
- Plugin does override the default editor now all the time for Kotlin files.  
  This is necessary because otherwise it is not possible to show preview when the user adds `@HotPreview` annotation.
- Added support for annotation classes. You can now create annotation classes with `@HotPreview` annotations.
- Self-preview is now working. So the source of this plugin can also be previewed.

### V 0.2.0
- Added support for macOS. Windows x64/arm64 should also work (untested).
- Using an independent classloader now. Could solve some issues with resource loading.
- Turned off recompile when opening a file.
- Wait until indexing is finished before analyzing files.
- Replaced `@HotPreview` annotation reflection code analyzer by PSI file analyzer.
- Set `LocalInspectionMode` to true in previews.
- Added navigation to `@HotPreview` annotation on preview click.
- Added on-hover focus for preview items.

### V 0.1.1
- Show errors when trying to compile and render the preview.
