Contributing to Silhouette
==========================

How to contribute
-----------------

Silhouette is an open source project. Contributions are appreciated.

Some ways in which you can contribute are: reporting errors, improving documentation, adding examples, adding support
for more services, fixing bugs, suggesting new features, adding test cases, translating messages, and whatever else
you can think of that may be helpful. If in doubt, just ask.


Development workflow
--------------------

Development is coordinated via [GitHub]. Ideas for improvements are discussed using the [chat] or historically were discussed using the (now unsupported) [mailing list].

To submit issues, please use the [GitHub issue tracker]. **Please do not use the issue tracker for questions, instead take to the official [chat] where we will be more than happy to help.**

The documentation can be improved by submitting change requests via [readme.io].

For a more streamlined experience for all people involved, we encourage contributors to follow the practices described
at [GitHub workflow for submitting pull requests].

Scala source code should follow the conventions documented in the [Scala Style Guide]. Additionally, acronyms should
be capitalized. To have your code automatically reformatted, run this command before committing your changes:

    scripts/reformat

After submitting your pull request, please [watch the result] of the automated Travis CI build and correct any reported
errors or inconsistencies.


Known issue
---------------------

Please use the scripts under the `scripts` directory to launch sbt.

You'll need sbt-launch 0.13.8 to start the scripts, it's available here: https://scala.jfrog.io/artifactory/ivy-releases/org.scala-sbt/sbt-launch/0.13.8/jars/sbt-launch.jar

If you use SBT 1.5.x, start sbt with ./scripts/sbt -Dsbt.boot.directory=/tmp/boot1 -Dsbt.launcher.coursier=false
If you get a weird issue (cannot redefine component. ID: org.scala-sbt-compiler-interface-0.13.18-bin_2...), remove the /tmp/boot1 directory.


License and Copyright
---------------------

By submitting work via pull requests, issues, documentation, or any other means, contributors indicate their agreement to
publish their work under this project's license and also attest that they are the authors of the work and grant a
copyright license to the Mohiva Organisation, unless the contribution clearly states a different copyright notice
(e.g., it contains original work by a third party).


[GitHub]: https://github.com/playframework/play-silhouette
[GitHub issue tracker]: https://github.com/playframework/play-silhouette/issues
[GitHub workflow for submitting pull requests]: https://www.playframework.com/documentation/2.8.x/WorkingWithGit
[chat]: https://gitter.im/mohiva/play-silhouette
[mailing list]: https://groups.google.com/forum/#!forum/play-silhouette
[Scala Style Guide]: http://docs.scala-lang.org/style/
[readme.io]: https://silhouette.readme.io/
