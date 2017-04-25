package de.otds.exi.impl

import java.io._
import java.net.URI
import java.util.logging.Logger
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.stream.StreamResult

import de.otds.exi.util.{FileUtils, IgnoreCloseInputStream}
import de.otds.exi.{Settings, _}
import org.openexi.proc.common.{AlignmentType, EXIOptions, GrammarOptions}
import org.openexi.proc.grammars.GrammarCache
import org.openexi.sax.{EXIReader, Transmogrifier}
import org.openexi.schema.EXISchema
import org.openexi.scomp.{EXISchemaFactory, EntityResolverEx}
import org.xml.sax.InputSource

import scala.language.implicitConversions
import scala.util.{Failure, Success}

class OpenExiLibraryImpl extends RealExiLibraryImpl with FileUtils {
  override val library = OpenExi

  private def log = Logger.getLogger(getClass.getName)

  private implicit def toOpenExiAlignmentType(codingMode: CodingMode): AlignmentType = {
    codingMode match {
      case BitPacked =>
        AlignmentType.bitPacked

      case BytePacked =>
        AlignmentType.byteAligned

      case PreCompression =>
        AlignmentType.preCompress

      case Compression =>
        AlignmentType.compress

      case Default | Size | Speed =>
        AlignmentType.compress
    }
  }

  private def getSchema(xsdFile: Option[XsdFile]): Option[EXISchema] = {
    xsdFile.map { f =>
      withResource(new BufferedInputStream(new FileInputStream(f))) { inputStream =>
        val is = new InputSource(inputStream)

        val factory = new EXISchemaFactory()

        factory.setEntityResolver(new EntityResolverEx {
          override def resolveEntity(publicId: String, systemId: String, namespaceURI: String): InputSource = {
            // null, file:///current_directory/otds-schema-common.xsd, http://otds-group.org/otds
            log.fine(s"$publicId, $systemId, $namespaceURI")

            // Lookup included .xsd in the same directory as the main .xsd
            val f2 = new File(f.getParentFile, new File(new URI(systemId).getPath).getName)
            if (f2.exists()) {
              log.fine(s"Resolved $systemId to $f2")
              new InputSource(f2.getPath)
            } else {
              log.severe(s"Not yet implemented: Cannot resolve $publicId, $systemId, $namespaceURI")
              null
            }
          }

          override def resolveEntity(publicId: String, systemId: String): InputSource = {
            log.info(s"$publicId, $systemId")
            log.severe(s"Not yet implemented: Cannot resolve $publicId, $systemId")
            null
          }
        })

        factory.compile(is)
      }
    }
  }

  private def createGrammarCache(settings: Settings): GrammarCache = {
    val options = {
      settings.fidelityOptionMode match {
        case Strict =>
          GrammarOptions.STRICT_OPTIONS

        case All =>
          val exiOptions = new EXIOptions()

          // See Exificient, FidelitiyOptions#createAll()

          // fo.options.add(FEATURE_COMMENT)
          exiOptions.setPreserveComments(true)

          // fo.options.add(FEATURE_PI)
          exiOptions.setPreservePIs(true)

          // fo.options.add(FEATURE_DTD)
          exiOptions.setPreserveDTD(true)

          // fo.options.add(FEATURE_PREFIX)
          exiOptions.setPreserveNS(true)

          // fo.options.add(FEATURE_LEXICAL_VALUE)
          exiOptions.setPreserveLexicalValues(true)

          exiOptions.toGrammarOptions

        case Customized =>
          val exiOptions = new EXIOptions()
          exiOptions.setPreserveComments(settings.fidelityOptions.preserveComments)
          exiOptions.setPreservePIs(settings.fidelityOptions.preserveProcessingInstructions)
          exiOptions.setPreserveDTD(settings.fidelityOptions.preserveDtdsAndEntityReferences)
          exiOptions.setPreserveNS(settings.fidelityOptions.preservePrefixes)
          exiOptions.setPreserveLexicalValues(settings.fidelityOptions.preserveLexicalValues)
          exiOptions.toGrammarOptions

        case NotApplicable =>
          GrammarOptions.STRICT_OPTIONS
      }
    }

    new GrammarCache(getSchema(settings.xsdFile).orNull, options)
  }

  override def encode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit = {
    val transmogrifier = new Transmogrifier()
    transmogrifier.setAlignmentType(settings.codingMode)

    val grammarCache = createGrammarCache(settings)
    transmogrifier.setGrammarCache(grammarCache)

    // Boolean values for preserving whitespace and lexical content are set directly in the Transmogrifier.
    settings.fidelityOptionMode match {
      case Strict | Customized =>
        transmogrifier.setPreserveLexicalValues(settings.fidelityOptions.preserveLexicalValues)

      case All | NotApplicable =>
      // Ignore
    }

    // Preserve whitespace
    if (false)
      transmogrifier.setPreserveWhitespaces(true)

    transmogrifier.setOutputStream(outputStream)
    transmogrifier.encode(new InputSource(inputStream))
  }

  private def read(inputStream: InputStream, outputStream: OutputStream, settings: Settings, validate: Boolean): Unit = {
    val grammarCache = createGrammarCache(settings)

    val saxParserFactory = SAXParserFactory.newInstance()
    saxParserFactory.setNamespaceAware(true)
    saxParserFactory.setValidating(validate)

    val tf = TransformerFactory.newInstance().asInstanceOf[SAXTransformerFactory]

    val th = tf.newTransformerHandler()
    th.setResult(new StreamResult(outputStream))

    val exiReader = new EXIReader()
    exiReader.setGrammarCache(grammarCache)
    exiReader.setAlignmentType(settings.codingMode)

    // Boolean values for preserving whitespace and lexical content are set directly in the EXI reader.
    settings.fidelityOptionMode match {
      case Strict | Customized =>
        exiReader.setPreserveLexicalValues(settings.fidelityOptions.preserveLexicalValues)

      case All | NotApplicable =>
      // Ignore
    }

    exiReader.setContentHandler(th)
    exiReader.parse(new InputSource(inputStream))
  }

  override def decode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit = {
    read(inputStream, outputStream, settings, validate = false)
  }

  override def validate(inputStream: InputStream, settings: Settings, xsdFile: XsdFile): Unit = {
    withResource(new FileOutputStream(getSinkDevice())) { os =>
      read(inputStream, os, settings, validate = true)
    }
  }
}
