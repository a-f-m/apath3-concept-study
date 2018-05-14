package org.afm.apath3.accessors

import java.io.FileReader

import scala.xml.{Elem, NodeSeq}


class A {

  var sub: Seq[A] = Seq.empty
}


case class B(s: String) extends A {
}

object U {

  /**
    *
    * @param it
    * @tparam A
    * @return
    */
  def first[A](it: Iterable[A]): Option[A] = if (it.iterator.hasNext) Some(it.iterator.next()) else None

  implicit class IIt[A](it: Iterable[A]) {

    def xxx: Option[A] = if (it.iterator.hasNext) Some(it.iterator.next()) else None
  }


}

object Scatch {

  implicit class I(a: A) {

    def fullString: String = {

      a.sub.map(x => x.fullString) + a.toString
    }
  }

  def main(args: Array[String]): Unit = {

    case class X(i: Int) {

      val l = List(1, 2, 3)
    }

    val x = X(99)

    x match {
      case X(99) => x.l match {
        case 1 :: 2 :: rest => println(rest)
        case _ => ()
      }
    }

    (x, x.l) match {
      case (X(i), 1 :: 2 :: rest) => println(s"$i $rest")
      case _ => ()
    }

    val acc = new XmlAcc()

    val elem:Elem = acc.parse("<root a='lala'> <a></a> <b></b>  <b></b>  </root>")
    println(elem)

    val seq = elem \ "root" \ "@a"
    println(seq)

    val a: Elem = (elem \ "root").head.asInstanceOf[Elem]

    val maybeNodes = a.attribute("a")
    println(maybeNodes.get.toString())


    val matcher = "(.*)-(.*)-(.*)".r.pattern.matcher("33-55-66")
    if (matcher.matches()) //
      for (i <- 1 to matcher.groupCount()) {
        println(matcher.group(i))
      }
    println(matcher.groupCount())

    for (i <- 0 to 0) println("lal")

    val maybeMatch = "\\d(\\d)?\\.(.*)".r.findFirstMatchIn("8.95")
    println(maybeMatch)
  }
}
