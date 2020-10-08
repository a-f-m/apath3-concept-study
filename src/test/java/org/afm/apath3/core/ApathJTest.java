package org.afm.apath3.core;

import net.minidev.json.JSONObject;
import org.afm.apath3.accessors.Accessor;
import org.afm.apath3.accessors.JsonSmartAcc;
import org.afm.apath3.accessors.JsoupAcc;
import org.afm.apath3.accessors.XmlAcc;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.afm.apath3.core.Apath.first;


public class ApathJTest {


    @Test // only scratch, no explicit test
    public void doJson() throws FileNotFoundException {

        // create the accessor to the underlying structure, here JsonSmart
        Accessor acc = new JsonSmartAcc();
        // parse the input file
        JSONObject jo = acc.parse(new FileInputStream("src/test/resources/samples/books.json"));

        // create apath
        Apath ap = new Apath(new Config(acc));

        // match path on json object
        boolean success = ap.doMatch(jo, "root.store.book.*?(price.text() $p and title $t)", match -> {
            // for each match
            // access variable 't'
            System.out.println(match.v("t").toString());
            // access the matched node
            System.out.println(match.current().toString());
        });
        System.out.println(success);

        // same as above but with via the standard iterator
        ap.jDoMatch(jo, "root.store.book.*?(price $p and title $t)").forEach(match -> {
            // ...
        });

        // get the first matched node
        JSONObject jo1 = ap.get(jo, "root.store.book.*?(price $p and title $t)");
        System.out.println(jo1);
        // get the first matched node or the default if no match occurred
        String s = ap.get(jo, "root.store.book.*.titlexxx", "no title");
        System.out.println(s);

        // get the first binding of variable 't'
        s = first(ap.jDoMatch(jo, "root.store.book.*?(price $p and title $t)")).get().v("t");
        System.out.println(s);
    }

    @Test // only scratch, no explicit test
    public void doHtml() throws FileNotFoundException {

        Accessor jsoupAcc = new JsoupAcc();
        Document doc = jsoupAcc.parse(new FileInputStream("src/test/resources/samples/mensa.html"));

        Apath ap = new Apath(new Config(jsoupAcc));

        boolean success = ap.doMatch(doc, //
                "..select" + //
                        "?(@id == 'listboxEinrichtungen')" + //
                        "   .option.text()?(_ ~'Mensa (.*)' groups $all, $m)",
                //
                ctx -> {

                    System.out.println((String) ctx.v("m"));
                });

        System.out.println(success);
    }

    @Test // only scratch, no explicit test
    public void doHtmlPou() throws FileNotFoundException {

        Accessor jsoupAcc = new JsoupAcc();
        Document doc = jsoupAcc.parse(new FileInputStream("C:\\Users\\andreas-fm\\Desktop\\source.html"));

        Apath ap = new Apath(new Config(jsoupAcc));

        String expr = "html\n" + "    // Match all 'select' tags filtered by 'id'\n" +
              "    ..article ?(@class=='account').dl. \n" +
              "    (dd[:0:].kbd.text()$username and  dd[:1:].kbd.text()$pass)\n";
        boolean success = ap.doMatch(doc, //
              expr,
                //
                ctx -> {
//                    System.out.println(ctx.varMap());
                    System.out.println(ctx.v("username") + ", " + ctx.v("pass"));
                });

        System.out.println(success);
    }

    @Test // only scratch, no explicit test
    public void doHVarIssue() throws FileNotFoundException {

        Accessor jsoupAcc = new JsoupAcc();
        Document doc = jsoupAcc.parse(
              "<root>\n" +
              "   <x a=\"1\"><y>1</y><z>1</z></x>\n" +
              "   <x a=\"2\"><y>2</y></x>\n" +
              "   <x a=\"3\"><y>3</y></x>\n" +
              "</root>\n");

        Apath ap = new Apath(new Config(jsoupAcc));

//        String expr = "html.body.root.x?(@a == '2').text()$x";
        String expr = "html.body.root.(x[:1:].y.text()$x and true)";
        boolean success = ap.doMatch(doc, //
              expr,
                //
                ctx -> {

                    System.out.println(ctx.varMap());
                });

        System.out.println(success);
    }

    @Test // only scratch, no explicit test
    public void doXml() throws FileNotFoundException {

        Accessor xmlAcc = new XmlAcc(true);
        Object doc = xmlAcc.parse(new FileInputStream("C:\\Users\\andreas-fm\\Desktop\\xml-1.txt"));

        Apath ap = new Apath(new Config(xmlAcc));

        String expr = "doc.root..books?(@store == 'A').book[:1:]?(author.text()$x and title.text()$xy)";
        boolean success = ap.doMatch(doc, //
              expr,
                //
                ctx -> {

                    System.out.println(ctx.varMap());
                });

        System.out.println(success);
    }

}
