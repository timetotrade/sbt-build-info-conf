libraryDependencies <+= sbtVersion { sv =>
  "org.scala-sbt" % "scripted-plugin" % sv
}
addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")