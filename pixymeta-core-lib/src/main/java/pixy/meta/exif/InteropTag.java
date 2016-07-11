/**
 * Copyright (c) 2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package pixy.meta.exif;

import java.util.HashMap;
import java.util.Map;

import pixy.api.IDataType;
import pixy.image.exifFields.*;
import pixy.string.StringUtils;

/**
 * Defines Interoperability tags
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2015
 */
public enum InteropTag implements Tag {
	// EXIF InteropSubIFD tags
	INTEROPERABILITY_INDEX("InteroperabilityIndex", (short)0x0001,FieldType.ASCII),
	INTEROPERABILITY_VERSION("InteroperabilityVersion", (short)0x0002),
	RELATED_IMAGE_FILE_FORMAT("RelatedImageFileFormat", (short)0x1000,FieldType.ASCII),
	RELATED_IMAGE_WIDTH("RelatedImageWidth", (short)0x1001,FieldType.SHORT),
	RELATED_IMAGE_LENGTH("RelatedImageLength", (short)0x1002,FieldType.SHORT),
	// unknown tag
	UNKNOWN("Unknown",  (short)0xffff); 
	// End of IneropSubIFD tags
		
	private InteropTag(String name, short value, FieldType fieldType) {
		this.name = name;
		this.value = value;

		if (fieldType != null) {
			this.fieldType = fieldType;
		}
	}

	private InteropTag(String name, short value)
	{
		this(name, value, null);
	}

	public String getName() {
		return name;
	}
	
	public short getValue() {
		return value;
	}
	
	@Override
    public String toString() {
		if (this == UNKNOWN)
			return name;
		return name + " [Value: " + StringUtils.toHexStringMM(value) +"]";
	}
	
    public static Tag fromShort(short value) {
       	InteropTag tag = tagMap.get(value);
    	if (tag == null)
    	   return pixy.image.exifFields.ExifTag.UNKNOWN;
   		return tag;
    }
    
    private static final Map<Short, InteropTag> tagMap = new HashMap<Short, InteropTag>();
       
    static
    {
      for(InteropTag tag : values()) {
           tagMap.put(tag.getValue(), tag);
      }
    }
    
    /**
     * Intended to be overridden by certain tags to provide meaningful string
     * representation of the field value such as compression, photo metric interpretation etc.
     * 
	 * @param value field value to be mapped to a string
	 * @return a string representation of the field value or empty string if no meaningful string
	 * 	representation exists.
	 */
    public String getFieldAsString(Object value) {
    	return "";
	}
	
	public FieldType getFieldType() {
		return fieldType;
	}
	
	private final String name;
	private final short value;
	private FieldType fieldType  = FieldType.UNKNOWN;;

	// implementation of api.IFieldDefinition
	@Override
	public IDataType getDataType() {
		return getFieldType();
	}

}