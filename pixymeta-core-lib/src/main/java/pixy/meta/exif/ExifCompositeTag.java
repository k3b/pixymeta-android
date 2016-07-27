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
import pixy.image.exifFields.DoubleField;
import pixy.image.exifFields.ExifField;
import pixy.image.exifFields.FieldType;
import pixy.image.exifFields.IFD;
import pixy.image.exifFields.RationalField;

/**
 * Define virtual exif tags that contain of more than 1 physical tags.
 *
 * Created by k3b on 25.07.2016.
 */
public enum ExifCompositeTag implements Tag {
    GPS_LATITUDE_EX(FieldType.GPSCoordinate, GPSTag.GPS_LATITUDE, GPSTag.GPS_LATITUDE_REF) {
        @Override
        public ExifField createVirtualField(IFD directory) {
            return getGpsField(this, directory, GPSTag.GPS_LATITUDE, GPSTag.GPS_LATITUDE_REF, "NS");
        }
    },
    GPS_DEST_LATITUDE_EX(FieldType.GPSCoordinate, GPSTag.GPS_DEST_LATITUDE, GPSTag.GPS_DEST_LATITUDE_REF) {
        @Override
        public ExifField createVirtualField(IFD directory) {
            return getGpsField(this, directory, GPSTag.GPS_DEST_LATITUDE, GPSTag.GPS_DEST_LATITUDE_REF, "NS");
        }
    },
    GPS_LONGITUDE_EX(FieldType.GPSCoordinate, GPSTag.GPS_LONGITUDE, GPSTag.GPS_LONGITUDE_REF) {
        @Override
        public ExifField createVirtualField(IFD directory) {
            return getGpsField(this, directory, GPSTag.GPS_LONGITUDE, GPSTag.GPS_LONGITUDE_REF, "WE");
        }
    },
    GPS_DEST_LONGITUDE_EX(FieldType.GPSCoordinate, GPSTag.GPS_DEST_LONGITUDE, GPSTag.GPS_DEST_LONGITUDE_REF) {
        @Override
        public ExifField createVirtualField(IFD directory) {
            return getGpsField(this, directory, GPSTag.GPS_DEST_LONGITUDE, GPSTag.GPS_DEST_LONGITUDE_REF, "WE");
        }
    };

    private final FieldType fieldType;

    private ExifCompositeTag(FieldType fieldType, Tag... replacementTags) {
        this.fieldType = fieldType;

        if ((replacementTags != null) && (replacementTags.length > 0))
        register(this, replacementTags);
    }

    abstract public ExifField createVirtualField(IFD directory);

    private static ExifField getGpsField(ExifCompositeTag resultTag, IFD directory, GPSTag valueTag, GPSTag refTag, String refValues) {
        RationalField rationalValue = (RationalField) directory.getField(valueTag);

        // degree, minutes, seconds
        int[] intValues = (rationalValue == null) ? null : rationalValue.getData ();

        if (intValues != null) {
            double result = 0;
            if (intValues.length > 1) result = 1.0 * intValues[0] / intValues[1];
            if (intValues.length > 3) result += 1.0 * intValues[2] / intValues[3] / 60.0;
            if (intValues.length > 5) result += 1.0 * intValues[4] / intValues[5] / 3600.0;

            ASCIIField signValue = (ASCIIField) directory.getField(refTag);
            String signString = (signValue == null) ? null : signValue.getValueAsString();
            if ((signString != null) && (signString.length() > 0)) {
                if (signString.charAt(0) != refValues.charAt(0)) result = result * -1;
            }
            return new DoubleField(resultTag, new double[] {result});
        }

        return null;
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

    private static Map<IFieldDefinition, ExifCompositeTag> replacements = null;

    private static void register(ExifCompositeTag exifCompositeTag, Tag[] replacementTags) {
        if (replacements == null) replacements = new HashMap<IFieldDefinition, ExifCompositeTag>();

        for(Tag old : replacementTags) {
            replacements.put(old, exifCompositeTag);
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
        if (replacements == null) replacements = new HashMap<IFieldDefinition, ExifCompositeTag>();

        return replacements.get(tag);
    }

    private static void include(List<IFieldValue> values, IFD directory, ExifCompositeTag replacement) {
        if (null == DefaultApiImpl.getValue(values, replacement)) {
            values.add(replacement.createVirtualField(directory));
        }
    }
}
