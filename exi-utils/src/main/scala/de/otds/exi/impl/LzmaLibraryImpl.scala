package de.otds.exi.impl

import java.io.{BufferedInputStream, BufferedOutputStream, FileInputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import javax.xml.transform.stream.StreamSource

import de.otds.exi._
import org.apache.commons.compress.compressors.CompressorStreamFactory

class LzmaLibraryImpl extends CommonsCompressLibraryImpl(CompressorStreamFactory.LZMA) {
  override val library = Lzma
}
