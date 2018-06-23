package org.afm.apath3.accessors

import java.io._

import org.afm.apath3.core._

import scala.xml.{Elem, MetaData, Null, TopScope}


class XmlAcc(val ignoreElementPrefixes: Boolean) extends Accessor {

  class XmlNodeIter(s: Iterator[xml.Node]) extends NodeIter {

    override def hasNext = s.hasNext
    override def next() = {

      incrCnt()
      val xmlNode = s.next()
      createNode(xmlNode, xmlNode.label, cnt)
    }
  }

  setIsPropertyMapFunc(obj => obj.isInstanceOf[Elem])

  setSelectorFunc((node, e) => {
    val o = node.obj
    e match {
      case Property(name, isAttribute, _) => o match {
        case elm: Elem => nameRes(elm, name, isAttribute)
        case _ => NilIter()
      }
      case ArraySubscript(_) => //
        throw new RuntimeException(s"no array subscript allowed, use sequence subscript (object: '${
          PrintUtil.abbr(node)
        }')")
      case Children() => o match {
        case elm: Elem => new XmlNodeIter((elm \ "_").iterator)
        case _ => NilIter()
      }
      case Text() => o match {
        case elm: Elem => iterO(elm.text, "")
        case s: String => iterO(s, "")
        case _ => NilIter()
      }
      case _ => new DelegatedIter()
    }
  })

  private def nameRes(elm: Elem, name: String, isAttribute: Boolean): NodeIter = {

    val (hasNs, ns, ln) = nsSplit(name)
    if (isAttribute) {
      val a = if (hasNs) elm.attribute(elm.scope.getURI(ns), ln) else elm.attribute(name)
      if (a.isEmpty) NilIter() else iterO(a.get.toString(), name)
    } else //<
      new XmlNodeIter(elm.child.filter(node => {

        val checkNs =
          if (ignoreElementPrefixes) true
          else if (hasNs) ns == node.prefix else node.prefix == null

        checkNs && node.label == ln

      }).iterator) //>
  }


  private def nsSplit(name: String) = {

    // in the context of name processing \u0000 is used as the namespace-name delimiter
    val m = "(.*)\u0000(.*)".r.findFirstMatchIn(name)
    if (m.nonEmpty) (true, m.get.group(1), m.get.group(2)) else (false, "", name)
  }

  override def parse[T](xml: String) = wrap(scala.xml.XML.loadString(xml)).asInstanceOf[T]

  override def parse[T](in: InputStream) = wrap(scala.xml.XML.load(in)).asInstanceOf[T]

  private def wrap(elem: Elem): Elem = new Elem(null, "_root", Null, TopScope, true, elem)
}
