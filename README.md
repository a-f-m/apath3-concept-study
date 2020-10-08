**apath** is a structure-**a**gnostic **path** language following the principles of xpath and jsonpath. It is written
in scala, minor parts are in java.

The main features are

* variables, enabling pattern matching known from term rewriting calculi – as an enhancement of classical path languages

* the ability to wrap/access arbitrary structures and made them ready for path selection. For now json, xml, and html is
supported by small [accessors](src/main/scala/org/afm/apath3/accessors/JsonSmartAcc.scala)

You can tryout the language via a [playground](http://207.154.243.28/apath-service/home/index.html).

For parsing according to the [apath grammar](parserV3.peg.txt) it uses the excellent [mouse](http://mousepeg.sourceforge.net) peg parser.

# Programmatic usage

## java

```java
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

// get the first matched node
JSONObject jo1 = ap.get(jo, "root.store.book.*?(price $p and title $t)");
System.out.println(jo1);
// get the first matched node or the default if no match occurred
String s = ap.get(jo, "root.store.book.*.titlexxx", "no title");
System.out.println(s);

// get the first binding of variable 't'
s = first(ap.jDoMatch(jo, "root.store.book.*?(price $p and title $t)")).get().v("t");
System.out.println(s);
```

With the books [input](src/test/resources/samples/books.json) it prints out

```
Sayings of the Century
{"author":"Nigel Rees","price":8.95,"onStock":true,"category":"reference","title":"Sayings of the Century"}
Sword of Honour
{"author":"Evelyn Waugh","price":12,"onStock":false,"category":"fiction","title":"Sword of Honour"}
true
{"author":"Nigel Rees","price":8.95,"onStock":true,"category":"reference","title":"Sayings of the Century"}
no title
Sayings of the Century
```

## scala

[here](src/test/scala/org/afm/apath3/core/ApathTest.scala)