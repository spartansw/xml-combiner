package org.atteo.xmlcombiner;

import java.util.Map;

import org.w3c.dom.Element;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

public class ElementOrderKeysGenerator implements Function<Element, Map<String, String>> {

	private final static String KEY_ORDER = "ElementOrderKeysGenerator.order";

	private int order = 0;

	@Override
	public Map<String, String> apply(Element input) {
		return ImmutableMap.of(KEY_ORDER, Integer.toString(order++));
	}

}