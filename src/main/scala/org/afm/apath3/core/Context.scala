package org.afm.apath3.core

import scala.collection.mutable


class Context {

  protected[core] val varMap: mutable.Map[String, Var] = mutable.LinkedHashMap.empty
//  varMap.withDefaultValue(new Var())

  var currNode: Node = new NilNode

  var preventClearing = false

  def register(name: String): (Var, Boolean) = {

    val b0 = varMap.get(name)
    val v = new Var(name)
    if (b0.isEmpty) {varMap.put(name, v); (v, true)} else (b0.get, false)
  }

  /**
    * The value of an variable.
    *
    * @param varName variable name
    * @tparam T type of the value
    * @return value as option
    */
  def opt[T](varName: String): Option[T] = {

    val v = varMap.get(varName)
    if (v.nonEmpty && v.get.getValue().nonEmpty) Some[T](v.get.getValue().get.obj.asInstanceOf[T]) else None
  }

  /**
    * The value of an variable or throws an exception if not defined.
    *
    * @param varName variable name
    * @tparam T type of the value
    * @return value
    */
  def v[T](varName: String): T = {

    val o = opt(varName)
    if (o.isEmpty) throw new RuntimeException(s"variable '$varName' has no value") else o.get
  }

  /**
    * Current matched object.
    *
    * @tparam T object type
    * @return matched object
    */
  def current[T]: T = currNode.obj.asInstanceOf[T]

  def clear(): Context = {varMap.clear(); currNode = new NilNode; this}

  override def toString: String = varMap.toString
}
