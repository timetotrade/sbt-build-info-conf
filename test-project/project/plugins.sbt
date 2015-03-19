// build root project
lazy val root = Project("plugins", file(".")) dependsOn sbtbuildinfoconf

lazy val sbtbuildinfoconf = file("..").getAbsoluteFile.toURI