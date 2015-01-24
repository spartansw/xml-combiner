package org.atteo.xmlcombiner;

import java.util.List;

import com.google.common.collect.ListMultimap;

public class OrderChildContextsMapper extends ChildContextsMapperSupport {

	@Override
	public ListMultimap<Key, Context> mapChildContexts(Context parent,
			List<String> keyAttributeNames) {

		ElementOrderKeysGenerator generator = new ElementOrderKeysGenerator();
		AllAttributesKeysGenerator allAttributes = new AllAttributesKeysGenerator();
		return mapChildContextsWithGenerators(parent, generator, allAttributes);
	}
}
