/*
 Copyright 2015 Sensatus UK Ltd

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

resolvers += Resolver.url("bintray-sbt-plugins", url("http://dl.bintray.com/sbt/sbt-plugin-releases"))(
  Resolver.ivyStylePatterns
)

lazy val root = (project in file("."))
  .settings(
    sbtPlugin        := true,
    crossSbtVersions := Vector("0.13.17", "1.1.0"),
    scalaVersion := (CrossVersion partialVersion (sbtVersion in pluginCrossBuild).value match {
      case Some((0, 13)) ⇒ "2.10.7"
      case Some((1, _))  ⇒ "2.12.4"
      case _             ⇒ sys error s"Unhandled sbt version ${(sbtVersion in pluginCrossBuild).value}"
    }),
    name                 := "sbt-build-info-conf",
    organization         := "com.sensatus",
    organizationHomepage := Some(url("http://www.sensatus.com")),
    organizationName     := "Sensatus UK Ltd",
    description          := "SBT AutoPlugin to add build information to reference.conf",
    startYear            := Some(2015),
    licenses             := Seq("Apache-2.0" → url("http://www.opensource.org/licenses/Apache-2.0")),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
    libraryDependencies ++= Seq(
      "com.typesafe.sbt" % "sbt-git" % "0.9.3",
      "com.typesafe"     % "config"  % "1.2.1"
    ),
    addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3"),
    // Publishing details:
    useGpg := true,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/timetotrade/sbt-build-info-config"),
        "git@github.com:timetotrade/sbt-build-info-config.git"
      )
    ),
    publishTo         := sonatypePublishTo.value,
    licenses          := Seq("Apache-2.0" → url("http://www.opensource.org/licenses/Apache-2.0")),
    homepage          := Some(url("http://github.com/timetotrade/sbt-build-info-config")),
    publishMavenStyle := true,
    developers := List(
      Developer("MaxWorgan", "Max Worgan", "max.worgan@sensatus.com", url("http://www.sensatus.com")),
      Developer("DomBlack", "Dominic Black", "dominic.black@sensatus.com", url("http://www.sensatus.com"))
    ),
    scalafmtVersion in ThisBuild   := "1.4.0",
    scalafmtOnCompile in ThisBuild := true
  )
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    // scripted settings
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedRun       := (scriptedRun dependsOn publishLocal).value,
    scriptedBufferLog := false
  )
