package de.otds.exi.impl

import java.io._
import javax.xml.transform.stream.StreamSource

import de.otds.exi._
import de.otds.exi.util.StreamUtils

class PassThroughLibraryImpl extends LibraryImpl with StreamUtils {
  override val library = PassThrough

  override def supports(codingMode: CodingMode, fidelityOptionMode: FidelityOptionMode): Boolean = {
    codingMode == Default && fidelityOptionMode == NotApplicable
  }

  override def encode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit = {
    copyStream(inputStream, outputStream)
  }

  override def decode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit = {
    copyStream(inputStream, outputStream)
  }

  override def validate(inputStream: InputStream, settings: Settings, xsdFile: XsdFile): Unit = {
    val source = new StreamSource(inputStream)
    validateBySource(source, xsdFile)
  }
}
