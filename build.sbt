sbtPlugin := true

name := "sbt-build-info-conf"

organization := "com.sensatus"

version := "1.0.0"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "3.7.0.201502260915-r"

libraryDependencies += "com.typesafe" % "config" % "1.2.1"

// scripted settings
ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
}

scriptedRun <<= scriptedRun dependsOn publishLocal

scriptedBufferLog := false

// Publishing details:

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>http://www.sensatus.com</url>
    <licenses>
      <license>
        <name>Apache-2.0</name>
        <url>http://www.opensource.org/licenses/Apache-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:sensatu/sbt-build-info-config.git</url>
      <connection>scm:git:git@github.com:sensatus/sbt-build-info-config.git</connection>
    </scm>
    <developers>
      <developer>
        <id>MaxWorgan</id>
        <name>Max Worgan</name>
        <url>http://www.sensatus.com</url>
      </developer>
    </developers>)
