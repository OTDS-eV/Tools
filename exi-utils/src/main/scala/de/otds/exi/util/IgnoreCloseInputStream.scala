package de.otds.exi.util

import java.io.InputStream
import java.util.logging.Logger

class IgnoreCloseInputStream(is: InputStream) extends InputStream {
  private val log = Logger.getLogger(getClass.getName)

  override def read(): Int = is.read()

  override def available(): Int = is.available()

  override def mark(readlimit: Int): Unit = is.mark(readlimit)

  override def skip(n: Long): Long = is.skip(n)

  override def read(b: Array[Byte]): Int = is.read(b)

  override def read(b: Array[Byte], off: Int, len: Int): Int = is.read(b, off, len)

  override def reset(): Unit = is.reset()

  override def markSupported: Boolean = is.markSupported()

  override def close(): Unit = {
    log.fine("Ignore close")
  }
}
