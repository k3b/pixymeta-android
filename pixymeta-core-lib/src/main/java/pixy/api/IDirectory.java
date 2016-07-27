package pixy.api;

import java.util.List;

/**
 * A {@link pixy.api.IDirectory} is a collection of iptc/exif tags ({@link pixy.api.IFieldDefinition})
 * with their current values.
 * <ul>
 * <li>A {@link pixy.api.IDirectory} (i.e. exif directory {@link pixy.image.exifFields.IFD})
 *    contains 0 or more {@link pixy.api.IFieldValue}-s.</li>
 * <li>Each {@link pixy.api.IFieldValue} (i.e. {@link pixy.image.exifFields.ExifField} )
 *    has a {@link pixy.api.IFieldDefinition} (i.e. exif-tag {@link pixy.meta.exif.Tag})
 *    and a {@link pixy.api.IDataType} (i.e. exif type {@link pixy.image.exifFields.FieldType#RATIONAL})</li>
 * </ul>
 *
 * Created by k3b on 17.06.2016.
 */
public interface IDirectory {
    IDirectory setName(String name);
    /** return the name of the directory */
    String getName();
    /** return all values that belong to the directory */
    List<IFieldValue> getValues();
    /** return values for fieldDefinition or null if it does not exist. */
    IFieldValue getValue(IFieldDefinition fieldDefinition);
}
