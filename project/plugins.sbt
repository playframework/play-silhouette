addSbtPlugin(dependency = "ch.epfl.scala" % "sbt-version-policy" % "2.1.3")
addSbtPlugin(dependency = "com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin(dependency = "com.github.sbt" % "sbt-unidoc" % "0.5.0")
addSbtPlugin(dependency = "com.jsuereth" % "sbt-pgp" % "2.1.1")
addSbtPlugin(dependency = "com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.2.2")
addSbtPlugin(dependency = "com.timushev.sbt" % "sbt-updates" % "0.6.4")
addSbtPlugin(dependency = "com.typesafe.play" % "sbt-plugin" % "2.9.0")
addSbtPlugin(dependency = "net.vonbuchholtz" % "sbt-dependency-check" % "4.0.0")
addSbtPlugin(dependency = "org.scalariform" % "sbt-scalariform" % "1.8.3")
addSbtPlugin(dependency = "org.scoverage" % "sbt-scoverage" % "2.0.9")
addSbtPlugin(dependency = "org.scoverage" % "sbt-coveralls" % "1.3.11")
addSbtPlugin(dependency = "org.xerial.sbt" % "sbt-sonatype" % "3.10.0")

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
