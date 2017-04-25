package de.otds.exi.impl

import java.io._
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult

import com.siemens.ct.exi.api.sax.{EXIResult, EXISource}
import com.siemens.ct.exi.helpers.DefaultEXIFactory
import com.siemens.ct.exi.{EXIFactory, FidelityOptions, GrammarFactory}
import de.otds.exi._
import org.xml.sax.InputSource
import org.xml.sax.helpers.XMLReaderFactory

import scala.language.implicitConversions

class ExificientLibraryImpl extends RealExiLibraryImpl {
  override val library = Exificient

  private implicit def toExificientCodingMode(alignment: CodingMode): com.siemens.ct.exi.CodingMode = {
    alignment match {
      case BitPacked =>
        com.siemens.ct.exi.CodingMode.BIT_PACKED

      case BytePacked =>
        com.siemens.ct.exi.CodingMode.BYTE_PACKED

      case PreCompression =>
        com.siemens.ct.exi.CodingMode.PRE_COMPRESSION

      case Compression =>
        com.siemens.ct.exi.CodingMode.COMPRESSION

      case Default | Size | Speed =>
        com.siemens.ct.exi.CodingMode.COMPRESSION
    }
  }

  private def createExiFactory(settings: Settings): EXIFactory = {
    val exiFactory = DefaultEXIFactory.newInstance()
    exiFactory.setCodingMode(settings.codingMode)

    val fo = {
      settings.fidelityOptionMode match {
        case Strict =>
          val fo = FidelityOptions.createStrict()
          fo.setFidelity(FidelityOptions.FEATURE_LEXICAL_VALUE, settings.fidelityOptions.preserveLexicalValues)
          fo

        case All =>
          FidelityOptions.createAll()

        case Customized =>
          val fo = FidelityOptions.createDefault()
          fo.setFidelity(FidelityOptions.FEATURE_COMMENT, settings.fidelityOptions.preserveComments)
          fo.setFidelity(FidelityOptions.FEATURE_PI, settings.fidelityOptions.preserveProcessingInstructions)
          fo.setFidelity(FidelityOptions.FEATURE_DTD, settings.fidelityOptions.preserveDtdsAndEntityReferences)
          fo.setFidelity(FidelityOptions.FEATURE_PREFIX, settings.fidelityOptions.preservePrefixes)
          fo.setFidelity(FidelityOptions.FEATURE_LEXICAL_VALUE, settings.fidelityOptions.preserveLexicalValues)
          fo

        case NotApplicable =>
          FidelityOptions.createStrict()
      }
    }
    exiFactory.setFidelityOptions(fo)

    settings.xsdFile.foreach { f =>
      val gf = GrammarFactory.newInstance()
      exiFactory.setGrammars(gf.createGrammars(f.getPath))
    }

    exiFactory
  }

  override def encode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit = {
    val exiFactory = createExiFactory(settings)
    val exiResult = new EXIResult(exiFactory)
    exiResult.setOutputStream(outputStream)

    val xmlReader = XMLReaderFactory.createXMLReader()
    xmlReader.setContentHandler(exiResult.getHandler)
    xmlReader.parse(new InputSource(inputStream))
  }

  private def getExiSource(inputStream: InputStream, settings: Settings): EXISource = {
    val exiFactory = createExiFactory(settings)
    val exiSource = new EXISource(exiFactory)
    exiSource.setInputSource(new InputSource(inputStream))
    exiSource
  }

  override def decode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit = {
    val exiSource = getExiSource(inputStream, settings)

    val tf = TransformerFactory.newInstance()
    val transformer = tf.newTransformer()
    val result = new StreamResult(outputStream)
    transformer.transform(exiSource, result)
  }

  override def validate(inputStream: InputStream, settings: Settings, xsdFile: XsdFile): Unit = {
    val exiSource = getExiSource(inputStream, settings)

    validateBySource(exiSource, xsdFile)
  }
}

