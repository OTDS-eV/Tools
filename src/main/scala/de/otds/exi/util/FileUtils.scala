package de.otds.exi.util

import java.io.File

trait FileUtils {
  def getSinkDevice(): File = {
    val devNull = new File("/dev/null")
    if (devNull.exists()) {
      devNull
    } else {
      // https://stackoverflow.com/questions/313111/dev-null-in-windows
      new File("NUL")
    }
  }
}
