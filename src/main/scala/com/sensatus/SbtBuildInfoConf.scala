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

package com.sensatus


import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions, ConfigValueFactory}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository._
import org.eclipse.jgit.lib.{Constants, PersonIdent, Repository}
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import sbt.Keys._
import sbt.plugins.JvmPlugin
import scala.reflect.runtime.universe._
import sbt._
import scala.collection.JavaConverters._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
 * Class to add build information into application.conf
 */
object SbtBuildInfoConf extends AutoPlugin {

  override def requires: Plugins = JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val buildinfo = taskKey[Seq[File]]("Add build info to application.conf")
  }

  import com.sensatus.SbtBuildInfoConf.autoImport._

  /**
   * method taken from: https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowFileDiff.java
   */
  def prepareTreeParser(repository: Repository, ref: String): CanonicalTreeParser = {
    // from the commit we can build the tree which allows us to construct the TreeParser
    val head = repository.getRef(ref)
    val walk = new RevWalk(repository)
    val commit = walk.parseCommit(head.getObjectId)
    val tree = walk.parseTree(commit.getTree.getId)
    val oldTreeParser = new CanonicalTreeParser()
    val oldReader = repository.newObjectReader()
    try {
      oldTreeParser.reset(oldReader, tree.getId)
    } finally {
      oldReader.release()
    }
    walk.dispose()
    oldTreeParser
  }

  /**
   * Get the 'git describe' string
   */
  lazy val GitDescribe : Try[String] = {
    Try {
      val builder = new FileRepositoryBuilder
      val repository = builder.readEnvironment.findGitDir.build()
      Option(Git.wrap(repository).describe().call())
        .getOrElse(throw new Exception("Error calling describe - no tags?"))
    }.recoverWith {
      case NonFatal(f) ⇒ Failure(new Exception("Could not call git describe", f))
    }
  }

  /**
   * Return a string representation of the last commit hash
   */
  lazy val GitCommit: Try[String] = {
    Try {
      val builder = new FileRepositoryBuilder
      val repository = builder.readEnvironment.findGitDir.build()
      val head = repository.getRef(org.eclipse.jgit.lib.Constants.HEAD)
      org.eclipse.jgit.lib.ObjectId.toString(head.getObjectId)
    }.recoverWith {
      case e: IllegalArgumentException ⇒ Failure(new Exception("Git repository not found?"))
    }
  }

  /**
   * Show which files have been modified/deleted e.t.c and not committed
   */
  lazy val GitDirtyFiles: Try[Map[String, String]] = {
    Try {
      val builder = new FileRepositoryBuilder
      val repository = builder.readEnvironment.findGitDir.build()
      val oldTreeParser = prepareTreeParser(repository, Constants.HEAD)
      val git = new Git(repository)
      val diff = git.diff().setOldTree(oldTreeParser).call()
      val l = asScalaIteratorConverter(diff.iterator()).asScala.toList
      l.map(d ⇒
        (
          if(d.getNewPath != "/dev/null") d.getNewPath
          else d.getOldPath
        ) → d.getChangeType.toString).toMap
    }.recoverWith {
      case e: IllegalArgumentException ⇒ Failure(new Exception("Git repository not found?"))
    }
  }

  /**
   * Get the identity of the last person to commit
   */
  lazy val GitIdent: Try[PersonIdent] = {
    Try {
      val builder = new FileRepositoryBuilder
      val repository = builder.readEnvironment.findGitDir.build()
      val git = new Git(repository)
      git.log().setMaxCount(1).call.iterator().next().getAuthorIdent
    }.recoverWith {
      case e: IllegalArgumentException ⇒ Failure(new Exception("Git repository not found?"))
    }
  }

  /**
   * Extract the name from the GitIdent
   */
  lazy val GitAuthor: Try[String] = {
    GitIdent.map(_.getName)
  }

  /**
   * Extract the last commit time from the GitIdent
   */
  lazy val GitLastCommitTime: Try[String] = {
    GitIdent.map(_.getWhen.toString)
  }

  /**
   * Get branch information - works if in detached head mode
   */
  lazy val GitBranch: Try[String] = {
    Try {
      val builder = new FileRepositoryBuilder
      val repository = builder.readEnvironment.findGitDir.build()
      Option(repository.getBranch).filter(_ != GitCommit).orElse {
        val head = repository.getRef(org.eclipse.jgit.lib.Constants.HEAD)
        repository.getAllRefs.values().iterator().asScala.find {
          ref ⇒ !"HEAD".equals(ref.getName) && ref.getObjectId.equals(head.getObjectId)
        }.map(ref ⇒ shortenRefName(ref.getName))
      }.getOrElse(throw new Exception("Could not get branch"))
    }.recoverWith {
      case e: IllegalArgumentException ⇒ Failure(new Exception("Git repository not found?"))
    }
  }

  /**
   * Get hostname
   */
  lazy val Hostname: Try[String] = Try(java.net.InetAddress.getLocalHost.getHostName)

  /**
   * get current time
   */
  lazy val BuildTime: Try[String] = {
    Success(new java.util.Date().toString)
  }

  /**
   * Get current username
   */
  lazy val Username: Try[String] = Try(System.getProperty("user.name"))

  /**
   * Set the project settings
   */
  override lazy val projectSettings = Seq(
    buildinfo := {
      generateResource(
        organization.value,
        moduleName.value,
        version.value,
        sbtVersion.value,
        (resourceDirectory in Compile).value / "reference.conf",
        (resourceManaged in Compile).value,
        (resourceManaged in Compile).value / "gen-reference.conf"
      )
    },

    /* Add to resourceGenerators to be run on 'package' */
    resourceGenerators in Compile += buildinfo.taskValue,

    /*
     * Remove all reference.conf files from mappings to avoid name clash in copyResources
     */
    mappings in(Compile, packageBin) <<= (mappings in(Compile, packageBin)).map(_.filter(!_._1
      .getName.contains("reference.conf"))),

    /*
     * Add our newly generated conf file back and rename it
     */
    mappings in(Compile, packageBin) <+= (resourceManaged in Compile) map { base =>
      base / "gen-reference.conf" -> "reference.conf"
    }

  )


  def generateResource(org: String, mod: String, modVersion:String, sbtVersion:String, existing:
  File, base:  File, out: File):
  Seq[File] = {
    implicit class pimpConf(c: Config) {

      import scala.collection.JavaConversions._

      def applySettingToConf(branch: String, value: Try[String]): Config = value match {
        case Success(t) ⇒
          c.withValue(org + ".buildinfo." + mod + branch, ConfigValueFactory.fromAnyRef(t))
        case Failure(f) ⇒
          streams.map(_.log.warn(s"Could not populate $branch. ${f.getMessage}"))
          c
      }
    }

    if (!base.exists()) base.mkdirs()
    /*
     * Load the existing reference.conf if it exists
     */
    val appConfig = ConfigFactory.parseFile(existing)

    /*
     * Set our new values
     */
    val conf = appConfig
      .applySettingToConf(".git.commit.hash", GitCommit)
      .applySettingToConf(".git.commit.author", GitAuthor)
      .applySettingToConf(".git.commit.time", GitLastCommitTime)
      .applySettingToConf(".git.branch", GitBranch)
      .applySettingToConf(".git.describe", GitDescribe)
      .applySettingToConf(".time", BuildTime)
      .applySettingToConf(".hostname", Hostname)
      .applySettingToConf(".version", Success(modVersion))
      .applySettingToConf(".sbtVersion", Success(sbtVersion))
      .applySettingToConf(".username", Username)

    val df = GitDirtyFiles.map(t ⇒
      conf.withValue(
        org + ".buildinfo." + mod + ".git.dirtyFiles", ConfigValueFactory.fromMap(t.asJava)
      )
    ).getOrElse{
      streams.map(_.log.warn(s"Could not populate .git.dirtyFiles")  )
      conf
    }
    /*
     *  Write back into file
     */
    IO.write(out, df.root.render(ConfigRenderOptions.defaults().setOriginComments(false)))
    Seq(out)
  }
}
