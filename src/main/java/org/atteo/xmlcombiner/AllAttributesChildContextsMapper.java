package org.atteo.xmlcombiner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

public class AllAttributesChildContextsMapper implements ChildContextsMapper {

    @Override
    public ListMultimap<Key, Context> mapChildContexts(Context parent,
            List<String> keyAttributeNames) {
        List<Context> contexts = parent.groupChildContexts();

        ListMultimap<Key, Context> map = LinkedListMultimap.create();
        for (Context context : contexts) {
            Element contextElement = context.getElement();

            if (contextElement != null) {
                Map<String, String> keys = getAttributesMap(contextElement);
                Key key = new Key(contextElement.getTagName(), keys);
                map.put(key, context);
            } else {
                map.put(Key.BEFORE_END, context);
            }
        }
        return map;
    }

    protected Map<String, String> getAttributesMap(Node node) {
        Map<String, String> attrMap = new HashMap<>();
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            attrMap.put(attr.getNodeName(), attr.getNodeValue());
        }

        return attrMap;
    }
}
