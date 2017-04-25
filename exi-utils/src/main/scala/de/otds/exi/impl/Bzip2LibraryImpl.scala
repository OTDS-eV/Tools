package de.otds.exi.impl

import de.otds.exi._
import org.apache.commons.compress.compressors.CompressorStreamFactory

class Bzip2LibraryImpl extends CommonsCompressLibraryImpl(CompressorStreamFactory.BZIP2) {
  override val library = Bzip2
}
