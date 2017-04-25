package de.otds.exi.impl

import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream, ZipInputStream}
import javax.xml.XMLConstants
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.stream.{StreamResult, StreamSource}
import javax.xml.transform.{Source, TransformerFactory}
import javax.xml.validation.SchemaFactory

import com.siemens.ct.exi.api.sax.{EXIResult, EXISource}
import com.siemens.ct.exi.helpers.DefaultEXIFactory
import com.siemens.ct.exi.{EXIFactory, FidelityOptions, GrammarFactory}
import de.otds.exi._
import de.otds.exi.util.{IgnoreCloseInputStream, IgnoreCloseOutputStream, StreamUtils}
import org.xml.sax.InputSource
import org.xml.sax.helpers.XMLReaderFactory

import scala.util.{Failure, Success, Try}

class GzipLibraryImpl extends LibraryImpl with StreamUtils {
  override val library = Gzip

  override def supports(codingMode: CodingMode, fidelityOptionMode: FidelityOptionMode): Boolean = {
    codingMode == Default && fidelityOptionMode == NotApplicable
  }

  override def encode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit = {
    withResource(new GZIPOutputStream(outputStream)) { gos =>
      copyStream(inputStream, gos)
    }
  }

  override def decode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit = {
    withResource(new GZIPInputStream(inputStream)) { gis =>
      copyStream(gis, outputStream)
    }
  }

  override def validate(inputStream: InputStream, settings: Settings, xsdFile: XsdFile): Unit = {
    withResource(new GZIPInputStream(inputStream)) { gis =>
      val source = new StreamSource(inputStream)
      validateBySource(source, xsdFile)
    }
  }
}
