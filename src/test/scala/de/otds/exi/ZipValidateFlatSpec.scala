package de.otds.exi

import de.otds.exi.util.ResourceManager
import org.scalatest.{FlatSpec, GivenWhenThen, MustMatchers}

class ZipValidateFlatSpec extends FlatSpec with MustMatchers with GivenWhenThen with ResourceManager with TestData {
  import TestData.CarParking._

  "exi-utils" must "zip-validate files" in {
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
      val encodeResults = ExiUseCases.zipEncode(encodeArgs)

      Then("there must be encode results")
      assert(encodeResults.size == libs.size)

      When("the encoded files are validated")
      val encodedFiles = encodeResults.collect { case (er, _) if er.ok => er.outputFile }
      val validateArgs = new Args(Array(
        "zip-validate") ++ encodedFiles.map(_.getPath))

      val validateResults = ExiUseCases.zipValidate(validateArgs).map { case (dr, _) => dr }

      Then("there must be validation results")
      assert(validateResults.size == libs.size)
      assert(validateResults.forall(_.ok))
    }
  }
}
