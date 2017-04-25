package de.otds.exi.util

import java.io._
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}

import de.otds.exi._
import de.otds.exi.util.RichZipInputStreamContainer._

object RichFileContainer {
  implicit class RichFile(val file: File) extends AnyVal {
    /**
      * Expands a java.io.File to itself (if it is a file) or to the files in the directory which match the given extension.
      *
      * @param ext The file extension
      * @return A list of files
      */
    def expand(ext: String): List[File] = {
      if (file.isFile) {
        file :: Nil
      } else {
        file.listFiles().toList.filter(_.getName.endsWith(ext))
      }
    }

    def unzip(targetDirectory: Directory)(callback: String => Unit): Seq[File] = {
      ResourceManager.instance.withResource(new ZipInputStream(new BufferedInputStream(new FileInputStream(file)))) { zis =>
        zis.map { zipEntry =>
          if (zipEntry.isDirectory == false) {
            callback(zipEntry.getName)
            val outputFile = new File(targetDirectory.getPath, zipEntry.getName)
            outputFile.getParentFile.mkdirs()

            ResourceManager.instance.withResource(new BufferedOutputStream(new FileOutputStream(outputFile))) { os =>
              StreamUtils.instance.copyStream(zis, os)
            }

            Some(outputFile)
          } else {
            None
          }
        }.flatten
      }
    }

    def zip(files: Seq[File])(callback: String => Unit): Seq[(File, Int)] = {
      ResourceManager.instance.withResource(new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) { zos =>
        files.map { f =>
          ResourceManager.instance.withResource(new BufferedInputStream(new FileInputStream(f))) { is =>
            callback(f.getPath)
            zos.putNextEntry(new ZipEntry(f.getName))
            (f, StreamUtils.instance.copyStream(is, zos))
          }
        }
      }
    }
  }
}