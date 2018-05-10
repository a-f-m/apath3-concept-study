package org.afm.apath3.accessors;

import net.minidev.json.JSONAwareEx;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.writer.JsonReader;
import net.minidev.json.writer.JsonReaderI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JJsonSmartMapMapper extends JsonReaderI<JSONAwareEx> {
	
	public JJsonSmartMapMapper(){
		this(new JsonReader());
	}
	
	protected JJsonSmartMapMapper(JsonReader base) {
		super(base);
	}

	@Override
	public JsonReaderI<JSONAwareEx> startObject(String key) {
		return new JJsonSmartMapMapper(base);
	}

	@Override
	public JsonReaderI<JSONAwareEx> startArray(String key) {
		return new JJsonSmartMapMapper(base);
	}

	@Override
	public Object createObject() {
		return new LinkedHashMap<>();
	}

	@Override
	public Object createArray() {
		return new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setValue(Object current, String key, Object value) {
		((Map<String, Object>) current).put(key, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addValue(Object current, Object value) {
		((List<Object>) current).add(value);
	}

	public static JSONParser parser() {
		return new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
	}
	
	public static JJsonSmartMapMapper create() {
		return new JJsonSmartMapMapper();
		
	}
}
