package org.afm.apath3.core

import java.io.FileInputStream

import net.minidev.json.JSONObject
import org.afm.apath3.accessors.JsonSmartAcc
import org.afm.apath3.core.Apath._
import org.afm.apath3.struct.Holder
import org.afm.util.Testing
import org.afm.util.Testing.IString
import org.junit.Assert._
import org.junit.Test

//import ApathUtil.IIter._
class ApathTest {

  // scratch, no full test
  @Test def testBasic(): Unit = {

    // create the accessor to the underlying structure, here JsonSmart
    val acc = new JsonSmartAcc()
    // parse the input file
    val jo: JSONObject = acc.parse(new FileInputStream("src/test/resources/samples/books.json"))

    // create apath
    val ap = new Apath(new Config(acc))

    // match path on json object
    val success = ap.doMatch(jo, "root.store.book.*?(price $p and title $t)", amatch => {
      // for each match
      // access variable 't'
      println(amatch.v("t"))
      // access the matched node
      println(amatch.current)
    })

    println(success)

    // same as above but via the standard iterator
    ap.doMatch(jo, "root.store.book.*?(price $p and title $t)").foreach(amatch => {
      // ...
    })

    // get the first matched node
    val jo1: JSONObject = ap.get(jo, "root.store.book.*?(price $p and title $t)")
    println(jo1.toString)
    assertEquals(Testing.expected(false, jo1.toString, "0"), jo1.toString.cr())

    // get the first matched node or the default if no match occurred
    var s: String = ap.get(jo, "root.store.book.*.titlexxx", "no title")
    println(s)
    assertEquals(Testing.expected(false, s, "1"), s.cr())

    // get the first binding of variable 't'
    s = ap.doMatch(jo, "root.store.book.*?(price $p and title $t)").first.get.v("t")
    println(s)
    assertEquals(Testing.expected(false, s, "2"), s.cr())
  }

  @Test def testHolder(): Unit = {

    // create the accessor to the underlying structure, here JsonSmart
    val acc = new JsonSmartAcc()

    // create the custom class as a Holder of an underlying (json) object
    class Person(val age: Int, config: Config) extends Holder(config)

    // initialize
    val person: Person = new Person(99, new Config(acc)).setObject("{\"name\":\"john\"}")

    // access the held object by apath
    println(person.get("name"))
    assertEquals("john", person.get("name"))
    // access the direct attribute
    println(person.age)
    assertEquals(99, person.age)
  }
}




