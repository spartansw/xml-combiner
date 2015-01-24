package org.atteo.xmlcombiner;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.google.common.base.Function;

class AllAttributesKeysGenerator implements Function<Element, Map<String, String>> {

	@Override
	public Map<String, String> apply(Element input) {
        Map<String, String> attrMap = new HashMap<>();
        NamedNodeMap attributes = input.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            attrMap.put(attr.getNodeName(), attr.getNodeValue());
        }

        return attrMap;
	}

}