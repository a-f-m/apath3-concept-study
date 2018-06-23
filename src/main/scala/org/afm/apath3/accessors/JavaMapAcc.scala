package org.afm.apath3.accessors

import java.io.InputStream
import java.util

import org.afm.apath3.core._


class JavaMapAcc extends Accessor {

  type JMap = util.Map[_, _]
  type JList = util.List[_]

  setIsArrayFunc(obj => obj.isInstanceOf[JList])
  setIsPropertyMapFunc(obj => obj.isInstanceOf[JMap])

  setSelectorFunc((node, e) => { //>
    val o = node.obj
    e match {
      case Property(name, false, _) => o match {
        case m: JMap => iterO(m.get(name), name)
        case _ => NilIter()
      }
      case ArraySubscript(idx) => o match {
        case l: JList => iterO(l.get(idx), idx.toString)
        case _ => NilIter()
      }
      case Children() => {
        o match {
          case m: JMap => iter(m.entrySet().iterator())
          case l: JList => iter(l.iterator())
          case _ => NilIter()
        }
      }
      case _ => new DelegatedIter()
    }
  })
  override def parse[T](s: String) = ???
  override def parse[T](in: InputStream) = ???
}



