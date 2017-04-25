package de.otds

import java.io._
import java.util.Properties

import de.otds.exi.impl.LibraryImpl
import de.otds.exi.util.ResourceManager

package object exi {
  type Directory = File

  type ExiFile = File
  type XmlFile = File
  type XsdFile = File
  type ZipFile = File

  //

  trait IdentifiableCaseObject {
    // Drop trailing "$"
    final def id: String = getClass.getSimpleName.dropRight(1)
  }

  trait Resolver[T <: IdentifiableCaseObject] {
    val Values: List[T]

    def apply(id: String): Option[T] = {
      Values.find(_.id.toLowerCase == id.toLowerCase)
    }
  }

  //

  sealed trait Library extends IdentifiableCaseObject {
    def instance(): LibraryImpl
  }

  case object Exificient extends Library {
    override def instance(): LibraryImpl = new impl.ExificientLibraryImpl()
  }

  case object OpenExi extends Library {
    override def instance(): LibraryImpl = new impl.OpenExiLibraryImpl()
  }

  /**
    * The JDK implementation of GZIP.
    */
  case object Gzip extends Library {
    override def instance(): LibraryImpl = new impl.GzipLibraryImpl()
  }

  case object Lzma extends Library {
    override def instance(): LibraryImpl = new impl.LzmaLibraryImpl()
  }

  case object Bzip2 extends Library {
    override def instance(): LibraryImpl = new impl.Bzip2LibraryImpl()
  }

  case object Xz extends Library {
    override def instance(): LibraryImpl = new impl.XzLibraryImpl()
  }

  /**
    * The GZIP implementation of Apache commons-compress.
    *
    * In contrast to the JDK implementation the compression level can be set.
    */
  case object GzipCc extends Library {
    override def instance(): LibraryImpl = new impl.GzipCcLibraryImpl()
  }

  /**
    * Just "pass through". No compression at all.
    *
    * This way we can use zip-encode, zip-decode, and zip-validate.
    * Thus we can compare the performance of "plain zip" with any other codecs.
    */
  case object PassThrough extends Library {
    override def instance(): LibraryImpl = new impl.PassThroughLibraryImpl()
  }

  object Library extends Resolver[Library] {
    val Values = List(Exificient, OpenExi, Gzip, Lzma, Bzip2, Xz, GzipCc, PassThrough)
  }

  //

  /**
    * Quoted from https://www.w3.org/TR/exi/#key-alignmentOption
    *
    * "The alignment option is used to control the alignment of event codes and content items.
    * The value is one of bit-packed, byte-alignment or pre-compression.
    *
    * When the value of compression option is set to true,
    * alignment of the EXI Body is governed by the rules specified in 9. EXI Compression instead of the alignment option value."
    */
  sealed trait CodingMode extends IdentifiableCaseObject

  /**
    * Quoted from http://openexi.sourceforge.net/tutorial/example2.htm:
    *
    * "EXI files, by default, are bit-packed.
    * Information is stored using the fewest number of bits required, regardless of the byte boundaries.
    * Conceptually, this allows 8 Boolean values to be stored in a single Byte."
    */
  case object BitPacked extends CodingMode

  /**
    * Quoted from http://openexi.sourceforge.net/tutorial/example2.htm:
    *
    * "The byte-aligned option encodes the document content aligned on byte boundaries.
    * This usually results in EXI streams that are larger than the bit-packed equivalent.
    * However, these files can be viewed with a text editor for troubleshooting encoding and decoding routines."
    */
  case object BytePacked extends CodingMode

  /**
    * Quoted from http://openexi.sourceforge.net/tutorial/example2.htm:
    *
    * "The pre-compression option performs all transformations on the XML file except for the final step of applying the DEFLATE algorithm.
    * The primary purpose of pre-compression is to avoid a duplicate compression step when compression capability is built in to the transport protocol."
    */
  case object PreCompression extends CodingMode

  /**
    * Quoted from http://openexi.sourceforge.net/tutorial/example2.htm:
    *
    * "When the compress option is selected, the W3C specification states explicitly that alignment options are ignored,
    * and the file is processed per the specification to achieve the smallest possible size."
    */
  case object Compression extends CodingMode

  /**
    * Used by Gzip
    */
  case object Size extends CodingMode

  /**
    * Used by Gzip
    */
  case object Speed extends CodingMode

  /**
    * Used by all implementations which are not supporting any parameters.
    */
  case object Default extends CodingMode

  object CodingMode extends Resolver[CodingMode] {
    val Values = List(BitPacked, BytePacked, PreCompression, Compression, Size, Speed, Default)
  }

  //

  sealed trait FidelityOptionMode extends IdentifiableCaseObject

  case object Strict extends FidelityOptionMode

  case object All extends FidelityOptionMode

  case object Customized extends FidelityOptionMode

  case object NotApplicable extends FidelityOptionMode

  object FidelityOptionMode extends Resolver[FidelityOptionMode] {
    val Values = List(Strict, All, Customized, NotApplicable)
  }

  //

  case class FidelityOptions(// Cannot be used in strict mode
                             preserveComments: Boolean,
                             // Cannot be used in strict mode
                             preserveProcessingInstructions: Boolean,
                             // Cannot be used in strict mode
                             preserveDtdsAndEntityReferences: Boolean,
                             // Cannot be used in strict mode
                             preservePrefixes: Boolean,

                             // Can be used in strict mode
                             preserveLexicalValues: Boolean) {

    def intoProperties(props: Properties) = {
      props.put("preserveComments", preserveComments.toString)
      props.put("preserveProcessingInstructions", preserveProcessingInstructions.toString)
      props.put("preserveDtdsAndEntityReferences", preserveDtdsAndEntityReferences.toString)
      props.put("preservePrefixes", preservePrefixes.toString)
      props.put("preserveLexicalValues", preserveLexicalValues.toString)
    }

    def formatted(): String = {
      val c = if (preserveComments) "C" else "-"
      val pi = if (preserveProcessingInstructions) "I" else "-"
      val d = if (preserveDtdsAndEntityReferences) "D" else "-"
      val p = if (preservePrefixes) "P" else "-"
      val l = if (preserveLexicalValues) "L" else "-"
      c + pi + d + p + l
    }
  }

  object FidelityOptions {
    def apply(properties: Properties): FidelityOptions = {
      FidelityOptions(
        Option(properties.getProperty("preserveComments")).contains("true"),
        Option(properties.getProperty("preserveProcessingInstructions")).contains("true"),
        Option(properties.getProperty("preserveDtdsAndEntityReferences")).contains("true"),
        Option(properties.getProperty("preservePrefixes")).contains("true"),
        Option(properties.getProperty("preserveLexicalValues")).contains("true"))
    }
  }

  case class Settings(library: Library,
                      xsdFile: Option[XsdFile],
                      codingMode: CodingMode,
                      fidelityOptionMode: FidelityOptionMode,
                      // Are ignored in coding mode "all"
                      // Are ignored in coding mode "strict" (except for "preserve lexical values")
                      fidelityOptions: FidelityOptions) {

    private val AllFidelityOptions =
      FidelityOptions(
        preserveComments = true,
        preserveProcessingInstructions = true,
        preserveDtdsAndEntityReferences = true,
        preservePrefixes = true,
        preserveLexicalValues = true)

    private val NoFidelityOptions =
      FidelityOptions(
        preserveComments = false,
        preserveProcessingInstructions = false,
        preserveDtdsAndEntityReferences = false,
        preservePrefixes = false,
        preserveLexicalValues = false)

    private val LexicalValueOnlyFidelityOptions =
      FidelityOptions(
        preserveComments = false,
        preserveProcessingInstructions = false,
        preserveDtdsAndEntityReferences = false,
        preservePrefixes = false,
        preserveLexicalValues = true)

    /**
      * Shows the effective fidelity options (considering the coding mode)
      *
      * @return The formatted string.
      */
    def formattedFidelityOptions(): String = {
      fidelityOptionMode match {
        case Strict if fidelityOptions.preserveLexicalValues =>
          LexicalValueOnlyFidelityOptions.formatted()

        case Strict if !fidelityOptions.preserveLexicalValues =>
          NoFidelityOptions.formatted()

        case All =>
          AllFidelityOptions.formatted()

        case Customized =>
          fidelityOptions.formatted()

        case NotApplicable =>
          NoFidelityOptions.formatted()
      }
    }

    /**
      * @return A fragment to build unique file names
      */
    def fileSuffix: String = {
      s"${library.id.toLowerCase}.${codingMode.id.toLowerCase}.${fidelityOptionMode.id.toLowerCase()}"
    }
  }

  object Settings extends ResourceManager {
    def apply(file: File): Settings = {
      //println(s"  Reading $propsFile")

      def string2option(s: String): Option[String] = {
        if (s == null || s.trim() == "") {
          None
        } else {
          Some(s.trim())
        }
      }

      val props = new Properties()
      withResource(new BufferedReader(new FileReader(file))) { r =>
        props.load(r)

        Settings(
          Library(props.getProperty("library")).get,
          string2option(props.getProperty("xsdFile.absolutePath")).map(s => new File(s)),
          CodingMode(props.getProperty("codingMode")).get,
          FidelityOptionMode(props.getProperty("fidelityOptionMode")).get,
          FidelityOptions(props))
      }
    }
  }

}
