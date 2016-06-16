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
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pixy.meta.IMetadataDirectory;
import pixy.meta.IMetadataTag;
import pixy.meta.MetaDataTagImpl;
import pixy.meta.Metadata;
import pixy.meta.MetadataDirectoryImpl;
import pixy.meta.MetadataType;
import pixy.string.XMLUtils;

public abstract class XMP extends Metadata  implements IMetadataDirectory {
	// Fields
	private final String MODUL_NAME;

	private Document xmpDocument;
	private Document extendedXmpDocument;
	//document contains the complete XML as a Tree.
	private Document mergedXmpDocument;
	private boolean hasExtendedXmp;
	private byte[] extendedXmpData;
	
	private String xmp;
		
	public XMP(String MODUL_NAME, byte[] data) {
		super(MetadataType.XMP, data);
		this.MODUL_NAME = MODUL_NAME;
	}
	
	public XMP(String MODUL_NAME, String xmp) {
		super(MetadataType.XMP, null);
		this.MODUL_NAME = MODUL_NAME;
		this.xmp = xmp;
	}
	
	public XMP(String MODUL_NAME, String xmp, String extendedXmp) {
		super(MetadataType.XMP, null);
		this.MODUL_NAME = MODUL_NAME;
		if(xmp == null) throw new IllegalArgumentException("Input XMP string is null");
		this.xmp = xmp;
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
			XMLUtils.removeAttribute(mergedXmpDocument, "rdf:Description", "xmpNote:HasExtendedXMP");
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
			if(xmp != null)
				xmpDocument = XMLUtils.createXML(xmp);
			else if(data != null)
				xmpDocument = XMLUtils.createXML(data);
			
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

	private MetadataDirectoryImpl metaData = null;

	// calculate metaData on demand
	private MetadataDirectoryImpl get() {
		if ((metaData == null)) {
			metaData = new MetadataDirectoryImpl().setName(MODUL_NAME);

			ensureDataRead();
			metaData.getTags().add(new MetaDataTagImpl("xml", XMLUtils.print(getMergedDocument(), "", new StringBuilder()).toString()));

			// MetadataDirectoryImpl child = new MetadataDirectoryImpl().setName(entry.getKey());
			// metaData.getSubdirectories().add(child);

			// final List<IMetadataTag> tags = child.getTags();
			// tags.add(new MetaDataTagImpl("type", thumbnail.getDataTypeAsString()));
		}
		return metaData;
	}

	/**
	 * Provides the name of the directory, for display purposes.  E.g. <code>Exif</code>
	 *
	 * @return the name of the directory
	 */
	@Override
	public String getName() {
		return get().getName();
	}

	/**
	 * @return sub-directories that belong to this Directory or null if there are no sub-directories
	 */
	@Override
	public List<IMetadataDirectory> getSubdirectories() {
		return get().getSubdirectories();
	}

	/**
	 * @return Tags that belong to this Directory or null if there are no tags
	 */
	@Override
	public List<IMetadataTag> getTags() {
		return get().getTags();
	}

}