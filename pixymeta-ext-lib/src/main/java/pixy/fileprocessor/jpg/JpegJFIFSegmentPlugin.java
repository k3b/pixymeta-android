package pixy.fileprocessor.jpg;

import pixy.image.jpeg.JpegSegmentMarker;
import pixy.meta.MetadataType;
import pixy.meta.jpeg.JFIFSegment;
import pixy.meta.jpeg.JPEGMeta;
import pixy.util.ArrayUtils;

/**
 * A jpg-file specific {@link JFIFSegment} plugin for {@link JpgFileProcessor}
 * Created by k3b on 09.07.2016.
 */
public class JpegJFIFSegmentPlugin extends JFIFSegment {
    static {

        JpgSegmentPluginFactory.register(MetadataType.JPG_JFIF, JpegSegmentMarker.JPG_SEGMENT_JFIF_APP0,
                JPEGMeta.JFIF_ID, JpegJFIFSegmentPlugin.class);

    }

    /** make shure that static constructor was called */
    public static void register(){}

    /** Created via reflection by {@link pixy.fileprocessor.jpg.JpgSegmentPluginFactory} */
    public JpegJFIFSegmentPlugin(byte[] data) {
        super(data);
    }

}
