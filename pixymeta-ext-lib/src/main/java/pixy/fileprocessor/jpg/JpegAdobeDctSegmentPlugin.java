package pixy.fileprocessor.jpg;

import pixy.image.jpeg.JpegSegmentMarker;
import pixy.meta.MetadataType;
import pixy.meta.jpeg.AdobeSegment;
import pixy.meta.jpeg.JPEGMeta;

/**
 * A jpg-file specific {@link pixy.meta.jpeg.AdobeSegment} plugin for {@link JpgFileProcessor}
 * Created by k3b on 09.07.2016.
 */
public class JpegAdobeDctSegmentPlugin extends AdobeSegment {

    static {
        JpgSegmentPluginFactory.register(MetadataType.JPG_ADOBE, JpegSegmentMarker.APP14,
                JPEGMeta.ADOBE_ID, JpegAdobeDctSegmentPlugin.class);
    }

    /** make shure that static constructor was called */
    public static void register(){}

    /** Created via reflection by {@link pixy.fileprocessor.jpg.JpgSegmentPluginFactory} */
    public JpegAdobeDctSegmentPlugin(byte[] data) {
        super(data);
    }
}
