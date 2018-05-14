package org.afm.apath3.core

import java.io.{FileInputStream, FileReader}

import net.minidev.json.{JSONObject, JSONValue}
import org.afm.apath3.accessors.JsonSmartAcc
import org.afm.apath3.core.Apath._
import org.afm.util.Testing
import org.afm.util.Testing.IString
import org.junit.Assert._
import org.junit.Test
//import ApathUtil.IIter._
class ApathTest {

  // scratch, no full test
  @Test def test1(): Unit = {

    // create the accessor to the underlying structure, here JsonSmart
    val acc = new JsonSmartAcc()
    // parse the input file
    val jo: JSONObject = acc.parse(new FileInputStream("src/test/resources/samples/books.json"))

    // create apath
    val ap = Apath(new Config(acc))

    // match path on json object
    val success = ap.doMatch(jo, "root.store.book.*?(price $p and title $t)", (ctx: Context) => {
      // for each match
      // access variable 'p'
      println(ctx.v("t"))
      // access the matched node
      println(ctx.curr)
    })

    println(success)

    println("---")

    // same as above but with via the standard iterator
    ap.doMatch(jo, "root.store.book.*?(price $p and title $t)").foreach(ctx => {
      // ...
    })

    println("---")

    // get the first matched node
    val jo1: JSONObject = ap.get(jo, "root.store.book.*?(price $p and title $t)")
    assertEquals(Testing.expected(false, jo1.toString, "0"), jo1.toString.cr())

    // get the first matched node or the default if no match occurred
    var s: String = ap.get(jo, "root.store.book.*.titlexxx", "def")
    assertEquals(Testing.expected(false, s, "1"), s.cr())

    // get the first binding of variable 't'
    s = ap.doMatch(jo, "root.store.book.*?(price $p and title $t)").first.get v "t"
    assertEquals(Testing.expected(false, s, "2"), s.cr())

  }
}
