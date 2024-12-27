## Compose Multiplatform plugin for IntelliJ Idea.

Still under heavy development!

Currently this plugin needs IntelliJ 2024.3 as minimum. Or Android Studio Meerkat (Canary build)

To run the example:

1. from directory `intellij-plugin`:

* Run command in terminal `./gradlew runIde`.
* Or choose **runIde** configuration in IDE and run it.
  ![ide-run-configuration.png](screenshots/ide-run-configuration.png)

2. create project or open any existing
3. Open a kotlin file
4. Annotate a composable function with the @HotPreview annotation


You could also look at this Sample repository. There you see how to use the @HotPreview annotation.

https://github.com/timo-drick/compose_desktop_dev_challenge
