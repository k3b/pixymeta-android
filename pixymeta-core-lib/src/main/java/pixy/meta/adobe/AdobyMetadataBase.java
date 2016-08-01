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
 * AdobyMetadataBase.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    13Mar2015  initial creation
 */

package pixy.meta.adobe;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.io.IOUtils;
import pixy.string.StringUtils;

/** living in an {@link pixy.meta.adobe.AdobeIRBSegment} */
public class AdobyMetadataBase {
	private final ImageResourceID tag;
	private String name;
	protected int size;
	protected byte[] data;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(AdobyMetadataBase.class);
	
	public AdobyMetadataBase(ImageResourceID tag, String name, byte[] data) {
		this(tag, name, (data == null)?0:data.length, data);
	}
	
	public AdobyMetadataBase(ImageResourceID tag, String name, int size, byte[] data) {
		this.tag = tag;
		this.name = name;
		this.size = size;
		this.data = data;
	}
	
	public byte[] getData() {
		return data.clone();
	}
	
	public String getName() {
		return name;
	}
	
	public short getID() {
		return tag.getValue();
	}
	
	public int getSize() {
		return size;
	}

	public String getTypeString() {
		short segmentCode = tag.getValue();

		if((segmentCode >= ImageResourceID.PATH_INFO0.getValue()) && (segmentCode <= ImageResourceID.PATH_INFO998.getValue())) {
			return "PATH_INFO[" + StringUtils.toHexStringMM(segmentCode) + "]";
		}
		else if((segmentCode >= ImageResourceID.PLUGIN_RESOURCE0.getValue()) && (segmentCode <= ImageResourceID.PLUGIN_RESOURCE999.getValue())) {
			return "PLUGIN_RESOURCE[" + StringUtils.toHexStringMM(segmentCode) + "]";
		}
		else if (tag == ImageResourceID.UNKNOWN) {
			return tag +"[" + StringUtils.toHexStringMM(segmentCode) + "]";
		} else {
			return "" + tag;
		}
	}

	@Override
	public String toString() {
		return getName() + " adoby-8BIM " + getTypeString();
	}

	public void print() {
		LOGGER.info(getTypeString());

		LOGGER.info("Type: 8BIM");
		LOGGER.info("Name: {}", name);
		LOGGER.info("Size: {}", size);	
	}
	
	public void write(OutputStream os) throws IOException {
		// Write AdobeIRBSegment tag.getValue
		os.write("8BIM".getBytes());
		// Write resource tag.getValue
		IOUtils.writeShortMM(os, tag.getValue());
		// Write name (Pascal string - first byte denotes length of the string)
		byte[] temp = name.trim().getBytes();
		os.write(temp.length); // Size of the string, may be zero
		os.write(temp);
		if(temp.length%2 == 0)
			os.write(0);
		// Now write data size
		IOUtils.writeIntMM(os, size);
		os.write(data); // Write the data itself
		if(data.length%2 != 0)
			os.write(0); // Padding the data to even size if needed
	}
}