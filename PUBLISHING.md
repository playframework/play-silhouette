# Publishing the Play Silhouette Libraries

***N.B.*** This library has been forked from the (now archived) [play-silhouette](https://github.com/mohiva/play-silhouette), and these steps are intended to be temporary in order to continue development and update packages in the short term. In the longer term, this will be an automated procedure with traceability in GitHub using tools like the [sbt-release plugin](https://github.com/sbt/sbt-release). The overall goal is to move to a continuous deployment culture to ensure there are no bottlenecks.

## Introduction

Play Silhouette is published to Maven Central via Sonatype. This means that the packages are built, signed (using a publically accessible pgp key) and pushed to [Sonatype Nexus](https://s01.oss.sonatype.org/) where it is reflected to Maven Central. The general elements of this process are explained in more detail in the [Sonatype Publishing guide](https://central.sonatype.org/publish/publish-guide/), where this project uses the [sbt tool](https://central.sonatype.org/publish/publish-sbt/) to publish.

## Prerequisites

You will need the following tooling to release play-silhouette, where more details can be found in

- Sonatype OSSRH account, details and signup [here](https://central.sonatype.org/publish/publish-guide/).
- gpg, where details of setting up your environment are [here](https://www.scala-sbt.org/release/docs/Using-Sonatype.html)

## Publish URLS
- [Snapshot](https://s01.oss.sonatype.org/content/repositories/snapshots/)
- [Release](https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/)

## Publish Stages

1. (With `-SNAPSHOT`) Run tests with `sbt + test` to run a cross-platform (Scala 2.12.x and 2.13.x) test suite and ensure that all pass.
2. Publish library artefacts with `sbt + publishSigned` as the final update to the current snapshot.
3. Update the project version number in `project/BuildSettings.scala` to the next version in accordance with [Semantic Versioning](https://semver.org/spec/v2.0.0.html) ensuring that there is no trailing `-SNAPSHOT` to permit a production release.
4. Add version and publishing information to CHANGELOG.md.
5. Commit changes.
6. Tag the release with the updated project version using Git.
7. Push changes and tags to central repository.
8. Release library artefacts with `sbt + publishSigned` to release play-silhouette to the world!
9. Update the project version number in `project/BuildSettings.scala` to the next version with `-SNAPSHOT` appended for the next development releases.
10. Commit changes.
11. Publish library artefacts with `sbt + publishSigned` as the first update to the current snapshot.
12. Push changes to central repository.
