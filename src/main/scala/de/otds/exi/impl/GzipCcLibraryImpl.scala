package de.otds.exi.impl

import java.io.{BufferedInputStream, BufferedOutputStream, InputStream, OutputStream}
import java.util.zip.Deflater

import de.otds.exi._
import de.otds.exi.util.{IgnoreCloseOutputStream, StreamUtils}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.gzip.{GzipCompressorOutputStream, GzipParameters}

import scala.language.implicitConversions

class GzipCcLibraryImpl extends CommonsCompressLibraryImpl(CompressorStreamFactory.GZIP) with StreamUtils {
  override val library = GzipCc

  override def supports(codingMode: CodingMode, fidelityOptionMode: FidelityOptionMode): Boolean = {
    List(Speed, Size, Default).contains(codingMode) && fidelityOptionMode == NotApplicable
  }

  private implicit def getCompressionLevel(codingMode: CodingMode): Int = {
    codingMode match {
      case Default =>
        Deflater.DEFAULT_COMPRESSION

      case Size =>
        Deflater.BEST_COMPRESSION

      case Speed =>
        Deflater.BEST_SPEED

      case BitPacked | BytePacked | PreCompression | Compression =>
        Deflater.DEFAULT_COMPRESSION
    }
  }

  override def encode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit = {
    val params = new GzipParameters()
    params.setCompressionLevel(settings.codingMode)
    withResource(new GzipCompressorOutputStream(outputStream, params)) { gos =>
      copyStream(inputStream, gos)
    }
  }
}
