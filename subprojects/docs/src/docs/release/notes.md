The Gradle team is excited to announce Gradle @version@.

This release features [1](), [2](), ... [n](), and more.

We would like to thank the following community contributors to this release of Gradle:
[Nathan Strong](https://github.com/NathanStrong-Tripwire),
[Roberto Perez Alcolea](https://github.com/rpalcolea),
[Tetsuya Ikenaga](https://github.com/ikngtty),
[Sebastian Schuberth](https://github.com/sschuberth),
[Andrey Mischenko](https://github.com/gildor),
[Alex Saveau](https://github.com/SUPERCILEX),
[Mike Kobit](https://github.com/mkobit),
[Robert Stupp](https://github.com/snazy),
[Nigel Banks](https://github.com/nigelgbanks),
[Sergey Shatunov](https://github.com/Prototik),
[Dan Sănduleac](https://github.com/dansanduleac),
and [Vladimir Sitnikov](https://github.com/vlsi).

<!-- 
Include only their name, impactful features should be called out separately below.
 [Some person](https://github.com/some-person)
-->

<!-- 
## Cancellable custom tasks

When a build is cancelled (e.g. using CTRL+C), the threads executing each task are interrupted.
Task authors only need to make their tasks respond to interrupts in order for the task to be cancellable.

details of 1

## 2

details of 2

## n
-->

## Upgrade Instructions

Switch your build to use Gradle @version@ by updating your wrapper:

`./gradlew wrapper --gradle-version=@version@`

See the [Gradle 5.x upgrade guide](userguide/upgrading_version_5.html#changes_@baseVersion@) to learn about deprecations, breaking changes and other considerations when upgrading to Gradle @version@.

<!-- Do not add breaking changes or deprecations here! Add them to the upgrade guide instead. --> 

### Compatibility Notes

* Java version between 8 and 13 is required to execute Gradle.
Java 6 and 7 are also supported for forked test execution.
Java 14 and later versions are not supported.
* This version of Gradle is tested with Android Gradle Plugin versions 3.4, 3.5 and 3.6.
Earlier AGP versions are not supported.
* Kotlin versions between 1.3.21 and 1.3.50 are tested.
Earlier Kotlin versions are not supported.

## Introducing subgraph constraints for dependency versions

When you declare a dependency to a module that is already on your dependency graph, due to a transitive dependency, you sometimes need to change the version of that module according to your needs.
So far, this was limited to cases where the existing constraint should be limited further (e.g. choosing a specific version from a range).
Version constraints can now made into [subgraph constraints](userguide/declaring_dependency_versions.html#sec:declaring_for_subgraph) by using `forSubgraph()`, which will prompt Gradle to ignore the corresponding version constraints defined further down the graph.
This can, for example, be used to downgrade a version. Subgraph constraints are published to [Gradle Module Metadata](userguide/publishing.html#understanding-gradle-module-md).

```groovy
dependencies {
    implementation('org.apache.hadoop:hadoop-common')
    implementation('commons-io:commons-io')
    constraints {
        // 'hadoop-common:3.2.0' brings in 'commons-io:2.5'
        implementation('org.apache.hadoop:hadoop-common:3.2.0') 
        implementation('commons-io:commons-io:2.4') {
            version { forSubgraph() } // '2.4' takes precedence
        }
    }
}
```

Subgraph constraints can also be defined in a platform.
Theses constraints will be _inherited_ when depending on the platform and treated as if they were defined directly.

```groovy
project(':platform') {
    dependencies {
        constraints {
            api('org.apache.hadoop:hadoop-common:3.2.0') 
            api('commons-io:commons-io:2.4') {
                version { forSubgraph() } 
            }
        }
    }
}

dependencies {
    // 'commons-io:commons-io:2.4' win over '2.5' because the platform defines the constraint as 'forSubgraph()'
    implementation(platform(project(':platform')))
    implementation('org.apache.hadoop:hadoop-common')
    implementation('commons-io:commons-io')
}
```

## Support for Java 13 EA

Gradle now supports running with Java 13 EA (tested with OpenJDK build 13-ea+32).

## More robust file deletion on Windows

Deleting complex file hierarchies on Windows can sometimes be tricky, and errors like `Unable to delete directory ...` can happen at times.
To avoid these errors, Gradle has been employing workarounds in some but not all cases when it had to remove files.
From now on Gradle uses these workarounds every time it removes file hierarchies.
The two most important cases that are now covered are cleaning stale output files of a task, and removing previous outputs before loading fresh ones from the build cache.

## Features for plugin authors

### File and directory property methods

TBD - Added `fileValue()` and `fileProvider()` methods.

### New `ConfigurableFileTree` and `FileCollection` factories

Previously, it was only possible to create a `ConfigurableFileTree` or a fixed `FileCollection` by using the APIs provided by a `Project`.
However, a `Project` object is not always available, for example in a project extension object or a [worker action](userguide/custom_tasks.html#worker_api).

The `ObjectFactory` service now has a [fileTree()](javadoc/org/gradle/api/model/ObjectFactory.html#fileTree--) method for creating `ConfigurableFileTree` instances.
The `Directory` and `DirectoryProperty` types now both have a `files(Object...)` method, respectively [`Directory.files(Object...)`](javadoc/org/gradle/api/file/Directory.html#files-java.lang.Object++...++-) and [`DirectoryProperty.files(Object...)`](javadoc/org/gradle/api/file/DirectoryProperty.html#files-java.lang.Object++...++-), for creating fixed `FileCollection` instances resolving files relatively to the referenced directory.

See the user manual for how to [inject services]((userguide/custom_gradle_types.html#service_injection)) and how to [work with files in lazy properties](userguide/lazy_configuration.html#sec:working_with_files_in_lazy_properties). 

### Injected `FileSystemOperations` and `ExecOperations` services

In the same vein, doing file system operations such as `copy()`, `sync()` and `delete()` or running external processes via `exec()` and `javaexec()` was only possible by using the APIs provided by a `Project`. Two new injectable services now allow to do all that when a `Project` is not available.

See the [user manual](userguide/custom_gradle_types.html#service_injection) for how to inject services and the [`FileSystemOperations`](javadoc/org/gradle/api/file/FileSystemOperations.html) and [`ExecOperations`](javadoc/org/gradle/process/ExecOperations.html) api documentation for more details and examples.

## Promoted features
Promoted features are features that were incubating in previous versions of Gradle but are now supported and subject to backwards compatibility.
See the User Manual section on the “[Feature Lifecycle](userguide/feature_lifecycle.html)” for more information.

The following are the features that have been promoted in this Gradle release.

IDE integration types and APIs. We promoted all API elements in `ide` and `tooling-api` sub-projects that were introduced before Gradle 5.5.
<!--
### Example promoted
-->

## Fixed issues

## Known issues

Known issues are problems that were discovered post release that are directly related to changes made in this release.

## External contributions

We love getting contributions from the Gradle community. For information on contributing, please see [gradle.org/contribute](https://gradle.org/contribute).

## Reporting Problems

If you find a problem with this release, please file a bug on [GitHub Issues](https://github.com/gradle/gradle/issues) adhering to our issue guidelines. 
If you're not sure you're encountering a bug, please use the [forum](https://discuss.gradle.org/c/help-discuss).

We hope you will build happiness with Gradle, and we look forward to your feedback via [Twitter](https://twitter.com/gradle) or on [GitHub](https://github.com/gradle).
