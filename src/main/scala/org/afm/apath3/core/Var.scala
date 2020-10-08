package org.afm.apath3.core

class Var(name: String) {

  private var value: Option[Node] = None

  def clear() =
  {
    value = Option(new Node(88, "lolo"))
  }

  def setValue(o: Option[Node]): Unit = value = o

  def getValue() : Option[Node] = value

  override def toString: String = value.toString
}
