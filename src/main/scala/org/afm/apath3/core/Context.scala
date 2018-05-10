package org.afm.apath3.core

import org.afm.apath3.accessors.Accessor

import scala.collection.mutable


class Context {

  protected[core] val varMap: mutable.Map[String, Var] = mutable.LinkedHashMap.empty

  var currNode: Node = new NilNode

  def register(name: String): (Var, Boolean) = {

    val b0 = varMap.get(name)
    val v = new Var(name)
    if (b0.isEmpty) {varMap.put(name, v); (v, true)} else (b0.get, false)
  }

  def opt[T](varName: String): Option[T] = {

    val v = varMap.get(varName)
    if (v.nonEmpty && v.get.value.nonEmpty) Some[T](v.get.value.get.obj.asInstanceOf[T]) else None
  }

  // for java
  def v[T](varName: String): T = {

    val o = opt(varName)
    if (o.isEmpty) throw new RuntimeException("no value") else o.get
  }

  def curr[T]: T = currNode.obj.asInstanceOf[T]

  def clear() = {varMap.clear(); currNode = new NilNode; this}

  override def toString: String = varMap.toString
}
