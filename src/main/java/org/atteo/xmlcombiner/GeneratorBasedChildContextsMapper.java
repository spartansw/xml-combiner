package org.atteo.xmlcombiner;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.google.common.base.Function;
import com.google.common.collect.ListMultimap;

public class GeneratorBasedChildContextsMapper extends ChildContextsMapperSupport {

	public GeneratorBasedChildContextsMapper(
			Function<Element, Map<String, String>>... generators) {
		super();
		this.generators = generators;
	}

	Function<Element, Map<String, String>>[] generators;

	@Override
    public ListMultimap<Key, Context> mapChildContexts(Context parent,
            List<String> keyAttributeNames) {
		return mapChildContextsWithGenerators(parent, generators);
    }

}
