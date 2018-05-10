package org.afm.apath3.core

class Var(name: String) {

  var value: Option[Node] = None

  def clear() = {value = None}

  override def toString: String = value.toString
}
