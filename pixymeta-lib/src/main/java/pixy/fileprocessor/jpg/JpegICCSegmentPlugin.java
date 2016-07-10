package pixy.fileprocessor.jpg;

import pixy.image.jpeg.JpegSegmentMarker;
import pixy.meta.MetadataType;
import pixy.meta.icc.ICCProfile;
import pixy.meta.jpeg.JPEGMeta;

/**
 * A jpg-file specific {@link ICCProfile} plugin for {@link JpgFileProcessor}
 * Created by k3b on 09.07.2016.
 */
class JpegICCSegmentPlugin extends ICCProfile {
    static {
        JpgSegmentPluginFactory.register(
                MetadataType.ICC_PROFILE, JpegSegmentMarker.JPG_SEGMENT_ICC_APP2,
                JPEGMeta.ICC_PROFILE_ID, JpegICCSegmentPlugin.class);
    }

    /** make shure that static constructor was called */
    public static void register(){}

    /** Created via reflection by {@link pixy.fileprocessor.jpg.JpgSegmentPluginFactory} */
    public JpegICCSegmentPlugin(byte[] data) {
        super(data);
    }
}
