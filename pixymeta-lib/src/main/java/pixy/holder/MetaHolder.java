package pixy.holder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Created by k3b on 06.07.2016.
 */
public class MetaHolder {
    protected Map<MetadataType, IMetadata> data = null;

    public MetaHolder(Map<MetadataType, IMetadata> data) {
        this.data = data;

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

    protected void add(IFieldValue value) {
        if (value != null) {
            defs2Values.put(value.getDefinition(), value);
        }
    }

    public static class Empty implements IMetadata {

        @Override
        public byte[] getData() {
            return new byte[0];
        }

        @Override
        public MetadataType getType() {
            return null;
        }

        @Override
        public boolean isDataRead() {
            return false;
        }

        @Override
        public void showMetadata() {

        }

        /**
         * Writes the metadata out to the output stream
         *
         * @param out OutputStream to write the metadata to
         * @throws IOException
         */
        @Override
        public void write(OutputStream out) throws IOException {

        }

        /**
         * @return directories that belong to this MetaData
         */
        @Override
        public List<IDirectory> getMetaData() {
            return null;
        }

        @Override
        public void read() throws IOException {

        }
    }

    public static final Empty EMPTY = new Empty();
}
