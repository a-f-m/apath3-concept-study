package org.afm.util

object Testing {

  var globalOv: Boolean = false

  def expected(ov: Boolean, actual: String, id: String = "") = {

    Trace.getFileContentByTrace(globalOv || ov, actual, id, false, 3, "src/test/resources/expected")
  }
}
