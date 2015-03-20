name := "test-project"

version := "1.0-SNAPSHOT"

organization := "com.sensatus"

libraryDependencies += "com.typesafe" % "config" % "1.2.1"

unmanagedClasspath in Runtime += resourceManaged.value / "main"
