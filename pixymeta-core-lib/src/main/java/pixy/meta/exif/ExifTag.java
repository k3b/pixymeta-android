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
import pixy.image.exifFields.Tag;
import pixy.string.StringUtils;

/**
 * Defines EXIF tags
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2015
 */
public enum ExifTag implements Tag {
	EXPOSURE_TIME("ExposureTime", (short)0x829a,FieldType.RATIONAL),
	FNUMBER("FNumber", (short)0x829d,FieldType.RATIONAL) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of EXIF FNumber data number: " + intValues.length);
			//formatting numbers up to 2 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.#");
	        return "F" + StringUtils.rationalToString(df, true, intValues);	
		}
	},
  	//EXIF_SUB_IFD("ExifSubIFD", (short)0x8769),	
	EXPOSURE_PROGRAM("ExposureProgram", (short)0x8822,FieldType.SHORT),
	SPECTRAL_SENSITIVITY("SpectralSensitivity", (short)0x8824),
	//GPS_SUB_IFD("GPSSubIFD", (short)0x8825),
	ISO_SPEED_RATINGS("ISOSpeedRatings", (short)0x8827,FieldType.SHORT),
	OECF("OECF", (short)0x8828),
	
	EXIF_VERSION("ExifVersion", (short)0x9000) {
		public String getFieldAsString(Object value) {
			return new String((byte[])value).trim();
		}
	},
	DATE_TIME_ORIGINAL("DateTimeOriginal", (short)0x9003, FieldType.ASCII),
	DATE_TIME_DIGITIZED("DateTimeDigitized", (short)0x9004, FieldType.ASCII),
	
	COMPONENT_CONFIGURATION("ComponentConfiguration", (short)0x9101),
	COMPRESSED_BITS_PER_PIXEL("CompressedBitsPerPixel", (short)0x9102, FieldType.RATIONAL),
	
	SHUTTER_SPEED_VALUE("ShutterSpeedValue", (short)0x9201,FieldType.SRATIONAL) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of EXIF ShutterSpeedValue data number: " + intValues.length);
			//formatting numbers up to 2 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.##");
	        return StringUtils.rationalToString(df, false, intValues);	
		}
	},
	APERTURE_VALUE("ApertureValue", (short)0x9202,FieldType.RATIONAL),
	BRIGHTNESS_VALUE("BrightValue", (short)0x9203,FieldType.SRATIONAL),
	EXPOSURE_BIAS_VALUE("ExposureBiasValue", (short)0x9204,FieldType.SRATIONAL),
	MAX_APERTURE_VALUE("MaxApertureValue", (short)0x9205,FieldType.RATIONAL),
	SUBJECT_DISTANCE("SubjectDistance", (short)0x9206,FieldType.RATIONAL ),
	METERING_MODE("MeteringMode", (short)0x9207,FieldType.SHORT),
	LIGHT_SOURCE("LightSource", (short)0x9208,FieldType.SHORT),
	FLASH("Flash", (short)0x9209, FieldType.SHORT),
	FOCAL_LENGTH("FocalLength", (short)0x920a,FieldType.RATIONAL) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of EXIF FocalLength data number: " + intValues.length);
			//formatting numbers up to 2 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.##");
	        return StringUtils.rationalToString(df, true, intValues) + "mm";	
		}
	},	
	SUBJECT_AREA("SubjectArea", (short)0x9214),	
	MAKER_NODE("MakerNote", (short)0x927c),
	USER_COMMENT("UserComment", (short)0x9286,FieldType. ASCII),
	
	SUB_SEC_TIME("SubSecTime", (short)0x9290,FieldType.ASCII),
	SUB_SEC_TIME_ORIGINAL("SubSecTimeOriginal", (short)0x9291,FieldType.ASCII),
	SUB_SEC_TIME_DIGITIZED("SubSecTimeDigitized", (short)0x9292,FieldType.ASCII),
	
	FLASH_PIX_VERSION("FlashPixVersion", (short)0xa000) {
		public String getFieldAsString(Object value) {
			return new String((byte[])value).trim();
		}
	},
	COLOR_SPACE("ColorSpace", (short)0xa001,FieldType.SHORT) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Warning: unknown color space value: " + intValue;
			
			switch(intValue) {
				case 1:	description = "sRGB"; break;
				case 65535: description = "Uncalibrated";	break;
			}
			
			return description;
		}
	},
	EXIF_IMAGE_WIDTH("ExifImageWidth", (short)0xa002,FieldType.LONG), // or short
	EXIF_IMAGE_HEIGHT("ExifImageHeight", (short)0xa003,FieldType.LONG), // or short
	RELATED_SOUND_FILE("RelatedSoundFile", (short)0xa004),
	
	EXIF_INTEROPERABILITY_OFFSET("ExifInteroperabilityOffset", (short)0xa005) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	FLASH_ENERGY("FlashEnergy", (short)0xa20b,FieldType.RATIONAL),
	SPATIAL_FREQUENCY_RESPONSE("SpatialFrequencyResponse", (short)0xa20c),
	FOCAL_PLANE_X_RESOLUTION("FocalPlanXResolution", (short)0xa20e,FieldType.RATIONAL),
	FOCAL_PLANE_Y_RESOLUTION("FocalPlanYResolution", (short)0xa20f,FieldType.RATIONAL),
	FOCAL_PLANE_RESOLUTION_UNIT("FocalPlanResolutionUnit", (short)0xa210,FieldType.SHORT),
	
	SUBJECT_LOCATION("SubjectLocation", (short)0xa214),
	EXPOSURE_INDEX("ExposureIndex", (short)0xa215, FieldType.RATIONAL),
	SENSING_METHOD("SensingMethod", (short)0xa217, FieldType.SHORT),
	
	FILE_SOURCE("FileSource", (short)0xa300),
	SCENE_TYPE("SceneType", (short)0xa301),
	CFA_PATTERN("CFAPattern", (short)0xa302),
	
	CUSTOM_RENDERED("CustomRendered", (short)0xa401, FieldType.SHORT),
	EXPOSURE_MODE("ExposureMode", (short)0xa402,FieldType.SHORT),
	WHITE_BALENCE("WhileBalence", (short)0xa403, FieldType.SHORT),
	DIGITAL_ZOOM_RATIO("DigitalZoomRatio", (short)0xa404,FieldType.RATIONAL),
	FOCAL_LENGTH_IN_35MM_FORMAT("FocalLengthIn35mmFormat", (short)0xa405,FieldType.SHORT),
	SCENE_CAPTURE_TYPE("SceneCaptureType", (short)0xa406, FieldType.SHORT),
	GAIN_CONTROL("GainControl", (short)0xa407,FieldType.SHORT),
	CONTRAST("Contrast", (short)0xa408, FieldType.SHORT),
	SATURATION("Saturation", (short)0xa409, FieldType.SHORT),
	SHARPNESS("Sharpness", (short)0xa40a, FieldType.SHORT),
	DEVICE_SETTING_DESCRIPTION("DeviceSettingDescription", (short)0xa40b),
	SUBJECT_DISTANCE_RANGE("SubjectDistanceRange", (short)0xa40c, FieldType.SHORT),
	
	IMAGE_UNIQUE_ID("ImageUniqueID", (short)0xa420),
	
	OWNER_NAME("OwnerName", (short)0xa430),
	BODY_SERIAL_NUMBER("BodySerialNumber", (short)0xa431),
	LENS_SPECIFICATION("LensSpecification", (short)0xa432),
	LENS_Make("LensMake", (short)0xa433),
	LENS_MODEL("LensModel", (short)0xa434),
	LENS_SERIAL_NUMBER("LensSerialNumber", (short)0xa435),
	
	EXPAND_SOFTWARE("ExpandSoftware", (short)0xafc0),
	EXPAND_LENS("ExpandLens", (short)0xafc1, FieldType.ASCII),
	EXPAND_FILM("ExpandFilm", (short)0xafc2, FieldType.ASCII),
	EXPAND_FILTER_LENS("ExpandFilterLens", (short)0xafc3),
	EXPAND_SCANNER("ExpandScanner", (short)0xafc4),
	EXPAND_FLASH_LAMP("ExpandFlashLamp", (short)0xafc5),
		
	PADDING("Padding", (short)0xea1c),
	
	UNKNOWN("Unknown",  (short)0xffff);

	private ExifTag(String name, short value, FieldType fieldType) {
		this.name = name;
		this.value = value;

		if (fieldType != null) {
			this.fieldType = fieldType;
		}
	}

	private ExifTag(String name, short value)
	{
		this(name, value, null);
	}

	public String getName()
	{
		return this.name;
	}	
	
	public short getValue()
	{
		return this.value;
	}
	
	@Override
    public String toString() {
		if (this == UNKNOWN)
			return name;
		return name + " [Value: " + StringUtils.toHexStringMM(value) +"]";
	}
	
    public static Tag fromShort(short value) {
       	ExifTag exifTag = tagMap.get(value);
    	if (exifTag == null)
    	   return pixy.image.exifFields.ExifTag.UNKNOWN;
   		return exifTag;
    }
    
    private static final Map<Short, ExifTag> tagMap = new HashMap<Short, ExifTag>();
       
    static
    {
      for(ExifTag exifTag : values()) {
           tagMap.put(exifTag.getValue(), exifTag);
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