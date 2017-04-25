package de.otds.exi

import java.io.File

import de.otds.exi.util.ResourceManager
import org.scalatest.{FlatSpec, GivenWhenThen, MustMatchers}

class EncodeFlatSpec extends FlatSpec with MustMatchers with GivenWhenThen with ResourceManager with TestData {
  "exi-utils" must "encode a single file (using short options)" in {
    import TestData.CarParking._
    withTmpDirectory { tmpDir =>
      Given("test data and encode args")
      setupTestData()
      val args = new Args(Array(
        "encode",
        "-x", XsdFile.getPath,
        "-o", tmpDir.getPath,
        InputFile.getPath))

      When("exi-utils is called with encode args")
      val results = ExiUseCases.encode(args)

      Then("there must be encoding results for all libraries/coding modes/fidelity options")
      assert(results.nonEmpty)
      val encodeResults = results.map { case (er, _) => er }
      assert(encodeResults.map(_.settings.library).distinct.size == Library.Values.size)
      assert(encodeResults.map(_.settings.codingMode).distinct.size == CodingMode.Values.size)
      assert(encodeResults.map(_.settings.fidelityOptionMode).distinct.size == FidelityOptionMode.Values.size)

      And("output files must exist")
      assert(encodeResults.forall(er => er.ok == false || er.outputFile.exists()))

      And("there must be a properties file for each output file")
      assert(encodeResults.map(er => new File(er.outputFile.getPath + ".properties")).forall(f => f.exists()))
    }
  }

  "exi-utils" must "encode all .xml files in a directory (using long options)" in {
    import TestData.CarParking._
    withTmpDirectory { tmpDir =>
      Given("test data and encode args")
      setupTestData()
      val args = new Args(Array(
        "encode",
        "--libraries", GzipCc.id,
        "--xsd-file", XsdFile.getPath,
        "--coding-modes", Size.id,
        "--fidelity-option-modes", NotApplicable.id,
        "--output-directory", tmpDir.getPath,
        InputDirectory.getPath))

      When("exi-utils is called with encode args")
      val results = ExiUseCases.encode(args)

      Then("there must be encoding results")
      assert(results.nonEmpty)

      val encodeResults = results.map { case (er, _) => er }
      assert(encodeResults.map(_.settings.library).distinct.size == 1)
      assert(encodeResults.head.settings.library == GzipCc)

      assert(encodeResults.map(_.settings.codingMode).distinct.size == 1)
      assert(encodeResults.head.settings.codingMode == Size)

      assert(encodeResults.map(_.settings.fidelityOptionMode).distinct.size == 1)
      assert(encodeResults.head.settings.fidelityOptionMode == NotApplicable)

      And("output files must exist")
      val inputFiles = InputDirectory.listFiles().filter(_.getName.endsWith(".xml"))
      assert(inputFiles.size == encodeResults.size)
      assert(encodeResults.forall(er => er.outputFile.exists()))

      And("there must be a properties file for each output file")
      assert(encodeResults.map(er => new File(er.outputFile.getPath + ".properties")).forall(f => f.exists()))
    }
  }

  "exi-utils" must "encode a single file using Exi.main()" in {
    import TestData.CarParking._
    withTmpDirectory { tmpDir =>
      Given("test data and encode args")
      setupTestData()
      val args = Array(
        "encode",
        "-o", tmpDir.getPath,
        InputFile.getPath)

      When("exi-utils is called with encode args")
      Exi.main(args)

      Then("output files must exist")
      val f = new File(tmpDir, InputFile.getName + "." + Exificient.id.toLowerCase() + "." + Compression.id.toLowerCase() + "." + Strict.id.toLowerCase + ".exi")
      assert(f.exists())
    }
  }

  "exi-utils" must "encode a single file using OpenExi" in {
    import TestData.Persons._
    withTmpDirectory { tmpDir =>
      Given("encode args")
      val args = new Args(Array(
        "encode",
        "-l", OpenExi.id,
        "-x", XsdFile.getPath,
        "-c", Compression.id,
        "-f", Strict.id,
        "-o", tmpDir.getPath,
        InputFile.getPath))

      When("exi-utils are called with encode args")
      val results = ExiUseCases.encode(args)

      Then("there must be encoding results for all libraries/coding modes/fidelity options")
      assert(results.size == 1)
      val encodeResults = results.map { case (er, _) => er }

      And("output files must exist")
      assert(encodeResults.forall(er => er.outputFile.exists()))

      And("there must be a properties file for each output file")
      assert(encodeResults.map(er => new File(er.outputFile.getPath + ".properties")).forall(f => f.exists()))
    }
  }
}
