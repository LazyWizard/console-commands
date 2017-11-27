# README #

This is the repository for the Console Commands mod, an unofficial developer's console for Fractal Softworks' indie space combat game **Starsector**. You can find the official forum thread for this mod, including installation instructions, [here](http://fractalsoftworks.com/forum/index.php?topic=4106.0). The remainder of this readme is for those who wish to contribute to this mod's development.


### Setting up your development environment ###

Console Commands uses [Mercurial](https://www.mercurial-scm.org/) for source control and [Gradle](https://gradle.org/) for building and packaging the mod. The following instructions should be enough to get you started with both.

A Gradle wrapper is included in this repository that will handle downloading and installing Gradle for you. You _will_ need to manually set a few properties, but that will be covered later.

For Mercurial, [TortoiseHg](https://tortoisehg.bitbucket.io/) is recommended for those who prefer a GUI. On Linux, a command line version is usually available under the package name _hg_.

To download and build the mod, follow these steps:

* Install the latest Java Development Kit (JDK) from Oracle's website, found [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html). Source compatibility is set to Java 7, so any JDK >= 7 should work.
* Download the mod's source code using Mercurial. If you are using TortoiseHg, simply right-click an empty folder and choose to clone a repository there using the context menu. The repository URL is `https://bitbucket.org/lazywizard/console-commands`.
   * If you prefer to use the command line, you can also clone the repository by opening a command prompt or terminal and running `hg clone https://bitbucket.org/lazywizard/console-commands`.
   * Alternatively, if you don't wish to use Mercurial you can download the repository manually using [this link](https://bitbucket.org/LazyWizard/console-commands/get/tip.zip). Be aware that this will make submitting patches and keeping your repository up to date _far_ more difficult, so it's highly recommended to use Mercurial if you intend to contribute.
* Rename _local.properties.example_ in your downloaded repository to _local.properties_. Open this file and set the first two properties to point at your Starsector install folder and LazyLib jar location respectively. These properties are used by Gradle to find the necessary libraries to build the mod.

That's it, you should be set! Most Java IDEs are capable of opening build.gradle as a project, so you should be able to start coding right away. If your IDE can't open it, check your IDE's available plugins for a Gradle plugin - the big three (IntelliJ, Eclipse, and Netbeans) all include one. The Gradle task to build the mod is, oddly enough, named 'buildMod'.

If you don't use an IDE, simply run gradlew.bat (on Windows) or ./gradlew (on Linux/Mac) in a command prompt or terminal and Gradle will do all of the work of compiling and assembling the mod for you. The generated mod folder will be placed in `build/mod` by default (this can be changed in local.properties). Note that the very first time you run the Gradle wrapper it will download several hundred megabytes of required libraries, but subsequent builds will be _much_ faster.


### Submitting patches ###

TODO