package org.afm.apath3.core

import org.afm.apath3.core.Apath._
import org.apache.commons.lang3.StringUtils

import scala.collection.JavaConverters._


/**
  * top level apath eval.
  *
  * @param config config
  */
case class Apath(config: Config) {

  val pp = new PathParser()

  def get[T](o: Any, expr: String, d: T = null): T = {

    val first = doMatch(o, expr).first
    if (first.isDefined) first.get.curr.asInstanceOf[T] //<
    else if (d == null) throw new RuntimeException("no match and no default given") else d //>
  }

  def doMatch(o: Any, expr: String, f: Context => Unit): Boolean = {

    val it = doMatch(o, expr).iterator
    var success = false
    while (it.hasNext) {
      success = true
      f(it.next())
    }
    success
  }

  /**
    *
    * @param obj
    * @param expr
    * @return
    */
  def doMatch(obj: Any, expr: String): Iterable[Context] = doMatchCtx(obj, expr)

  private def doMatchCtx(obj: Any, expr: String) = {

    config.acc.checkObjectAllowed(obj)

    val ctx = new Context()
    val it = pp.parse(expr).eval(Node(obj, "", config.acc.isArrayFunc.get.apply()), ctx, config)
    new Iterable[Context] {
      override def iterator = new Iterator[Context] {
        override def hasNext = it.hasNext
        override def next() = {ctx.currNode = it.next(); ctx}
      }
    }
  }

  def parse(expr: String): Expr = {

    pp.parse(expr)
  }

  ////// for java usage
  // Rem.: in fact we could name it 'doMatch' because parameter polymorphism but we do it uniformly
  def jDoMatch(o: Any, expr: String, f: java.util.function.Consumer[Context]): Boolean = doMatch(o, expr, f)
  def jDoMatch(o: Any, expr: String): java.lang.Iterable[Context] = doMatchCtx(o, expr).asJava
  def jget[T](o: Any, expr: String): T = jget[T](o, expr, AnyRef.asInstanceOf[T])
  def jget[T](o: Any, expr: String, d: T): T = get[T](o, expr, d)
}

object Apath {

  var globalLoggingMatches: Boolean = false
  var globalLoggingNonMatches: Boolean = false
  var loggText: StringBuilder = new StringBuilder

  def globalLoggingMatches_(v: Boolean) = globalLoggingMatches = v
  def globalLoggingNonMatches_(v: Boolean) = globalLoggingNonMatches = v

  def consumeLoggText(): String = {

    val ret = new String(loggText.toString())
    loggText.clear()
    ret
  }

  def loggStep(noMatch: Boolean, node: Node, e: Expr) = {

    if (globalLoggingMatches || globalLoggingNonMatches) {
      val os = s"${StringUtils.abbreviate(node.obj.toString.replaceAll("\\s+", " "), 80)} [:${node.order}:]"
      if (globalLoggingNonMatches && noMatch) loggText.append(s"> no match of ${e.prettyString} on $os\n")
      if (globalLoggingMatches && !noMatch) loggText.append(s"> match of ${e.prettyString} on $os\n")
    }
  }

  def loggSolution(node: Node) = {

    if (globalLoggingMatches || globalLoggingNonMatches) loggText.append(s"! solution at (sub-)path end: ${
      StringUtils.abbreviate(node.obj.toString.replaceAll("\\s+", " "), 80)
    } [:${node.order}:]\n")
  }

  /**
    *
    * @param it
    * @tparam A
    * @return
    */
  def first[A](it: Iterable[A]): Option[A] = if (it.iterator.hasNext) Some(it.iterator.next()) else None

  implicit class IIter[A](it: Iterable[A]) {

    def first: Option[A] = Apath.first(it)
  }

  implicit def convJConsumer[A](f: java.util.function.Consumer[A]): A => Unit = x => {f.accept(x)}

  ////// for java usage
  def jfirst[A](it: java.lang.Iterable[A]): Option[A] = first(it.asScala)
}