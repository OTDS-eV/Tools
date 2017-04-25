package de.otds.exi.util

import java.io.File
import java.net.URL

import scala.language.postfixOps
import scala.sys.process._

object RichUrlContainer {
  implicit class RichUrl(val url: URL) extends AnyVal {
    def download(file: File): Unit = {
      url #> file !!
    }
  }
}
