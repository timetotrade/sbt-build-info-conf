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
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}
import ReleaseKeys._

resolvers += Resolver.url("bintray-sbt-plugins", url("http://dl.bintray.com/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

lazy val root = (project in file("."))
  .settings(sonatypeSettings:_*)
  .settings(releaseSettings: _*)
  .settings(publishArtifactsAction := PgpKeys.publishSigned.value)
  .settings(
    sbtPlugin := true,
    name := "sbt-build-info-conf",
    organization := "com.sensatus",
    organizationHomepage := Some(url("http://www.sensatus.com")),
    description := "SBT AutoPlugin to add build information to reference.conf",
    startYear := Some(2015),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
    libraryDependencies ++= Seq(
      "com.typesafe.sbt" % "sbt-git" % "0.9.2",
      "com.typesafe" % "config" % "1.2.1"
    ),
    addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.2"),
    // Publishing details:
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/sensatus/sbt-build-info-config"),
        "git@github.com:sensatus/sbt-build-info-config.git"
      )
    ),
    publishTo := {
      val sonatype = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at sonatype + "content/repositories/snapshots")
      else
        Some("releases"  at sonatype + "service/local/staging/deploy/maven2")
    },
    licenses := Seq("Apache-2.0" -> url("http://www.opensource.org/licenses/Apache-2.0")),
    homepage := Some(url("http://github.com/sensatus/sbt-build-info-config")),
    publishMavenStyle := true,
    developers := List(
      Developer("MaxWorgan","Max Worgan", "max.worgan@sensatus.com",url("http://www.sensatus.com"))
    ),
    // workaround for sbt/sbt#1834
    pomPostProcess := { (node: XmlNode) =>
      new RuleTransformer(new RewriteRule {
        override def transform(node: XmlNode): XmlNodeSeq = node match {
          case e: Elem
            if e.label == "developers" =>
            <developers>
              {developers.value.map { dev =>
              <developer>
                <id>{dev.id}</id>
                <name>{dev.name}</name>
                <email>{dev.email}</email>
                <url>{dev.url}</url>
              </developer>
            }}
            </developers>
          case _ => node
        }
      }).transform(node).head
    }
  )
  .settings(ScriptedPlugin.scriptedSettings:_*)
  .settings(
    // scripted settings
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
    },
    scriptedRun <<= scriptedRun dependsOn publishLocal,
    scriptedBufferLog := false
  )


