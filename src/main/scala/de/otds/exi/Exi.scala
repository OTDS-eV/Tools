package de.otds.exi

import java.io.File

import de.otds.exi.build.BuildInfo
import de.otds.exi.impl.{DecodeResult, EncodeResult, ValidateResult, ZipDecodeResult}
import de.otds.exi.util.RichFileContainer._
import de.otds.exi.util.{ResourceManager, Timing}
import org.rogach.scallop._

import scala.concurrent.duration.Duration
import scala.language.{implicitConversions, reflectiveCalls}

class Args(args: Array[String]) extends ScallopConf(args) {
  version(s"${BuildInfo.name} ${BuildInfo.version} build ${BuildInfo.buildInfoBuildNumber}")

  banner(
    """
      |usage: exi-utils [--version] [--help] <sub-command> <args>
    """.stripMargin)

  val testData = new Subcommand("testdata") {
    val download = toggle(name = "download", short = 'd')
    banner("Download test data")
    footer("")
  }
  addSubcommand(testData)

  val encode = new Subcommand("encode") {
    val libraryIds = opt[String](name = "libraries", short = 'l', required = true, default = Some(Library.Values.map(_.id).mkString(",")))

    val xsdFile = opt[File](name = "xsd-file", short = 'x')

    val codingModeIds = opt[String](name = "coding-modes", short = 'c', required = true, default = Some(CodingMode.Values.map(_.id).mkString(",")))

    val fidelityOptionModeIds = opt[String](name = "fidelity-option-modes", short = 'f', required = true, default = Some(FidelityOptionMode.Values.map(_.id).mkString(",")))

    val preserveComments = toggle(name = "preserve-comments", short = 'C')
    val preserveProcessingInstructions = toggle(name = "preserve-processing-instructions", short = 'I')
    val preserveDtdsAndEntityReferences = toggle(name = "preserve-dtds-and-entity-references", short = 'D')
    val preservePrefixes = toggle(name = "preserve-prefixes", short = 'P')
    val preserveLexicalValues = toggle(name = "preserve-lexical-values", short = 'L')

    val outputDirectory = opt[File](name = "output-directory", short = 'o', required = true, default = Some(new File("/tmp")))

    val verbose = toggle(name = "verbose", short = 'v')

    val fileNames = trailArg[List[String]]()

    def libraries(): List[Library] = libraryIds().split(",").flatMap(id => Library(id)).toList

    def codingModes(): List[CodingMode] = codingModeIds().split(",").flatMap(id => CodingMode(id)).toList

    def fidelityOptionModes(): List[FidelityOptionMode] = fidelityOptionModeIds().split(",").flatMap(id => FidelityOptionMode(id)).toList

    def rawFiles(): List[File] = fileNames().map(f => new File(f))

    def files(): List[File] = rawFiles().flatMap(_.expand(".xml"))

    banner("Encode files")
    footer(
      s"""
         |Supported libraries: ${Library.Values.map(_.id).mkString(", ")}
         |Supported coding modes: ${CodingMode.Values.map(_.id).mkString(", ")}
         |Supported fidelity options: ${FidelityOptionMode.Values.map(_.id).mkString(", ")}
         |
        |""".stripMargin)
  }
  addSubcommand(encode)

  val decode = new Subcommand("decode") {
    val outputDirectory = opt[File](name = "output-directory", short = 'o', required = true, default = Some(new File("/tmp")))

    val fileNames = trailArg[List[String]]()

    def rawFiles(): List[File] = fileNames().map(f => new File(f))

    def files(): List[File] = rawFiles().flatMap(_.expand(".exi"))

    banner("Decode files")
    footer("")
  }
  addSubcommand(decode)

  val validate = new Subcommand("validate") {
    val fileNames = trailArg[List[String]]()

    def rawFiles(): List[File] = fileNames().map(f => new File(f))

    def files(): List[File] = rawFiles().flatMap(_.expand(".exi"))

    banner("Validate encoded files")
    footer("")
  }
  addSubcommand(validate)

  val zipEncode = new Subcommand("zip-encode") {
    val libraryIds = opt[String](name = "libraries", short = 'l', required = true, default = Some(Library.Values.map(_.id).mkString(",")))

    val xsdFile = opt[File](name = "xsd-file", short = 'x')

    val codingModeIds = opt[String](name = "coding-modes", short = 'c', required = true, default = Some(CodingMode.Values.map(_.id).mkString(",")))

    val fidelityOptionModeIds = opt[String](name = "fidelity-option-modes", short = 'f', required = true, default = Some(FidelityOptionMode.Values.map(_.id).mkString(",")))

    val preserveComments = toggle(name = "preserve-comments", short = 'C')
    val preserveProcessingInstructions = toggle(name = "preserve-processing-instructions", short = 'I')
    val preserveDtdsAndEntityReferences = toggle(name = "preserve-dtds-and-entity-references", short = 'D')
    val preservePrefixes = toggle(name = "preserve-prefixes", short = 'P')
    val preserveLexicalValues = toggle(name = "preserve-lexical-values", short = 'L')

    val skipFileNames = opt[String](name = "skip", short = 's', required = false, default = Some(""))

    val outputDirectory = opt[File](name = "output-directory", short = 'o', required = true, default = Some(new File("/tmp")))

    val verbose = toggle(name = "verbose", short = 'v')

    val fileNames = trailArg[List[String]]()

    def libraries(): List[Library] = libraryIds().split(",").flatMap(id => Library(id)).toList

    def codingModes(): List[CodingMode] = codingModeIds().split(",").flatMap(id => CodingMode(id)).toList

    def fidelityOptionModes(): List[FidelityOptionMode] = fidelityOptionModeIds().split(",").flatMap(id => FidelityOptionMode(id)).toList

    def rawFiles(): List[File] = fileNames().map(f => new File(f))

    def files(): List[File] = rawFiles().flatMap(_.expand(".zip"))

    banner("Zip-Encode files: Read a .zip file, encode the .xml files, and write the encoded files into another .zip")
    footer(
      s"""
         |Supported libraries: ${Library.Values.map(_.id).mkString(", ")}
         |Supported coding modes: ${CodingMode.Values.map(_.id).mkString(", ")}
         |Supported fidelity options: ${FidelityOptionMode.Values.map(_.id).mkString(", ")}
         |
        |""".stripMargin)
  }
  addSubcommand(zipEncode)

  val zipDecode = new Subcommand("zip-decode") {
    val outputDirectory = opt[File](name = "output-directory", short = 'o', required = true, default = Some(new File("/tmp")))

    val fileNames = trailArg[List[String]]()

    def rawFiles(): List[File] = fileNames().map(f => new File(f))

    def files(): List[File] = rawFiles().flatMap(_.expand(".zip"))

    banner("Zip-Decode files: Read .zip file with encoded files, and write decoded files into a directory")
    footer("")
  }
  addSubcommand(zipDecode)

  val zipValidate = new Subcommand("zip-validate") {
    val fileNames = trailArg[List[String]]()

    def rawFiles(): List[File] = fileNames().map(f => new File(f))

    def files(): List[File] = rawFiles().flatMap(_.expand(".zip"))

    banner("Zip-Validate encoded files: Validate encoded files in a .zip file")
    footer("")
  }
  addSubcommand(zipValidate)

  verify()
}

object ExiUseCases extends Timing with ResourceManager with TestData {
  def testData(args: Args): Unit = {
    if (args.testData.download.isDefined) {
      setupTestData()
    }
  }

  def encode(args: Args): List[(EncodeResult, Duration)] = {
    checkXsdFile(args.encode.xsdFile.toOption)
    checkFiles(args.encode.rawFiles())

    val fo =
      FidelityOptions(
        args.encode.preserveComments.toOption.getOrElse(false),
        args.encode.preserveProcessingInstructions.toOption.getOrElse(false),
        args.encode.preserveDtdsAndEntityReferences.toOption.getOrElse(false),
        args.encode.preservePrefixes.toOption.getOrElse(false),
        args.encode.preserveLexicalValues.toOption.getOrElse(false))

    val results = args.encode.libraries().zipWithIndex.flatMap { case (lib, libIndex) =>
      println(s"= Encode using ${lib.id} =")
      val impl = lib.instance()

      val (libResults, libDuration) = measure {
        args.encode.files().flatMap { f =>
          for {
            cm <- args.encode.codingModes()
            fom <- args.encode.fidelityOptionModes()
            if impl.supports(cm, fom)
          } yield {
            print(s"  Encoding $f ... ")
            val (result, duration) = measure {
              impl.encode(f, args.encode.outputDirectory(), Settings(lib, args.encode.xsdFile.toOption, cm, fom, fo))
            }

            if (args.encode.verbose.isDefined) {
              println(s"$duration ${if (result.ok) "OK" else s"FAILED: ${result.settings}"}")
            } else {
              println(s"$duration")
            }

            (result, duration)
          }
        }
      }

      println()
      println(s"$lib duration: $libDuration")
      println()

      libResults
    }

    if (results.nonEmpty) {
      val (okResults, errorResults) = results.partition { case (result, _) => result.ok }
      val width = results.map { case (result, _) => result.outputFile.getPath.length }.max

      println("= Results = ")
      println()

      println("== By size ==")
      println()
      println(EncodeResult.header(width))
      // Group by input file!
      // Sort by library ID: If there is no .xsd, the files will have the same size!
      okResults
        .sortBy { case (result, _) => (result.inputFile, result.outputFileSize, result.settings.library.id) }
        .foreach { case (result, duration) =>
          println(result.formatted(width, duration))
        }

      println()

      println("== By duration ==")
      println()
      println(EncodeResult.header(width))
      okResults
        .sortBy { case (result, duration) => (result.inputFile, duration, result.settings.library.id) }
        .foreach { case (result, duration) =>
          println(result.formatted(width, duration))
        }

      if (errorResults.nonEmpty) {
        println()
        println("= Errors =")
        println()
        errorResults
          .sortBy { case (result, _) => result.outputFile }
          .foreach { case (result, duration) =>
            println(result.formatted(width, duration))
          }
      }
    }

    results
  }

  def decode(args: Args): List[(DecodeResult, Duration)] = {
    checkFiles(args.decode.rawFiles())

    case class Param(inputFile: File, settings: Settings)

    val params = args.decode.files().map { f =>
      val settings = Settings(new File(f + ".properties"))
      Param(f, settings)
    }

    val results = params.groupBy(_.settings.library).toList.flatMap { case (library, ps) =>
      println(s"= Decode using $library =")
      val (libResults, libDuration) = measure {
        val impl = library.instance()
        val result = ps.map { case Param(f, settings) =>
          print(s"  Decoding $f ... ")
          val (result, duration) = measure {
            impl.decode(f, args.decode.outputDirectory(), settings)
          }
          println(s"$duration")

          (result, duration)
        }
        println()

        result
      }
      println(s"$library duration: $libDuration")
      println()

      libResults
    }

    val (okResults, errorResults) = results.partition { case (result, _) => result.ok }
    val width = results.map { case (result, _) => result.outputFile.getPath.length }.max

    println()
    println("= Results =")
    println()
    println(DecodeResult.header(width))
    okResults
      .sortBy { case (result, duration) => (result.baseFileName, duration.toMillis) }
      .foreach { case (result, duration) =>
        println(result.formatted(width, duration))
      }

    if (errorResults.nonEmpty) {
      println()
      println("= Errors =")
      println()
      errorResults
        .sortBy { case (result, _) => result.outputFile }
        .foreach { case (result, duration) =>
          println(result.formatted(width, duration))
        }
    }

    results
  }

  def validate(args: Args): List[(ValidateResult, Duration)] = {
    checkFiles(args.validate.rawFiles())

    case class Param(inputFile: File, settings: Settings)

    val params = args.validate.files().map { f =>
      val settings = Settings(new File(f + ".properties"))
      Param(f, settings)
    }

    val results = params.groupBy(_.settings.library).toList.flatMap { case (library, ps) =>
      println(s"= Validate using $library =")
      val (libResults, libDuration) = measure {
        val impl = library.instance()
        val result = ps.map { case Param(f, settings) =>
          print(s"  Validating $f ... ")
          val (result, duration) = measure {
            impl.validate(f, settings)
          }
          println(s"$duration")

          (result, duration)
        }
        println()

        result
      }
      println(s"$library duration: $libDuration")
      println()

      libResults
    }

    val (okResults, errorResults) = results.partition { case (result, _) => result.ok }
    val width = results.map { case (result, _) => result.inputFile.getPath.length }.max

    println()
    println("= Results =")
    println()
    println(ValidateResult.header(width))
    okResults
      .sortBy { case (result, duration) => (result.baseFileName, duration.toMillis) }
      .foreach { case (result, duration) =>
        println(result.formatted(width, duration))
      }

    if (errorResults.nonEmpty) {
      println()
      println("= Errors =")
      println()
      errorResults
        .sortBy { case (result, _) => result.inputFile }
        .foreach { case (result, duration) =>
          println(result.formatted(width, duration))
        }
    }

    results
  }

  def zipEncode(args: Args): List[(EncodeResult, Duration)] = {
    checkXsdFile(args.zipEncode.xsdFile.toOption)
    checkFiles(args.zipEncode.rawFiles())

    val fo =
      FidelityOptions(
        args.zipEncode.preserveComments.toOption.getOrElse(false),
        args.zipEncode.preserveProcessingInstructions.toOption.getOrElse(false),
        args.zipEncode.preserveDtdsAndEntityReferences.toOption.getOrElse(false),
        args.zipEncode.preservePrefixes.toOption.getOrElse(false),
        args.zipEncode.preserveLexicalValues.toOption.getOrElse(false))

    val results = args.zipEncode.libraries().zipWithIndex.flatMap { case (lib, libIndex) =>
      println(s"= Zip-Encode using ${lib.id} =")
      val impl = lib.instance()

      val (libResults, libDuration) = measure {
        args.zipEncode.files().flatMap { f =>
          for {
            cm <- args.zipEncode.codingModes()
            fom <- args.zipEncode.fidelityOptionModes()
            if impl.supports(cm, fom)
          } yield {
            print(s"  Encoding $f ... ")
            val (result, duration) = measure {
              impl.zipEncode(
                f,
                args.zipEncode.outputDirectory(),
                Settings(lib, args.zipEncode.xsdFile.toOption, cm, fom, fo),
                args.zipEncode.skipFileNames().split(",").toList)(f => print(s"$f "))
            }

            if (args.zipEncode.verbose.isDefined) {
              println(s"... $duration ${if (result.ok) "OK" else s"FAILED: ${result.settings}"}")
            } else {
              println(s"... $duration")
            }

            (result, duration)
          }
        }
      }

      println()
      println(s"$lib duration: $libDuration")
      println()

      libResults
    }

    if (results.nonEmpty) {
      val (okResults, errorResults) = results.partition { case (result, _) => result.ok }
      val width = results.map { case (result, _) => result.outputFile.getPath.length }.max

      println("= Results = ")
      println()

      println("== By size ==")
      println()
      println(EncodeResult.header(width))
      // Group by input file!
      // Sort by library ID: If there is no .xsd, the files will have the same size!
      okResults
        .sortBy { case (result, _) => (result.inputFile, result.outputFileSize, result.settings.library.id) }
        .foreach { case (result, duration) =>
          println(result.formatted(width, duration))
        }

      println()

      println("== By duration ==")
      println()
      println(EncodeResult.header(width))
      okResults
        // TODO C In contrast to encode(), duration.toMillis is required, otherwise compile error
        .sortBy { case (result, duration) => (result.inputFile, duration.toMillis, result.settings.library.id) }
        .foreach { case (result, duration) =>
          println(result.formatted(width, duration))
        }

      if (errorResults.nonEmpty) {
        println()
        println("= Errors =")
        println()
        errorResults
          .sortBy { case (result, _) => result.outputFile }
          .foreach { case (result, duration) =>
            println(result.formatted(width, duration))
          }
      }
    }

    results
  }

  def zipDecode(args: Args): List[(ZipDecodeResult, Duration)] = {
    checkFiles(args.zipDecode.rawFiles())

    case class Param(inputFile: File, settings: Settings)

    val params = args.zipDecode.files().map { f =>
      val settings = Settings(new File(f + ".properties"))
      Param(f, settings)
    }

    val results = params.groupBy(_.settings.library).toList.flatMap { case (library, ps) =>
      println(s"= Zip-Decode using $library =")
      val (libResults, libDuration) = measure {
        val impl = library.instance()
        val result = ps.map { case Param(f, settings) =>
          print(s"  Decoding $f ... ")
          val (result, duration) = measure {
            impl.zipDecode(f, args.zipDecode.outputDirectory(), settings)(f => print(s"$f "))
          }
          println(s"... $duration")

          (result, duration)
        }
        println()

        result
      }
      println(s"$library duration: $libDuration")
      println()

      libResults
    }

    val (okResults, errorResults) = results.partition { case (result, _) => result.ok }
    val width = results.map { case (result, _) => result.inputFile.getPath.length }.max

    println()
    println("= Results =")
    println()
    println(ZipDecodeResult.header(width))
    okResults
      .sortBy { case (result, duration) => (result.baseFileName, duration.toMillis) }
      .foreach { case (result, duration) =>
        println(result.formatted(width, duration))
      }

    if (errorResults.nonEmpty) {
      println()
      println("= Errors =")
      println()
      errorResults
        .sortBy { case (result, _) => result.inputFile }
        .foreach { case (result, duration) =>
          println(result.formatted(width, duration))
        }
    }

    results
  }

  def zipValidate(args: Args): List[(ValidateResult, Duration)] = {
    checkFiles(args.validate.rawFiles())

    case class Param(inputFile: File, settings: Settings)

    val params = args.validate.files().map { f =>
      val settings = Settings(new File(f + ".properties"))
      Param(f, settings)
    }

    val results = params.groupBy(_.settings.library).toList.flatMap { case (library, ps) =>
      println(s"= Zip-Validate using $library =")
      val (libResults, libDuration) = measure {
        val impl = library.instance()
        val result = ps.map { case Param(f, settings) =>
          print(s"  Validating $f ... ")
          val (result, duration) = measure {
            impl.zipValidate(f, settings)(f => print(s"$f "))
          }
          println(s"... $duration")

          (result, duration)
        }
        println()

        result
      }
      println(s"$library duration: $libDuration")
      println()

      libResults
    }

    val (okResults, errorResults) = results.partition { case (result, _) => result.ok }
    val width = results.map { case (result, _) => result.inputFile.getPath.length }.max

    println()
    println("= Results =")
    println()
    println(ValidateResult.header(width))
    okResults
      .sortBy { case (result, duration) => (result.baseFileName, duration.toMillis) }
      .foreach { case (result, duration) =>
        println(result.formatted(width, duration))
      }

    if (errorResults.nonEmpty) {
      println()
      println("= Errors =")
      println()
      errorResults
        .sortBy { case (result, _) => result.inputFile }
        .foreach { case (result, duration) =>
          println(result.formatted(width, duration))
        }
    }

    results
  }

  /**
    * If an .xsd file was specified on the command-line, it must exist and it must be readable.
    * @param xsdFile
    */
  private def checkXsdFile(xsdFile: Option[XsdFile]): Unit = {
    xsdFile.foreach { f =>
      if (f.canRead == false) {
        System.err.println(s"XSD file $f does not exist")
        System.exit(1)
      }
    }
  }

  /**
    * Files/directories specified on the command-line must exist.
    * @param files
    */
  private def checkFiles(files: List[File]): Unit = {
    files.foreach { f =>
      if (f.exists() == false) {
        System.err.println(s"File/directory $f does not exist")
        System.exit(1)
      }
    }
  }

  def showMemorySettings(): Unit = {
    val rt = Runtime.getRuntime
    val maxMb = rt.maxMemory() / 1024 / 1024
    val totalMb = rt.totalMemory() / 1024 / 1024
    val freeMb = rt.freeMemory() / 1024 / 1024
    val usedMb = totalMb - freeMb
    println("= Memory =")
    println()
    println(f"Max  : $maxMb%12s MB")
    println(f"Total: $totalMb%12s MB")
    println(f"Free : $freeMb%12s MB")
    println(f"Used : $usedMb%12s MB")
    println()
  }
}

object Exi extends App {
  val exiUtilsArgs = new Args(args)

  exiUtilsArgs.subcommand match {
    case Some(exiUtilsArgs.testData) =>
      ExiUseCases.testData(exiUtilsArgs)

    case Some(exiUtilsArgs.encode) =>
      ExiUseCases.encode(exiUtilsArgs)

    case Some(exiUtilsArgs.decode) =>
      ExiUseCases.decode(exiUtilsArgs)

    case Some(exiUtilsArgs.validate) =>
      ExiUseCases.validate(exiUtilsArgs)

    case Some(exiUtilsArgs.zipEncode) =>
      ExiUseCases.zipEncode(exiUtilsArgs)

    case Some(exiUtilsArgs.zipDecode) =>
      ExiUseCases.zipDecode(exiUtilsArgs)

    case Some(exiUtilsArgs.zipValidate) =>
      ExiUseCases.zipValidate(exiUtilsArgs)

    case _ =>
      exiUtilsArgs.printHelp()
      println()
      ExiUseCases.showMemorySettings()
  }
}
