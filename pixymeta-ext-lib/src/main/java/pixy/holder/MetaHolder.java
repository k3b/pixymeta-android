package pixy.holder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pixy.api.DefaultApiImpl;
import pixy.api.IDirectory;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.api.IMetadata;
import pixy.meta.Metadata;
import pixy.meta.MetadataType;
import pixy.meta.exif.ExifTag;
import pixy.meta.exif.JpegExif;
import pixy.meta.jpeg.JPEGMeta;

/**
 * Contain all known metadata in a uniform format.
 *
 * Created by k3b on 06.07.2016.
 */
public class MetaHolder {
    public MetaHolder(Map<MetadataType, IMetadata> data) {
        for (IMetadata meta : data.values()) {
            if (meta != null) add(meta.getMetaData());
        }
    }

    protected Map<IFieldDefinition, IFieldValue> defs2Values = new HashMap<IFieldDefinition, IFieldValue>();

    public IFieldValue getValue(IFieldDefinition tag) {
        return defs2Values.get(tag);
    }

    public String getValueAsString(IFieldDefinition tag) {
        IFieldValue value = defs2Values.get(tag);
        return (value == null) ? null : value.getValueAsString();
    }

    protected void add(List<IDirectory> dirs) {
        if (dirs != null) {
            for (IDirectory dir : dirs) {
                add(dir);
            }
        }
    }

    protected void add(IDirectory dir) {
        final List<IFieldValue> values = (dir == null) ? null : dir.getValues();
        if (values != null) {
            for (IFieldValue value : values) {
                add(value);
            }
        }
    }

    protected IFieldValue add(IFieldValue value) {
        if (value != null) {
            defs2Values.put(value.getDefinition(), value);
        }
        return value;
    }

    public IFieldValue add(IFieldDefinition tag, String value) {
        IFieldValue fieldValue = new DefaultApiImpl(tag, value);
        add(fieldValue);
        return fieldValue;
    }
}
