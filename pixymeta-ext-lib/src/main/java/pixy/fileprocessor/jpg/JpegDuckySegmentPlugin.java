package pixy.fileprocessor.jpg;

import pixy.image.jpeg.JpegSegmentMarker;
import pixy.meta.MetadataType;
import pixy.meta.jpeg.DuckySegment;
import pixy.meta.jpeg.JPEGMeta;

/**
 * A jpg-file specific {@link pixy.meta.exif.Exif} plugin for {@link JpgFileProcessor}
 * Created by k3b on 09.07.2016.
 */
public class JpegDuckySegmentPlugin extends DuckySegment {

    static {
        JpgSegmentPluginFactory.register(MetadataType.JPG_DUCKY, JpegSegmentMarker.APP12,
                JPEGMeta.DUCKY_ID, JpegDuckySegmentPlugin.class);
    }

    /** make shure that static constructor was called */
    public static void register(){}

    /** Created via reflection by {@link pixy.fileprocessor.jpg.JpgSegmentPluginFactory} */
    public JpegDuckySegmentPlugin(byte[] data) {
        super(data);
    }

}
