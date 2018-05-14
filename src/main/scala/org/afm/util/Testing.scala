package org.afm.util

object Testing {

  var globalOv: Boolean = false
  var normalizeCR: Boolean = true

  def expected(ov: Boolean, actual: String, id: String = "") = {

    Trace.getFileContentByTrace(globalOv || ov, actual, id, false, 3, "src/test/resources/expected").cr()
  }

  implicit class IString(s: String) {

    def cr(): String = if (normalizeCR) s.replaceAll("\\r\\n|\\r|\\n", System.getProperty("line.separator")) else s
  }
}
