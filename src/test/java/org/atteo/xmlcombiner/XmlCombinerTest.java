/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atteo.xmlcombiner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static org.assertj.core.api.Assertions.assertThat;

import org.custommonkey.xmlunit.Diff;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLIdentical;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLNotEqual;

import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class XmlCombinerTest {
	@Test
	public void identity() throws SAXException, IOException, ParserConfigurationException,
			TransformerConfigurationException, TransformerException {
		String content = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "    </service>\n"
				+ "</config>";
		assertXMLIdentical(new Diff(content, combineWithIdKey(content, content)), true);
	}

	@Test
	public void mergeChildren() throws SAXException, IOException, ParserConfigurationException,
			TransformerConfigurationException, TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "        <parameter>other value</parameter>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "        <parameter>other value</parameter>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "</config>";
		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, dominant)), true);
	}

	@Test
	public void appendChildren() throws SAXException, IOException, ParserConfigurationException,
			TransformerConfigurationException, TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service id='1' combine.children='append'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "        <parameter>other value</parameter>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "        <parameter>other value</parameter>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "</config>";
		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, dominant)), true);
	}

	@Test
	public void commentPropagation() throws SAXException, IOException, ParserConfigurationException,
			TransformerConfigurationException, TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <!-- Service 1 -->\n"
				+ "    <service id='1'>\n"
				+ "        <!-- This comment will be removed -->\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <!-- Service 1 with different configuration -->\n"
				+ "    <service id='1'>\n"
				+ "        <!-- Changed value -->\n"
				+ "        <parameter>other value</parameter>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "    <!-- End of configuration file -->\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <!-- Service 1 with different configuration -->\n"
				+ "    <service id='1'>\n"
				+ "        <!-- Changed value -->\n"
				+ "        <parameter>other value</parameter>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "    <!-- End of configuration file -->\n"
				+ "</config>";
		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, dominant)), true);
	}

	@Test
	public void attributes() throws SAXException, IOException, ParserConfigurationException,
			TransformerConfigurationException, TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service id='1' parameter='parameter' parameter2='parameter2'/>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service id='1' parameter='other value' parameter3='parameter3'/>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service id='1' parameter='other value' parameter2='parameter2' parameter3='parameter3'/>\n"
				+ "</config>";
		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, dominant)), true);
	}

	@Test
	public void remove() throws SAXException, IOException, ParserConfigurationException,
			TransformerConfigurationException, TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "    </service>\n"
				+ "    <service id='2' combine.self='remove'/>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service id='1' combine.self='remove'/>\n"
				+ "    <service id='2'/>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service id='2'/>\n"
				+ "</config>";

		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, dominant)), true);
	}

	@Test
	public void override() throws SAXException, IOException, ParserConfigurationException,
			TransformerConfigurationException, TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service id='1' combine.self='override'>\n"
				+ "        <parameter>other value</parameter>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "        <parameter>other value</parameter>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "</config>";
		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, dominant)), true);
	}

	@Test
	public void multipleChildren() throws SAXException, IOException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter9>parameter2</parameter9>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "    </service>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service id='1'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter9>parameter2</parameter9>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "</config>";
		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, dominant)), true);
	}

	@Test
	public void defaults() throws SAXException, IOException, ParserConfigurationException, TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service id='1' combine.self='DEFAULTS'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter9>parameter2</parameter9>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "    <service id='2' combine.self='DEFAULTS'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service id='2'>\n"
				+ "    </service>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service id='2'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "    </service>\n"
				+ "</config>";
		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, dominant)), true);
	}

	@Test
	public void overridable() throws SAXException, IOException, ParserConfigurationException, TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service id='id1' combine.self='overridable'>\n"
				+ "        <test/>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service id='id1'>\n"
				+ "        <test/>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant2 = "\n"
				+ "<config>\n"
				+ "    <service id='id1'/>\n"
				+ "</config>";
		String dominant3 = "\n"
				+ "<config>\n"
				+ "    <service id='id2'/>\n"
				+ "</config>";
		String result3 = "\n"
				+ "<config>\n"
				+ "    <service id='id1'>\n"
				+ "        <test/>\n"
				+ "    </service>\n"
				+ "    <service id='id2'/>\n"
				+ "</config>";

		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, dominant)), true);
		assertXMLIdentical(new Diff(dominant2, combineWithIdKey(recessive, dominant2)), true);
		assertXMLIdentical(new Diff(result3, combineWithIdKey(recessive, dominant3)), true);
		assertXMLIdentical(new Diff(result3, combineWithIdKey(recessive, dominant, dominant3)), true);
	}

	@Test
	public void overridableByTag() throws SAXException, IOException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service id='id1' combine.self='overridable_by_tag'>\n"
				+ "        <test/>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service id='id1'>\n"
				+ "        <test/>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant2 = "\n"
				+ "<config>\n"
				+ "    <service id='id1'/>\n"
				+ "</config>";
		String dominant3 = "\n"
				+ "<config>\n"
				+ "    <service id='id2'/>\n"
				+ "</config>";

		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, dominant)), true);
		assertXMLIdentical(new Diff(dominant2, combineWithIdKey(recessive, dominant2)), true);
		assertXMLIdentical(new Diff(dominant3, combineWithIdKey(recessive, dominant3)), true);
	}

	@Test
	public void subnodes() throws SAXException, IOException, ParserConfigurationException,
			TransformerConfigurationException, TransformerException {
		String recessive = "\n"
				+ "<outer>\n"
				+ "  <inner>\n"
				+ "    content\n"
				+ "  </inner>\n"
				+ "  <inner2>\n"
				+ "    content2\n"
				+ "  </inner2>\n"
				+ "</outer>";
		String dominant = "\n"
				+ "<outer>\n"
				+ "  <inner>\n"
				+ "    content3\n"
				+ "  </inner>\n"
				+ "</outer>";
		String result = "\n"
				+ "<outer>\n"
				+ "  <inner>\n"
				+ "    content3\n"
				+ "  </inner>\n"
				+ "  <inner2>\n"
				+ "    content2\n"
				+ "  </inner2>\n"
				+ "</outer>";
		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, dominant)), true);

		String dominant2 = "\n"
				+ "<outer combine.children='APPEND'>\n"
				+ "  <inner>\n"
				+ "    content3\n"
				+ "  </inner>\n"
				+ "</outer>";
		String result2 = "\n"
				+ "<outer>\n"
				+ "  <inner>\n"
				+ "    content\n"
				+ "  </inner>\n"
				+ "  <inner2>\n"
				+ "    content2\n"
				+ "  </inner2>\n"
				+ "  <inner>\n"
				+ "    content3\n"
				+ "  </inner>\n"
				+ "</outer>";
		assertXMLIdentical(new Diff(result2, combineWithIdKey(recessive, dominant2)), true);

		String dominant3 = "\n"
				+ "<outer combine.self='override'>\n"
				+ "  <inner>\n"
				+ "    content3\n"
				+ "  </inner>\n"
				+ "</outer>";
		String result3 = "\n"
				+ "<outer>\n"
				+ "  <inner>\n"
				+ "    content3\n"
				+ "  </inner>\n"
				+ "</outer>";

		assertXMLIdentical(new Diff(result3, combineWithIdKey(recessive, dominant3)), true);
	}

	@Test
	public void threeDocuments() throws SAXException, IOException, ParserConfigurationException,
			TransformerConfigurationException, TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service id='1' combine.self='DEFAULTS'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "    </service>\n"
				+ "    <service id='2' combine.self='DEFAULTS'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "    </service>\n"
				+ "    <service id='3'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "    </service>\n"
				+ "</config>";
		String middle = "\n"
				+ "<config>\n"
				+ "    <service id='1' combine.self='DEFAULTS'>\n"
				+ "        <parameter3>parameter3</parameter3>\n"
				+ "    </service>\n"
				+ "    <service id='2' combine.self='DEFAULTS'>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "    </service>\n"
				+ "    <service id='3' combine.self='DEFAULTS'>\n"
				+ "        <parameter2>parameter</parameter2>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service id='2'>\n"
				+ "    </service>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service id='2'>\n"
				+ "        <parameter>parameter</parameter>\n"
				+ "        <parameter2>parameter2</parameter2>\n"
				+ "    </service>\n"
				+ "</config>";
		assertXMLIdentical(new Diff(result, combineWithIdKey(recessive, middle, dominant)), true);
	}

	@Test
	public void shouldWorkWithCustomKeys() throws IOException, SAXException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service name='a'>\n"
				+ "        <parameter>old value2</parameter>\n"
				+ "    </service>\n"
				+ "    <service name='b'>\n"
				+ "        <parameter>old value</parameter>\n"
				+ "    </service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service name='b'>\n"
				+ "        <parameter>new value</parameter>\n"
				+ "    </service>\n"
				+ "    <service name='a'>\n"
				+ "        <parameter>new value2</parameter>\n"
				+ "    </service>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service name='a'>\n"
				+ "        <parameter>new value2</parameter>\n"
				+ "    </service>\n"
				+ "    <service name='b'>\n"
				+ "        <parameter>new value</parameter>\n"
				+ "    </service>\n"
				+ "</config>";

		assertXMLNotEqual(result, combineWithIdKey(recessive, dominant));
		assertXMLNotEqual(result, combineWithKey("n", recessive, dominant));
		assertXMLIdentical(new Diff(result, combineWithKey("name", recessive, dominant)), true);
	}

	@Test
	public void shouldWorkWithCustomIdAttribute2() throws IOException, SAXException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <nested>\n"
				+ "        <service name='a'>\n"
				+ "            <parameter>old value2</parameter>\n"
				+ "        </service>\n"
				+ "    </nested>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <nested>\n"
				+ "        <service name='a'>\n"
				+ "            <parameter>new value</parameter>\n"
				+ "        </service>\n"
				+ "    </nested>\n"
				+ "</config>";
		String result = dominant;

		assertXMLIdentical(new Diff(result, combineWithKey("name", recessive, dominant)), true);
	}

	@Test
	public void shouldSupportManyCustomKeys() throws IOException, SAXException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <nested>\n"
				+ "        <service name='a'>\n"
				+ "            <parameter>old value2</parameter>\n"
				+ "        </service>\n"
				+ "        <service name='b' id='2'>\n"
				+ "            <parameter>old value2</parameter>\n"
				+ "        </service>\n"
				+ "    </nested>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <nested>\n"
				+ "        <service name='a' id='2'>\n"
				+ "            <parameter>new value</parameter>\n"
				+ "        </service>\n"
				+ "        <service name='b' id='2'>\n"
				+ "            <parameter>new value</parameter>\n"
				+ "        </service>\n"
				+ "    </nested>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <nested>\n"
				+ "        <service name='a'>\n"
				+ "            <parameter>old value2</parameter>\n"
				+ "        </service>\n"
				+ "        <service name='b' id='2'>\n"
				+ "            <parameter>new value</parameter>\n"
				+ "        </service>\n"
				+ "        <service name='a' id='2'>\n"
				+ "            <parameter>new value</parameter>\n"
				+ "        </service>\n"
				+ "    </nested>\n"
				+ "</config>";

		assertXMLIdentical(new Diff(result, combineWithKeys(Lists.newArrayList("name", "id"),
				recessive, dominant)), true);
	}

	@Test
	public void shouldAllowToSpecifyKeys() throws IOException, SAXException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service id='1'/>\n"
				+ "    <service id='2'/>\n"
				+ "    <nested combine.keys='id'>\n"
				+ "        <service id='1'/>\n"
				+ "        <service id='2'/>\n"
				+ "        <nested>\n"
				+ "            <service id='1'/>\n"
				+ "            <service id='2'/>\n"
				+ "        </nested>\n"
				+ "    </nested>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service id='1'/>\n"
				+ "    <service id='2'/>\n"
				+ "    <nested>\n"
				+ "        <service id='1'/>\n"
				+ "        <service id='2'/>\n"
				+ "        <nested combine.keys='name'>\n"
				+ "            <service id='1'/>\n"
				+ "            <service id='2'/>\n"
				+ "        </nested>\n"
				+ "    </nested>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service id='1'/>\n"
				+ "    <service id='2'/>\n"
				+ "    <nested>\n"
				+ "        <service id='1'/>\n"
				+ "        <service id='2'/>\n"
				+ "        <nested>\n"
				+ "            <service id='1'/>\n"
				+ "            <service id='2'/>\n"
				+ "            <service id='1'/>\n"
				+ "            <service id='2'/>\n"
				+ "        </nested>\n"
				+ "    </nested>\n"
				+ "    <service id='1'/>\n"
				+ "    <service id='2'/>\n"
				+ "</config>";

		assertXMLIdentical(new Diff(result, combineWithKey("", recessive, dominant)), true);
	}

	@Test
	public void shouldAllowToSpecifyArtificialKey() throws IOException, SAXException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service combine.id='1' name='a'/>\n"
				+ "    <service combine.id='2' name='b'/>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service combine.id='1' name='c'/>\n"
				+ "    <service combine.id='3' name='d'/>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service name='c'/>\n"
				+ "    <service name='b'/>\n"
				+ "    <service name='d'/>\n"
				+ "</config>";

		//System.out.println(combineWithKey("", recessive, dominant));
		assertXMLIdentical(new Diff(result, combineWithKey("", recessive, dominant)), true);
	}

	@Test
	public void shouldSupportFilters() throws SAXException, IOException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service combine.id='1' value='1'/>\n"
				+ "    <service combine.id='2' value='2'/>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service combine.id='1' value='10'/>\n"
				+ "    <service combine.id='3' value='20'/>\n"
				+ "</config>";
		String result = "\n"
				+ "<config processed='true'>\n"
				+ "    <service value='11' processed='true'/>\n"
				+ "    <service value='2' processed='true'/>\n"
				+ "    <service value='20' processed='true'/>\n"
				+ "</config>";

		XmlCombiner.Filter filter = new XmlCombiner.Filter() {
			@Override
			public void postProcess(Element recessive, Element dominant, Element result) {
				result.setAttribute("processed", "true");
				if (recessive == null || dominant == null) {
					return;
				}
				Attr recessiveNode = recessive.getAttributeNode("value");
				Attr dominantNode = dominant.getAttributeNode("value");
				if (recessiveNode == null || dominantNode == null) {
					return;
				}

				int recessiveValue = Integer.parseInt(recessiveNode.getValue());
				int dominantValue = Integer.parseInt(dominantNode.getValue());

				result.setAttribute("value", Integer.toString(recessiveValue + dominantValue));
			}
		};
		//System.out.println(combineWithKeysAndFilter(Lists.<String>newArrayList(), filter, recessive, dominant));
		assertXMLIdentical(new Diff(result, combineWithKeysAndFilter(Lists.<String>newArrayList(), filter, recessive,
				dominant)), true);
	}

	@Test
	public void shouldAllowToUseAllAttributesAsKeys() throws IOException, SAXException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service name='a'><one/></service>\n"
				+ "    <service name='b'><one/></service>\n"
				+ "    <service name='c'><one/></service>\n"
				+ "    <service name='c'><one/></service>\n"
				+ "    <service name='d'><one/></service>\n"
				+ "    <service name='e'><one/></service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service name='b'><two/></service>\n"
				+ "    <service name='a'><two/></service>\n"
				+ "    <service name='c'><two/></service>\n"
				+ "    <service name='d'><two/></service>\n"
				+ "    <service name='d'><two/></service>\n"
				+ "    <service name='f'><two/></service>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
                + "    <service name='a'><one/><two/></service>\n"
                + "    <service name='b'><one/><two/></service>\n"
				+ "    <service name='c'><one/></service>\n"
				+ "    <service name='c'><one/></service>\n"
				+ "    <service name='d'><one/></service>\n"
				+ "    <service name='e'><one/></service>\n"
				+ "    <service name='c'><two/></service>\n"
				+ "    <service name='d'><two/></service>\n"
				+ "    <service name='d'><two/></service>\n"
				+ "    <service name='f'><two/></service>\n"
				+ "</config>";

		ChildContextsMapper mapper = new GeneratorBasedChildContextsMapper(new AllAttributesKeysGenerator());
		final String actual = combineWithMapper(mapper, recessive, dominant);
		//System.out.println(actual);
		assertXMLIdentical(new Diff(result, actual), true);
	}

	@Test
	public void shouldAllowOnlyCombineIdAsKeys() throws IOException, SAXException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service name='a'><one/></service>\n"
				+ "    <service name='b'><one/></service>\n"
				+ "    <service name='c'><one/></service>\n"
				+ "    <service name='c' combine.id='1'><abc/></service>\n"
				+ "    <service name='d'><one/></service>\n"
				+ "    <service name='e'><one/></service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service name='b'><two/></service>\n"
				+ "    <service name='a'><two/></service>\n"
				+ "    <service name='c'><two/></service>\n"
				+ "    <service name='d' combine.id='1'><xyz/></service>\n"
				+ "    <service name='d'><two/></service>\n"
				+ "    <service name='f'><two/></service>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service name='a'><one/></service>\n"
				+ "    <service name='b'><one/></service>\n"
				+ "    <service name='c'><one/></service>\n"
				+ "    <service name='d'><abc/><xyz/></service>\n"
				+ "    <service name='d'><one/></service>\n"
				+ "    <service name='e'><one/></service>\n"
				+ "    <service name='b'><two/></service>\n"
				+ "    <service name='a'><two/></service>\n"
				+ "    <service name='c'><two/></service>\n"
				+ "    <service name='d'><two/></service>\n"
				+ "    <service name='f'><two/></service>\n"
				+ "</config>";

		ChildContextsMapper mapper = new GeneratorBasedChildContextsMapper(
				new KeyAttributesKeysGenerator(ImmutableList.<String>of()));
		final String actual = combineWithMapper(mapper, recessive, dominant);
		//System.out.println(actual);
		assertXMLIdentical(new Diff(result, actual), true);
	}

	@Test
	public void shouldCombineInOrder() throws IOException, SAXException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<config>\n"
				+ "    <service><a1/></service>\n"
				+ "    <service><b1/></service>\n"
				+ "    <service id='1'><c1/></service>\n"
				+ "    <service id='2'><d1/></service>\n"
				+ "</config>";
		String dominant = "\n"
				+ "<config>\n"
				+ "    <service><a2/></service>\n"
				+ "    <service><b2/></service>\n"
				+ "    <service id='2'><d2/></service>\n"
				+ "    <service id='1'><c2/></service>\n"
				+ "</config>";
		String result = "\n"
				+ "<config>\n"
				+ "    <service><a1/><a2/></service>\n"
				+ "    <service><b1/><b2/></service>\n"
				+ "    <service id='1'><c1/></service>\n"
				+ "    <service id='2'><d1/></service>\n"
				+ "    <service id='2'><d2/></service>\n"
				+ "    <service id='1'><c2/></service>\n"
				+ "</config>";

		ChildContextsMapper mapper = new OrderChildContextsMapper();
		String actual = combineWithMapper(mapper, recessive, dominant);
		//System.out.println(actual);
		assertXMLIdentical(new Diff(result, actual), true);
	}

	@Test
	public void shouldCombineXliff() throws IOException, SAXException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<xliff>\n"
				+ "    <header>foo</header>\n"
				+ "    <header>bar</header>\n"
				+ "    <file id='1'>\n"
				+ "         <trans-unit id='2'>1</trans-unit>\n"
				+ "         <trans-unit id='2'>2</trans-unit>\n"
				+ "         <trans-unit>3</trans-unit>\n"
				+ "    </file>\n"
				+ "    <file id='2'>\n"
				+ "    </file>\n"
				+ "</xliff>";
		String dominant = "\n"
				+ "<xliff>\n"
				+ "    <header>foo</header>\n"
				+ "    <header>bar</header>\n"
				+ "    <file id='1'>\n"
				+ "         <trans-unit id='2'>4</trans-unit>\n"
				+ "    </file>\n"
				+ "    <file id='2'>\n"
				+ "         <trans-unit>5</trans-unit>\n"
				+ "         <trans-unit id='3'>6</trans-unit>\n"
				+ "    </file>\n"
				+ "</xliff>";
		String result = "\n"
				+ "<xliff>\n"
				+ "    <header>foo</header>\n"
				+ "    <header>bar</header>\n"
				+ "    <file id='1'>\n"
				+ "         <trans-unit id='2'>1</trans-unit>\n"
				+ "         <trans-unit id='2'>2</trans-unit>\n"
				+ "         <trans-unit>3</trans-unit>\n"
				+ "         <trans-unit id='2'>4</trans-unit>\n"
				+ "    </file>\n"
				+ "    <file id='2'>\n"
				+ "         <trans-unit>5</trans-unit>\n"
				+ "         <trans-unit id='3'>6</trans-unit>\n"
				+ "    </file>\n"
				+ "</xliff>";

		ChildContextsMapper mapper = new XliffChildContextsMapper();
		String actual = combineWithMapper(mapper, recessive, dominant);
		//System.out.println(actual);
		assertXMLIdentical(new Diff(result, actual), true);
	}

	@Test
	public void shouldCombineMultipartXliff() throws IOException, SAXException, ParserConfigurationException,
			TransformerException {
		String part0 = "\n"
				+ "<xliff>\n"
				+ "    <header>foo</header>\n"
				+ "    <header>bar</header>\n"
				+ "    <file id='1'><body>\n"
				+ "         <trans-unit id='2'>1</trans-unit>\n"
				+ "         <trans-unit>2</trans-unit>\n"
				+ "         <trans-unit>3</trans-unit>\n"
				+ "    </body></file>\n"
				+ "    <file id='2'><body>\n"
				+ "    </body></file>\n"
				+ "</xliff>";
		String part1 = "\n"
				+ "<xliff>\n"
				+ "    <header>foo</header>\n"
				+ "    <header>bar</header>\n"
				+ "    <file id='1'><body>\n"
				+ "         <trans-unit id='2'>4</trans-unit>\n"
				+ "    </body></file>\n"
				+ "    <file id='2'><body>\n"
				+ "         <trans-unit>5</trans-unit>\n"
				+ "         <trans-unit id='3'>6</trans-unit>\n"
				+ "    </body></file>\n"
				+ "</xliff>";
		String part2 = "\n"
				+ "<xliff>\n"
				+ "    <header>foo</header>\n"
				+ "    <header>bar</header>\n"
				+ "    <file id='1'><body>\n"
				+ "         <trans-unit id='2'>2</trans-unit>\n"
				+ "    </body></file>\n"
				+ "    <file id='2'><body>\n"
				+ "         <trans-unit>7</trans-unit>\n"
				+ "         <trans-unit id='3'>8</trans-unit>\n"
				+ "         <trans-unit>9</trans-unit>\n"
				+ "         <trans-unit id='3'>10</trans-unit>\n"
				+ "         <trans-unit>11</trans-unit>\n"
				+ "         <trans-unit id='2'>12</trans-unit>\n"
				+ "    </body></file>\n"
				+ "</xliff>";
		String result = "\n"
				+ "<xliff>\n"
				+ "    <header>foo</header>\n"
				+ "    <header>bar</header>\n"
				+ "    <file id='1'><body>\n"
				+ "         <trans-unit id='2'>1</trans-unit>\n"
				+ "         <trans-unit>2</trans-unit>\n"
				+ "         <trans-unit>3</trans-unit>\n"
				+ "         <trans-unit id='2'>4</trans-unit>\n"
				+ "         <trans-unit id='2'>2</trans-unit>\n"
				+ "    </body></file>\n"
				+ "    <file id='2'><body>\n"
				+ "         <trans-unit>5</trans-unit>\n"
				+ "         <trans-unit id='3'>6</trans-unit>\n"
				+ "         <trans-unit>7</trans-unit>\n"
				+ "         <trans-unit id='3'>8</trans-unit>\n"
				+ "         <trans-unit>9</trans-unit>\n"
				+ "         <trans-unit id='3'>10</trans-unit>\n"
				+ "         <trans-unit>11</trans-unit>\n"
				+ "         <trans-unit id='2'>12</trans-unit>\n"
				+ "    </body></file>\n"
				+ "</xliff>";

		ChildContextsMapper mapper = new XliffChildContextsMapper();
		String actual = combineWithMapper(mapper, part0, part1, part2);
		//System.out.println(actual);
		assertXMLIdentical(new Diff(result, actual), true);
	}

	@Test
	public void shouldCombineSingleSegmentXliff() throws IOException, SAXException, ParserConfigurationException,
			TransformerException {
		String recessive = "\n"
				+ "<xliff>\n"
				+ "    <header>foo</header>\n"
				+ "    <header>bar</header>\n"
				+ "    <file id='1'>\n"
				+ "         <trans-unit>1</trans-unit>\n"
				+ "    </file>\n"
				+ "</xliff>";
		String dominant = "\n"
				+ "<xliff>\n"
				+ "    <header>foo</header>\n"
				+ "    <header>bar</header>\n"
				+ "    <file id='1'>\n"
				+ "         <trans-unit>2</trans-unit>\n"
				+ "    </file>\n"
				+ "</xliff>";
		String result = "\n"
				+ "<xliff>\n"
				+ "    <header>foo</header>\n"
				+ "    <header>bar</header>\n"
				+ "    <file id='1'>\n"
				+ "         <trans-unit>1</trans-unit>\n"
				+ "         <trans-unit>2</trans-unit>\n"
				+ "    </file>\n"
				+ "</xliff>";

		ChildContextsMapper mapper = new XliffChildContextsMapper();
		String actual = combineWithMapper(mapper, recessive, dominant);
		//System.out.println(actual);
		assertXMLIdentical(new Diff(result, actual), true);
	}


	@Test
	public void shouldSupportReadingAndStoringFiles() throws IOException, ParserConfigurationException, SAXException,
			TransformerException {
		// given
		Path input = Paths.get("target/test.in");
		Path output = Paths.get("target/test.out");
		Files.write("<config/>", input.toFile(), StandardCharsets.UTF_8);

		// when
		XmlCombiner combiner = new XmlCombiner();
		combiner.combine(input);
		combiner.buildDocument(output);
		List<String> lines = Files.readLines(output.toFile(), StandardCharsets.UTF_8);

		// then
		assertThat(lines).hasSize(1);
		assertThat(lines.iterator().next()).contains("<config/>");
	}

	private static String combineWithIdKey(String... inputs) throws IOException,
			ParserConfigurationException, SAXException, TransformerConfigurationException,
			TransformerException {
		return combineWithKey("id", inputs);
	}

	private static String combineWithKey(String keyAttributeName, String... inputs) throws IOException,
			ParserConfigurationException, SAXException, TransformerConfigurationException,
			TransformerException {
		return combineWithKeys(Lists.newArrayList(keyAttributeName), inputs);
	}

	private static String combineWithMapper(ChildContextsMapper mapper, String... inputs) throws IOException,
			ParserConfigurationException, SAXException, TransformerConfigurationException,
			TransformerException {
		XmlCombiner combiner = new XmlCombiner();
		combiner.setChildContextMapper(mapper);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		for (String input : inputs) {
			Document document = builder.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
			combiner.combine(document);
		}
		Document result = combiner.buildDocument();

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(result), new StreamResult(writer));
		return writer.toString();
	}

	private static String combineWithKeys(List<String> keyAttributeNames, String... inputs) throws IOException,
			ParserConfigurationException, SAXException, TransformerConfigurationException,
			TransformerException {
		XmlCombiner combiner = new XmlCombiner(keyAttributeNames);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		for (String input : inputs) {
			Document document = builder.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
			combiner.combine(document);
		}
		Document result = combiner.buildDocument();

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(result), new StreamResult(writer));
		return writer.toString();
	}

	private static String combineWithKeysAndFilter(List<String> keyAttributeNames, XmlCombiner.Filter filter,
			String... inputs) throws IOException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		XmlCombiner combiner = new XmlCombiner(keyAttributeNames);
		combiner.setFilter(filter);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		for (String input : inputs) {
			Document document = builder.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
			combiner.combine(document);
		}
		Document result = combiner.buildDocument();

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(result), new StreamResult(writer));
		return writer.toString();
	}
}
