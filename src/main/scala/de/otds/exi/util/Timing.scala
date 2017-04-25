package de.otds.exi.util

import scala.concurrent.duration._
import scala.language.postfixOps

trait Timing {
  def measure[R](f: => R): (R, Duration) = {
    val startMillis = System.currentTimeMillis()
    val result = f
    val stopMillis = System.currentTimeMillis()
    (result, (stopMillis - startMillis) milliseconds)
  }

}
