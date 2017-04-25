package de.otds.exi.util

import java.io.{Closeable, File}
import java.nio.file.Files

import RichDirectoryContainer._

trait ResourceManager {
  def withResource[T <: Closeable, R](r: T)(f: T => R): R = {
    try {
      f(r)
    } finally {
      r.close()
    }
  }

  def withTmpDirectory[R](f: File => R): R = {
    withTmpDirectory(delete = true)(f)
  }

  def withTmpDirectory[R](delete: Boolean, prefix: String = "wtd")(f: File => R): R = {
    val dir = Files.createTempDirectory(prefix).toFile
    try {
      f(dir)
    } finally {
      if (delete) {
        dir.deleteRecursively()
      }
    }
  }
}

object ResourceManager {
  /**
   * Provide an instance for implicit value classes.
   */
  val instance = new ResourceManager() {}
}
