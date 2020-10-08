package org.afm.apath3.accessors

import java.io.InputStream

import net.minidev.json.{JSONArray, JSONObject, JSONValue}
import org.afm.apath3.core._

class JsonSmartAcc extends Accessor {

  // set type for arrays
  setIsArrayFunc(obj => obj.isInstanceOf[JSONArray])
  // set type for property equipped objects
  setIsPropertyMapFunc(obj => obj.isInstanceOf[JSONObject])

  // set selector function that maps an 'node' and apath step expression 'e' to an iterator over result nodes
  setSelectorFunc((node, e) => {
    val o = node.obj
    e match {
      // result for property selection
      case Property(name, false, _) => o match {
        case jo: JSONObject => iterO(jo.get(name), name)
        case _ => NilIter()
      }
      // result for array subscript
      case ArraySubscript(idx) => o match {
        case ja: JSONArray => if (idx < ja.size()) iterO(ja.get(idx), idx.toString) else NilIter()
        case _ => NilIter()
      }
      // result for children selection
      case Children() => o match {
        case jo: JSONObject => iter(jo.entrySet().iterator())
        case ja: JSONArray => iter(ja.iterator())
        case _ => NilIter()
      }
      case _ => new DelegatedIter()
    }
  })

  override def parse[T](in: InputStream) = JSONValue.parse(in).asInstanceOf[T]

  override def parse[T](s: String) = JSONValue.parse(s).asInstanceOf[T]
}
