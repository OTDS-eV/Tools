package de.otds.exi.util

import java.io.{InputStream, OutputStream}

trait StreamUtils {
  /**
    * Copy from one stream to another one.
    *
    * @param inputStream
    * @param outputStream
    * @param bufferSize The default value is 8192 (like in BufferedInputStream/BufferedOutputStream)
    * @return The number of copied bytes
    */
  def copyStream(inputStream: InputStream, outputStream: OutputStream, bufferSize: Int = 8192): Int = {
    val buf = Array.ofDim[Byte](bufferSize)

    // Nicer, but possibly slower, and not yet complete
    if (false) {
      Stream.continually(inputStream.read(buf)).takeWhile(_ != -1).foreach(outputStream.write(buf, 0, _))
      -1
    } else {
      var totalRead = 0
      var read: Int = 1
      while (read > 0) {
        read = inputStream.read(buf, 0, bufferSize)
        if (read > 0) {
          totalRead = totalRead + read
          outputStream.write(buf, 0, read)
        }
      }
      totalRead
    }
  }
}

object StreamUtils {
  /**
    * Provide an instance for implicit value classes.
    */
  val instance = new StreamUtils() {}
}