package org.afm.apath3.core

import org.afm.apath3.core.ApathAdt._
import org.afm.apath3.core.PrintUtil._
import org.apache.commons.lang3.StringUtils

import scala.collection.Iterator
import scala.collection.JavaConverters._

// ------------------ adt -------------------------
//
abstract sealed class Expr(var subExpr: Seq[Expr] = Seq.empty) {

  //  var subExpr: Seq[Expr] = Seq.empty
  def setArgs(s: Seq[Expr]): Expr = {

    s.foreach(e => this :+ e)
    this
  }

  def :+(e: Expr): Expr = {if (!e.isInstanceOf[EmptyExpr]) subExpr = subExpr :+ e; this}

  def setArgs(s: java.util.List[Expr]): Expr = setArgs(s.asScala)
  //  {subExpr = s.asScala; this }
  def clearVars(ctx: Context): Unit = subExpr.foreach(e => e.clearVars(ctx))

  def reset(): Unit = subExpr.foreach(e => e.reset())

  def defined(i: Int) = i < subExpr.size

  def eval(node: Node, ctx: Context, config: Config) = {

    val r = config.acc.selectorFunc.get.apply(node, this)
    // so far:
    if (r.isInstanceOf[DelegatedIter]) throw new RuntimeException(s"delegated iter for $this") else r
  }

  def prettyString: String = {

    var i = 0
    var s: String = "["
    subExpr.foreach(x => {
      i = i + 1
      s = s + (if (i > 1) " " else "") + x.prettyString
    })
    s = s + "]"
    toString + (if (subExpr.nonEmpty) s else "")
  }
}

// every subclass of Expr uses 'subExpr' for its subexpressions -
// if subexpressions not obvious there is a comment on the class line
case class EmptyExpr() extends Expr

case class GatherExpr() extends Expr // for parsing purposes
case class Self() extends Expr { // !!! ->  .._[0]
  override def eval(node: Node, ctx: Context, config: Config) = SingleNodeIter(node, Some(ctx), Some(config))
}

case class Conditional() extends Expr { // test, consequent, alternate
  override def eval(node: Node, ctx: Context, config: Config) = {

    if (nodeTest(subExpr(0).eval(node, ctx, config))) subExpr(1).eval(node, ctx, config) //<
    else if (defined(2)) subExpr(2).eval(node, ctx, config) else NilIter() //>
  }
}

case class BoolExpr(boolOp: String) extends Expr {

  override def eval(node: Node, ctx: Context, config: Config) = {

    val b1 = nodeTest(subExpr(0).eval(node, ctx, config))
    val b2 = if (defined(1)) nodeTest(subExpr(1).eval(node, ctx, config)) else false
    boolOp match {
      case "and" => singleBool(b1 && b2)
      case "or" => singleBool(b1 || b2)
      case "not" => singleBool(!b1)
      case _ => throw new RuntimeException(s"op '$boolOp' not implemented")
    }
  }
}

case class SetExpr(setOp: String) extends Expr {

  override def eval(node: Node, ctx: Context, config: Config) = setOp match {
    case "union" => MultiNodeIter( //
                                   subExpr(0).eval(node, ctx, config) //
                                     ++ action(() => subExpr(0).clearVars(ctx)) //
                                     ++ subExpr(1).eval(node, ctx, config))
    case _ => throw new RuntimeException(s"op '$setOp' not implemented")
  }
}

case class RelationalExpr(relOp: String, kind: String = "first") extends Expr {

  def cmp(n1: Node, n2: Node): Boolean = {

    relOp match {
      case "equal" => n1.obj == n2.obj
      case "notequal" => n1.obj != n2.obj
      case _ => throw new RuntimeException(s"bad op '$relOp'")
    }
  }

  override def eval(node: Node, ctx: Context, config: Config) = {

    // for now only 'first'
    kind match {
      case "first" => //
        val it0 = subExpr(0).eval(node, ctx, config)
        val it1 = subExpr(1).eval(node, ctx, config)
        if (it0.isEmpty || it1.isEmpty) singleBool(false) else singleBool(cmp(it0.next(), it1.next()))
      case _ => throw new RuntimeException(s"for now only comparison kind 'first' allowed ($this)")
    }
  }
}

case class Path() extends Expr {

  override def eval(node: Node, ctx: Context, config: Config): NodeIter = {

    eval(subExpr, node, ctx, config)
  }

  def eval(steps: Seq[Expr], node: Node, ctx: Context, config: Config): NodeIter = {

    if (steps.isEmpty) {
      Apath.loggSolution(node)
      SingleNodeIter(node, Some(ctx), Some(config))
    } else {
      val itY = steps.head.eval(node, ctx, config)
      Apath.loggStep(itY.isEmpty, node, steps.head)
      new NodeIter {

        var itZ: NodeIter = NilIter()

        override def hasNext: Boolean = {

          //<
          if (!itZ.hasNext)
            if (itY.hasNext) {
              steps.tail.foreach(e => e.clearVars(ctx))
              itZ = eval(steps.tail, itY.next(), ctx, config)
              if (itZ.hasNext) true else hasNext
            } else false
          else true
          //>
        }
        override def next() = new Node(itZ.next(), incrCnt())
      }
    }
  }
}

case class Property(name: String, isAttribute: Boolean = false, namespace: String = "none") extends Expr {

  // for java
  def this(name: String) {

    this(name, false, "none")
  }
  def this(name: String, isAttribute: Boolean) {

    this(name, isAttribute, "none")
  }
}

case class ArraySubscript(idx: Int = -1) extends Expr {

  override def eval(node: Node, ctx: Context, config: Config) = //
    if (idx == -1) Children().eval(node, ctx, config) else super.eval(node, ctx, config)

  override def toString = s"ArraySubscript(${if (idx == -1) "*" else idx})"
}

case class SequenceSubscript(idx: Int = -1) extends Expr {

  override def eval(node: Node, ctx: Context, config: Config) = //
    if (idx == -1 || idx == node.order) SingleNodeIter(node) else NilIter()

  override def toString = s"SequenceSubscript(${if (idx == -1) "*" else idx})"
}

case class Children() extends Expr

case class Descendants() extends Expr {

  override def eval(node: Node, ctx: Context, config: Config) = descendants(node, Children(), ctx, config)
}

case class Filter() extends Expr { // test
  override def eval(node: Node, ctx: Context, config: Config) = //
    singleBool(nodeTest(subExpr(0).eval(node, ctx, config)), node)
}

case class VarMatch(var varName: String) extends Expr {

  var variable: Option[Var] = None
  var firstOcc: Boolean = false

  override def eval(node: Node, ctx: Context, config: Config) = {

    if (variable.isEmpty) {
      varName = buildVarName(node)
      val p: (Var, Boolean) = ctx.register(varName)
      variable = Some(p._1)
      firstOcc = p._2
    }
    if (firstOcc) {variable.get.value = Some(node); SingleNodeIter(node, Some(ctx), Some(config))} //<
    else if (node eqObj variable.get.value.get) SingleNodeIter(node, Some(ctx), Some(config))
         else NilIter() //>
  }

  override def clearVars(ctx: Context) = if (variable.nonEmpty && firstOcc) variable.get.value = None
  override def reset() = variable = None

  private def buildVarName(node: Node) = {

    if (varName == "") //<
      if (node.selector.isEmpty)
        throw new RuntimeException(s"no selector for implicit variable name available (at node ${abbr(node)})")
      else node.selector
    else varName //>
  }

  override def toString = s"VarMatch($varName,$firstOcc)"
}

case class VarAppl(varName: String) extends Expr {

  override def eval(node: Node, ctx: Context, config: Config) = {

    val v = ctx.varMap.get(varName)
    if (v.isEmpty) throw new RuntimeException(s"node variable '$varName' not defined") //<
    if (v.get.value.isEmpty) throw new RuntimeException(s"node variable '$varName' not bound") //<
    else SingleNodeIter(v.get.value.get, Some(ctx), Some(config)) //>
  }
}

case class Selector() extends Expr {

  override def eval(node: Node, ctx: Context, config: Config) = SingleNodeIter(Node(node.selector))
}

case class Const(value: Any) extends Expr {

  override def eval(node: Node, ctx: Context, config: Config) = SingleNodeIter(Node(value))

  def eqValue(thatValue: Any) = thatValue == value
}

case class Text() extends Expr {

  override def eval(node: Node, ctx: Context, config: Config) = {

    val r = config.acc.selectorFunc.get.apply(node, this)
    if (r.isInstanceOf[DelegatedIter]) SingleNodeIter(Node(node.obj.toString, node.selector)) //<
    else r //>
  }
}

case class RegexMatch() extends Expr {

  override def eval(node: Node, ctx: Context, config: Config): NodeIter = {

    val obj = node.obj
    if (!obj.isInstanceOf[String]) return NilIter()

    val regexIt = subExpr(0).eval(node, ctx, config)
    if (regexIt.isEmpty) {
      NilIter()
    } else {
      val rnode = regexIt.next()
      var success = true
      rnode.obj match {
        case str: String =>  //
          val matcher = str.r.pattern.matcher(obj.asInstanceOf[String])
          val cnt = matcher.groupCount()
          success = matcher.matches()
          if (success) {
            for (i <- 0 to cnt) //
              if (defined(i + 1)) {
                val arg = subExpr(i + 1)
                val gi = matcher.group(i)
                val t = arg match {
                  case e: Const => e.eqValue(gi)
                  case _ => nodeTest(arg.eval(Node(gi), ctx, config))
                }
                success = success && t
              }
            if (success) SingleNodeIter(node, Some(ctx), Some(config)) else NilIter()
          } else //
            NilIter()
        case _ => throw new RuntimeException(s"type 'String' expected (found '${abbr(rnode)}')")
      }
    }
  }
}

// ------------------ nodes & iters -------------------------
//
case class Node(obj: Any, selector: String = "", //<
                isArray: Boolean = false, parent: Option[Any] = None, order: Int = 0) {
  //>
  // for java
  def this(obj: Any) {

    this(obj, "")
  }

  // to introduce node order in paths
  def this(node: Node, order: Int) {

    this(node.obj, node.selector, node.isArray, node.parent, order)
  }

  def eqObj(that: Node) = obj == that.obj

  override def toString: String = {

    s"Node($obj,$selector,$isArray,$parent,$order)"
//    s"Node($obj,$selector,$parent)"
  }
}

class NilNode extends Node(AnyRef)

abstract class NodeIter extends Iterator[Node] {

  var cnt = -1
  def incrCnt(): Int = {cnt += 1; cnt}

//  // for debugging purposes
//  override def next() = super.next()
}

case class SingleNodeIter(node: Node, ctx: Option[Context] = None, config: Option[Config] = None) extends NodeIter {

  var consumed: Boolean = false

  //  val flatArray = acc.nonEmpty && acc.get.arrayAsFirstClass
  //  val ch: Option[NodeIter] = if (flatArray)
  override def hasNext: Boolean = {

    !consumed
  }

  override def next() = {

    if (consumed) throw new RuntimeException("no next in Single") else {
      consumed = true
      node
    }
  }
}

// for now I see no other possibility to 'downcast'
case class MultiNodeIter(it: Iterator[Node]) extends NodeIter {

  override def hasNext: Boolean = it.hasNext

  override def next() = new Node(it.next(), incrCnt())
}

case class NilIter() extends NodeIter {

  override def hasNext: Boolean = false

  override def next = throw new RuntimeException("no next")
}

class DelegatedIter() extends NilIter

//case class ObjIter(nid: NodeIter) extends Iterable[Any] {
//
//  def iterator = new Iterator[Any] {
//    def hasNext = nid.hasNext
//    def next() = nid.next().obj
//  }
//}
object ApathAdt {

  def ofType(e: Expr, c: Class[_]): Boolean = c.isInstance(e)

  def descendants(node: Node, ch: Children, ctx: Context, config: Config): NodeIter = {

    val chs = ch.eval(node, ctx, config)
    val itself = SingleNodeIter(node, Some(ctx), Some(config))
    MultiNodeIter(itself ++ chs.flatMap(n => descendants(n, ch, ctx, config)))
  }

  def nodeTest(it: NodeIter): Boolean = {

    if (it.hasNext) {
      val on = it.next()
      if (it.hasNext) true //<
      else on.obj match {
        case b: Boolean => b
        case _ => true
      } //>
    } else false
  }

  def singleBool(b: Boolean): NodeIter = SingleNodeIter(Node(b))

  def singleBool(b: Boolean, node: Node): NodeIter = if (b) SingleNodeIter(node) else NilIter()

  def action(a: () => Unit): NodeIter = {

    new NodeIter {
      override def hasNext = {a(); false}
      override def next() = throw new UnsupportedOperationException()
    }
  }
}

object PrintUtil {

  implicit class IExpr(e: Expr) {
  }

  def abbr(node: Node): String = StringUtils.abbreviate(node.toString.replaceAll("\\s+", " "), 80)
}

