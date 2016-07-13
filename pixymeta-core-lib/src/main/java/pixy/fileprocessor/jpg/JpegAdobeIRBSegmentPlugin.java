package pixy.fileprocessor.jpg;

import pixy.image.jpeg.JpegSegmentMarker;
import pixy.meta.MetadataType;
import pixy.meta.adobe.AdobeIRBSegment;
import pixy.fileprocessor.jpg.JpegMetaDef;
import pixy.meta.iptc.IPTCApplicationTag;

/**
 * A jpg-file specific {@link AdobeIRBSegment} plugin for {@link JpgFileProcessor}
 * Created by k3b on 09.07.2016.
 */
public class JpegAdobeIRBSegmentPlugin extends AdobeIRBSegment {
    static {
        JpgSegmentPluginFactory.register(MetadataType.PHOTOSHOP_IRB, JpegSegmentMarker.JPG_SEGMENT_IPTC_APP13,
                JpegMetaDef.PHOTOSHOP_IRB_ID, JpegAdobeIRBSegmentPlugin.class, IPTCApplicationTag.class)
                // .setDebug(true)
        ;
    }

    /** make shure that static constructor was called */
    public static void register(){}

    /** Created via reflection by {@link pixy.fileprocessor.jpg.JpgSegmentPluginFactory} */
    public JpegAdobeIRBSegmentPlugin(byte[] data) {
        super(data);
    }

}
