# README #

This is the repository for the Console Commands mod, an unofficial developer's console for Fractal Softworks' indie space combat game **Starsector**. You can find the official forum thread for this mod, including installation instructions, [here](http://fractalsoftworks.com/forum/index.php?topic=4106.0). The remainder of this readme is for those who wish to contribute to this mod's development.


### Setting up your development environment ###

Console Commands uses [Mercurial](https://www.mercurial-scm.org/) for source control and [Gradle](https://gradle.org/) for building and packaging the mod. The following instructions should be enough to get you started with both.

A Gradle wrapper is included in this repository that will handle downloading and installing Gradle for you. You will need to manually set a few properties, but that will be covered later.

For Mercurial, [TortoiseHg](https://tortoisehg.bitbucket.io/) is recommended for those who prefer a GUI. On Linux, a command line version is usually available under the package name _hg_.

To download and build the mod, follow these steps:

* Install the latest Java Development Kit (JDK) from Oracle's website, found [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html). Source compatibility is set to Java 7, so any JDK >= 7 should work.
* Open your IDE of choice and ensure that a plugin enabling Gradle support is installed. If you are using Eclipse or Netbeans, you may also want to search for and install the Kotlin plugin (IntelliJ includes native Kotlin support). The Kotlin plugin is only required if you intend to work on one of [these files](https://bitbucket.org/LazyWizard/console-commands/src/tip/src/main/kotlin/org/lazywizard/console/?at=default).
* Download the mod's source code using Mercurial. If you are using TortoiseHg, simply right-click an empty folder and choose to clone a repository there using the context menu. The repository URL is `https://bitbucket.org/LazyWizard/console-commands`.
    * If you prefer to use the command line, you can also clone the repository by opening a command prompt or terminal, navigating to your chosen folder, and running `hg clone https://bitbucket.org/LazyWizard/console-commands`.
    * Alternatively, if you don't wish to use Mercurial you can download the repository manually using [this link](https://bitbucket.org/LazyWizard/console-commands/get/tip.zip). Be aware that this will make it _far_ more difficult to submit patches and keep your repository up to date, so it's highly recommended to use Mercurial if you intend to contribute.
* Make a copy of `local.properties.example` in your downloaded repository and name it `local.properties`. Open this file and set the first two properties to point at your Starsector install folder and LazyLib jar location respectively. These properties are used by Gradle to find the necessary libraries to build the mod.

That's it, you should be set and ready to code! Provided you have a Gradle plugin installed, your IDE should be capable of opening the file `build.gradle` as a project. The Gradle task to build the mod is, oddly enough, named 'buildMod'. The generated mod folder will be placed in `build/mod` (this can be changed in local.properties if you wish for it to use your actual mod folder).

If you don't use an IDE, simply run gradlew.bat (on Windows) or ./gradlew (on Linux/Mac) in a command prompt or terminal and Gradle will do all of the work of compiling and assembling the mod for you. As stated above, the generated mod folder will be placed in `build/mod` by default. Note that the very first time you run the Gradle wrapper it will download several hundred megabytes of required libraries, but subsequent builds will be much faster.


### Working with the code ###

Console Commands' codebase is mainly Java, with a few core components written in [Kotlin](http://kotlinlang.org/). The source code can be found in `src/main` with separate directories for each language. All command implementations should be written in Java, but any other class is coder's choice. Classes can freely reference each other regardless of language, with [some](https://kotlinlang.org/docs/reference/java-interop.html) [caveats](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html).

Non-code files such as `mod_info.json` can be found in `src/main/mod`. These will be copied over to the generated mod folder using the `buildMod` task.

Any extra files that should be included in the jar should go in `src/main/resources`. At the moment this only includes this project's license.txt.


### Submitting patches ###

TODO