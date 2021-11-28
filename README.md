# Silhouette

[![Gitter](https://badges.gitter.im/Join%20Chat.svg?style=for-the-badge)](https://gitter.im/mohiva/play-silhouette)

| Build | Version
| ----- | ----- |
| Scala 2.13.x | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.honeycomb-cheesecake/play-silhouette_2.13/badge.svg?style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/io.github.honeycomb-cheesecake/play-silhouette_2.13) |
| Scala 2.12.x | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.honeycomb-cheesecake/play-silhouette_2.12/badge.svg?style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/io.github.honeycomb-cheesecake/play-silhouette_2.12) |

**Silhouette** is an authentication library for Play Framework applications that supports several authentication methods, including OAuth1, OAuth2, OpenID, CAS, Credentials, Basic Authentication, Two Factor Authentication or custom authentication schemes.

See [the project documentation] for more information.

## Installation

### Latest Production Versions

To get the latest production release(s) from this repository, add the following to your project's `build.sbt` file, replacing `x.x.x` (>= 7.0.1) with the `play-silhouette` version of choice:

```
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette" % "x.x.x"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-cas" % "x.x.x"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-crypto-jca" % "x.x.x"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-password-argon2" % "x.x.x"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-password-bcrypt" % "x.x.x"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-persistence" % "x.x.x"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-totp" % "x.x.x"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-testkit" % "x.x.x" % Test
```

### Latest Snapshot Versions

This fork of `play-silhouette` shall also be releasing SNAPSHOTS of the latest passed builds which can be used for the latest, bleeding-edge features, patches and code fixes prior to official production releases. If you want to pull these for testing with your code in development environments, please ensure that you add the appropriate resolver below, and select the appropriate version with `x.x.x` (>= 7.0.1).

```
resolvers += "snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette" % "x.x.x-SNAPSHOT"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-cas" % "x.x.x-SNAPSHOT"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-crypto-jca" % "x.x.x-SNAPSHOT"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-password-argon2" % "x.x.x-SNAPSHOT"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-password-bcrypt" % "x.x.x-SNAPSHOT"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-persistence" % "x.x.x-SNAPSHOT"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-totp" % "x.x.x-SNAPSHOT"
libraryDependencies += "io.github.honeycomb-cheesecake" %% "play-silhouette-testkit" % "x.x.x-SNAPSHOT" % Test
```

### Legacy Production Versions

The legacy (i.e. not supported by this project) versions of `play-silhouette` are available via the following, and versions of `x.x.x` are available up to `7.0.0`.

```
libraryDependencies += "com.mohiva" %% "play-silhouette" % "x.x.x"
libraryDependencies += "com.mohiva" %% "play-silhouette-cas" % "x.x.x"
libraryDependencies += "com.mohiva" %% "play-silhouette-crypto-jca" % "x.x.x"
libraryDependencies += "com.mohiva" %% "play-silhouette-password-bcrypt" % "x.x.x"
libraryDependencies += "com.mohiva" %% "play-silhouette-persistence" % "x.x.x"
libraryDependencies += "com.mohiva" %% "play-silhouette-testkit" % "x.x.x"
libraryDependencies += "com.mohiva" %% "play-silhouette-totp" % "x.x.x"
```

## Support

If you have question regarding Silhouette, please use the [chat]. **Please do not use the issue tracker for questions!**

## Contribution

Please read the [contributing guide] before you contribute. It contains very useful tips for a successful contribution.

## License

The code is licensed under [Apache License v2.0] and the documentation under [CC BY 3.0].

[the project documentation]: https://silhouette.readme.io/
[chat]: https://gitter.im/mohiva/play-silhouette
[forum]: http://discourse.silhouette.rocks/
[contributing guide]: CONTRIBUTING.md
[Apache License v2.0]: http://www.apache.org/licenses/LICENSE-2.0
[CC BY 3.0]: http://creativecommons.org/licenses/by/3.0/

**This repository is a fork for the [main repository](https://github.com/mohiva/play-silhouette) which is no longer maintained. Thank you very much to previous maintainer Christian Kaps ([@akkie](https://github.com/akkie)) and all contributors for the work you've done to bring us to this point. This library was made fantastic and we are looking to continue along the path you set for us.**
