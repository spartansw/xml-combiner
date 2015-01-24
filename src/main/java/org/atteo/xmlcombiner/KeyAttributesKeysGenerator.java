package org.atteo.xmlcombiner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.atteo.xmlcombiner.Context;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import com.google.common.base.Function;

class KeyAttributesKeysGenerator implements Function<Element, Map<String, String>> {

	private final List<String> keyAttributeNames;

	public KeyAttributesKeysGenerator(List<String> keyAttributeNames) {
		super();
		this.keyAttributeNames = keyAttributeNames;
	}

	@Override
	public Map<String, String> apply(Element input) {
        Map<String, String> keys = new HashMap<>();
        for (String keyAttributeName : keyAttributeNames) {
            Attr keyNode = input.getAttributeNode(keyAttributeName);
            if (keyNode != null) {
                keys.put(keyAttributeName, keyNode.getValue());
            }
        }
        {
            Attr keyNode = input.getAttributeNode(Context.ID_ATTRIBUTE_NAME);
            if (keyNode != null) {
                keys.put(Context.ID_ATTRIBUTE_NAME, keyNode.getValue());
            }
        }
        return keys;
	}

}