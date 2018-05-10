package org.afm.apath3.core;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.afm.apath3.accessors.Accessor;
import org.afm.apath3.accessors.JJsonSmartAcc;
import org.afm.util.Testing;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileReader;
import java.util.Arrays;

import static org.afm.apath3.core.Apath.jfirst;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

//import scala.collection.Iterable;


public class ApathJTest {

    JSONObject jo;

    //    Accessor acc = JJsonSmart.acc;
    Accessor acc = new JJsonSmartAcc();

    @Before
    public void setUp() throws Exception {

        jo = (JSONObject) JSONValue.parse(new FileReader("src/test/resources/core/books.json"));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {

        NodeIter it = //
                new Path().setArgs(Arrays.asList(new Property("root"), new Property("store"), new Property("book"),
                        new ArraySubscript(1))).eval(new Node(jo), new Context(), new Config(acc));

        if (!it.hasNext()) fail();
        String s = it.next().toString();
        assertEquals(Testing.expected(false, s, ""), s);

        Apath ap = new Apath(new Config(acc));

        ap.jDoMatch(jo, "root.store.book.*?(price $p and title $t)").forEach(ctx -> {

            System.out.println((Object) ctx.v("p"));

            System.out.println(ctx.currNode());
        });

        System.out.println("---");

        boolean success = ap.jDoMatch(jo, "root.store.book.*?(price $p and title $t)", ctx -> {

            System.out.println((Object) ctx.v("p"));

            System.out.println(ctx.currNode());
        });
        System.out.println(success);

        System.out.println("---");

        System.out.println((String) jfirst(ap.jDoMatch(jo, "root.store.book.*?(price $p and title $t)")).get().v("t"));
        System.out
                .println((JSONObject) jfirst(ap.jDoMatch(jo, "root.store.book.*?(price $p and title $t)")).get().curr());

        JSONObject jo_ = ap.jget(jo, "root.store.book.*?(price $p and title $t)");
        System.out.println(jo_);
        s = ap.jget(jo, "root.store.book.*?(price $p and titlexxx $t)", "def");
        System.out.println(s);
    }
}
