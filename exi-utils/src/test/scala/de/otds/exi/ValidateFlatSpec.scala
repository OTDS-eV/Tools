package de.otds.exi

import de.otds.exi.util.ResourceManager
import org.scalatest.{FlatSpec, GivenWhenThen, MustMatchers}

class ValidateFlatSpec extends FlatSpec with MustMatchers with GivenWhenThen with ResourceManager with TestData {
  import TestData.CarParking._

  "exi-utils" must "validate files" in {
    withTmpDirectory { tmpDir =>
      Given("test data and encode args")
      setupTestData()
      val encodeArgs = new Args(Array(
        "encode",
        "-o", tmpDir.getPath,
        "-x", XsdFile.getPath,
        InputFile.getPath))

      When("exi-utils is called with encode args")
      val encodeResults = ExiUseCases.encode(encodeArgs)

      Then("there must be encode results")
      assert(encodeResults.nonEmpty)

      When("the encoded files are validated")
      val encodedFiles = encodeResults.collect { case (er, _) if er.ok => er.outputFile }
      val validateArgs = new Args(Array("validate") ++ encodedFiles.map(_.getPath))
      val validateResults = ExiUseCases.validate(validateArgs).map { case (vr, _) => vr }

      Then("there must be validation results")
      assert(validateResults.size == encodedFiles.size)
      assert(validateResults.map(_.settings.library).distinct.size == Library.Values.size)
      assert(validateResults.map(_.settings.codingMode).distinct.size == CodingMode.Values.size)
      assert(validateResults.map(_.settings.fidelityOptionMode).distinct.size == FidelityOptionMode.Values.size)
    }
  }
}