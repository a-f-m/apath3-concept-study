package org.afm.apath3.core;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.afm.apath3.accessors.Accessor;
import org.afm.apath3.accessors.JJsonSmartAcc;
import org.afm.apath3.accessors.JsonSmartAcc;
import org.afm.apath3.accessors.JsoupAcc;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;

import static org.afm.apath3.core.Apath.first;


public class ApathJTest {


    @Test // only scratch, no explicit test
    public void doJson() throws FileNotFoundException {

        // create the accessor to the underlying structure, here JsonSmart
        Accessor jsonSmartAcc = new JsonSmartAcc();
        // parse the input file
        JSONObject jo = jsonSmartAcc.parse(new FileInputStream("src/test/resources/samples/books.json"));

        // create apath
        Apath ap = new Apath(new Config(jsonSmartAcc));

        // match path on json object
        boolean success = ap.doMatch(jo, "root.store.book.*?(price $p and title $t)", ctx -> {
            // for each match
            // access variable 'p'
            System.out.println((Object) ctx.v("p"));
            // access the matched node
            System.out.println((JSONObject) ctx.curr());
        });
        System.out.println(success);

        System.out.println("---");

        // same as above but with via the standard iterator
        ap.jDoMatch(jo, "root.store.book.*?(price $p and title $t)").forEach(ctx -> {
            // ...
        });

        System.out.println("---");

        // get the first matched node
        JSONObject jo_ = ap.get(jo, "root.store.book.*?(price $p and title $t)");
        System.out.println(jo_);
        // get the first matched node or the default if no match occurred
        String s = ap.get(jo, "root.store.book.*.titlexxx", "def");
        System.out.println(s);

        // get the first binding of variable 't'
        System.out.println((String) first(ap.jDoMatch(jo, "root.store.book.*?(price $p and title $t)")).get().v("t"));
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

}
