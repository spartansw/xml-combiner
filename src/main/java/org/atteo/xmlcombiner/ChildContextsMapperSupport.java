package org.atteo.xmlcombiner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.google.common.base.Function;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

public abstract class ChildContextsMapperSupport implements ChildContextsMapper {

    public ListMultimap<Key, Context> mapChildContextsWithGenerators(Context parent,
            Function<Element, Map<String, String>>... keyGenerators) {
        List<Context> contexts = parent.groupChildContexts();

        ListMultimap<Key, Context> map = LinkedListMultimap.create();
        for (Context context : contexts) {
            Element contextElement = context.getElement();

            if (contextElement != null) {
                Map<String, String> keys = new HashMap<>();
                for (Function<Element, Map<String, String>> generator: keyGenerators) {
                	keys.putAll(generator.apply(contextElement));
                }

                Key key = new Key(contextElement.getTagName(), keys);
                map.put(key, context);
            } else {
                map.put(Key.BEFORE_END, context);
            }
        }
        return map;
    };

}
