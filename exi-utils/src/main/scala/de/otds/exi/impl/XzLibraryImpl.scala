package de.otds.exi.impl

import de.otds.exi._
import org.apache.commons.compress.compressors.CompressorStreamFactory

class XzLibraryImpl extends CommonsCompressLibraryImpl(CompressorStreamFactory.XZ) {
  override val library = Xz
}
