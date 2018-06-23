package org.afm.apath3.core


import java.util

import afm.mouse.DefaultSemantics
import mouse.runtime.SourceString
import org.afm.apath3.parsing.PathPegParserV3

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * TODO make buildExpr more 'functional'
  */
class PathParser() {

  val parser = new PathPegParserV3()

  var selectorStack: mutable.ArrayStack[String] = mutable.ArrayStack()

  var varCnt: Int = 0

  val opm: Map[String, String] = Map("and" -> "and", "&&" -> "and", "or" -> "or", "||" -> "or", "union" -> "union",
                                     "|" -> "union", "==" -> "equal", "!=" -> "notequal")

  val arity: Map[String, (Int, Int)] = Map("not" -> (1, 1), "selector" -> (0, 0), "text" -> (0, 0))

  val funcMatchVarIdx: Map[String, Int] = Map()


  // due to deprecation synchronized maps in scala 2.12 we follow their recommendation to use java
  val exprCache: java.util.Map[String, Expr] = java.util.Collections.synchronizedMap[String, Expr](createLRUMap(500))

  def parse(expr: String): Expr = {

    selectorStack.clear()

    var bexpr = exprCache.get(expr)
    if (bexpr != null) {bexpr.reset(); return bexpr}

    DefaultSemantics.astAsList = true
    val ok = parser.parse(new SourceString(expr))
    if (!ok) throw new RuntimeException("syntax error:\n" +
                                          parser.semantics.getErrorMsg.replace("[ \\t\\r\\n\\f] or ", ""))

    val ast = parser.semantics.getCurrList
    bexpr = buildExpr(javaListToScala(ast.asInstanceOf[util.List[Any]]))
    exprCache.put(expr, bexpr)
    bexpr
  }

  def ensureFuncArity(size: Int, func: String): Unit = {

    val a = arity(func)
    val s = size - 2
    if (s < a._1 || s > a._2) throw new RuntimeException(s"arity (min ${a._1}, max ${a._2} for func $func expected")
  }

  def buildExpr(sexpr: Seq[Any]): Expr = {

    if (sexpr.isEmpty) return EmptyExpr()

    val func: String = functor(sexpr)
    val toBinaryExpr = sexpr.size > 4
    val toPass = sexpr.size == 2

    if (!toPass) selectorStack.push(func)
    val e = func match {
      case "Start" => buildExpr(sub(sexpr, 1))
      case "boolAndExpr" | "boolOrExpr" if !toPass => //
        if (toBinaryExpr) buildExpr(toBinary(sexpr)) else {
          BoolExpr(opm(asString(sexpr, 2))) :+ buildExpr(sub(sexpr, 1)) :+ buildExpr(sub(sexpr, 3))
        }
      case "setExpr" if !toPass => //
        if (toBinaryExpr) buildExpr(toBinary(sexpr)) else {
          SetExpr(opm(asString(sexpr, 2))) :+ buildExpr(sub(sexpr, 1)) :+ buildExpr(sub(sexpr, 3))
        }
      case "comparisonExpr" if !toPass => //
        RelationalExpr(opm(asString(sexpr, 2))) :+ buildExpr(sub(sexpr, 1)) :+ buildExpr(sub(sexpr, 3))
      case "ifExpr" if !toPass => //
        Conditional() :+ buildExpr(sub(sexpr, 1)) :+ buildExpr(sub(sexpr, 2)) :+ buildExpr(sub(sexpr, 3))
      case "regexMatch" => //
        RegexMatch().setArgs(gather(sexpr.tail).subExpr)
      case "funcCall" => buildFunc(sexpr)
      case "val" => Const(asObj(sexpr))
      case "path" => Path().setArgs(gather(sexpr.tail).subExpr)
      case "relativePath" | "step" => gather(sexpr.tail)
      case "selection" => gather(sexpr.tail)
      case "current" => Self()
      case "property" => //
        val (isAttribute, name) = escape2(asString(sexpr))
        Property(name.substring(if (isAttribute) 1 else 0), isAttribute)
      case "children" => Children()
      case "selector" => Selector()
      case "arraySubscript" => ArraySubscript(idx(sexpr))
      case "sequenceSubscript" => SequenceSubscript(idx(sexpr))
      case "descendants" => GatherExpr() :+ Descendants() :+ buildExpr(sub(sexpr, 1))
      case "filter" => Filter() :+ buildExpr(sub(sexpr, 1))
      case "varMatch" => VarMatch(asString(sub(sexpr, 1)).substring(1))
      case "var" => VarAppl(asString(sexpr).substring(1))
      case _ if toPass => buildExpr(sub(sexpr, 1))
      case _ => throw new RuntimeException(s"bad functor $func")
    }
    if (!toPass) selectorStack.pop()
    e
  }

  private def buildFunc(sexpr: Seq[Any]) = {

    val funcName = asString(sexpr, 1)
    if (arity.get(funcName).isEmpty) throw new RuntimeException(s"func '${asString(sexpr, 1)}' not implemented")
    ensureFuncArity(sexpr.size, funcName)
    funcName match {
      case "not" => BoolExpr("not") :+ buildExpr(sub(sexpr, 2))
      case "selector" => Selector()
      case "text" => Text()
      case _ => throw new RuntimeException(s"bad func name match (${asString(sexpr, 1)})")
    }
  }
  private def idx(sexpr: Seq[Any]) = {

    val m = "\\[\\:(.*)\\:\\]|\\[(.*)\\]".r.findFirstMatchIn(asString(sexpr)).get
    var s = m.group(1)
    if (s == null) s = m.group(2)
    if (s == "*") -1 else s.toInt
  }

  def gather(sexpr: Seq[Any]): GatherExpr = {

    val ge = GatherExpr()
    sexpr.foreach(x => {
      val arg = buildExpr(asL(x))
      arg match {
        case g: GatherExpr => ge.subExpr = ge.subExpr ++ g.subExpr
        case _ => ge.subExpr = ge.subExpr :+ arg
      }
    })
    ge
  }

  // from http://stackoverflow.com/users/57695/peter-lawrey
  def createLRUMap[K, V](maxEntries: Int) = new util.LinkedHashMap[K, V](maxEntries * 10 / 7, 0.7f, true) {
    override protected def removeEldestEntry(eldest: util.Map.Entry[K, V]): Boolean = size > maxEntries
  }

  // base ops for s-expr's as scala seq's
  def toBinary(l: Seq[Any]): Seq[Any] = toBinaryRec(l).asInstanceOf[Seq[Any]]

  def toBinaryRec(l: Seq[Any]): Any = //
    if (l.size == 4) l else ArrayBuffer(l.head, l(1), l(2), toBinaryRec(l.head +: l.slice(3, l.size)))

  def sub(l: Seq[Any], i: Int): Seq[Any] = //
    if (i >= l.size || !l(i).isInstanceOf[Seq[Any]]) Seq.empty else l(i).asInstanceOf[Seq[Any]]


  def asL(x: Any): Seq[Any] = x.asInstanceOf[Seq[Any]]

  def asString(l: Seq[Any]): String = l(1).asInstanceOf[String]

  def asObj(l: Seq[Any]): Any = l(1)

  def asString(l: Seq[Any], i: Int): String = asString(sub(l, i))

  def functor(l: Seq[Any]) = l.head.asInstanceOf[String]

  // custom peg java s-expr's to s-expr's as scala seq's
  def javaListToScala(sexpr: util.List[Any]): Seq[Any] = javaListToScalaRec(sexpr).asInstanceOf[Seq[Any]]

  def javaListToScalaRec(sexpr: util.List[Any]): Any = {

    sexpr.asScala.map(x => if (x.isInstanceOf[util.List[_]]) javaListToScalaRec(x.asInstanceOf[util.List[Any]]) else x)
  }

  private def escape2(name: String) =
//    name.replace("\\:", "\u0000").replaceAll("\\\\(.)", "$1")
  {

    var isAttribute = name.startsWith("@")
    (isAttribute, name.replaceFirst("(?<!\\\\)\\:", "\u0000").replaceAll("\\\\([^u])", "$1"))
  }
}

