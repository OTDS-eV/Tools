**exi-utils - Compare XML encoding/decoding performance using various libraries/codecs**


# Quick start for remaining use cases #

 * Download some test data:

```
exi-utils testdata --download
```

 * Encode the given .xml file:

```
exi-utils encode \
  --xsd-file data/sample-data/car-parking/carparking.xsd \
  --output-directory /tmp/exi/encode \
  data/sample-data/car-parking/CarParkData_1.xml
```

 * As for the error about "attribute schemaLocation", see the appendix "Limitations" below

 * Decode:

```
exi-utils decode \
  --output-directory /tmp/exi/decode \
  /tmp/exi/encode
```

 * Validate:

```
exi-utils validate \
  /tmp/exi/encode
```

# Using exi-utils #

## Show help ##

 * Show all options (UNIX, shell script):

```
exi-utils --help
```

 * Show all options (Windows, batch file - not tested):

```
exi-utils.bat --help
```

 * Show options for each sub command (testdata, encode, decode, validate). For example:

```
exi-utils encode --help
```

## Encode .xml files ##

### Encode a single file ###

```
exi-utils encode \
  --output-directory /tmp/exi/encode \
  data/sample-data/car-parking/CarParkData_1.xml
```

 * The output directory is automatically created, if it does not exist!
   * Existing files will be overwritten!

 * For each .xml there will be an encoded .exi file
   * The name of the encoded file contains most encode settings to get a unique file name

 * For each encoded .exi file there will be a properties file as well
   * The properties file will store all encode parameters
   * It is required by the decode/validate operations!

 * Multiple files to be encoded may be given on the command line as trailing arguments
   * If a trailing argument points to a directory, all .xml files in this directory will be encoded

### Encode all files in a given directory, using an .xsd file ###

```
exi-utils encode \
  --xsd-file data/sample-data/car-parking/carparking.xsd \
  --output-directory /tmp/exi/encode \
  data/sample-data/car-parking
```

 * If an .xsd file is used, this is indicated by a "Y" in the "S" column (schema column)

### Using a sub set of libraries/codecs ###

 * Default: Use all available libraries
 * Supported libraries: Exificient, OpenExi, Gzip, Lzma, Bzip2, Xz, GzipCc
   * The full list of available libraries is shown by --help
   * "Gzip" is the "native" GZIP-Java implementation
   * "GzipCc" is provided by Apache commons-compress; this implementation supports different modes (optimize for speed/size)
 * You can define a subset using:

```
exi-utils encode \
  --libraries exificient,openexi \
  --output-directory /tmp/exi/encode \
  data/sample-data/car-parking/CarParkData_1.xml
```

### Using a sub set of coding modes ###

 * Supported coding modes: BitPacked, BytePacked, PreCompression, Compression, Size, Speed, Default
   * EXI libraries support: BitPacked, BytePacked, PreCompression, Compression
   * Other libraries possibly support: Size, Speed, Default

```
exi-utils encode \
  --coding-modes compression \
  --output-directory /tmp/exi/encode \
  data/sample-data/car-parking/CarParkData_1.xml
```

### Using a sub set of fidelity options ###

 * Supported fidelity options: Strict, All, Customized, NotApplicable
   * EXI libraries support: Strict, All, Customized
   * Other libraries support: NotApplicable
   * "All" ... A predefined set which includes all five preserve options (see below)

```
exi-utils encode \
  --fidelity-option-modes strict \
  --output-directory /tmp/exi/encode \
  data/sample-data/car-parking/CarParkData_1.xml
```


### Getting the best compression ###

 * To compare EXI libraries only: Requires the .xsd file, coding mode "compression", fidelity mode "strict":

```
exi-utils encode \
  --libraries exificient,openexi \
  --xsd-file data/sample-data/car-parking/carparking.xsd \
  --coding-modes compression \
  --fidelity-option-modes strict \
  --output-directory /tmp/exi/encode \
  data/sample-data/car-parking/CarParkData_1.xml
```

 * To compare all libraries:

```
exi-utils encode \
  --xsd-file data/sample-data/car-parking/carparking.xsd \
  --coding-modes compression,size,default \
  --fidelity-option-modes strict,notapplicable \
  --output-directory /tmp/exi/encode \
  data/sample-data/car-parking/CarParkData_1.xml
```

### Customize fidelity options ###

 * Use "--fidelity-option-modes customized" together with any combination of
   * --preserve-comments
   * --preserve-processing-instructions
   * --preserve-dtds-and-entity-references
   * --preserve-prefixes
   * --preserve-lexical-values

```
exi-utils encode --xsd-file data/sample-data/car-parking/carparking.xsd --coding-modes compression --fidelity-option-modes customized --preserve-comments                   --output-directory /tmp/exi/encode data/sample-data/car-parking/CarParkData_1.xml
exi-utils encode --xsd-file data/sample-data/car-parking/carparking.xsd --coding-modes compression --fidelity-option-modes customized --preserve-processing-instructions    --output-directory /tmp/exi/encode data/sample-data/car-parking/CarParkData_1.xml
exi-utils encode --xsd-file data/sample-data/car-parking/carparking.xsd --coding-modes compression --fidelity-option-modes customized --preserve-dtds-and-entity-references --output-directory /tmp/exi/encode data/sample-data/car-parking/CarParkData_1.xml
exi-utils encode --xsd-file data/sample-data/car-parking/carparking.xsd --coding-modes compression --fidelity-option-modes customized --preserve-prefixes                   --output-directory /tmp/exi/encode data/sample-data/car-parking/CarParkData_1.xml
exi-utils encode --xsd-file data/sample-data/car-parking/carparking.xsd --coding-modes compression --fidelity-option-modes customized --preserve-lexical-values             --output-directory /tmp/exi/encode data/sample-data/car-parking/CarParkData_1.xml
```

 * Used fidelity options are shown in the "CIDPL" column:
   * C ... Preserve comments
   * I ... Preserve processing instructions
   * D ... Preserve DTDs and entity preferences
   * P ... Preserve prefixes
   * L ... Preserve lexical values

 * Another example
   * Strict mode with "preserve lexical values"
   * Customized mode with  "preserve lexical values" and "preserve comments"
   * Compared with all mode

```
exi-utils encode \
  --libraries exificient,openexi \
  --xsd-file data/sample-data/car-parking/carparking.xsd \
  --coding-modes compression \
  --fidelity-option-modes strict,customized,all \
  --preserve-comments \
  --preserve-lexical-values \
  --output-directory /tmp/exi/encode \
  data/sample-data/car-parking/CarParkData_1.xml
```

## Decode .exi files ##

 * Decode all encoded files into the given output directory:

```
exi-utils decode \
  --output-directory /tmp/exi/decode \
  /tmp/exi/encode
```

 * The output directory is automatically created, if it does not exist!
   * Existing files will be overwritten!

## Validate .exi files ##

 * Validate all encoded files in the given directory:

```
exi-utils validate \
  /tmp/exi/encode
```

## Max. memory used ##

 * Using /usr/bin/time

```
/usr/bin/time -v exi-utils decode --output-directory /tmp/exi/decode /tmp/exi/encode
```

 * See "Maximum resident set size (kbytes)"

# Development #

## Using sbt to encode ##

```
sbt "run-main de.otds.exi.Exi encode --xsd-file data/sample-data/car-parking/carparking.xsd --output-directory /tmp/exi/encode data/sample-data/car-parking/CarParkData_1.xml"
```

## Using sbt to decode ##

```
sbt "run-main de.otds.exi.Exi decode --output-directory /tmp/exi/decode /tmp/exi/encode"
```

## Build fat jar ##

 * You can build a "fat jar" using:

```
sbt assembly
```

 * This creates *target/scala-2.11/exi-utils-assembly-1.1.jar* (version suffix may differ)

## Use fat jar ##

```
java -jar target/scala-2.11/exi-utils-assembly-1.1.jar
```

# Limitations #

 * No tests have been done on Windows
   * Batch file exi-utils.bat (not tested)
   * The shell script exi-utils possibly works in the *bash* provided by *git portable* on Windows (not tested)
   * Using NUL instead of /dev/null (not tested)

 * In spite of "--preserve-comments", there are no comments in the decoded XML files
   * The same happens, if the Exificient/OpenEXI UIs are used
   * http://stackoverflow.com/questions/43541198/using-exi-how-to-preserve-comments

 * OpenExi cannot handle attribute "xsi:schemaLocation" in coding mode "strict"
   * The same happens, if the OpenEXI UI is used
   * Workaround: Remove the attribute in the .xml file

 * OpenExi is not multi-thread safe. This error happens, if tests are run in parallel:
```
  Encoding data/sample-data/car-parking/CarParkData_1.xml ... java.lang.StringIndexOutOfBoundsException: String index out of range: 8
        at java.lang.String.charAt(String.java:646)
        at org.openexi.proc.io.DateTimeValueScriberBase.parseHourField(Unknown Source)
        at org.openexi.proc.io.TimeValueScriber.process(Unknown Source)
        at org.openexi.sax.Transmogrifier$SAXEventHandler.do_characters(Unknown Source)
        at org.openexi.sax.Transmogrifier$SAXEventHandler.endElement(Unknown Source)
        at org.apache.xerces.parsers.AbstractSAXParser.endElement(Unknown Source)
        at org.apache.xerces.impl.XMLNSDocumentScannerImpl.scanEndElement(Unknown Source)
        at org.apache.xerces.impl.XMLDocumentFragmentScannerImpl$FragmentContentDispatcher.dispatch(Unknown Source)
        at org.apache.xerces.impl.XMLDocumentFragmentScannerImpl.scanDocument(Unknown Source)
        at org.apache.xerces.parsers.XML11Configuration.parse(Unknown Source)
        at org.apache.xerces.parsers.XML11Configuration.parse(Unknown Source)
        at org.apache.xerces.parsers.XMLParser.parse(Unknown Source)
        at org.apache.xerces.parsers.AbstractSAXParser.parse(Unknown Source)
        at org.apache.xerces.jaxp.SAXParserImpl$JAXPSAXParser.parse(Unknown Source)
        at org.openexi.sax.Transmogrifier.encode(Unknown Source)
        at de.otds.exi.impl.OpenExiLibraryImpl.encode(OpenExiLibraryImpl.scala:148)
```

 * OpenExi fails with large files:
```
 = Zip-Decode using OpenExi =
   Decoding /mnt/data7/exi-utils/scala-2.11-local/exi-utils-output/4/encode/otds-1.zip.openexi.compression.all.zip ... 001_basis.xml.exi 077_hotels.xml.exi java.lang.ArrayIndexOutOfBoundsException: 1000000
 	at org.openexi.proc.io.compression.ChannellingScanner.doElement(Unknown Source)
 	at org.openexi.proc.io.compression.ChannellingScanner.readStructureChannel(Unknown Source)
 	at org.openexi.proc.io.compression.ChannellingScanner.processBlock(Unknown Source)
 	at org.openexi.proc.io.compression.ChannellingScanner.prepare(Unknown Source)
 	at org.openexi.proc.EXIDecoder.processHeader(Unknown Source)
 	at org.openexi.sax.ReaderSupport.processHeader(Unknown Source)
 	at org.openexi.sax.EXIReader.parse(Unknown Source)
 	at org.openexi.sax.EXIReader.parse(Unknown Source)
 	at de.otds.exi.impl.OpenExiLibraryImpl.de$otds$exi$impl$OpenExiLibraryImpl$$read(OpenExiLibraryImpl.scala:178)
```
