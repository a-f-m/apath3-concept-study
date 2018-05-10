package org.afm.apath3.core

import java.io.{FileInputStream, FileReader}

import net.minidev.json.{JSONObject, JSONValue}
import org.afm.apath3.accessors._
import org.afm.util.Testing
import org.jsoup.nodes.{Document, Element}
import org.junit.Assert._
import org.junit.{Before, Ignore, Test}

import scala.xml.Elem

class ApathAdtTest {

  var jo: Option[JSONObject] = None

  val acc = new JsonSmartAcc()

  val sbuilder = new StringBuilder()

  val ctx = new Context()

  @Before def setUp(): Unit = {

    jo = Some(JSONValue.parse(new FileReader("src/test/resources/core/books.json")).asInstanceOf[JSONObject])
  }

  @Test def test2(): Unit = {

    val it = Path().setArgs(Seq(Property("root"), Property("store")))
             .eval(new Node(jo.get), new Context(), new Config(acc))
    if (it.isEmpty) fail()
    val r = it.next().toString
    assertEquals(Testing.expected(false, r), r)
  }

  @Test def test3(): Unit = {

    val it = Path().setArgs(Seq(Property("root"), Property("store"), Property("book"), ArraySubscript(1)))
             .eval(Node(jo.get), new Context(), new Config(acc))
    if (it.isEmpty) fail()
    val r = it.next().toString
    assertEquals(Testing.expected(false, r), r)
  }

  @Test def testSimple(): Unit = {

    val expr = new PathParser().parse("root.store.book.*")
    println(expr.prettyString)

    val it = expr.eval(Node(jo.get), new Context(), new Config(acc))

    val r = it.toList.toString()
    assertEquals(Testing.expected(false, r), r)
  }

  @Test def testChildren(): Unit = {

    val it = Path().setArgs(Seq(Property("root"), Property("store"), Property("book"), Children()))
             .eval(Node(jo.get), new Context(), new Config(acc))

    val s = it.toList.toString()
    assertEquals(Testing.expected(false, s), s)
  }

  @Test def testDescendants(): Unit = {

    var r = ApathAdt.descendants(Node(jo.get), Children(), new Context(), new Config(acc)).toList.toString()
    var exp = Testing.expected(false, r, "0")
    assertEquals(exp, r)

    var it = Path().setArgs(Seq(Descendants())).eval(Node(jo.get), new Context(), new Config(acc))
    r = it.toList.toString()
    exp = Testing.expected(false, r, "0.1")
    assertEquals(exp, r)

    it = Path().setArgs(Seq(Descendants(), Property("title"))).eval(Node(jo.get), new Context(), new Config(acc))

    val ls: String = it.toList.toString()
    assertEquals(Testing.expected(false, ls, "1"), ls)
  }

  @Test def testPred(): Unit = {

    sbuilder.clear()
    var expr = new PathParser().parse("root.store..book[*]?(_.category == 'fiction').title")
    println(expr.prettyString)

    expr.eval(Node(jo.get), ctx, new Config(acc)).foreach(n => {
      sbuilder.append(n.obj)
    })

    assertEquals("Sword of Honour", sbuilder.toString())
  }

  @Test def testVars(): Unit = {

    sbuilder.clear()

    var it = Path()
             .setArgs(Seq(Property("root"), Property("store"), Descendants(), VarBind("v"), Property("title"),
                          VarBind("o"))).eval(Node(jo.get), ctx, new Config(acc))

    it.foreach(n => {
      sbuilder.append(ctx.toString + " | ")
      sbuilder.append(n + " >>> ")
    })

    assertEquals(Testing.expected(false, sbuilder.toString(), "0"), sbuilder.toString())

    val path = Path().setArgs(Seq(Property("root"), Property("store"), Property("book"), Children(), //
                                  Filter() :+ (BoolExpr("and") :+ //
                                    (Path() :+ Property("price") :+ VarBind("p")) :+
                                    (Path() :+ Property("title") :+ VarBind("t")))))
    it = path.eval(Node(jo.get), ctx, new Config(acc))

    sbuilder.clear()
    it.foreach(n => {
//      if (ApathAdt.no)
      sbuilder.append("(" + ctx.varMap.get("p") + ", " + ctx.varMap.get("t") + ");")
    })

    assertEquals(Testing.expected(false, sbuilder.toString(), "1"), sbuilder.toString())

    sbuilder.clear()

    var expr = new PathParser().parse("root.store..title $o")
    println(expr.prettyString)

    ctx.clear()
    expr.eval(Node(jo.get), ctx, new Config(acc)).foreach(n => {
      sbuilder.append(" " + n.obj)
    })

    assertEquals(" Sayings of the Century Sword of Honour", sbuilder.toString())

    sbuilder.clear()
    expr = new PathParser().parse("root?(expensive $e).store.book.*?(price ==  $e).title")
    println(expr.prettyString)

    ctx.clear()
    expr.eval(Node(jo.get), ctx, new Config(acc)).foreach(n => {
      sbuilder.append(" " + n.obj)
    })

    assertEquals(" Sword of Honour", sbuilder.toString())

    sbuilder.clear()
    expr = new PathParser().parse("root.store.book.*?(price $ and title $)")
    println(expr.prettyString)

    ctx.clear()
    expr.eval(Node(jo.get), ctx, new Config(acc)).foreach(n => {
      sbuilder.append(s" ${ctx.varMap("price")} ${ctx.varMap("title")}")
    })

//    assertEquals(" Some(Node(8.95,None,None)) Some(Node(Sayings of the Century,None,None)) Some(Node(12,None,None)) Some(Node(Sword of Honour,None,None))",
//                 sbuilder.toString())
    assertEquals(Testing.expected(false, sbuilder.toString(), "2"), sbuilder.toString())
  }

  @Test def testTransform1(): Unit = {

    val jo_ = JSONValue.parse(new FileReader("src/test/resources/core/expr.json")).asInstanceOf[JSONObject]

    sbuilder.clear()
    val expr = new PathParser().parse("expr.times ?(" //
                                        + "     _[0] $x1 and " //
                                        + "     _[1].plus ?(" //
                                        + "         _[0] $x2 and " //
                                        + "         _[1] $x3))" //
                                     )
    println(expr.prettyString)

    ctx.clear()
    expr.eval(Node(jo_), ctx, new Config(acc)).foreach(n => {
//      sbuilder.append(s" ${ctx.varMap("1")} ${ctx.varMap("2")}")
      sbuilder.append(ctx.varMap)
    })

    assertEquals(Testing.expected(false, sbuilder.toString()), sbuilder.toString())
  }

  @Test def testBoolOp(): Unit = {

    sbuilder.clear()
    val expr = new PathParser().parse("root.store..book.*?(category == 'fiction' && price != 10 && 1==1).title")
    println(expr.prettyString)

    ctx.clear()
    expr.eval(Node(jo.get), ctx, new Config(acc)).foreach(n => {
      sbuilder.append(s" ${n.obj}")
    })

    assertEquals(" Sword of Honour", sbuilder.toString())
  }

  @Test def testConditional(): Unit = {

    sbuilder.clear()
    var expr = new PathParser().parse("  root.store.book[*]." //
                                        + "   (price != 8.95 -> " //
                                        + "       price ~ " //
                                        + "    onStock -> " //
                                        + "       title ~ " //
                                        + "    99)")
    println(expr.prettyString)

    ctx.clear()
    expr.eval(Node(jo.get), ctx, new Config(acc)).foreach(n => {
      sbuilder.append(s" ${n.obj}")
    })

    assertEquals(" Sayings of the Century 12 99.0", sbuilder.toString())

    sbuilder.clear()
    expr = new PathParser().parse("  root.store.book[*]." //
                                    + "   (if (price != 8.95) " //
                                    + "       then price  " //
                                    + "    else if (onStock) " //
                                    + "       then title " //
                                    + "    )")
    println(expr.prettyString)

    ctx.clear()
    expr.eval(Node(jo.get), ctx, new Config(acc)).foreach(n => {
      sbuilder.append(s" ${n.obj}")
    })

    assertEquals(" Sayings of the Century 12", sbuilder.toString())
  }

  @Test def testNot(): Unit = {

    sbuilder.clear()
    val expr = new PathParser().parse("not(root.store.book[0].category)")
    println(expr.prettyString)

    ctx.clear()
    expr.eval(Node(jo.get), ctx, new Config(acc)).foreach(n => {
      sbuilder.append(s" ${n.obj}")
    })

    assertEquals(" false", sbuilder.toString())
  }

  @Test def testVarOr(): Unit = {

    val jo1: JSONObject = JSONValue.parse("{ " //
                                            + "   'root': { " //
                                            + "      'a': { " //
                                            + "         'b': 99, " //
                                            + "         'c': 'ccc' " //
                                            + "      }, " //
                                            + "      'd': { " //
                                            + "         'c': 11 " //
                                            + "      }, " //
                                            + "      'e': 88 " //
                                            + "   } " + "} ").asInstanceOf[JSONObject]


    sbuilder.clear()
    val expr = new PathParser().parse("root $r.* $ ?(b $b or c $c)")
    println(expr.prettyString)

    ctx.clear()
    expr.eval(Node(jo1), ctx, new Config(acc)).foreach(n => {
      sbuilder.append(s" ${ctx.varMap}")
    })

    assertEquals(Testing.expected(false, sbuilder.toString()), sbuilder.toString())
  }

  @Test def testEnhVars(): Unit = {

    val jo_ = JSONValue.parse(new FileReader("src/test/resources/core/texpr.json")).asInstanceOf[JSONObject]

    sbuilder.clear()
    var expr = new PathParser().parse("expr.plus[*]?(times[0].const $c1 and times[0].const $c2)")
    println(expr.prettyString)

    ctx.clear()
    expr.eval(Node(jo_), ctx, new Config(acc)).foreach(n => {
      sbuilder.append(s" ${ctx.varMap}")
    })

    assertEquals(Testing.expected(false, sbuilder.toString(), "1"), sbuilder.toString())

    sbuilder.clear()
    expr = new PathParser().parse("expr.plus[*]?((times[0].const $c1 and times[1].const $c2) or times[2].cast $ca)")
    println(expr.prettyString)

    ctx.clear()
    expr.eval(Node(jo_), ctx, new Config(acc)).foreach(n => {
      sbuilder.append(s" ${ctx.varMap}")
    })

    assertEquals(Testing.expected(false, sbuilder.toString(), "2"), sbuilder.toString())
  }

  @Test def testUnion(): Unit = {

    sbuilder.clear()

    val expr = new PathParser().parse("root.*.(book|bicycle union xxx)")
    println(expr.prettyString)

    expr.eval(Node(jo.get), new Context(), new Config(acc)).foreach(n => sbuilder.append(" " + n.obj))

    assertEquals(" [{\"author\":\"Nigel Rees\",\"price\":8.95,\"onStock\":true,\"category\":\"reference\",\"title\":\"Sayings of the Century\"},{\"author\":\"Evelyn Waugh\",\"price\":12,\"onStock\":false,\"category\":\"fiction\",\"title\":\"Sword of Honour\"},[1,2,3]] {\"color\":\"red\",\"price\":19.95}",
                 sbuilder.toString())
  }

  @Test def testSmartMapper(): Unit = {

    val m = JJsonSmartMapMapper.parser
            .parse(new FileReader("src/test/resources/core/books.json"), new JJsonSmartMapMapper())
            .asInstanceOf[java.util.Map[_, _]]
    println(m)


    val it = Path().setArgs(Seq(Property("root"), Property("store"), Property("book"), ArraySubscript(1)))
             .eval(Node(jo.get), new Context(), new Config(new JavaMapAcc()))
    if (it.isEmpty) fail()
    val s = it.next().toString
    assertEquals(Testing.expected(false, s), s)
  }

  @Test def testXmlLike(): Unit = {

    // we start with html/jsoup
    var jsoupAcc = new JsoupAcc()
    var doc = jsoupAcc.parse(new FileInputStream("src/test/resources/core/simple-1.xml"))

    // jsoup put the xml inside an html body
    var root: Element = doc.asInstanceOf[Document].getElementsByTag("html").get(0).getElementsByTag("body").get(0)

    val jsoupSeqAcc = new JsoupAcc()
    val ap = Apath(new Config(jsoupSeqAcc))

    sbuilder.clear()
    ap.doMatch(root, "ValueSet.codeSystem..").foreach(ctx => {
      sbuilder.append(s">> ${ctx.curr}\n")
    })
    var r: String = sbuilder.toString()
    assertEquals(Testing.expected(false, r, "0"), r)

    sbuilder.clear()
    ap.doMatch(root, "ValueSet[:0:].codeSystem[:0:]..").foreach(ctx => {
      sbuilder.append(s">> ${ctx.curr}\n")
    })
    assertEquals(r, sbuilder.toString())

    sbuilder.clear()
    r = ap.get(root, "ValueSet.codeSystem.extension?(@url == 'ext:groupName').valueString.@value")
    assertEquals("EXA", r)

    doc = jsoupAcc.parse(new FileInputStream("src/test/resources/core/books.xml"))
    root = doc.asInstanceOf[Document].getElementsByTag("html").get(0).getElementsByTag("body").get(0)

    sbuilder.clear()
    var elm: Element = ap.get(root, "doc.root..books.book?(@id == '2').category")
    assertEquals("fiction", elm.text())

    sbuilder.clear()
    r = ap.get(root, "doc.root..books.book?(@id == '2').category.text()")
    assertEquals("fiction", r)
  }

  @Test def testXml(): Unit = {

    // we start with html/jsoup
    val xmlAcc = new XmlAcc()
    var root = xmlAcc.parse(new FileInputStream("src/test/resources/core/simple-1.xml"))

    val ap = Apath(new Config(xmlAcc))

    sbuilder.clear()
    ap.doMatch(root, "ValueSet.codeSystem..").foreach(ctx => {
      sbuilder.append(s">> ${ctx.curr}\n")
    })
    var r: String = sbuilder.toString()
    assertEquals(Testing.expected(false, r, "0"), r)

    sbuilder.clear()
    ap.doMatch(root, "ValueSet[:0:].codeSystem[:0:]..").foreach(ctx => {
      sbuilder.append(s">> ${ctx.curr}\n")
    })
    assertEquals(r, sbuilder.toString())

    r = ap.get(root, "ValueSet.codeSystem.extension?(@url == 'ext:groupName').valueString.@value")
    assertEquals("EXA", r)

    root = xmlAcc.parse(new FileInputStream("src/test/resources/core/books.xml"))

    var elm: Elem = ap.get(root, "doc.root..books.book?(@id == '2').category")
    assertEquals("fiction", elm.text)

    r = ap.get(root, "doc.root..books.book?(@id == '2').category.text()")
    assertEquals("fiction", r)

    r = ap.get(root, "doc..store.*[:1:].*[:1:].text()")
    assertEquals("19.95", r)


  }

  //  @Ignore
  @Test def testSelectorVars(): Unit = {

    val ap = Apath(new Config(acc))

    sbuilder.clear()
    ap.doMatch(jo.get, "root.store..§").foreach(ctx => {
      sbuilder.append(s">> ${ctx.curr} ")
    })
    assertEquals(">> store >> bicycle >> color >> price >> book >> 0 >> author >> price >> onStock >> category >> title >> 1 >> author >> price >> onStock >> category >> title >> 2 >> 0 >> 1 >> 2 ",
                 sbuilder.toString())
  }

  @Test def testRegex(): Unit = {

    var jsoupAcc = new JsoupAcc()

    val doc = jsoupAcc.parse(new FileInputStream("src/test/resources/core/books.xml"))
    val root: Element = doc.asInstanceOf[Document].getElementsByTag("html").get(0).getElementsByTag("body").get(0)

    val ap = Apath(new Config(jsoupAcc))
    sbuilder.clear()

    ap.doMatch(root, "doc.root..books.book?(@id.rmatch('\\d')).price.text() $x?(rmatch( $x))").foreach(ctx => {
      sbuilder.append(s" >> ${ctx.curr}")
    })
    assertEquals(" >> 8.95 >> 12.99", sbuilder.toString())
  }


  @Test def testSeqSubscript(): Unit = {
  }

  @Ignore
  @Test def doTimes(): Unit = {

    var path = Path()
               .setArgs(Seq(Property("root"), Property("store"), Descendants(), VarBind("v"), Property("title"),
                            VarBind("o")))

    times(path, jo.get, 10000)

    path = Path().setArgs(Seq(Property("root"), Property("store"), Property("book"), ArraySubscript(1)))

    times(path, jo.get, 1000000)

    path = Path().setArgs(Seq(Property("root"), Property("store"), Property("book"), Children(), //
                              Filter() :+ (BoolExpr("and") :+ //
                                (Path() :+ Property("price") :+ VarBind("p")) :+
                                (Path() :+ Property("title") :+ VarBind("t")))))

    times(path, jo.get, 1000000)
  }

  private def times(path: Expr, root: JSONObject, j: Int) = {

    var t = System.currentTimeMillis()
    var k = 0
    for (i <- 0 to j) {
      val it = path.eval(Node(root), new Context(), new Config(acc))
      while (it.hasNext) {
        k += 1
        it.next()
      }
    }
    println(k)
    println(s"ms: ${System.currentTimeMillis() - t}")
  }
}
