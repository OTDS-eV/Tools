package de.otds.exi.impl

import java.io.{File, _}
import java.util.Properties
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import javax.xml.XMLConstants
import javax.xml.validation.SchemaFactory

import de.otds.exi._
import de.otds.exi.build.BuildInfo
import de.otds.exi.util.RichDirectoryContainer._
import de.otds.exi.util.RichZipInputStreamContainer._
import de.otds.exi.util.{IgnoreCloseInputStream, IgnoreCloseOutputStream, ResourceManager, StreamUtils}

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait LibraryImpl extends ResourceManager with StreamUtils {
  val library: Library

  /**
    * Handles I/O from file to file. Normally used by encode and decode. OpenExi uses it for validation as well.
    */
  private def io[R](inputFile: File, outputFile: File)(operation: (InputStream, OutputStream) => R): R = {
    withResource(new BufferedInputStream(new FileInputStream(inputFile))) {
      is =>
        withResource(new BufferedOutputStream(new FileOutputStream(outputFile))) {
          os =>
            io(is, os)(operation)
        }
    }
  }

  /**
    * Handles I/O from stream to stream. Normally used by encode and decode. OpenExi uses it for validation as well.
    *
    * This method makes sure that the streams are not closed by the underlying implementation.
    *
    * The abstract encode/decode/validate methods of derived classes must only be called by this method or by read().
    */
  private def io[R](inputStream: InputStream, outputStream: OutputStream)(operation: (InputStream, OutputStream) => R): R = {
    // In case of a zip input stream: prevent close()! This is handled by withResource() in this method
    // In case of a zip output stream: prevent close()! This is handled by withResource() in this method
    operation(new IgnoreCloseInputStream(inputStream), new IgnoreCloseOutputStream(outputStream))
  }

  /**
    * Handles reading from a file. Normally used by validate.
    */
  private def read[R](inputFile: File)(operation: (InputStream) => R): R = {
    withResource(new BufferedInputStream(new FileInputStream(inputFile))) {
      is =>
        read(is)(operation)
    }
  }

  /**
    * Handles reading from a stream. Normally used by validate.
    *
    * This method makes sure that the streams are not closed by the underlying implementation.
    *
    * The abstract encode/decode/validate methods of derived classes must only be called by this method or by io().
    */
  private def read[R](inputStream: InputStream)(operation: (InputStream) => R): R = {
    // In case of a zip input stream: prevent close()! This is handled by withResource() in this method
    operation(new IgnoreCloseInputStream(inputStream))
  }


  /**
    * Indicates whether the given combination of options are supported by this implementation.
    *
    * @return True if the combination of options is supported
    */
  def supports(codingMode: CodingMode, fidelityOptionMode: FidelityOptionMode): Boolean

  def encode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit

  def encode(inputFile: XmlFile, outputDir: Directory, settings: Settings): EncodeResult = {
    outputDir.mkdirsWithCheck()
    val outputFile = new ExiFile(outputDir, inputFile.getName + s".${settings.fileSuffix}.exi")

    val result = Try(io(inputFile, outputFile)(encode(_, _, settings))) match {
      case Success(_) =>
        EncodeResult(inputFile, outputFile, settings, ok = true, inputFile.length(), Some(outputFile.length()), feedback = None)

      case Failure(ex) =>
        // Otherwise there are empty files (or with some bytes) left over by OpenExi
        // which in turn give another error while decoding
        if (outputFile.exists()) {
          outputFile.delete()
        }
        EncodeResult(inputFile, outputFile, settings, ok = false, inputFile.length(), outputFileSize = None, feedback = Some(s"Error: ${ex.getMessage})"))
    }

    // Always write the properties file (even in case of a failure)
    result.writeAsProperties()

    result
  }

  def decode(inputStream: InputStream, outputStream: OutputStream, settings: Settings): Unit

  def decode(inputFile: ExiFile, outputDir: Directory, settings: Settings): DecodeResult = {
    outputDir.mkdirsWithCheck()
    val outputFile = new XmlFile(outputDir, inputFile.getName + s".${library.id.toLowerCase}.xml")

    Try(io(inputFile, outputFile)(decode(_, _, settings))) match {
      case Success(_) =>
        DecodeResult(inputFile, outputFile, settings, ok = true, inputFile.length(), Some(outputFile.length()), feedback = None)

      case Failure(ex) =>
        DecodeResult(inputFile, outputFile, settings, ok = false, inputFile.length(), outputFileSize = None, feedback = Some(s"Error: ${ex.getMessage}"))
    }
  }

  def validate(inputStream: InputStream, settings: Settings, xsdFile: XsdFile): Unit

  def validate(inputFile: ExiFile, settings: Settings): ValidateResult = {
    validateWithXsdFile(inputFile, settings) { xsdFile =>
      Try(read(inputFile)(validate(_, settings, xsdFile))) match {
        case Success(_) =>
          ValidateResult(inputFile, settings, ok = true, inputFile.length(), feedback = Some("Validation OK"))

        case Failure(ex) =>
          ValidateResult(inputFile, settings, ok = false, inputFile.length(), feedback = Some(s"Validation failed: ${ex.getMessage}"))
      }
    }
  }

  /**
   * Used by validate() and zipValidate().
   * In both cases there must be an .xsd file for successful validation; this is to be handled by the given operation.
   * All other (error) cases are handled in this method.
   */
  private def validateWithXsdFile[R](inputFile: ExiFile, settings: Settings)(operation: XsdFile => ValidateResult): ValidateResult = {
    settings.xsdFile match {
      case Some(xsdFile) if xsdFile.canRead =>
        operation(xsdFile)

      case Some(xsdFile) if xsdFile.canRead == false =>
        ValidateResult(inputFile, settings, ok = false, inputFile.length(), feedback = Some(s"Cannot read xsd file $xsdFile"))

      case None =>
        ValidateResult(inputFile, settings, ok = false, inputFile.length(), feedback = Some("Cannot validate without .xsd file"))
    }
  }

  /**
    * Most libraries use this method for validation eventually, except for OpenExi.
    */
  protected def validateBySource(source: javax.xml.transform.Source, xsdFile: XsdFile): Unit = {
    val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
    val schema = schemaFactory.newSchema(xsdFile)
    val validator = schema.newValidator()
    validator.validate(source)
  }

  def zipEncode(inputFile: ZipFile, outputDir: Directory, settings: Settings, skipFileNames: List[String])(callback: String => Unit): EncodeResult = {
    outputDir.mkdirsWithCheck()
    val outputFile = new ZipFile(outputDir, inputFile.getName + s".${library.id.toLowerCase}.${settings.codingMode.id.toLowerCase}.${settings.fidelityOptionMode.id.toLowerCase()}.zip")

    // TODO A Duplicate code
    case class Result(ok: Boolean, feedback: Option[String])

    // Buffered! https://stackoverflow.com/questions/14462371/preferred-way-to-use-java-zipoutputstream-and-bufferedoutputstream#17190212
    val results = withResource(new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) { zos =>
      // Buffered! http://www.oracle.com/technetwork/articles/java/compress-1565076.html
      withResource(new ZipInputStream(new BufferedInputStream(new FileInputStream(inputFile)))) { zis =>
        // TODO C flatMap
        zis.map { zipEntry =>
          if (zipEntry.isDirectory == false) {
            if (skipFileNames.contains(zipEntry.getName)) {
              callback(s"[${zipEntry.getName}]")
              zos.putNextEntry(new ZipEntry(zipEntry.getName))
              copyStream(zis, zos)
              Some(Result(ok = true, None))
            } else {
              callback(zipEntry.getName)
              val zipEntryName = zipEntry.getName + ".exi"
              zos.putNextEntry(new ZipEntry(zipEntryName))
              Try(io(zis, zos)(encode(_, _, settings))) match {
                case Success(_) =>
                  Some(Result(ok = true, None))
                case Failure(ex) =>
                  Some(Result(ok = false, Some(s"Error encoding ${zipEntry.getName}: ${ex.getMessage}")))
              }
            }
          } else {
            None
          }
        }.flatten
      }
    }

    // TODO A Duplicate code
    val feedbacks = results.flatMap(_.feedback)

    val result = EncodeResult(inputFile, outputFile, settings, ok = results.forall(_.ok), inputFile.length(), Some(outputFile.length()),
      // TODO A Duplicate code
      feedback = if (feedbacks.isEmpty) None else Some(feedbacks.mkString(", ")))

    result.writeAsProperties()
    result
  }

  def zipDecode(inputFile: ZipFile, outputDir: Directory, settings: Settings)(callback: String => Unit): ZipDecodeResult = {
    outputDir.mkdirsWithCheck()

    case class Result(ok: Boolean, feedback: Option[String])

    // Buffered! http://www.oracle.com/technetwork/articles/java/compress-1565076.html
    val results = withResource(new ZipInputStream(new BufferedInputStream(new FileInputStream(inputFile)))) { zis =>
      // TODO C flatMap
      zis.map { zipEntry =>
        // The 2nd directory is to get a unique path. Otherwise files would be overwritten.
        val outputDir2 = new Directory(outputDir, settings.fileSuffix)
        outputDir2.mkdirsWithCheck()

        if (zipEntry.isDirectory == false) {
          if (zipEntry.getName.endsWith(".exi") == false) {
            callback(s"[${zipEntry.getName}]")

            val outputFile = new XmlFile(outputDir2, zipEntry.getName)
            withResource(new BufferedOutputStream(new FileOutputStream(outputFile))) { os =>
              copyStream(zis, os)
            }
            Some(Result(ok = true, feedback = None))

          } else {
            callback(zipEntry.getName)

            val outputFile = new XmlFile(outputDir2, zipEntry.getName.replaceAll("\\.exi$", ""))
            withResource(new BufferedOutputStream(new FileOutputStream(outputFile))) { os =>
              Try(io(zis, os)(decode(_, _, settings))) match {
                case Success(_) =>
                  Some(Result(ok = true, feedback = None))
                case Failure(ex) =>
                  Some(Result(ok = false, Some(s"Error decoding ${zipEntry.getName}: ${ex.getMessage}")))
              }
            }
          }
        } else {
          None
        }
      }.flatten
    }

    val feedbacks = results.flatMap(_.feedback)

    ZipDecodeResult(inputFile, settings, ok = results.forall(_.ok), inputFile.length(),
      feedback = if (feedbacks.isEmpty) None else Some(feedbacks.mkString(", ")))
  }

  def zipValidate(inputFile: ZipFile, settings: Settings)(callback: String => Unit): ValidateResult = {
    validateWithXsdFile(inputFile, settings) { xsdFile =>
      case class Result(ok: Boolean, feedback: Option[String])

      // Buffered! http://www.oracle.com/technetwork/articles/java/compress-1565076.html
      val results = withResource(new ZipInputStream(new BufferedInputStream(new FileInputStream(inputFile)))) { zis =>
        // TODO C flatMap
        zis.map { zipEntry =>
          if (zipEntry.isDirectory == false) {
            if (zipEntry.getName.endsWith(".exi") == false) {
              callback(s"[${zipEntry.getName}]")
              // Whatever has been skipped, we do not always have a correct .xsd for this case
              None
            } else {
              callback(zipEntry.getName)
              Try(read(zis)(validate(_, settings, xsdFile))) match {
                case Success(_) =>
                  Some(Result(ok = true, feedback = None))
                case Failure(ex) =>
                  Some(Result(ok = false, Some(s"Error validating ${zipEntry.getName}: ${ex.getMessage}")))
              }
            }
          } else {
            None
          }
        }.flatten
      }

      val feedbacks = results.flatMap(_.feedback)

      ValidateResult(inputFile, settings, ok = true, inputFile.length(),
        feedback = if (feedbacks.isEmpty) None else Some(feedbacks.mkString(", ")))
    }
  }
}

trait RealExiLibraryImpl extends LibraryImpl {
  final override def supports(codingMode: CodingMode, fidelityOptionMode: FidelityOptionMode): Boolean = {
    List(BytePacked, BitPacked, PreCompression, Compression).contains(codingMode) &&
      List(Strict, All, Customized).contains(fidelityOptionMode)
  }
}

case class EncodeResult(// .xml or .zip
                        inputFile: File,
                        // .exi or .zip
                        outputFile: File,
                        settings: Settings,
                        ok: Boolean,
                        inputFileSize: Long,
                        outputFileSize: Option[Long],
                        feedback: Option[String]) extends ResourceManager {

  def formatted(fileWidth: Int, duration: Duration): String = {
    val ratioFormatted = if (ok) {
      outputFileSize.map { ofs =>
        if (inputFileSize != 0) {
          val ratio = 100F - (ofs * 100F / inputFileSize)
          new java.text.DecimalFormat("##").format(ratio)
        } else {
          ""
        }
      }.getOrElse("?")
    } else {
      ""
    }

    f"${outputFile.getPath.padTo(fileWidth, ' ')} $inputFileSize%12s ${outputFileSize.getOrElse("")}%12s $ratioFormatted%2s%% ${settings.library}%-11s ${settings.codingMode}%-15s ${settings.fidelityOptionMode}%-13s ${settings.formattedFidelityOptions()} ${settings.xsdFile.map(_ => "Y").getOrElse("-")} ${duration.toMillis}%10s ${feedback.getOrElse("")}"
  }

  def writeAsProperties() {
    val props = new Properties()
    props.put("library", settings.library.id)

    props.put("inputFile.path", inputFile.getPath)
    props.put("inputFile.absolutePath", inputFile.getAbsolutePath)
    props.put("outputFile", outputFile.getPath)

    props.put("xsdFile.path", settings.xsdFile.map(_.getPath).getOrElse(""))
    props.put("xsdFile.absolutePath", settings.xsdFile.map(_.getAbsolutePath).getOrElse(""))
    props.put("codingMode", settings.codingMode.id)
    props.put("fidelityOptionMode", settings.fidelityOptionMode.id)

    settings.fidelityOptions.intoProperties(props)

    withResource(new BufferedWriter(new FileWriter(new File(outputFile.getPath + ".properties")))) { w =>
      props.store(w, s"${BuildInfo.name} ${BuildInfo.version}")
    }
  }
}

object EncodeResult {
  def header(fileWidth: Int): String = {
    val tmp = "File".padTo(fileWidth, ' ')
    f"$tmp ${"Size"}%12s ${"Enc size"}%12s ${"%"}%3s ${"Library"}%-11s ${"Coding mode"}%-15s ${"FidOM"}%-13s CIDPL S ${"T [ms]"}%10s"
  }
}

case class DecodeResult(// .exi or .zip
                        inputFile: File,
                        // .xml or .zip
                        outputFile: File,
                        settings: Settings,
                        ok: Boolean,
                        inputFileSize: Long,
                        outputFileSize: Option[Long],
                        feedback: Option[String]) {

  val baseFileName = {
    val pos = outputFile.getPath.indexOf(".xml")
    if (pos != -1) {
      outputFile.getPath.substring(0, pos)
    } else {
      val pos = outputFile.getPath.indexOf(".zip")
      outputFile.getPath.substring(0, pos)
    }
  }

  def formatted(fileWidth: Int, duration: Duration): String = {
    // As in EncodeResult, without ratio
    f"${outputFile.getPath.padTo(fileWidth, ' ')} $inputFileSize%12s ${outputFileSize.getOrElse("")}%12s ${settings.library}%-11s ${settings.codingMode}%-15s ${settings.fidelityOptionMode}%-13s ${settings.formattedFidelityOptions()} ${settings.xsdFile.map(_ => "Y").getOrElse("-")} ${duration.toMillis}%10s ${feedback.getOrElse("")}"
  }
}

object DecodeResult {
  def header(fileWidth: Int): String = {
    // As in EncodeResult, without ratio; swap "Size" and "Enc size"
    val tmp = "File".padTo(fileWidth, ' ')
    f"$tmp ${"Enc size"}%12s ${"Size"}%12s ${"Library"}%-11s ${"Coding mode"}%-15s ${"FidOM"}%-13s CIDPL S ${"T [ms]"}%10s"
  }
}

case class ValidateResult(// .exi or .zip
                          inputFile: File,
                          settings: Settings,
                          ok: Boolean,
                          inputFileSize: Long,
                          feedback: Option[String]) {

  val baseFileName = {
    val pos = inputFile.getPath.indexOf(".xml")
    if (pos != -1) {
      inputFile.getPath.substring(0, pos)
    } else {
      val pos = inputFile.getPath.indexOf(".zip")
      inputFile.getPath.substring(0, pos)
    }
  }

  def formatted(fileWidth: Int, duration: Duration): String = {
    // As in DecodeResult, using inputFile instead of outputFile; only input file size
    f"${inputFile.getPath.padTo(fileWidth, ' ')} $inputFileSize%12s ${settings.library}%-11s ${settings.codingMode}%-15s ${settings.fidelityOptionMode}%-13s ${settings.formattedFidelityOptions()} ${settings.xsdFile.map(_ => "Y").getOrElse("-")} ${duration.toMillis}%10s ${feedback.getOrElse("")}"
  }
}

object ValidateResult {
  def header(fileWidth: Int): String = {
    val tmp = "File".padTo(fileWidth, ' ')
    f"$tmp ${"Enc size"}%12s ${"Library"}%-11s ${"Coding mode"}%-15s ${"FidOM"}%-13s CIDPL S ${"T [ms]"}%10s"
  }
}

case class ZipDecodeResult(inputFile: ZipFile,
                           settings: Settings,
                           ok: Boolean,
                           inputFileSize: Long,
                           feedback: Option[String]) {

  val baseFileName = {
    val pos = inputFile.getPath.indexOf(".zip")
    inputFile.getPath.substring(0, pos)
  }

  def formatted(fileWidth: Int, duration: Duration): String = {
    f"${inputFile.getPath.padTo(fileWidth, ' ')} $inputFileSize%12s ${settings.library}%-11s ${settings.codingMode}%-15s ${settings.fidelityOptionMode}%-13s ${settings.formattedFidelityOptions()} ${settings.xsdFile.map(_ => "Y").getOrElse("-")} ${duration.toMillis}%10s ${feedback.getOrElse("")}"
  }
}

object ZipDecodeResult {
  def header(fileWidth: Int): String = {
    val tmp = "File".padTo(fileWidth, ' ')
    f"$tmp ${"Enc size"}%12s ${"Library"}%-11s ${"Coding mode"}%-15s ${"FidOM"}%-13s CIDPL S ${"T [ms]"}%10s"
  }
}
