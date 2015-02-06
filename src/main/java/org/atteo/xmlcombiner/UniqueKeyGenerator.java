package org.atteo.xmlcombiner;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.google.common.base.Function;

class UniqueKeyGenerator implements Function<Element, Map<String, String>> {

	private final static String KEY = UniqueKeyGenerator.class.getSimpleName() + ".uniqueId";
	private long id = 0;

	@Override
	public Map<String, String> apply(Element input) {
        Map<String, String> attrMap = new HashMap<>();
        attrMap.put(KEY, Long.toString(id++));

        return attrMap;
	}

}