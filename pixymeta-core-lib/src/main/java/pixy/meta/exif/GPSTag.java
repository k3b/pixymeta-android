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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import pixy.api.IDataType;
import pixy.image.exifFields.FieldType;
import pixy.string.StringUtils;

/**
 * Defines GPS tags
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2015
 */
public enum GPSTag implements Tag {
	// EXIF GPSSubIFD tags
	GPS_VERSION_ID("GPSVersionID", (short)0x0000,FieldType.BYTE),
	GPS_LATITUDE_REF("GPSLatitudeRef", (short)0x0001,FieldType.ASCII),
	GPS_LATITUDE("GPSLatitude", (short)0x0002,FieldType.RATIONAL) {
		public String getFieldAsString(Object value) {
			return getDegreeMinuteSecondString(this, (int[]) value);
		}
	},
	GPS_LONGITUDE_REF("GPSLongitudeRef", (short)0x0003,FieldType. ASCII),
	GPS_LONGITUDE("GPSLongitude", (short)0x0004,FieldType.RATIONAL) {
		public String getFieldAsString(Object value) {
			return getDegreeMinuteSecondString(this, (int[]) value);
		}
	},
	GPS_ALTITUDE_REF("GPSAltitudeRef", (short)0x0005,FieldType.BYTE),
	GPS_ALTITUDE("GPSAltitude", (short)0x0006,FieldType.RATIONAL) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of " +
						this +
						" data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return StringUtils.rationalToString(df, true, intValues) + "m";	
		}
	},
	GPS_TIME_STAMP("GPSTimeStamp", (short)0x0007,FieldType.RATIONAL) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 6)
				throw new IllegalArgumentException("Wrong number of " +
						this +
						" data number: " + intValues.length);
			//formatting numbers up to 2 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.##");
	        return StringUtils.rationalToString(df, true, intValues[0], intValues[1]) + ":" + StringUtils.rationalToString(df, true, intValues[2], intValues[3])
	        		+ ":" + StringUtils.rationalToString(df, true, intValues[4], intValues[5]);	
		}
	},
	GPS_SATELLITES("GPSSatellites", (short)0x0008,FieldType. ASCII),
	GPS_STATUS("GPSStatus", (short)0x0009),
	GPS_MEASURE_MODE("GPSMeasureMode", (short)0x000a),	
	GPS_DOP("GPSDOP/ProcessingSoftware", (short)0x000b),
	GPS_SPEED_REF("GPSSpeedRef", (short)0x000c),
	GPSSpeed("GPSSpeed", (short)0x000d),
	GPS_TRACK_REF("GPSTrackRef", (short)0x000e),
	GPS_TRACK("GPSTrack", (short)0x000f),
	GPS_IMG_DIRECTION_REF("GPSImgDirectionRef", (short)0x0010,FieldType. ASCII),
	GPS_IMG_DIRECTION("GPSImgDirection", (short)0x0011,FieldType.RATIONAL) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of " +
						this +
						" data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return StringUtils.rationalToString(df, true, intValues) + '\u00B0';	
		}
	},
	GPS_MAP_DATUM("GPSMapDatum", (short)0x0012,FieldType. ASCII),
	GPS_DEST_LATITUDE_REF("GPSDestLatitudeRef", (short)0x0013),
	GPS_DEST_LATITUDE("GPSDestLatitude", (short)0x0014) {
		public String getFieldAsString(Object value) {
			return getDegreeMinuteSecondString(this, (int[]) value);
		}
	},
	GPS_DEST_LONGITUDE_REF("GPSDestLongitudeRef", (short)0x0015),
	GPS_DEST_LONGITUDE("GPSDestLongitude", (short)0x0016) {
		public String getFieldAsString(Object value) {
			return getDegreeMinuteSecondString(this, (int[]) value);
		}
	},
	GPS_DEST_BEARING_REF("GPSDestBearingRef", (short)0x0017),
	GPS_DEST_BEARING("GPSDestBearing", (short)0x0018) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of " +
						this +
						" data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return StringUtils.rationalToString(df, true, intValues) + "m";	
		}
	},
	GPS_DEST_DISTANCE_REF("GPSDestDistanceRef", (short)0x0019),
	GPS_DEST_DISTANCE("GPSDestDistance", (short)0x001a) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of " +
						this +
						" data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return StringUtils.rationalToString(df, true, intValues) + "m";	
		}
	},
	GPS_PROCESSING_METHOD("GPSProcessingMethod", (short)0x001b),
	GPS_AREA_INFORMATION("GPSAreaInformation", (short)0x001c),
	GPS_DATE_STAMP("GPSDateStamp", (short)0x001d,FieldType. ASCII),
	GPS_DIFFERENTIAL("GPSDifferential", (short)0x001e),
	GPS_HPOSITIONING_ERROR("GPSHPositioningError", (short)0x001f),
	// unknown tag
	UNKNOWN("Unknown",  (short)0xffff);
    // End of EXIF GPSSubIFD tags
	
	private GPSTag(String name, short value, FieldType fieldType) {
		this.name = name;
		this.value = value;

		if (fieldType != null) {
			this.fieldType = fieldType;
		}
	}

	private GPSTag(String name, short value)
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
       	GPSTag tag = tagMap.get(value);
    	if (tag == null)
    	   return ExifImageTag.UNKNOWN;
   		return tag;
    }
    
    private static final Map<Short, GPSTag> tagMap = new HashMap<Short, GPSTag>();
       
    static
    {
      for(GPSTag tag : values()) {
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

	private static String getDegreeMinuteSecondString(GPSTag owner, int[] value) {
		int[] intValues = value;
		if(intValues.length != 6)
			throw new IllegalArgumentException("Wrong number of " +
					owner +
					" data number: " + intValues.length);
		//formatting numbers up to 3 decimal places in Java
		DecimalFormat df = new DecimalFormat("#,###,###.###");
		return StringUtils.rationalToString(df,  true, intValues[0], intValues[1]) + '\u00B0' + StringUtils.rationalToString(df, true, intValues[2], intValues[3])
				+ "'" + StringUtils.rationalToString(df, true, intValues[4], intValues[5]) + "\"";
	}
}
