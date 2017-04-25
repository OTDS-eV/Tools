**exi-utils - Compare XML encoding/decoding performance using various libraries/codecs**

# Quick start #

## Filesystem setup ##

### Setup .xml test data ###

 * Install "git large file storage": https://git-lfs.github.com/

 * Then clone the repository containing the test data:
```
git clone https://github.com/OTDS-eV/OTDS-Flight.git
```

 * Set an environment variable:
```
export EXI_XML=/mnt/data7/exi-utils/scala-2.11-local/OTDS-Flight/testdata
```

 * Result: There should be a .zip file like this:
```
$ ls -l $EXI_XML/FTI_OTDS.xml.2016-11-01.zip
-rw-r--r-- 1 scala-2.11-local scala-2.11-local 1093702110 Apr 24 10:21 FTI_OTDS.xml.2016-11-01.zip
```

### Setup .xsd files ###

 * Download and extract: https://forum.otds.de/redmine/attachments/download/1094/OTDS_V1_9_1b.zip

 * Set an environment variable:
```
export EXI_XSD=/mnt/data7/exi-utils/scala-2.11-local/otds-v1-9-1b/xsd
```

 * Result: There should be .xsd files like this:
```
$ ls -l $EXI_XSD
insgesamt 238
drwxr-xr-x 3 scala-2.11-local scala-2.11-local     12 Aug  7  2015 ./
drwxr-xr-x 4 scala-2.11-local scala-2.11-local      4 Apr 24 10:38 ../
-rw-r--r-- 1 scala-2.11-local scala-2.11-local   2418 Mai  8  2015 delivery.xsd
drwxr-xr-x 3 scala-2.11-local scala-2.11-local     12 Jun  9  2015 incremental_xsds/
-rw-r--r-- 1 scala-2.11-local scala-2.11-local    737 Nov 11  2013 otds-booking-merlin.xsd
-rw-r--r-- 1 scala-2.11-local scala-2.11-local    900 Nov 11  2013 otds-booking-toma.xsd
-rw-r--r-- 1 scala-2.11-local scala-2.11-local  18675 Aug  7  2014 otds-schema-accommodation.xsd
-rw-r--r-- 1 scala-2.11-local scala-2.11-local  15499 Nov 11  2013 otds-schema-addon.xsd
-rw-r--r-- 1 scala-2.11-local scala-2.11-local 513846 Aug  7  2015 otds-schema-common.xsd
-rw-r--r-- 1 scala-2.11-local scala-2.11-local  24029 Aug  7  2015 otds-schema-flight.xsd
-rw-r--r-- 1 scala-2.11-local scala-2.11-local   7255 Nov 11  2013 otds-schema-products.xsd
-rw-r--r-- 1 scala-2.11-local scala-2.11-local   7231 Jun  2  2015 otds.xsd
```

### Choose a temporary directory ###

 * With sufficient free space
 * Other considerations: No encryption, no USB device, no ZFS compression, SSD or HDD etc.

 * Set an environment variable:
```
export EXI_TMP=/mnt/data7/exi-utils/scala-2.11-local/exi-tmp
```

## Prepare test data ##

```
mkdir -p $EXI_TMP/input
cd $EXI_TMP/input
unzip $EXI_XML/FTI_OTDS.xml.2016-11-01.zip 001_basis.xml 078_hotels.xml delivery.xml
zip otds-1.zip 001_basis.xml 078_hotels.xml delivery.xml
```

 * The resulting file should look like this:
```
$ unzip -l $EXI_TMP/input/otds-1.zip
Archive:  otds-2.zip
  Length      Date    Time    Name
---------  ---------- -----   ----
    75344  2016-10-31 23:16   001_basis.xml
385782181  2016-10-31 18:03   078_hotels.xml
      169  2016-10-31 23:29   delivery.xml
---------                     -------
385857694                     3 files
```

## Step 1: zip-encode ##

 * This re-encodes the .zip file: .xml files are replaced with encoded files:

```
exi-utils zip-encode \
  -x $EXI_XSD/otds.xsd \
  --libraries exificient,gzipcc,lzma,passthrough \
  --coding-modes compression,default \
  --fidelity-option-modes strict,notapplicable \
  --skip delivery.xml \
  -o $EXI_TMP/output/1/encode \
  $EXI_TMP/input/otds-1.zip
```

 * For comparison, non-EXI-codecs can be used as well: This is the case with "gzip" and "lzma"
   * Additionally "passthrough" does no compression at all
   * Yet it creates a .zip archive compatible to the other ones for comparison as well

 * Result: This shows that Exificient results in the smallest file size, yet it needs more time than Gzip:

```
== By size ==

File                                                                                                                   Size     Enc size   % Library     Coding mode     FidOM         CIDPL S     T [ms]
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.exificient.compression.strict.zip         11196963      2569262 77% Exificient  Compression     Strict        ----- Y      39793
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.lzma.default.notapplicable.zip            11196963      4219265 62% Lzma        Default         NotApplicable ----- Y     100472
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.gzipcc.default.notapplicable.zip          11196963     10230542  9% GzipCc      Default         NotApplicable ----- Y       6600
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.passthrough.default.notapplicable.zip     11196963     11172906  0% PassThrough Default         NotApplicable ----- Y       5804

== By duration ==

File                                                                                                                   Size     Enc size   % Library     Coding mode     FidOM         CIDPL S     T [ms]
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.passthrough.default.notapplicable.zip     11196963     11172906  0% PassThrough Default         NotApplicable ----- Y       5804
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.gzipcc.default.notapplicable.zip          11196963     10230542  9% GzipCc      Default         NotApplicable ----- Y       6600
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.exificient.compression.strict.zip         11196963      2569262 77% Exificient  Compression     Strict        ----- Y      39793
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.lzma.default.notapplicable.zip            11196963      4219265 62% Lzma        Default         NotApplicable ----- Y     100472
```

 * Result: A .zip file for each possible encoding
   * Accompanied by a .properties file which contains the encoding parameters - required for validate/decode
```
$ ls -l $EXI_TMP/output/1/encode/
-rw-r--r-- 1 scala-2.11-local scala-2.11-local  2569262 Apr 25 08:44 otds-1.zip.exificient.compression.strict.zip
-rw-r--r-- 1 scala-2.11-local scala-2.11-local      707 Apr 25 08:44 otds-1.zip.exificient.compression.strict.zip.properties
-rw-r--r-- 1 scala-2.11-local scala-2.11-local 10230542 Apr 25 08:44 otds-1.zip.gzipcc.default.notapplicable.zip
-rw-r--r-- 1 scala-2.11-local scala-2.11-local      705 Apr 25 08:44 otds-1.zip.gzipcc.default.notapplicable.zip.properties
-rw-r--r-- 1 scala-2.11-local scala-2.11-local  4219265 Apr 25 08:46 otds-1.zip.lzma.default.notapplicable.zip
-rw-r--r-- 1 scala-2.11-local scala-2.11-local      701 Apr 25 08:46 otds-1.zip.lzma.default.notapplicable.zip.properties
-rw-r--r-- 1 scala-2.11-local scala-2.11-local 11172906 Apr 25 08:46 otds-1.zip.passthrough.default.notapplicable.zip
-rw-r--r-- 1 scala-2.11-local scala-2.11-local      715 Apr 25 08:46 otds-1.zip.passthrough.default.notapplicable.zip.properties
```

 * Result: Encoded files in the new .zip got an .exi extension, the name of skipped files is not changed:
```
$ unzip -l $EXI_TMP/output/1/encode/otds-1.zip.exificient.compression.strict.zip
Archive:  exi-utils-output/1/encode/otds-1.zip.exificient.compression.strict.zip
  Length      Date    Time    Name
---------  ---------- -----   ----
     3213  2017-04-24 15:47   001_basis.xml.exi
  2586860  2017-04-24 15:47   078_hotels.xml.exi
      169  2017-04-24 15:48   delivery.xml
---------                     -------
  2590242                     3 files
```

## Step 2: zip-validate ##

 * This reads the encoded files and performs a validation against the .xsd:

```
exi-utils zip-validate \
  $EXI_TMP/output/1/encode/*.zip
```

 * Result: This shows that Exificient requires more time to validate compared with LZMA

```
= Results =

File                                                                                                               Enc size Library     Coding mode     FidOM         CIDPL S     T [ms]
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.lzma.default.notapplicable.zip             4219265 Lzma        Default         NotApplicable ----- Y      20694
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.gzipcc.default.notapplicable.zip          10230542 GzipCc      Default         NotApplicable ----- Y      26373
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.passthrough.default.notapplicable.zip     11172906 PassThrough Default         NotApplicable ----- Y      25685
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.exificient.compression.strict.zip          2569262 Exificient  Compression     Strict        ----- Y      49332
```

## Step 3: zip-decode ##

 * This extracts the content to the filesystem:

```
exi-utils zip-decode \
  -o $EXI_TMP/output/1/decode \
  $EXI_TMP/output/1/encode/*.zip
```

 * Result: This shows that Exificient requires more time to decode compared with GZIP/LZMA

```
= Results =

File                                                                                                               Enc size Library     Coding mode     FidOM         CIDPL S     T [ms]
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.gzipcc.default.notapplicable.zip          10230542 GzipCc      Default         NotApplicable ----- Y       1361
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.passthrough.default.notapplicable.zip     11172906 PassThrough Default         NotApplicable ----- Y       1482
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.lzma.default.notapplicable.zip             4219265 Lzma        Default         NotApplicable ----- Y       3152
/mnt/data7/exi-utils/scala-2.11-local/exi-tmp/output/1/encode/otds-1.zip.exificient.compression.strict.zip          2569262 Exificient  Compression     Strict        ----- Y      30291
```

# Other tests #

## Preserve everything: Coding mode "all"; OpenExi ##

 * Coding mode "all" instead of "strict": This preserves all as defined by EXI
 * OpenExi cannot handle the attribute "xsi:schemaLocation" in coding mode "strict"
 * So another test sequence can be started with:

```
exi-utils zip-encode \
  -x $EXI_XSD/otds.xsd \
  --libraries exificient,openexi \
  --coding-modes compression \
  --fidelity-option-modes all \
  --skip delivery.xml \
  -o $EXI_TMP/output/1/encode \
  $EXI_TMP/input/otds-1.zip
```

## Using larger input files ##

```
cd $EXI_TMP/input
unzip $EXI_XML/FTI_OTDS.xml.2016-11-01.zip 077_hotels.xml
zip otds-2.zip 001_basis.xml 077_hotels.xml 078_hotels.xml delivery.xml
```

 * Re-run the tests
 * zip-encode should work, but on systems with 8 GB RAM, suing zip-validate or zip-decode, Exificient possibly fails with OutOfMemory
 * You can set options like this; this will be used by the exi-utils script:
```
export JAVA_OPTS="-Xmx6g -XX:+UseG1GC -XX:+UseStringDeduplication"
```
 * Even with these options zip-validate and zip-decode possibly fail using real EXI libraries
   * Exificient fails with OutOfMemory
   * OpenExi fails with an ArrayIndexOutOfBoundsException: 1000000
   * No problems with GZIP/LZMA

# Further reading #

 * See DOCUMENTATION.md
