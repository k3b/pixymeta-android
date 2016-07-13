package pixy.meta.xmp;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pixy.api.IDataType;
import pixy.string.XMLUtils;

/**
 * Created by k3b on 12.07.2016.
 */
public enum XmpFieldType implements IDataType {
    GuidStringAttribute();

    @Override
    public String getName() {
        return toString();
    }

    public void remove(Document doc, XmpTag tag) {
        XMLUtils.removeAttribute(doc, tag.getXmlElementName(), tag.getAttribute());
    }


    public String getValueAsString(Document document, XmpTag tag) {
        return XMLUtils.getAttribute(document, tag.getXmlElementName(), tag.getAttribute());
    }
    /*
    public void setValue(Document document, XmpTag tag, String value){
        NodeList descriptions = document.getElementsByTagName(tag.getXmlElementName());
        int length = descriptions.getLength();
        if(length > 0) {
            Element node = (Element)descriptions.item(length - 1);
            String xmlns = getXmlnsID(tag.getAttribute());
            if (xmlns != null) {
                if (!node.hasAttribute("xmlns:" + xmlns)) {
                    node.setAttribute("xmlns:" + xmlns, XmpTag.);
                }
            }
            document.getN.getNamespaceURI()
            node.setAttribute("xmlns:xmpNote", "http://ns.adobe.com/xmp/extension/");
            node.setAttribute("xmpNote:HasExtendedXMP", guid);
        }

    }
*/

    private String getXmlnsID(String name) {
        String[] subFields = name.split(":");
        if (subFields.length > 1) return subFields[0];
        return null;
    }
}
