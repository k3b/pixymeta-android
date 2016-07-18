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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pixy.meta.MetadataBase;
import pixy.meta.MetadataType;
import pixy.string.XMLUtils;

public abstract class XMP extends MetadataBase {
	// Fields
	// data translated to xml
	private Document xmpDocument;

	// additional xml as bytes
	private byte[] extendedXmpData;
	private Document extendedXmpDocument;
	private boolean hasExtendedXmp;

	// document contains the complete XML as a Tree: xmpDocument+extendedXmpDocument
	private Document mergedXmpDocument;

	// data as string
	private String xmpDataAsXmlString;
		
	public XMP(byte[] data) {
		super(MetadataType.XMP, data);
	}
	
	public XMP(String xmpDataAsXmlString) {
		super(MetadataType.XMP, null);
		this.xmpDataAsXmlString = xmpDataAsXmlString;
	}
	
	public XMP(String xmpDataAsXmlString, String extendedXmp) {
		super(MetadataType.XMP, null);
		if(xmpDataAsXmlString == null) throw new IllegalArgumentException("Input XMP string is null");
		this.xmpDataAsXmlString = xmpDataAsXmlString;
		if(extendedXmp != null) { // We have ExtendedXMP
			try {
				setExtendedXMPData(XMLUtils.serializeToByteArray(XMLUtils.createXML(extendedXmp)));
			} catch (IOException e) {				
				e.printStackTrace();
			}
		}
	}

	public byte[] getData() {
		byte[] data = super.getData();
		if(data != null && !hasExtendedXmp)
			return data;
		try {
			return XMLUtils.serializeToByteArray(getMergedDocument());
		} catch (IOException e) {
			return null;
		}
	}
	
	public byte[] getExtendedXmpData() {
		return extendedXmpData;
	}
	
	public Document getExtendedXmpDocument() {
		if(hasExtendedXmp && extendedXmpDocument == null)
			extendedXmpDocument = XMLUtils.createXML(extendedXmpData);

		return extendedXmpDocument;
	}
	
	/**
	 * Merge the standard XMP and the extended XMP DOM
	 * <p>
	 * This is a very expensive operation, avoid if possible
	 * 
	 * @return a merged Document for the entire XMP data with the GUID from the standard XMP document removed
	 */
	public Document getMergedDocument() {
		if(mergedXmpDocument != null)
			return mergedXmpDocument;
		else if(getExtendedXmpDocument() != null) { // Merge document
			mergedXmpDocument = XMLUtils.createDocumentNode();
			Document rootDoc = getXmpDocument();
			NodeList children = rootDoc.getChildNodes();
			for(int i = 0; i< children.getLength(); i++) {
				Node importedNode = mergedXmpDocument.importNode(children.item(i), true);
				mergedXmpDocument.appendChild(importedNode);
			}
			// Remove GUID from the standard XMP
			XmpTag.Note_HasExtendedXMP.remove(mergedXmpDocument);
			// XMLUtils.removeAttribute(mergedXmpDocument, "rdf:Description", "xmpNote:HasExtendedXMP");
			// Copy all the children of rdf:RDF element
			NodeList list = extendedXmpDocument.getElementsByTagName("rdf:RDF").item(0).getChildNodes();
			Element rdf = (Element)(mergedXmpDocument.getElementsByTagName("rdf:RDF").item(0));
		  	for(int i = 0; i < list.getLength(); i++) {
	    		Node curr = list.item(i);
	    		Node newNode = mergedXmpDocument.importNode(curr, true);
    			rdf.appendChild(newNode);
	    	}
	    	return mergedXmpDocument;
		} else
			return getXmpDocument();
	}
	
	public Document getXmpDocument() {
		ensureDataRead();		
		return xmpDocument;
	}
	
	public boolean hasExtendedXmp() {
		return hasExtendedXmp;
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			if(xmpDataAsXmlString != null)
				xmpDocument = XMLUtils.createXML(xmpDataAsXmlString);
			else if(super.getData() != null)
				xmpDocument = XMLUtils.createXML(super.getData());
			
			isDataRead = true;
		}
	}
	
	public void setExtendedXMPData(byte[] extendedXmpData) {
		this.extendedXmpData = extendedXmpData;
		hasExtendedXmp = true;
	}
	
	public void showMetadata() {
		ensureDataRead();
		XMLUtils.showXML(getMergedDocument());
	}
	
	public abstract void write(OutputStream os) throws IOException;
}