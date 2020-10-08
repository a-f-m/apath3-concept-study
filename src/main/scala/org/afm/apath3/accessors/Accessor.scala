package org.afm.apath3.accessors

import java.io.InputStream
import java.util
import java.util.Map.Entry

import org.afm.apath3.core._
import org.apache.commons.lang3.StringUtils

/**
  * Accessor to the underlying structure (e.g. an json structure as an input for matching)
  */
abstract class Accessor {

  var selectorFunc: Option[(Node, Expr) => NodeIter] = None
  var isArrayFunc: Option[Any => Boolean] = Some(_ => false)
  var isPropertyMapFunc: Option[Any => Boolean] = None

  /**
    * Sets the selector function for mapping (''node'': Node, ''expr'': Expr) => NodeIter. At evaluation time ''node''
    * is the current context node (like in xpath) and ''expr'' is the apath step expression (e.g. a property selection).
    *
    * @param f selector function
    * @return this
    */
  def setSelectorFunc(f: (Node, Expr) => NodeIter): Accessor = {

    selectorFunc = Some(f)
    this
  }

  /**
    * Sets the check function whether an object is an array.
    *
    * @param f check func
    * @return this
    */
  def setIsArrayFunc(f: Any => Boolean): Accessor = {

    isArrayFunc = Some(f)
    this
  }

  /**
    * Sets the check function whether an object is a property equipped object.
    *
    * @param f check func
    * @return this
    */
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

  /**
    * Creates a node for an object of the underlying structure.
    *
    * @param o        actual object
    * @param selector selector for ''o'' within its parent
    * @param order    order within parent
    */
  def createNode(o: Any, selector: String, order: Int = 0) = Node(o, selector, isArrayFunc.get.apply(o), None, order)


  /**
    * Returns a singleton node iterator.
    *
    * @param o        object of the underlying structure
    * @param selector selector within parent of ''o''
    */
  def iterO(o: Any, selector: String) = //
    if (o == null) NilIter() else SingleNodeIter(createNode(o, selector), None, None)


  type JIter = util.Iterator[_]

  /**
    * Returns a node iterator based of java iterators.
    *
    * @param it java iterator over objects of the underlying structure
    */
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

      override def next(): Node = get(it.next())
    }
  }

  /**
    * Parses the string representation of the underlying structure (e.g. json) to produce an result object (e.g. JsonObject).
    *
    * @param s string representation
    * @tparam T the type of the result object
    * @return
    */
  def parse[T](s: String): T

  /**
    * same as [[org.afm.apath3.accessors.Accessor#parse(java.lang.String, T)]] but for input streams.
    */
  def parse[T](in: InputStream): T
}
