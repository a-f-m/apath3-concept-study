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

  /**
    * The first match. If d == null a match has to occur.
    *
    * @param o input object
    * @param expr apath expression
    * @param d default if no match
    * @tparam T type of the matched object
    * @return matched object
    */
  def get[T](o: Any, expr: String, d: T): T = {

    val first = doMatch(o, expr).first
    if (first.isDefined) first.get.curr.asInstanceOf[T] //<
    else if (d == null) throw new RuntimeException("no match and no default given") else d //>
  }

  /**
    * equivalent to [[org.afm.apath3.core.Apath#get(java.lang.Object, java.lang.String, T)]] apply (_, _, null)
    */
  // Rem.: no scala parameter default when usage from java; we use 'null' for "emptiness"
  def get[T](o: Any, expr: String): T = {
    get(o, expr, null.asInstanceOf[T])
  }

  /**
    * Iterates over matches.
    *
    * @param o input object
    * @param expr apath expression
    * @param f consumption of the context for each match
    * @return a match occurred
    */
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
    * Iterator over the matches.
    *
    * @param o input object
    * @param expr apath expression
    * @return iterator over the match-contexts
    */
  def doMatch(o: Any, expr: String): Iterable[Context] = {

    config.acc.checkObjectAllowed(o)

    val ctx = new Context()
    val it = pp.parse(expr).eval(Node(o, "", config.acc.isArrayFunc.get.apply()), ctx, config)
    new Iterable[Context] {
      override def iterator = new Iterator[Context] {
        override def hasNext = it.hasNext
        override def next() = {ctx.currNode = it.next(); ctx}
      }
    }
  }

  /**
    *
    * @param expr apath expression
    * @return equivalent adt-expression
    */
  def parse(expr: String): Expr = {

    pp.parse(expr)
  }

  ////// for java usage
  /**
    * see [[org.afm.apath3.core.Apath#doMatch(java.lang.Object, java.lang.String, Context => Unit)]]
    */
  def doMatch(o: Any, expr: String, f: java.util.function.Consumer[Context]): Boolean = doMatch(o, expr, x => {f.accept(x)})

  /**
    * see [[org.afm.apath3.core.Apath#doMatch(java.lang.Object, java.lang.String)]]
    */
  // Rem.: 'j..'-func for java-usage because scala do not have return-type-polymorphism (as in Haskell)
  def jDoMatch(o: Any, expr: String): java.lang.Iterable[Context] = doMatch(o, expr).asJava
}

object Apath {

  var globalLoggingMatches: Boolean = false
  var globalLoggingNonMatches: Boolean = false
  var globalLoggingSolutions: Boolean = false
  var loggText: StringBuilder = new StringBuilder

  def globalLoggingMatches_(v: Boolean): Unit = globalLoggingMatches = v
  def globalLoggingNonMatches_(v: Boolean): Unit = globalLoggingNonMatches = v
  def globalLoggingSolutions_(v: Boolean): Unit = globalLoggingSolutions = v

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

    if (globalLoggingSolutions) loggText.append(s"! solution at (sub-)path end: ${
      StringUtils.abbreviate(node.obj.toString.replaceAll("\\s+", " "), 80)
    } [:${node.order}:]\n")
  }

  /**
    * Gets the first of an iterator or None if it is empty
    */
  def first[A](it: Iterable[A]): Option[A] = if (it.iterator.hasNext) Some(it.iterator.next()) else None

  // for java usage
  /**
    * Gets the first of an iterator or None if it is empty
    */
  def first[A](it: java.lang.Iterable[A]): Option[A] = first(it.asScala)

  implicit class IIter[A](it: Iterable[A]) {

    def first: Option[A] = Apath.first(it)
  }

//  implicit def convJConsumer[A](f: java.util.function.Consumer[A]): A => Unit = x => {f.accept(x)}
}