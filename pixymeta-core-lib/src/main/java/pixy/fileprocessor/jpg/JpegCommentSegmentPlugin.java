package pixy.fileprocessor.jpg;

import pixy.image.jpeg.JpegSegmentMarker;
import pixy.meta.MetadataType;
import pixy.meta.image.Comments;

/**
 * A jpg-file specific {@link Comments} plugin for {@link JpgFileProcessor}
 * Created by k3b on 11.07.2016.
 */
public class JpegCommentSegmentPlugin extends Comments {
    static {
        JpgSegmentPluginFactory.register(MetadataType.COMMENT, JpegSegmentMarker.JPG_SEGMENT_COMMNENTS_COM,
                null, JpegCommentSegmentPlugin.class, CommentTag.getClass());
        JpgSegmentPluginFactory.register(MetadataType.COMMENT, JpegSegmentMarker.JPG_SEGMENT_COMMENT_APP10,
                null, JpegCommentSegmentPlugin.class, CommentTag.getClass());
    }

    /** make shure that static constructor was called */
    public static void register(){}

    /** Created via reflection by {@link pixy.fileprocessor.jpg.JpgSegmentPluginFactory} */
    public JpegCommentSegmentPlugin(byte[] data) {
        super(data);
    }

}
