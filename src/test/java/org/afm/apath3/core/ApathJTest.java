package org.afm.apath3.core;

import net.minidev.json.JSONObject;
import org.afm.apath3.accessors.Accessor;
import org.afm.apath3.accessors.JsonSmartAcc;
import org.afm.apath3.accessors.JsoupAcc;
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
        boolean success = ap.doMatch(jo, "root.store.book.*?(price $p and title $t)", match -> {
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

}
