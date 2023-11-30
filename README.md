# Silhouette

[![Scala CI](https://github.com/playframework/play-silhouette/actions/workflows/scala.yml/badge.svg)](https://github.com/playframework/play-silhouette/actions/workflows/scala.yml)
[![Coverage Status](https://coveralls.io/repos/github/playframework/play-silhouette/badge.svg?branch=main)](https://coveralls.io/github/honeycomb-cheesecake/play-silhouette?branch=main)
[![Discord](https://img.shields.io/discord/975331299692773447)](https://discord.com/channels/975331299692773447)
[![Gitter](https://img.shields.io/badge/Gitter-Join%20Chat-blue?color=informational)](https://gitter.im/mohiva/play-silhouette)

| Module | Scala 2.13.x | Scala 2.12.x |
| ------ | ------------ | ------------ |
| **play-silhouette** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette_2.13.svg?label=Maven%20Central%202.13&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette_2.13%22) | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette_2.12.svg?label=Maven%20Central%202.12&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette_2.12%22) |
| **play-silhouette-cas** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-cas_2.13.svg?label=Maven%20Central%202.13&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-cas_2.13%22) | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-cas_2.12.svg?label=Maven%20Central%202.12&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-cas_2.12%22) |
| **play-silhouette-crypto-jca** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-crypto-jca_2.13.svg?label=Maven%20Central%202.13&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-crypto-jca_2.13%22) | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-crypto-jca_2.12.svg?label=Maven%20Central%202.12&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-crypto-jca_2.12%22) |
| **play-silhouette-password-argon2** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-password-argon2_2.13.svg?label=Maven%20Central%202.13&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-password-argon2_2.13%22) | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-password-argon2_2.12.svg?label=Maven%20Central%202.12&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-password-argon2_2.12%22) |
| **play-silhouette-password-bcrypt** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-password-bcrypt_2.13.svg?label=Maven%20Central%202.13&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-password-bcrypt_2.13%22) | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-password-bcrypt_2.12.svg?label=Maven%20Central%202.12&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-password-bcrypt_2.12%22) |
| **play-silhouette-persistence** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-persistence_2.13.svg?label=Maven%20Central%202.13&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-persistence_2.13%22) | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-persistence_2.12.svg?label=Maven%20Central%202.12&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-persistence_2.12%22) |
| **play-silhouette-totp** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-totp_2.13.svg?label=Maven%20Central%202.13&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-totp_2.13%22) | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-totp_2.12.svg?label=Maven%20Central%202.12&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-totp_2.12%22) |
| **play-silhouette-testkit** | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-testkit_2.13.svg?label=Maven%20Central%202.13&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-testkit_2.13%22) | [![Maven Central](https://img.shields.io/maven-central/v/io.github.honeycomb-cheesecake/play-silhouette-testkit_2.12.svg?label=Maven%20Central%202.12&style=for-the-badge&logo=scala&color=brightgreen&logoColor=green)](https://search.maven.org/search?q=g:%22io.github.honeycomb-cheesecake%22%20AND%20a:%22play-silhouette-testkit_2.12%22) |

**Silhouette** is an authentication library for Play Framework applications that supports several authentication methods, including OAuth1, OAuth2, OpenID, CAS, Credentials, Basic Authentication, Two Factor Authentication or custom authentication schemes.

See [the project documentation] for more information.

## Installation

### Latest Production Versions

To get the latest production release(s) from this repository, add the following to your project's `build.sbt` file, replacing `x.x.x` (>= 7.0.1) with the `play-silhouette` version of choice:

```
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette" % "x.x.x"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-cas" % "x.x.x"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-crypto-jca" % "x.x.x"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-password-argon2" % "x.x.x"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-password-bcrypt" % "x.x.x"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-persistence" % "x.x.x"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-totp" % "x.x.x"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-testkit" % "x.x.x" % Test
```

### Latest Snapshot Versions

This fork of `play-silhouette` shall also be releasing SNAPSHOTS of the latest passed builds which can be used for the latest, bleeding-edge features, patches and code fixes prior to official production releases. If you want to pull these for testing with your code in development environments, please ensure that you add the appropriate resolver below, and select the appropriate version with `x.x.x` (>= 7.0.1).

```
resolvers += "snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette" % "x.x.x-SNAPSHOT"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-cas" % "x.x.x-SNAPSHOT"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-crypto-jca" % "x.x.x-SNAPSHOT"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-password-argon2" % "x.x.x-SNAPSHOT"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-password-bcrypt" % "x.x.x-SNAPSHOT"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-persistence" % "x.x.x-SNAPSHOT"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-totp" % "x.x.x-SNAPSHOT"
libraryDependencies += "org.playframework.silhouette" %% "play-silhouette-testkit" % "x.x.x-SNAPSHOT" % Test
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

**This repository started life as a fork from the [main repository](https://github.com/mohiva/play-silhouette) but now exists as it's own project. Thank you very much to previous maintainer Christian Kaps ([@akkie](https://github.com/akkie)) and all contributors for the work you've done to bring us to this point. This library was made fantastic and we are looking to continue along the path you set for us.**
