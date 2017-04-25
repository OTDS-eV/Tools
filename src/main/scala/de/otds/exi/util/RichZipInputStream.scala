package de.otds.exi.util

import java.util.zip.{ZipEntry, ZipInputStream}

import scala.collection.generic.CanBuildFrom

object RichZipInputStreamContainer {

  implicit class RichZipInputStream(val zis: ZipInputStream) extends AnyVal {
    def foreach(f: ZipEntry => Unit) {
      Stream.continually(zis.getNextEntry).takeWhile(_ != null).foreach { zipEntry =>
        f(zipEntry)
      }
    }

    def map[E, C <: Iterable[E]](f: ZipEntry => E)(implicit cbf: CanBuildFrom[Stream[ZipEntry], E, C]): C = {
      // flatMap does not work - foreach is required! toList doesn't work either, this kills the file streams!
      if (false) {
        Stream.continually(zis.getNextEntry).takeWhile(_ != null).map { zipEntry =>
          f(zipEntry)
        }(cbf)
      } else {
        val b = cbf()
        var ze = zis.getNextEntry
        while (ze != null) {
          b += f(ze)
          ze = zis.getNextEntry
        }
        b.result()
      }
    }
  }
}
