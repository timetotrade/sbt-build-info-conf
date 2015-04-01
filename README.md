# sbt-build-info-conf 

[![Build Status](https://travis-ci.org/Sensatus/sbt-build-info-conf.svg?branch=master)](https://travis-ci.org/Sensatus/sbt-build-info-conf) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.sensatus/sbt-build-info-conf/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.sensatus/sbt-build-info-conf)

This is a simple [SBT](http://www.scala-sbt.org) [AutoPlugin](http://www.scala-sbt.org/0.13/docs/Plugins.html)
that uses [jGit](https://eclipse.org/jgit/) and [Typesafe config](https://github.com/typesafehub/config)
to magically add some build information into the projects reference.conf at package time.

## Usage

Add to your project/plugins.sbt

```scala
addSbtPlugin("com.sensatus" % "sbt-build-info-conf" % "1.0.0")
```
### Details

You might find it useful if you have lots of separate modules within a single project and you'd 
like to know the build details of each one.
 
It places the information in the path:

```
{organization}.buildinfo.{projectname}
```

where organization and projectname are pulled from the existing sbt settings.

It merges any existing reference.conf file found with the new generated details before copying it
over to the packaged jar.


The following values are generated:

 value            |description
------------------|----------------------------------------
username          | The username of the user who built it
time              | The time the project was built
hostname          | The hostname of the machine it was built on
version           | The version of the software
sbtVersion        | The version of sbt used to build
git.branch        | The git branch (still works in detached head mode)
git.describe      | The git describe string
git.commit.author | The git commit author name
git.commit.hash   | The git commit hash
git.commit.time   | The git commit time
git.dirtyFiles    | Which 'dirty' files were in the project and how they have changed (MODIFY, DELETE e.t.c)

### Example output

```json
{
    "com" : {
        "sensatus" : {
            "buildinfo" : {
                "test-project" : {
                    "git" : {
                        "branch" : "master",
                        "commit" : {
                            "author" : "Max Worgan",
                            "hash" : "36ce84ee57f4448a2181295ce3018eeda19549f7",
                            "time" : "Thu Mar 19 14:13:22 GMT 2015"
                        }, 
                        "describe" : "v1.0.0-3-g7a67e51",
                        "dirtyFiles" : {
                            "README.md" : "DELETE",
                            "build.sbt" : "MODIFY",
                            "src/main/scala/com/sensatus/SbtBuildInfoConf.scala" : "MODIFY",
                        }
                    },
                    "hostname" : "maxlaptop",
                    "sbtVersion" : "0.13.7",
                    "time" : "Fri Mar 20 16:44:42 GMT 2015",
                    "username" : "max",
                    "version" : "1.0-SNAPSHOT"
                }
            }
        }
    }
}
```

It was inspired by the more configurable [sbt-build-info](https://github.com/sbt/sbt-buildinfo)
plugin; we needed something that could be included in our projects transparently with no
configuration needed.

