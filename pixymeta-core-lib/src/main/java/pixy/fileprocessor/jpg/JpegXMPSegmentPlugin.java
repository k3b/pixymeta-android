package pixy.fileprocessor.jpg;

import pixy.image.jpeg.JpegSegmentMarker;
import pixy.meta.MetadataType;
import pixy.meta.jpeg.JpegXMP;

/**
 * A jpg-file specific {@link JpegXMP} plugin for {@link JpgFileProcessor}
 * Created by k3b on 09.07.2016.
 */
public class JpegXMPSegmentPlugin extends JpegXMP {

    static {
        JpgSegmentPluginFactory.register(MetadataType.XMP, JpegSegmentMarker.JPG_SEGMENT_EXIF_XMP_APP1,
                JpegMetaDef.XMP_ID, JpegXMPSegmentPlugin.class);
        JpgSegmentPluginFactory.register(MetadataType.XMP, JpegSegmentMarker.JPG_SEGMENT_EXIF_XMP_APP1,
                JpegMetaDef.NON_STANDARD_XMP_ID, JpegXMPSegmentPlugin.class);
        JpgSegmentPluginFactory.register(MetadataType.XMP, JpegSegmentMarker.JPG_SEGMENT_EXIF_XMP_APP1,
                JpegMetaDef.XMP_EXT_ID, JpegXMPSegmentPlugin.class);
    }

    /** make shure that static constructor was called */
    public static void register(){}

    /** Created via reflection by {@link pixy.fileprocessor.jpg.JpgSegmentPluginFactory} */
    public JpegXMPSegmentPlugin(byte[] data) {
        super(data);
    }

}
