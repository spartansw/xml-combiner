package org.atteo.xmlcombiner;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.google.common.base.Function;
import com.google.common.collect.ListMultimap;

public class KeyAttributesChildContextsMapper extends ChildContextsMapperSupport {

	@Override
	public ListMultimap<Key, Context> mapChildContexts(Context parent,
    		List<String> keyAttributeNames) {
		Function<Element, Map<String, String>> selectedAttributes = new KeyAttributesKeysGenerator(keyAttributeNames);

        return mapChildContextsWithGenerators(parent, selectedAttributes);
	};

}
