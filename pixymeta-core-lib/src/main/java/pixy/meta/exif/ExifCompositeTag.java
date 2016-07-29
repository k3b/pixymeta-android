package pixy.meta.exif;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pixy.api.DefaultApiImpl;
import pixy.api.IDataType;
import pixy.api.IDirectory;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.image.exifFields.ASCIIField;
import pixy.image.exifFields.AbstractByteField;
import pixy.image.exifFields.DoubleField;
import pixy.image.exifFields.ExifField;
import pixy.image.exifFields.FieldType;
import pixy.image.exifFields.IFD;
import pixy.image.exifFields.RationalField;

/**
 * Define virtual exif tags that contain of more than 1 physical tags.
 *
 * Copyright (C) 2016 by k3b.
 */
public enum ExifCompositeTag implements Tag {
    GPS_LATITUDE_EX(FieldType.GPSCoordinate, GPSTag.GPS_LATITUDE, GPSTag.GPS_LATITUDE_REF) {
        @Override
        public ExifField createVirtualField(IFD directory) {
            return getGpsField(this, directory, "NS", this.replacementTags);
        }
    },
    GPS_DEST_LATITUDE_EX(FieldType.GPSCoordinate, GPSTag.GPS_DEST_LATITUDE, GPSTag.GPS_DEST_LATITUDE_REF) {
        @Override
        public ExifField createVirtualField(IFD directory) {
            return getGpsField(this, directory, "NS", this.replacementTags);
        }
    },
    GPS_LONGITUDE_EX(FieldType.GPSCoordinate, GPSTag.GPS_LONGITUDE, GPSTag.GPS_LONGITUDE_REF) {
        @Override
        public ExifField createVirtualField(IFD directory) {
            return getGpsField(this, directory, "EW", this.replacementTags);
        }
    },
    GPS_DEST_LONGITUDE_EX(FieldType.GPSCoordinate, GPSTag.GPS_DEST_LONGITUDE, GPSTag.GPS_DEST_LONGITUDE_REF) {
        @Override
        public ExifField createVirtualField(IFD directory) {
            return getGpsField(this, directory, "EW", this.replacementTags);
        }
    },
    GPS_ALTITUDE_EX(FieldType.GPSCoordinate, GPSTag.GPS_ALTITUDE, GPSTag.GPS_ALTITUDE_REF) {
        @Override
        public ExifField createVirtualField(IFD directory) {
            // 0==abov sea; 1==below sea
            return getGpsField(this, directory, "\0\1", this.replacementTags);
        }
    }

    ;

    private static final Map<IFieldDefinition, ExifCompositeTag> allReplacements = new HashMap<IFieldDefinition, ExifCompositeTag>();

    private final FieldType fieldType;
    protected final Tag[] replacementTags;

    private ExifCompositeTag(final FieldType fieldType, final Tag... replacementTags) {
        this.fieldType = fieldType;
        this.replacementTags = replacementTags;
    }

    abstract public ExifField createVirtualField(IFD directory);

    private static ExifField getGpsField(ExifCompositeTag resultTag, IFD directory, String posNegValueDefinition, Tag... srcTags) {
        RationalField rationalValue = (RationalField) directory.getField(srcTags[0]);

        // degree, minutes, seconds
        int[] intValues = (rationalValue == null) ? null : rationalValue.getData ();

        if (intValues != null) {
            double result = 0;
            if (intValues.length > 1) result = 1.0 * intValues[0] / intValues[1];
            if (intValues.length > 3) result += 1.0 * intValues[2] / intValues[3] / 60.0;
            if (intValues.length > 5) result += 1.0 * intValues[4] / intValues[5] / 3600.0;

            ExifField<?> signValue = directory.getField(srcTags[1]);
            if (isNegtive(signValue, posNegValueDefinition)) result = result * -1;

            return new DoubleField(resultTag, new double[] {result});
        }

        return null;
    }

    private static boolean isNegtive(ExifField<?> signValue, String posNegValueDefinition) {
        char posValue = posNegValueDefinition.charAt(0);
        char signByte = posValue;
        if (signValue instanceof AbstractByteField) {
            byte[] bytes = ((AbstractByteField) signValue).getData();
            if ((bytes != null) && (bytes.length > 0)) signByte = (char) bytes[0];
        } else {
            String signString = (signValue == null) ? null : signValue.getValueAsString();
            if ((signString != null) && (signString.length() > 0))
                signByte = signString.charAt(0);
        }

        return signByte != posValue;
    }

    @Override
    public String getFieldAsString(Object value) {
        if (value instanceof IDirectory) {
            IFieldValue result = ((IDirectory) value).getValue(this);
            if (result != null) return result.getValueAsString();
        }
        return null;
    }

    @Override
    public FieldType getFieldType() {
        return fieldType;
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public IDataType getDataType() {
        return fieldType;
    }

    @Override
    public short getValue() {
        return Tag.DONOT_WRITE;
    }

    static
    {
        for(ExifCompositeTag tag : values()) {
            register(tag, tag.replacementTags);
        }
    }

    private static void register(final ExifCompositeTag exifCompositeTag, final Tag[] replacementTags) {
        if (replacementTags != null) {
            for (Tag old : replacementTags) {
                allReplacements.put(old, exifCompositeTag);
            }
        }
    }

    public static List<IFieldValue> replace(IFD directory, List<IFieldValue> values) {
        for (int i = values.size()-1; i >= 0; i--) {
            final IFieldValue value = values.get(i);
            ExifCompositeTag replacement = getReplacement(value);
            if (replacement != null) {
                values.remove(i);
                include(values, directory, replacement);
            }
        }
        return values;
    }

    public static ExifCompositeTag getReplacement(IFieldValue value) {
        Tag tag = (Tag) value.getDataType();
        return getReplacement(tag);
    }

    public static ExifCompositeTag getReplacement(IFieldDefinition tag) {
        if (tag == null) return null;

        return allReplacements.get(tag);
    }

    private static void include(List<IFieldValue> values, IFD directory, ExifCompositeTag replacement) {
        if (null == DefaultApiImpl.getValue(values, replacement)) {
            values.add(replacement.createVirtualField(directory));
        }
    }
}
