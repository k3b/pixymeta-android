package pixy.fileprocessor.jpg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.image.jpeg.JpegSegmentMarker;
import pixy.meta.MetadataType;
import pixy.meta.exif.JpegExif;
import pixy.meta.jpeg.JPEGMeta;

/**
 * A jpg-file specific {@link pixy.meta.exif.Exif} plugin for {@link JpgFileProcessor}
 * Created by k3b on 09.07.2016.
 */
public class JpegExifSegmentPlugin extends JpegExif {
    static {
        JpgSegmentPluginFactory.register(MetadataType.EXIF, JpegSegmentMarker.JPG_SEGMENT_EXIF_XMP_APP1,
                JPEGMeta.EXIF_ID, JpegExifSegmentPlugin.class);
    }

    /** make shure that static constructor was called */
    public static void register(){}

    /** Created via reflection by {@link pixy.fileprocessor.jpg.JpgSegmentPluginFactory} */
    public JpegExifSegmentPlugin(byte[] data) {
        super(data);
    }

}
