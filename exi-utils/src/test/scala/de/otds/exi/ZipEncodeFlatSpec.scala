package de.otds.exi

import java.io.File

import de.otds.exi.util.ResourceManager
import org.scalatest.{FlatSpec, GivenWhenThen, MustMatchers}

class ZipEncodeFlatSpec extends FlatSpec with MustMatchers with GivenWhenThen with ResourceManager with TestData {
  "exi-utils" must "zip-encode all files in a .zip file into another .zip file" in {
    import TestData.CarParking._
    withTmpDirectory { tmpDir =>
      Given("test data and encode args")
      setupTestData()
      val libs = Seq(Exificient, OpenExi, Gzip, GzipCc, Lzma, PassThrough)
      val codingModes = Seq(Compression, Default)
      // TODO B Strict breaks OpenExi
      val foms = Seq(All, NotApplicable)
      val encodeArgs = new Args(Array(
        "zip-encode",
        "-x", XsdFile.getPath,
        "--libraries", libs.map(_.id).mkString(","),
        "--coding-modes", codingModes.map(_.id).mkString(","),
        "--fidelity-option-modes", foms.map(_.id).mkString(","),
        "--skip", SkipFileName,
        "-o", tmpDir.getPath,
        "--verbose",
        ZipInputFile.getPath))

      When("exi-utils is called with encode args")
      val results = ExiUseCases.zipEncode(encodeArgs)

      Then("there must be encoding results")
      assert(results.nonEmpty)
      val encodeResults = results.map { case (er, _) => er }
      assert(encodeResults.map(_.settings.library).distinct.size == libs.size)
      assert(encodeResults.map(_.settings.codingMode).distinct.size == codingModes.size)
      assert(encodeResults.map(_.settings.fidelityOptionMode).distinct.size == foms.size)

      And("output files must exist")
      // Flag empty files as errors
      assert(encodeResults.forall(er => er.ok == false || (er.outputFile.exists() && er.outputFile.length() > 10)))

      And("there must be a properties file for each output file")
      assert(encodeResults.map(er => new File(er.outputFile.getPath + ".properties")).forall(f => f.exists()))
    }
  }
}
