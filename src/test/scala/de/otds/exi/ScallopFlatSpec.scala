package de.otds.exi

import org.rogach.scallop.{ScallopConf, Subcommand}
import org.scalatest.{FlatSpec, GivenWhenThen, MustMatchers}

/**
  * Based on https://github.com/scallop/scallop/wiki/Subcommands
  */
class ScallopFlatSpec extends FlatSpec with MustMatchers with GivenWhenThen {
  "Scallop" must "handle a config defined in an object" in {
    object Conf extends ScallopConf(Seq("-a", "tree", "-b")) {
      val apples = opt[Boolean]("apples")
      val tree = new Subcommand("tree") {
        val bananas = opt[Boolean]("bananas")
      }
      addSubcommand(tree)

      verify()
    }

    Conf.apples() must equal(true)
    Conf.subcommand must equal(Some(Conf.tree))
    Conf.subcommands must equal(List(Conf.tree))

    // Requires reflection
    locally {
      import scala.language.reflectiveCalls
      Conf.tree.bananas() must equal(true)
    }
  }

  "Scallop" must "handle a config defined in a class" in {
    class Conf(args: Seq[String]) extends ScallopConf(args) {
      val apples = opt[Boolean]("apples")
      val tree = new Subcommand("tree") {
        val bananas = opt[Boolean]("bananas")
      }
      addSubcommand(tree)

      verify()
    }

    val conf = new Conf(Seq("-a", "tree", "-b"))

    conf.apples() must equal(true)
    conf.subcommand must equal(Some(conf.tree))
    conf.subcommands must equal(List(conf.tree))

    // Requires reflection
    locally {
      import scala.language.reflectiveCalls
      conf.tree.bananas() must equal(true)
    }

    locally {
      // Does not show anything
      println(conf.subcommand)

      // Always prints "scallop", so useless as well
      println(conf.subcommand.map(_.printedName))

      println(conf.subcommands)
      println(conf.subcommands.map(_.printedName))
    }
  }
}
