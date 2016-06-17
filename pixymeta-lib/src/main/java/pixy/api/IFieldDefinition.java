package pixy.api;

import pixy.image.tiff.FieldType;

/**
 * Created by k3b on 17.06.2016.
 */
public interface IFieldDefinition {
    String getName();
    IDataType getDataType();
}
