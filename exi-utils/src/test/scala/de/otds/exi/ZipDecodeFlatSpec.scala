package de.otds.exi

import java.io.File

import de.otds.exi.util.ResourceManager
import org.scalatest.{FlatSpec, GivenWhenThen, MustMatchers}

class ZipDecodeFlatSpec extends FlatSpec with MustMatchers with GivenWhenThen with ResourceManager with TestData {
  import TestData.CarParking._

  "exi-utils" must "zip-decode files" in {
    withTmpDirectory(false) { tmpDir =>
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

      When("the encoded files are decoded")
      val encodedFiles = encodeResults.collect { case (er, _) if er.ok => er.outputFile }
      val decodeArgs = new Args(Array(
        "zip-decode",
        "-o", tmpDir.getPath) ++ encodedFiles.map(_.getPath))

      val decodeResults = ExiUseCases.zipDecode(decodeArgs).map { case (dr, _) => dr }

      Then("there must be decode results")
      assert(decodeResults.size == libs.size)
      assert(decodeResults.forall(_.ok))

      And("decoded files must exist")
      for {
        dr <- decodeResults
        f <- FilesInZipInputFile
      } {
        val outputFile = new File(tmpDir, s"${dr.settings.fileSuffix}/${f.getName}")
        assert(outputFile.exists(), outputFile)
        // Flag empty files as errors
        assert(outputFile.length() > 10)

        if (f.getName != SkipFileName) {
          info(s"  $outputFile exists")
        } else {
          info(s"  $outputFile exists [skipped]")
        }
      }
    }
  }
}
