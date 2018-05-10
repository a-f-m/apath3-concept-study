package org.afm.apath3.core

import org.afm.apath3.accessors.Accessor


class Config(val acc: Accessor) {

  var arrayAsFirstClass: Boolean = false
  def setArrayAsFirstClass(b: Boolean): Config = {arrayAsFirstClass = b; this}

  acc.checkCompleteness()
}
