// Comment to get more information during initialization
logLevel := Level.Warn

addSbtPlugin(dependency = "com.typesafe.play" % "sbt-plugin" % "2.8.14")

addSbtPlugin(dependency = "org.scoverage" % "sbt-scoverage" % "1.6.1")

addSbtPlugin(dependency = "org.scoverage" % "sbt-coveralls" % "1.2.7")

addSbtPlugin(dependency = "org.scalariform" % "sbt-scalariform" % "1.6.0")

addSbtPlugin(dependency = "org.xerial.sbt" % "sbt-sonatype" % "0.5.1")

addSbtPlugin(dependency = "com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin(dependency = "com.eed3si9n" % "sbt-unidoc" % "0.4.2")

addSbtPlugin(dependency = "com.typesafe.sbt" % "sbt-site" % "0.8.2")

addSbtPlugin(dependency = "com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")

addSbtPlugin(dependency = "com.dwijnand" % "sbt-travisci" % "1.2.0")
