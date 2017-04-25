package de.otds.exi.util

import java.io.{File, IOException}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

object RichDirectoryContainer {

  implicit class RichDirectory(val directory: File) extends AnyVal {
    def mkdirsWithCheck(): Unit = {
      if (directory.exists() == false) {
        if (directory.mkdirs() == false) {
          throw new IllegalStateException(s"Cannot create directory $directory")
        }
      } else if (directory.canWrite == false) {
        throw new IllegalStateException(s"Cannot write to directory $directory")
      }
    }

    def deleteRecursively(): Unit = {
      Files.walkFileTree(directory.toPath, new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      })
    }
  }
}

