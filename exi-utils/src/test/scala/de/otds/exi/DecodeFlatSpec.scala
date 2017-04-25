package de.otds.exi

import de.otds.exi.util.ResourceManager
import org.scalatest.{FlatSpec, GivenWhenThen, MustMatchers}

class DecodeFlatSpec extends FlatSpec with MustMatchers with GivenWhenThen with ResourceManager with TestData {
  import TestData.CarParking._

  "exi-utils" must "decode files" in {
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

      When("the encoded files are decoded")
      val encodedFiles = encodeResults.collect { case (er, _) if er.ok => er.outputFile }
      val decodeArgs = new Args(Array("decode", "-o", tmpDir.getPath) ++ encodedFiles.map(_.getPath))
      val decodeResults = ExiUseCases.decode(decodeArgs).map { case (dr, _) => dr }

      Then("there must be decode results")
      assert(decodeResults.size == encodedFiles.size)
      assert(decodeResults.map(_.settings.library).distinct.size == Library.Values.size)
      assert(decodeResults.map(_.settings.codingMode).distinct.size == CodingMode.Values.size)
      assert(decodeResults.map(_.settings.fidelityOptionMode).distinct.size == FidelityOptionMode.Values.size)

      And("decoded files must exist")
      assert(decodeResults.forall(_.outputFile.exists()))
    }
  }
}
