package afm.mouse;

import java.util.*;
import java.util.Map.Entry;

import org.json.JSONObject;

import mouse.runtime.Phrase;

public class DefaultSemantics extends mouse.runtime.SemanticsBase {

	private Map<String, Collection<String>> memMap = new HashMap<String, Collection<String>>();
	
	private Map<String, List<?>> currTree;

	private List<?> currList;

	private String errorMsg;
	
	private boolean textNode;

	public static boolean astAsList = false;

	@FunctionalInterface
	public interface Callback {
		 void process(String rule, Object args);
	}

	Callback callback;

	public void setCallback(Callback callback) {
		this.callback = callback;
	}

	public void T() {

		extendMemMap();
		if (astAsList) currList = buildList(); else currTree = buildTerm(false);
		if (callback != null) callback.process(lhs().rule(), currTree);
//		printRule();
	}

	public void TL() {
		
		extendMemMap();
		if (astAsList) currList = buildList(); else currTree = buildTerm(true);
		if (callback != null) callback.process(lhs().rule(), currTree);
//		printRule();
	}
	
	private void extendMemMap() {
		int size = rhsSize();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < size; i++) {
			sb.append(rhs(i).text());
		}
		String ruleName = lhs().rule();
		Collection<String> x = extendMap(ruleName);
		x.add(sb.toString().trim());
	}
	
	/**
	 * Passing.
	 */
	public void P() {
		Object o = null;
		for (int i = 0; i < rhsSize(); i++) {
			Phrase p = rhs(i);
			if (p.get() != null) {
				if (o == null) {
					o = p.get();
				} else {
					throw new RuntimeException("more than one object");
				}
			}
		}
		lhs().put(o);
	}

	/**
	 * sub rule name.
	 */
	public void S() {
		String name = null;
		for (int i = 0; i < rhsSize(); i++) {
			Phrase p = rhs(i);
			if (!p.isEmpty()) {
				if (name == null) {
					name = p.rule();
				} else {
					throw new RuntimeException("more than one object");
				}
			}
		}
		lhs().put(name);
	}
	
	/**
	 * Constant.
	 */
	public void C() {
		parseConstant(false);
	}

	/**
	 * String Constant with '//' comments.
	 */
	public void CS() {
		
		parseConstant(true);
	}
	
	private void parseConstant(boolean stringLiteral) {
		
		Object o = null;
		String s = rhsText();
		int c = s.indexOf("//");
		if (!stringLiteral) {
			if (c != -1) s = s.substring(0, c).trim();
		} else {
			s = s.replaceFirst("('[^']*').*", "$1");
		}
		int l = s.length() - 1;
		try {
			o = Double.parseDouble(s);
		} catch (Exception e) {
			o = s.equalsIgnoreCase("true") ? true
					: s.equalsIgnoreCase("false") ? false
							: (s.charAt(0) == '\'' && s.charAt(l) == '\'') || (s.charAt(0) == '\"' && s.charAt(l) == '\"')
									? s.substring(1, l) : s;
		}
		if (astAsList) {
			lhs().put(Arrays.asList(lhs().rule(), o));
		} else {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put(lhs().rule(), o);
			lhs().put(map);
		}
	}
	
	private Collection<String> extendMap(String ruleName) {

		Collection<String> coll = memMap.get(ruleName);
		if (coll == null) {
			try {
				coll = new ArrayList<>();
				memMap.put(ruleName, coll);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return coll;
	}

	public void printMap() {

		for (Entry<String, Collection<String>> entry : memMap.entrySet()) {
			System.out.println(entry.getKey() + ":");
			for (String v : entry.getValue()) {
				System.out.println("\t" + v);
			}
		}
	}

	public void error() {

		// System.out.println("###" + lhs().errMsg());
		errorMsg = lhs().errMsg();
		lhs().errClear();
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	@Override
	public void init() {
		super.init();
		memMap = new HashMap<String, Collection<String>>();
	}

	private String rhsText() {
		return rhsText(0, rhsSize()).trim();
	}

	private boolean isSpace(Phrase p) {
		return p.isA("__") || p.isA("Spacing") || p.isA("NLOD");

	}

	public List buildList() {

		List<Object> l = new ArrayList<>();
		l.add(lhs().rule());

		for (int i = 0; i < rhsSize(); i++) {
			Phrase p = rhs(i);
			if (!isSpace(p) && !p.isTerm()) {
				l.add(p.get());
			}
		}
		lhs().put(l);
		return l;
	}

	public Map buildTerm(boolean asList) {

		Map map = new LinkedHashMap<>();
		List<Object> subs = new ArrayList<>();
//		Map<String, String> text = new LinkedHashMap<>();
//		text.put("_text", textNode ? rhsText() : null);
//		subs.add(text);

//		for (int i = 0; i < rhsSize(); i++) {
//			Phrase p = rhs(i);
//			if (!isSpace(p) && !p.isTerm()) {
//				subs.add(p.get());
//			}
//		}
		
		Object term = null;
		Map subsMap = new LinkedHashMap<>();
		for (int i = 0; i < rhsSize(); i++) {
			Phrase p = rhs(i);
			if (!isSpace(p) && !p.isTerm()) {
				if (asList) {
					subs.add(p.get());
				} else {
					Object subMap = p.get();
					if (subMap instanceof Map) {
						subsMap.putAll((Map<String, Object>) subMap);
					} else {
						term = subMap;
					}
				}
			}
		}
		
//		map.put(lhs().rule(), subs);
//		lhs().put(map);
		map.put(lhs().rule(), asList ? subs : (term != null ? term : subsMap));
		lhs().put(map);
		
//		System.out.println(new JSONObject(map).toString(3));
		return map;
	}
	
	public void printRule() {
		
		System.out.println("rule: " + lhs().rule());
		boolean isTerm = true;
		for (int i = 0; i < rhsSize(); i++) {
			Phrase rhs = rhs(i);
			if (!isSpace(rhs)) {
				if (!rhs.isTerm()) {
					isTerm = false;
					System.out.print("sub-rule: " + rhs.rule() + ", ");
					System.out.print("text: '" + rhs.text() + "', ");
					// System.out.print("get: " + rhs.get() + ", ");
					System.out.print("isEmpty: " + rhs.isEmpty() + ", ");
					// System.out.print("isTerm: " + rhs.isTerm() + ", ");
					System.out.println();
				}
			}
		}
		if (isTerm) {
			System.out.println(rhsText());
		}
		System.out.println("------");
	}

	public Map<String, List<?>> getCurrTree() {
		return currTree;
	}

	public List<?> getCurrList() {
		return currList;
	}

	public JSONObject asJson() {
		return new JSONObject(currTree);
		
	}
	
	public static void main(String[] args) {
		System.out.println("'xx//xx' // lala\n".replaceFirst("('[^']+').*", "$1"));
	}

}
