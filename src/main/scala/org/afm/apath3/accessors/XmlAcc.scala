package org.afm.apath3.accessors

import java.io._


import org.afm.apath3.core._

import scala.xml.Elem


class XmlAcc extends Accessor {

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
        case _ => throw new RuntimeException("fatal should not happen")
      }
      case ArraySubscript(_) => //
        throw new RuntimeException(s"no array subscript allowed, use sequence subscript (object: '${
          PrintUtil.abbr(node)
        }')")
      case Children() => o match {
        case elm: Elem => new XmlNodeIter((elm \ "_").iterator)
        case _ => throw new RuntimeException("fatal should not happen")
      }
      case Text() => o match {
        case elm: Elem => iterO(elm.text, "")
        case s: String => iterO(s, "")
        case _ => throw new RuntimeException("fatal should not happen")
      }
      case _ => new DelegatedIter()
    }
  })

  private def nameRes(elm: Elem, name: String, isAttribute: Boolean): NodeIter = {

    if (isAttribute) {
      val a = elm.attribute(name)
      if (a.isEmpty) NilIter() else iterO(a.get.toString(), name)
    } else //<
        new XmlNodeIter((elm \ name).iterator) //>
  }


  override def parse(xml: String) = wrap(scala.xml.XML.loadString(xml))

  override def parse(in: InputStream) = wrap(scala.xml.XML.load(in))

  private def wrap(elem: Elem) = new Elem(elem.prefix, "_root", elem.attributes, elem.scope, elem.minimizeEmpty, elem)
}
