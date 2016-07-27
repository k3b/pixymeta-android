/**
 * This Package defines the structure of meta data as a set of interfaces.<br/>
 *
 * <ul>
 * <li>A {@link pixy.api.IDirectory} (i.e. exif directory {@link pixy.image.exifFields.IFD})
 *    contains 0 or more {@link pixy.api.IFieldValue}-s.</li>
 * <li>Each {@link pixy.api.IFieldValue} (i.e. {@link pixy.image.exifFields.ExifField} )
 *    has a {@link pixy.api.IFieldDefinition} (i.e. exif-tag {@link pixy.meta.exif.Tag})
 *    and a {@link pixy.api.IDataType} (i.e. exif type {@link pixy.image.exifFields.FieldType#RATIONAL})</li>
 * </ul>
 *
 **/
package pixy.api;
