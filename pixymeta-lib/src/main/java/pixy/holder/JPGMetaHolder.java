package pixy.holder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.api.IMetadata;
import pixy.image.ImageType;
import pixy.image.tiff.Tag;
import pixy.meta.MetadataType;
import pixy.meta.exif.ExifTag;
import pixy.meta.exif.JpegExif;
import pixy.meta.jpeg.JPEGMeta;

/**
 * Created by k3b on 06.07.2016.
 */
public class JPGMetaHolder extends MetaHolder {
    private JpegExif exif = null;

    public JPGMetaHolder(Map<MetadataType, IMetadata> data) {
        super(data);

        exif = (JpegExif) data.get(MetadataType.EXIF);
    }

    public static JPGMetaHolder readMetadata(InputStream is) throws IOException {
        Map<MetadataType, IMetadata> data = JPEGMeta.readMetadata(is);
        return new JPGMetaHolder(data);
    }

    public IFieldValue putValue(IFieldDefinition tag, String value) {
        IFieldValue result = getValue(tag);
        // TODO !!! if (result != null) result.setValue(value);
        add(result);
        return defs2Values.get(tag);
    }


}
