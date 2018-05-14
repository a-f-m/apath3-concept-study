package org.afm.apath3.accessors

import java.io.InputStream
import java.util
import java.util.Map.Entry

import org.afm.apath3.core._
import org.apache.commons.lang3.StringUtils

class Accessor {

  var selectorFunc: Option[(Node, Expr) => NodeIter] = None
  var isArrayFunc: Option[Any => Boolean] = Some(_ => false)
  var isPropertyMapFunc: Option[Any => Boolean] = None

  def setSelectorFunc(f: (Node, Expr) => NodeIter): Accessor = {

    selectorFunc = Some(f)
    this
  }

  def setIsArrayFunc(f: Any => Boolean): Accessor = {

    isArrayFunc = Some(f)
    this
  }

  def setIsPropertyMapFunc(f: Any => Boolean): Accessor = {

    isPropertyMapFunc = Some(f)
    this
  }

  def checkObjectAllowed(obj: Any): Unit = //
    if (!isPropertyMapFunc.get.apply(obj) && !isArrayFunc.get.apply(obj)) //<
      throw new
        RuntimeException(s"object type '${obj.getClass}' not allowed (object: '${StringUtils.abbreviate(obj.toString.replaceAll("\\s+", " "), 80)}')") //>
  def checkCompleteness(): Unit = //
    if (selectorFunc.isEmpty || isArrayFunc.isEmpty || isPropertyMapFunc.isEmpty) //
      throw new RuntimeException("selectorFunc or isArrayFunc or isPropertyMapFunc not defined")

  def createNode(o: Any, selector: String, order: Int = 0) = Node(o, selector, isArrayFunc.get.apply(o), None, order)


  def iterO(o: Any, selector: String) = if (o == null) NilIter() else {
    SingleNodeIter(createNode(o, selector), None, None)
  }

  type JIter = util.Iterator[_]

  def iter(it: JIter) = {

    new NodeIter {
      def get(next: Any): Node = {

        incrCnt()
        next match {
          case e: Entry[_, _] => createNode(e.getValue, e.getKey.toString, cnt)
          case _ => createNode(next, cnt.toString, cnt)
        }
      }

      override def hasNext: Boolean = it.hasNext

      override def next() = get(it.next())
    }
  }


  def parse[T](s: String): T = {

    throw new RuntimeException("to be overridden")
  }

  def parse[T](in: InputStream): T = {

    throw new RuntimeException("to be overridden")
  }
}
