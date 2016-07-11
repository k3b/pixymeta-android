package pixy.image.exifFields;

import pixy.api.IDataType;

/**
 * Created by k3b on 16.06.2016.
 */
public class UnknownTag implements Tag {
    private final short value;
    private final pixy.image.exifFields.FieldType fieldType;
    private final String name;

    public UnknownTag(short value, String name, pixy.image.exifFields.FieldType fieldType) {
        this.value = value;
        this.fieldType = fieldType;
        this.name = name;
    }

    @Override
    public String getFieldAsString(Object value) {
        return (value != null) ? value.toString() : "";
    }

    @Override
    public pixy.image.exifFields.FieldType getFieldType() {
        return fieldType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public short getValue() {
        return value;
    }


    // implementation of api.IFieldDefinition
    @Override
    public IDataType getDataType() {
        return getFieldType();
    }
}
