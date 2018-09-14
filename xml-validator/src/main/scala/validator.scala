import javax.xml.transform.stax._
import javax.xml.stream._
import javax.xml._
import javax.xml.transform.stream._
import javax.xml.validation.{Validator, SchemaFactory}
import scopt._
import java.io.{OutputStream, FileInputStream, File, FileOutputStream}
import java.util.zip.{GZIPInputStream, ZipEntry, ZipFile}
import scala.collection.JavaConversions._


case class Config(in: File = new File("."), xsd:File = new File("."),
                  verbose: Boolean = false, debug: Boolean = false)

object validator {

  // Command line aufsetzen
  val parser = new scopt.OptionParser[Config]("validator") {
    head("validator", "1.0")
    opt[File]('i', "in") required() valueName ("<file>") action { (x, c) =>
      c.copy(in = x)
    } text ("in xml is a required in file")
    opt[File]('x', "xsd") required() valueName ("<file>") action { (x, c) =>
      c.copy(xsd = x)
    } text ("xsd is a required xsd file")
    opt[Unit]("verbose") action { (_, c) =>
      c.copy(verbose = true)
    } text ("verbose is a flag")
    opt[Unit]("debug") hidden() action { (_, c) =>
      c.copy(debug = true)
    } text ("this option is hidden in the usage text")
    help("help") text ("prints this usage text")
    checkConfig { c =>
      if (!c.in.exists) failure("in xml file does not exist") else
      if (!c.in.getName.endsWith(".zip") && !c.in.getName.endsWith(".gz") && !c.in.getName.endsWith(".xml"))
        failure("gzip or zip or xml file required") else success
    }
    checkConfig { c =>
      if (!c.xsd.exists) failure("xsd file does not exist") else success
    }
  }

  def main(args: Array[String]) {

    val total = System.nanoTime
    var filename = ""
    try {

      // Kommandozeile auslesen
      parser.parse(args, Config()) match {

        case Some(config) =>

          filename = config.in.getName
          val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
          val schema = factory.newSchema(new StreamSource(config.xsd))
          val validator = schema.newValidator()
          val extension = filename.substring(filename.lastIndexOf('.')+1)

          extension match {
            case "xml"  => processXmlFile (config, validator)
            case "gz" => processGZipFile (config, validator)
            case "zip"  => processZipFile (config, validator)
            case _  => println("unknown extension " + extension)
          }

        case None =>
        // arguments are bad, error message will have been displayed
      }
      println("total time: " + (System.nanoTime - total) / 1e6 + "ms")

      // allgemeine Exception anfangen
    } catch {
      case e: Exception => println("file not valid (" + filename + "): " + e.getMessage)
        System.exit(1)
    }
  }

  def processZipFile(config: Config, validator: Validator) = {
    println("validating zip: " + config.in.getName + ".." )

    var part = ""
    val rootzip = new java.util.zip.ZipFile(config.in)
    val entries = rootzip.entries


    // nur xml Dateien im Archiv lesen
    entries.
      filter (_.getName.endsWith(".xml")).
      // enthält nicht "delivery.xml"
      filter (!_.getName.endsWith("delivery.xml")).
      foreach { f =>
      val start = System.nanoTime
      part = f.getName
      println("validating part: " + part + ".." )

      val reader = XMLInputFactory.newInstance().createXMLStreamReader(rootzip.getInputStream(f))

      validator.validate(new StAXSource(reader))

      //no exception thrown, so valid
      println("document part is valid: " + part )
      println("time: " + (System.nanoTime - start) / 1e6 + "ms")
    }

    rootzip.close()
  }

  def processGZipFile(config: Config, validator: Validator) = {
    println("validating gzip: " + config.in.getName + ".." )

    val gzip = new GZIPInputStream(new FileInputStream(config.in))
    val reader = XMLInputFactory.newInstance().createXMLStreamReader(gzip)

    validator.validate(new StAXSource(reader))

    gzip.close()

    println("document is valid: " + config.in.getName )
  }

  def processXmlFile(config: Config, validator: Validator) = {
    println("validating xml: " + config.in.getName + "..")

    val xml = new FileInputStream(config.in)
    val reader = XMLInputFactory.newInstance().createXMLStreamReader(xml)

    validator.validate(new StAXSource(reader))

    xml.close()

    println("document is valid: " + config.in.getName)
  }

  }
