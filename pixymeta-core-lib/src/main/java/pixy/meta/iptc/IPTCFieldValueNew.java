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
 * IPTCFieldValue.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    16Jul2015  Added two new constructors for IPTCApplicationTag
 * WY    13Mar2015  Initial creation
 */

package pixy.meta.iptc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import pixy.api.DefaultApiImpl;
import pixy.api.IDataType;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.io.IOUtils;
import pixy.string.StringUtils;
import pixy.util.ArrayUtils;

/**
 * International Press Telecommunications Council (IPTC) field value
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/10/2013
 */
public class IPTCFieldValueNew implements IFieldValue {
	private static final Logger LOGGER = LoggerFactory.getLogger(IPTCFieldValueNew.class);

	public void append(byte[] data, int offset, int recordsize) {
	}


	public static class IPTCFieldValueList extends ArrayList<IPTCFieldValueNew> {
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder(getClass().getSimpleName()).append(":");
			for (IPTCFieldValueNew item : this) {
				if (result.length() > 0) result.append("\n");
				result.append(item);
			}
			return result.toString();
		}


		public IFieldValue getValue(IFieldDefinition tag) {
			if (this.size() == 0) return null;
			if (this.size() > 1) {
				LOGGER.warn("getValue(" + tag + ") ignoring multible values " );
			}
			return this.get(0);
		}
	}

	public static class IPTCFieldValueMap extends HashMap<String, IPTCFieldValueList> {


	}

	// Fields
	private int iptcSegmentTypeId; // Corresponds to IPTCRecord enumeration iptcSegmentTypeId
	private int tag; // Corresponds to IPTC tag enumeration tag field
	private int size;
	private byte[] data;
	private int offset;
	private IPTCTag tagEnum;

	// A unique name used as HashMap key
	private String name;

	public IPTCFieldValueNew(IFieldDefinition tag, Object data, int offset, int recordsize) {
		// TODO wrong params
		byte[] bytes = (data instanceof String) ? ((String) data).getBytes() : (byte[]) data;
		init(IPTCRecord.APPLICATION.getRecordNumber(), ((IPTCApplicationTag) tag).getTag(), recordsize, bytes, offset);
	}

	public IPTCFieldValueNew(int iptcSegmentTypeId, int tag, int size, byte[] data, int offset) {
		init(iptcSegmentTypeId, tag, size, data, offset);
	}

	public IPTCFieldValueNew(IPTCApplicationTag appTag, String value) {
		byte[] data = value.getBytes();
		init(IPTCRecord.APPLICATION.getRecordNumber(), appTag.getTag(), data.length, data, 0);
	}

	private void init(int iptcSegmentTypeId, int tag, int size, byte[] data, int offset) {
		this.iptcSegmentTypeId = iptcSegmentTypeId;
		this.tag = tag;
		this.size = size;
		this.data = data;
		this.offset = offset;
		this.name = getTagName();
	}

	public boolean allowMultiple() {
		return tagEnum.allowMultiple();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IPTCFieldValueNew other = (IPTCFieldValueNew) obj;
		byte[] thisData = ArrayUtils.subArray(data, offset, size);
		byte[] thatData = ArrayUtils.subArray(other.data, other.offset, other.size);
		if (!Arrays.equals(thisData, thatData))
			return false;
		if (iptcSegmentTypeId != other.iptcSegmentTypeId)
			return false;
		if (tag != other.tag)
			return false;
		return true;
	}
	
	public byte[] getData() {
		return ArrayUtils.subArray(data, offset, size);
	}
	
	public String getDataAsString() {
		return tagEnum.getDataAsString(getData());
	}
	
	public String getName() {
		return name;
	}
	
	public int getRecordNumber() {
		return iptcSegmentTypeId;
	}

	public int getSize() {
		return size;
	}
	
	public int getTag() {
		return tag;
	}
	
	public IPTCTag getTagEnum() {
		return tagEnum;
	}
	
	private String getTagName() {
		switch(IPTCRecord.fromRecordNumber(iptcSegmentTypeId)) {
			case APPLICATION:
				tagEnum = IPTCApplicationTag.fromTag(tag);
				break;
			case ENVELOP:
				tagEnum = IPTCEnvelopeTag.fromTag(tag);
				break;
			case FOTOSTATION:
				tagEnum = IPTCFotoStationTag.fromTag(tag);
				break;
			case NEWSPHOTO:
				tagEnum = IPTCNewsPhotoTag.fromTag(tag);
				break;
			case OBJECTDATA:
				tagEnum = IPTCObjectDataTag.fromTag(tag);
				break;
			case POST_OBJECTDATA:
				tagEnum = IPTCPostObjectDataTag.fromTag(tag);
				break;
			case PRE_OBJECTDATA:
				tagEnum = IPTCPreObjectDataTag.fromTag(tag);
				break;
			default:
				tagEnum = IPTCApplicationTag.UNKNOWN;
		}
		
		return tagEnum.getName();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(ArrayUtils.subArray(data, offset, size));
		result = prime * result + iptcSegmentTypeId;
		result = prime * result + tag;
		return result;
	}

	public String getTypeAsName() {
		switch (iptcSegmentTypeId) {
			case 1: //Envelope Record
				return "Envelope";
			case 2: //Application Record
				return "Application";
			case 3: //NewsPhoto Record
				return "NewsPhoto";
			case 7: //PreObjectData Record
				return "PreObjectData";
			case 8: //ObjectData Record
				return "ObjectData";
			case 9: //PostObjectData Record
				return "PostObjectData";
			case 240: //FotoStation Record
				return "FotoStation";
			default:
				return "Unknown";
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + getTypeAsName()
				+ "=" + iptcSegmentTypeId + "]: " + name +
				"[" + StringUtils.toHexStringMM((short)tag) + "] = " + getValueAsString();
	}

	public void print() {
		
		switch (iptcSegmentTypeId) {
			case 1: //Envelope Record
				LOGGER.info("Record number {}: Envelope Record", iptcSegmentTypeId);
				break;
			case 2: //Application Record
				LOGGER.info("Record number {}: Application Record", iptcSegmentTypeId);
				break;
			case 3: //NewsPhoto Record
				LOGGER.info("Record number {}: NewsPhoto Record", iptcSegmentTypeId);
				break;
			case 7: //PreObjectData Record
				LOGGER.info("Record number {}: PreObjectData Record", iptcSegmentTypeId);
				break;
			case 8: //ObjectData Record
				LOGGER.info("Record number {}: ObjectData Record", iptcSegmentTypeId);
				break;				
			case 9: //PostObjectData Record
				LOGGER.info("Record number {}: PostObjectData Record", iptcSegmentTypeId);
				break;	
			case 240: //FotoStation Record
				LOGGER.info("Record number {}: FotoStation Record", iptcSegmentTypeId);
				break;	
			default:
				LOGGER.info("Record number {}: Unknown Record", iptcSegmentTypeId);
				break;
		}		
		
		LOGGER.info("Dataset name: {}", name);
		LOGGER.info("Dataset tag: {}[{}]", tag, StringUtils.toHexStringMM((short)tag));
		LOGGER.info("Dataset size: {}", size);
		
		LOGGER.info("Dataset value: {}", getDataAsString());
	}
	
	/**
	 * Write the current IPTCFieldValue to the OutputStream
	 * 
	 * @param out OutputStream to write the IPTCFieldValue
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		out.write(0x1c); // tag marker
		out.write(iptcSegmentTypeId);
		out.write(getTag());
		IOUtils.writeShortMM(out, size);
		out.write(data, offset, size);
	}

	@Override
	public IFieldDefinition getDefinition() {
		return tagEnum;
	}

	@Override
	public String getValueAsString() {
		return getDataAsString();
	}

	@Override
	public void setValue(String value) {
		tagEnum.getDataAsString(getData());

	}

	@Override
	public IDataType getDataType() {
		return DefaultApiImpl.UNKNOWN;
	}
}