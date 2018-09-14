# xml validator
## Version 
* 1.0: initial version

## Usage

java -jar validator.jar [options]

  -i <file> | --in <file>

​        in xml is a required in file

  -x <file> | --xsd <file>

​        xsd is a required xsd file

  --verbose

​        verbose is a flag

  --help

​        prints this usage text

### Example

java -jar lib/xml-validator-1.0.2.jar --in ${OTDS}/OTDS-FTI-ACE256.xml --xsd ${SCHEMA}/xsd/otds.xsd"

## Note

It works also on compressed files such as gzip and zip. 

It also handles specific features of Otds such as ignoring delivery.xml.

### Examples

 java -jar lib/xml-validator-1.0.2.jar --in OTDS-FTI-ACE256.xml.gz  --xsd ${SCHEMA}/xsd/otds.xsd

and

 java -jar lib/xml-validator-1.0.2.jar --in FTI-OTDS.xml.zip  --xsd ${SCHEMA}/xsd/otds.xsd

## License

BSD-2-Clause.
