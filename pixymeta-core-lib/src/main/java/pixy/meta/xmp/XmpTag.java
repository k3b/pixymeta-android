package pixy.meta.xmp;

import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

import pixy.api.IFieldDefinition;

/**
 * Created by k3b on 12.07.2016.
 */
public enum XmpTag  implements IFieldDefinition {
    /** md5 checksum of extended xml. null if there is no extended xml  */
    Note_HasExtendedXMP(XmpFieldType.GuidStringAttribute, "rdf:Description", "xmpNote:HasExtendedXMP");

    private final XmpFieldType type;
    private final String element;
    private final String attribute;

    XmpTag(XmpFieldType type, String element, String attribute) {

        this.type = type;
        this.element = element;
        this.attribute = attribute;
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public XmpFieldType getDataType() {
        return this.type;
    }

    public String getXmlElementName() {
        return element;
    }

    public String getAttribute() {
        return attribute;
    }

    public void remove(Document doc) {
        getDataType().remove(doc, this);
    }

    public String getValueAsString(Document document) {
        return getDataType().getValueAsString(document, this);
    }

    private static HashMap<String,String> namespaces = new HashMap<>();
    static {
        namespaces.put("xmpNote", "http://ns.adobe.com/xmp/extension/");
    }

	public static NamespaceContext getNamespaceContext() {
		return new NamespaceContext() {
			public String getNamespaceURI(String prefix) {
				return namespaces.get(prefix);
			}

			// This method isn't necessary for XPath processing.
			public String getPrefix(String uri) {
				throw new UnsupportedOperationException();
			}

			// This method isn't necessary for XPath processing either.
			public Iterator getPrefixes(String uri) {
				throw new UnsupportedOperationException();
			}
		};
	}
	/*
	// http://stackoverflow.com/questions/6390339/how-to-query-xml-using-namespaces-in-java-with-xpath
		XPathFactory factory = XPathFactory.newInstance();
		??? factory.setNamespaceAware(true);
		
		XPath xpath = factory.newXPath();

		// there's no default implementation for NamespaceContext...seems kind of silly, no?
		xpath.setNamespaceContext(XmpTag.getNamespaceContext());

		// note that all the elements in the expression are prefixed with our namespace mapping!
		XPathExpression expr = xpath.compile("/spreadsheet:workbook/spreadsheet:sheets/spreadsheet:sheet[1]");

		// assuming you've got your XML document in a variable named doc...
		Node result = (Node) expr.evaluate(doc, XPathConstants.NODE);	
		
		
	// http://stackoverflow.com/questions/13702637/xpath-with-namespace-in-java
		String xml = "<urn:ResponseStatus version=\"1.0\" xmlns:urn=\"urn:camera-org\">\r\n" + //
				"\r\n" + //
				"<urn:requestURL>/CAMERA/Streaming/status</urn:requestURL>\r\n" + //
				"<urn:statusCode>4</urn:statusCode>\r\n" + //
				"<urn:statusString>Invalid Operation</urn:statusString>\r\n" + //
				"<urn:id>0</urn:id>\r\n" + //
				"\r\n" + //
				"</urn:ResponseStatus>";
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true); //!!! else xpath with namespaces wouldn-t work
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes()));
		XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new NamespaceContext() {
			public String getNamespaceURI(String prefix) {
				return prefix.equals("urn") ? "urn:camera-org" : null;
			}

			public Iterator<?> getPrefixes(String val) {
				return null;
			}

			public String getPrefix(String uri) {
				return null;
			}
		});
		XPathExpression expr = xpath.compile("//urn:ResponseStatus");
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		for (int i = 0; i < nodes.getLength(); i++) {
			Node currentItem = nodes.item(i);
			System.out.println("found node -> " + currentItem.getLocalName() + " (namespace: " + currentItem.getNamespaceURI() + ")");
		}		
	*/
}
