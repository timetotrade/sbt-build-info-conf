/*
  __                      _
 / _\ ___ _ __  ___  __ _| |_ _   _ ___
 \ \ / _ \ '_ \/ __|/ _  | __| | | / __|
 _\ \  __/ | | \__ \ (_| | |_| |_| \__ \ Copyright © 2015 Sensatus UK Ltd
 \__/\___|_| |_|___/\__,_|\__|\__,_|_ _/ All Rights Reserved
 NOTICE: All information contained herein is, and remains the property of
 Sensatus UK Ltd. The intellectual and technical concepts contained herein are
 proprietary to Sensatus UK Ltd and may be covered by patents,
 patents in process, and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material is strictly
 forbidden unless prior written permission is obtained from Sensatus UK Ltd.
 */

package com.sensatus

import java.io.{BufferedWriter, FileWriter}

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions, ConfigValueFactory}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository._
import org.eclipse.jgit.lib.{Constants, PersonIdent, Repository}
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import sbt.Keys._
import sbt._
 import scala.reflect.runtime.universe._
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object SbtBuildInfoConf extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    lazy val buildinfo = TaskKey[Unit]("sbt-build-info-conf", "Add build info to reference.conf")
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
   * (not including changes made to reference.conf since we write to it)
   */
  lazy val GitDirtyFiles: Try[Map[String,String]] = {
    Try {
      val builder = new FileRepositoryBuilder
      val repository = builder.readEnvironment.findGitDir.build()
      val oldTreeParser = prepareTreeParser(repository, Constants.HEAD)
      val git = new Git(repository)
      val diff = git.diff().setOldTree(oldTreeParser).call()
      val l = asScalaIteratorConverter(diff.iterator()).asScala.toList
      l.filterNot(_.getNewPath.endsWith("reference.conf")).map(d ⇒ d.getOldPath
        → d .getChangeType.toString).toMap
    }.recoverWith{
      case e:IllegalArgumentException ⇒ Failure(new Exception("Git repository not found?"))
    }
  }

  /**
   * Get the identity of the last person to commit
   */
  lazy val GitIdent: Try[PersonIdent] = {
    Try{
      val builder = new FileRepositoryBuilder
      val repository = builder.readEnvironment.findGitDir.build()
      val git = new Git(repository)
      git.log().setMaxCount(1).call.iterator().next().getAuthorIdent
    }.recoverWith{
      case e:IllegalArgumentException ⇒ Failure(new Exception("Git repository not found?"))
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
  lazy val GitLastCommitTime:Try[String] = {
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
    }.recoverWith{
      case e:IllegalArgumentException ⇒ Failure(new Exception("Git repository not found?"))
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

  lazy val Username: Try[String] = Try(System.getProperty("user.name"))



  override lazy val projectSettings = Seq(
    buildinfo := {

      implicit class pimpConf(c:Config) {

        import scala.collection.JavaConversions._
        def applySettingToConf[T:TypeTag](branch: String, value: Try[T]): Config = {
          value match {
            case Success(t:Map[String @unchecked,_]) ⇒
                c.withValue(organization.value + ".buildinfo." + moduleName.value + branch,
                  ConfigValueFactory.fromMap(mapAsJavaMap(t)))
            case Success(t) ⇒
              c.withValue(organization.value + ".buildinfo." + moduleName.value + branch,
                ConfigValueFactory.fromAnyRef(t))
            case Failure(f) ⇒
              streams.value.log.warn(s"Could not populate $branch. ${f.getMessage}")
              c
          }
        }
      }

      /*
       * Get the reference.conf if it exists
       */
      val resourceDir = (resourceDirectory in Compile).value
      if (!resourceDir.exists()) resourceDir.mkdirs()
      val appConfig = ConfigFactory.parseFile(resourceDir / "reference.conf")

      /*
       * Set our new values
       */
      val conf = appConfig
        .applySettingToConf(".git.commit.hash",GitCommit)
        .applySettingToConf(".git.commit.author",GitAuthor)
        .applySettingToConf(".git.commit.time", GitLastCommitTime)
        .applySettingToConf(".git.branch", GitBranch)
        .applySettingToConf(".git.dirtyfiles", GitDirtyFiles)
        .applySettingToConf(".time",BuildTime)
        .applySettingToConf(".hostname",Hostname)
        .applySettingToConf(".username",Username)

      /*
       *  Write back into our file
       */
      val file = new File((resourceDir / "reference.conf").toString)
      val br = new BufferedWriter(new FileWriter(file))
      try {
        br.write(conf.root.render(ConfigRenderOptions.defaults().setOriginComments(false)))
      } finally {
        br.close()
      }
    },

    /*
     *  Insert into compile stage (might be better in publish?)
     */
    compile <<= (compile in Compile) dependsOn buildinfo
  )
}
