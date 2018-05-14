

Json o = ap.get(jo, "a.b.c")

boolean b = ap.get(jo, "a.b.c", false)

ctx = ap.getC(jo, "a.b.cccc`")

aaa.bbb?(selector().rmatch('.*lala'))

aaa.*?($.rmatch('.*lala'))

aaa.*?(rmatch('.*lala'))

aaa.bbb?($`x.~'.*lala')

aaa.bbb.~(.*lala).ccc

aaa.bbb.<.*lala>.ccc

xxx.fff('sss', ^$x)

xxx.(rmatch '...' -> aa )

root.store.book[*].*
    ?(onStock and selector() $s and $s.rmatch('#.\d+'))
        .price

root.store.book[*].*
    ?(onStock and selector() $s and $s.rmatch('#.\d+'))
        .price
