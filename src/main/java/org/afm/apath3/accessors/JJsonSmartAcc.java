package org.afm.apath3.accessors;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.afm.apath3.core.*;

import static org.afm.apath3.core.ApathAdt.ofType;
import static scala.compat.java8.JFunction.func;


public class JJsonSmartAcc extends Accessor {

    public JJsonSmartAcc() {

        setIsArrayFunc(func(obj -> obj instanceof JSONArray));
        setIsPropertyMapFunc(func(obj -> obj instanceof JSONObject));
        setSelectorFunc(func((Node node, Expr e) -> { //>
            Object o = node.obj();
            if (ofType(e, Property.class)) {
                if (o instanceof JSONObject) {
                    String name = ((Property) e).name();
                    return iterO(((JSONObject) o).get(name), name);
                } else {
                    return new NilIter();
                }
            }
            if (ofType(e, ArraySubscript.class)) {
                if (o instanceof JSONArray) {
                    int idx = ((ArraySubscript) e).idx();
                    return iterO(((JSONArray) o).get(idx), Integer.toString(idx));
                } else {
                    return new NilIter();
                }
            }
            if (ofType(e, Children.class)) {
                if (o instanceof JSONObject) {
                    return iter(((JSONObject) o).entrySet().iterator());
                } else if (o instanceof JSONArray) {
                    return iter(((JSONArray) o).iterator());
                }
                return new DelegatedIter();
            } else {
                return new DelegatedIter();
            }

        }));
    }
}
