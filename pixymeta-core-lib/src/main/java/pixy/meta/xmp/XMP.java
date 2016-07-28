/**
 * Copyright (c) 2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * XMP.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    06Apr2016  Moved to new package
 * WY    03Jul2015  Added override method getData()
 * WY    13Mar2015  Initial creation
 */

package pixy.meta.xmp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pixy.api.DefaultApiImpl;
import pixy.api.IDirectory;
import pixy.api.IFieldValue;
import pixy.meta.MetadataBase;
import pixy.meta.MetadataType;
import pixy.meta.iptc.IPTCDataSet;
import pixy.string.StringUtils;
import pixy.string.XMLUtils;

public abstract class XMP extends MetadataBase {
	// Obtain a logger instance
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(XMP.class);

	// Fields
	// data translated to xml
	private Document xmpDocument;

	// used while development to find errors
	protected final static boolean extendedXmpLogging = false;

	public XMP(byte[] data) {
		super(MetadataType.XMP, data);
	}
	
	public XMP(String xmpDataAsXmlString) {
		super(MetadataType.XMP, null);
		set(xmpDataAsXmlString);
	}
	
	public XMP(String xmpDataAsXmlString, String extendedXmp) {
		this(xmpDataAsXmlString);
		if(extendedXmp != null) { // We have ExtendedXMP
			try {
				merge(XMLUtils.serializeToByteArray(XMLUtils.createXML(extendedXmp)));
			} catch (IOException e) {				
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setData(byte[] data) {
		if (data != null) {
			this.xmpDocument = XMLUtils.createXML(data);
			isDataRead = true;
		}
		super.setData(null);
	}

	@Override
	public byte[] getData() {
		if (this.xmpDocument != null) {
			try {
				return XMLUtils.serializeToByteArray(this.xmpDocument);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * @return directories that belong to this MetaData.
	 * */
	@Override
	public List<IDirectory> getMetaData() {
		ArrayList<IDirectory> result = new ArrayList<IDirectory>();
		try {
			if (xmpDocument != null) {
				String xml = XMLUtils.serializeToStringLS(xmpDocument);
				IDirectory dir = DefaultApiImpl.createDirecotry("xml", XmpTag.XmlRaw, xml);
				result.add(dir);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public Document getXmpDocument() {
		return xmpDocument;
	}


	public void set(String xmpDataAsXmlString) {
		if(xmpDataAsXmlString == null) throw new IllegalArgumentException("Input XMP string is null");
		this.xmpDocument = XMLUtils.createXML(xmpDataAsXmlString);
		isDataRead = true;
	}

	@Override
	public void read() throws IOException {
		// xml is internal format. no processing is neccessary
	}

	@Override
	public void merge(byte[] data) {
		if (data != null) {
			if (extendedXmpLogging) {
				String dbg = new String(data);
				LOGGER.debug(this.getClass().getSimpleName() + ".merge: " + dbg);
			}
			try {
				Document rootDoc = getXmpDocument();
				Document extendedXmpDocument = XMLUtils.createXML(data);
				if (extendedXmpDocument == null) {
					String dbg = new String(data);
					LOGGER.error(this.getClass().getSimpleName() + ".merge cannot parse xml: " + dbg);
					return;
				}

				// Remove GUID from the standard XMP
				XmpTag.Note_HasExtendedXMP.remove(rootDoc);
				// XMLUtils.removeAttribute(mergedXmpDocument, "rdf:Description", "xmpNote:HasExtendedXMP");
				// Copy all the children of rdf:RDF element
				final Node rdfSource = getRdfRootNode(extendedXmpDocument);


				NodeList itemsSource = (rdfSource == null) ? null : rdfSource.getChildNodes();
				Element rdfDest = (Element) getRdfRootNode(rootDoc);
				for (int i = 0; i < itemsSource.getLength(); i++) {
					Node curr = itemsSource.item(i);
					Node newNode = rootDoc.importNode(curr, true);
					rdfDest.appendChild(newNode);
				}
			} catch (Exception ex) {
				String dbg = new String(data);
				LOGGER.error(this.getClass().getSimpleName() + ".merge cannot parse xml " + ex.getMessage() + dbg, ex);
			}
		}
	}

	protected Node getRdfRootNode(Document doc) {
		// x:xmpmeta/rdf:RDF/rdf:Description
		final NodeList rdfs = (doc == null) ? null : doc.getElementsByTagName("rdf:RDF");
		return (rdfs == null) ? null : rdfs.item(0);
	}

	public void showMetadata() {
		ensureDataRead();
		XMLUtils.showXML(getXmpDocument());
	}
	
	public abstract void write(OutputStream os) throws IOException;
}