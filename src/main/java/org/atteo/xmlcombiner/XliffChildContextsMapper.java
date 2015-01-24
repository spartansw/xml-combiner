package org.atteo.xmlcombiner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;

public class XliffChildContextsMapper extends ChildContextsMapperSupport {

	private final Function<Element, Map<String, String>> allAttributes = new AllAttributesKeysGenerator();
	private final Function<Element, Map<String, String>> nameOnly = new KeyAttributesKeysGenerator(ImmutableList.<String>of());

	@Override
    public ListMultimap<Key, Context> mapChildContexts(Context parent,
            List<String> keyAttributeNames) {

		CompositeGenerator compositeGenerator = new CompositeGenerator(
				ImmutableList.of(allAttributes, new ElementOrderKeysGenerator()));
		XliffGenerator xliffGenerator = new XliffGenerator(
				compositeGenerator, nameOnly);
		return mapChildContextsWithGenerators(parent, xliffGenerator);
    }

	private class XliffGenerator implements Function<Element, Map<String, String>> {

		private final Function<Element, Map<String, String>> defaultGenerator;
		private final Function<Element, Map<String, String>> transUnitGenerator;

		public XliffGenerator(
				Function<Element, Map<String, String>> defaultGenerator,
				Function<Element, Map<String, String>> transUnitGenerator) {
			super();
			this.defaultGenerator = defaultGenerator;
			this.transUnitGenerator = transUnitGenerator;
		}

		@Override
		public Map<String, String> apply(Element input) {
			if (input != null && input.getNodeName() == "trans-unit") {
				return transUnitGenerator.apply(input);
			} else {
				return defaultGenerator.apply(input);
			}
		}

	}

	public class CompositeGenerator implements Function<Element, Map<String, String>> {

		List<Function<Element, Map<String, String>>> generators;

		public CompositeGenerator(
				List<Function<Element, Map<String, String>>> generators) {
			super();
			this.generators = generators;
		}

		@Override
		public Map<String, String> apply(Element input) {
			Map<String, String> results = new HashMap<>();
			for (Function<Element, Map<String, String>> generator : generators) {
				results.putAll(generator.apply(input));
			}
			return results;
		}

	}
}
