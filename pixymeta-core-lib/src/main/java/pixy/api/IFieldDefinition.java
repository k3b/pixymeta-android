package pixy.api;

/**
 * The base of all exif and iptc-tags
 *
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
public interface IFieldDefinition {
    String getName();
    IDataType getDataType();
}
