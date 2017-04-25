package de.otds.exi.impl

import java.io._
import java.util.zip.ZipInputStream
import javax.xml.transform.stream.StreamSource

import de.otds.exi._
import de.otds.exi.util.{IgnoreCloseInputStream, IgnoreCloseOutputStream, StreamUtils}
import org.apache.commons.compress.compressors.CompressorStreamFactory

abstract class CommonsCompressLibraryImpl(name: String) extends LibraryImpl with StreamUtils {

  override def supports(codingMode: CodingMode, fidelityOptionMode: FidelityOptionMode): Boolean = {
    codingMode == Default && fidelityOptionMode == NotApplicable
  }

  override def encode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit = {
    val factory = new CompressorStreamFactory()
    withResource(factory.createCompressorOutputStream(name, outputStream)) { cos =>
      copyStream(inputStream, cos)
    }
  }

  private def read(inputStream: InputStream, settings: Settings)
                  (f: InputStream => Unit): Unit = {
    val factory = new CompressorStreamFactory()
    /**
      * Quote from the Javadoc of GzipCompressorInputStream:
      *
      * If <code>decompressConcatenated</code> is {@code false}:
      * This decompressor might read more input than it will actually use.
      * If <code>inputStream</code> supports <code>mark</code> and
      * <code>reset</code>, then the input position will be adjusted
      * so that it is right after the last byte of the compressed stream.
      * If <code>mark</code> isn't supported, the input position will be
      * undefined.
      */
    // Adjust stream position, so that it is right after the last byte.
    // Otherwise zip-decode and zip-validate won't work.
    val decompressConcatenated = false

    withResource(factory.createCompressorInputStream(name, inputStream, decompressConcatenated)) { cis =>
      f(cis)
    }
  }

  override def decode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit = {
    read(inputStream, settings) { cis =>
      copyStream(cis, outputStream)
    }
  }

  override def validate(inputStream: InputStream, settings: Settings, xsdFile: XsdFile): Unit = {
    read(inputStream, settings) { cis =>
      val source = new StreamSource(cis)
      validateBySource(source, xsdFile)
    }
  }
}
