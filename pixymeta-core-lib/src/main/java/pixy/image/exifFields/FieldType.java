/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package pixy.image.exifFields;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import pixy.api.IDataType;
import pixy.meta.exif.Tag;

/**
 * TIFF and exif field type.
 *
 * Exif file Segments (or Image File Directories {@link pixy.image.exifFields.IFD}s)
 * for exif-tags {@link pixy.meta.exif.Tag}
 * contain {@link pixy.image.exifFields.ExifField}s
 * with exif {@link pixy.image.exifFields.FieldType}s (i.e. {@link pixy.image.exifFields.FieldType#RATIONAL})
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/06/2013
 */
public enum FieldType implements IDataType {
	BYTE("Byte", (short)0x0001),
	ASCII("ASCII", (short)0x0002),
	SHORT("Short", (short)0x0003),
	LONG("Long", (short)0x0004),
	RATIONAL("Rational", (short)0x0005),
	SBYTE("SByte", (short)0x0006),
	UNDEFINED("Undefined", (short)0x0007),
	SSHORT("SShort", (short)0x0008),
	SLONG("SLong", (short)0x0009),
	SRATIONAL("SRational", (short)0x000a),
	FLOAT("Float", (short)0x000b),
	DOUBLE("Double", (short)0x000c),
	IFD("IFD", (short)0x000d),
	// This is actually not a TIFF field type, internally it is a BYTE field
	WINDOWSXP("WindowsXP", (short)0x000e),
	
	UNKNOWN("Unknown", (short)0x0000),
	GPSCoordinate("GPSCoordinate", Tag.DONOT_WRITE) // virtual rational3 plus sign
	;

	private FieldType(String name, short value) {
		this.name = name;
		this.value = value;
	}
	
	public static ExifField<?> createField(pixy.meta.exif.Tag tag, FieldType type, Object data) {
		if(data == null) throw new IllegalArgumentException("Input data is null");
    	ExifField<?> retValue = null;
    	Class<?> typeClass = data.getClass();
    	switch(type) {
    		case ASCII:
    			if(typeClass == String.class) {
    				retValue = new ASCIIField(tag, (String)data);
    			}
    			break;
    		case BYTE:
    		case SBYTE:
    		case UNDEFINED:
    			if(typeClass == byte[].class) {
    				byte[] byteData = (byte[])data;
    				if(byteData.length > 0) {
    					if(type == FieldType.BYTE)
    						retValue = new pixy.image.exifFields.ByteField(tag, byteData);
    					else if(type == FieldType.SBYTE)
    						retValue = new SByteField(tag, byteData);
    					else
    						retValue = new UndefinedField(tag, byteData);
    				}
    			}
    			break;
    		case SHORT:
    		case SSHORT:
    			if(typeClass == short[].class) {
    				short[] shortData = (short[])data;
    				if(shortData.length > 0) {
    					if(type == FieldType.SHORT)
    						retValue = new ShortField(tag, shortData);
    					else
    						retValue = new pixy.image.exifFields.SShortField(tag, shortData);
    				}
    			}
    			break;
    		case LONG:
    		case SLONG:
    			if(typeClass == int[].class) {
    				int[] intData = (int[])data;
    				if(intData.length > 0) {
    					if(type == FieldType.LONG)
    						retValue = new LongField(tag, intData);
    					else
    						retValue = new SLongField(tag, intData);
    				}
    			}
    			break;
    		case RATIONAL:
    		case SRATIONAL:
    			if(typeClass == int[].class) {
    				int[] intData = (int[])data;
    				if(intData.length > 0 && intData.length % 2 == 0) {
    					if(type == FieldType.RATIONAL)
    						retValue = new RationalField(tag, intData);
    					else
    						retValue = new SRationalField(tag, intData);
    				}
    			}
    			break;
    		case WINDOWSXP: // Not a real TIFF field type, just a convenient way to add Windows XP field as a sting
    			if(typeClass == String.class) {
    				try {
						byte[] xp = ((String)data).getBytes("UTF-16LE");
						retValue = new pixy.image.exifFields.ByteField(tag, xp);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}    				   				
    			}
    			break;
    		default:
    	}
    	
		return retValue;
	}
	
	public String getName() {
		return name;
	}
	
	public short getValue() {
		return value;
	}
	
	@Override
    public String toString() {
		return name;
	}
	
    public static FieldType fromShort(short value) {
       	FieldType fieldType = typeMap.get(value);
    	if (fieldType == null)
    	   return UNKNOWN;
   		return fieldType;
    }
    
    private static final Map<Short, FieldType> typeMap = new HashMap<Short, FieldType>();
       
    static
    {
      for(FieldType fieldType : values())
           typeMap.put(fieldType.getValue(), fieldType);
    }
    
    public static boolean validateData(FieldType type, Object data) {
    	if(data == null) throw new IllegalArgumentException("Input data is null");
    	boolean retValue = false;
    	Class<?> typeClass = data.getClass();
    	switch(type) {
    		case ASCII:
    		case WINDOWSXP: // Not a real TIFF field type, just a convenient way to add Windows XP field as a sting
				if(typeClass == String.class) {
					retValue = true;    				
				}
				break;
    		case BYTE:
    		case SBYTE:
    		case UNDEFINED:
    			if(typeClass == byte[].class) {
    				byte[] byteData = (byte[])data;
    				if(byteData.length > 0) retValue = true;
    			}
    			break;
    		case SHORT:
    		case SSHORT:
    			if(typeClass == short[].class) {
    				short[] shortData = (short[])data;
    				if(shortData.length > 0) retValue = true;    				
    			}
    			break;
    		case LONG:
    		case SLONG:
    			if(typeClass == int[].class) {
    				int[] intData = (int[])data;
    				if(intData.length > 0) retValue = true;    				
    			}
    			break;
    		case RATIONAL:
    		case SRATIONAL:
    			if(typeClass == int[].class) {
    				int[] intData = (int[])data;
    				if(intData.length > 0 && intData.length % 2 == 0) retValue = true;  				
    			}
    			break;
    		default:
    	}
    	
		return retValue;    	
    }
	
	private final String name;
	private final short value;
}