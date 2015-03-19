# sbt-build-info-config

This is a simple [SBT](http://www.scala-sbt.org) [AutoPlugin](http://www.scala-sbt.org/0.13/docs/Plugins.html)
that uses [jGit](https://eclipse.org/jgit/) and [Typesafe config](https://github.com/typesafehub/config)
to magically add some build information into the projects reference.conf at compile time.

## Usage

Add to your project/plugins.sbt

```scala
addSbtPlugin("com.sensatus" % "sbt-build-info-config" % "1.0.0")
```
### Details

You might find it useful if you have lots of separate modules within a single project and you'd 
like to know the build details of each one.
 
It places the information in the path:

```
{organisation}.buildinfo.{projectname}
```

It currently extracts:

* The time the project was built 
* The user who built it
* The hostname of the machine it was built on
* The git branch (still works in detached head mode)
* The git commit author name
* The git commit hash
* The git commit time
* Which 'dirty' files were in the project and how they have changed (MODIFY, DELETE e.t.c)


It was inspired by the more configurable [sbt-build-info](https://github.com/sbt/sbt-buildinfo)
plugin; we needed something that could be included in our projects transparently with no
configuration needed.

