libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.7")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.2")

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "4.1.0")
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.15")
