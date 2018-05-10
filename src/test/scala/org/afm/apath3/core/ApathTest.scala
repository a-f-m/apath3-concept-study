package org.afm.apath3.core

import java.io.FileReader

import net.minidev.json.{JSONObject, JSONValue}
import org.afm.apath3.accessors.JsonSmartAcc
import org.afm.apath3.core.Apath._
import org.afm.util.Testing
import org.junit.Assert._
import org.junit.Test
//import ApathUtil.IIter._



class ApathTest {

  val jo = JSONValue.parse(new FileReader("src/test/resources/core/books.json")).asInstanceOf[JSONObject]

  @Test def test(): Unit = {

    val ap = Apath(new Config(new JsonSmartAcc()))

    ap.doMatch(jo, "root.store.book.*?(price $p and title $t)").foreach(ctx => {

      println(ctx v "t")

      println(ctx.currNode)
    })

    println("---")

    val success = ap.doMatch(jo, "root.store.book.*?(price $p and title $t)", (ctx: Context) => {

      println(ctx.currNode)
    })

    println(success)

    println("---")

    var s: String = first(ap.doMatch(jo, "root.store.book.*?(price $p and title $t)")).get v "t"
    assertEquals(Testing.expected(false, s, "0"), s)

    val n: Node = ap.doMatch(jo, "root.store.book.*?(price $p and title $t)").first.get.currNode
    assertEquals(Testing.expected(false, n.toString, "1"), n.toString)

    val jo1: JSONObject = ap.get(jo, "root.store.book.*?(price $p and title $t)")
    assertEquals(Testing.expected(false, jo1.toString, "2"), jo1.toString)

    s = ap.get(jo, "root.store.book.*?(price $p and titlexxx $t)", "def")
    assertEquals(Testing.expected(false, s, "3"), s)
  }
}
