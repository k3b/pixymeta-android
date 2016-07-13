package pixy.fileprocessor.jpg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import pixy.api.IFieldDefinition;
import pixy.api.IMetadata;
import pixy.image.jpeg.JpegSegmentMarker;
import pixy.meta.MetadataType;
import pixy.util.ArrayUtils;

/**
 * Factory to create SegmentPlugin-s
 * Created by k3b on 09.07.2016.
 */
public class JpgSegmentPluginFactory {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(JpgSegmentPluginFactory.class);


    static List<JpgSegmentPluginFactory> factories = new ArrayList<JpgSegmentPluginFactory>();

    public final MetadataType type;
    private final JpegSegmentMarker marker;
    private final String subMarker;
    private final Class<? extends IMetadata> jpegExifClass;
    private final Class<? extends IFieldDefinition>[] fieldDefs;
    private boolean debug = false;

    private JpgSegmentPluginFactory(MetadataType type, JpegSegmentMarker marker, String subMarker, Class<? extends IMetadata> jpegExifClass
            , Class<? extends IFieldDefinition>... fieldDefs) {
        this.type = type;
        this.marker = marker;
        this.subMarker = subMarker;
        this.jpegExifClass = jpegExifClass;
        this.fieldDefs = fieldDefs;
    }

    /** each plugin implementation registeres here in it-s static constructor */
    public static JpgSegmentPluginFactory register(MetadataType type, JpegSegmentMarker marker, String subMarker,
                                                   Class<? extends IMetadata> jpegExifClass, Class<? extends IFieldDefinition>... fieldDefs) {
        LOGGER.info("JpgSegmentPluginFactory.register " + jpegExifClass.getSimpleName());
        final JpgSegmentPluginFactory factory = new JpgSegmentPluginFactory(type, marker, subMarker, jpegExifClass, fieldDefs);
        factories.add(factory);
        return factory;
    }

    public static JpgSegmentPluginFactory find(JpegSegmentMarker marker, byte[] data) {
        for (JpgSegmentPluginFactory processor : factories) {
            if ((processor.marker != null) && (marker == processor.marker)) {
                String subMarker = processor.subMarker;
                if ((subMarker == null) || (new String(data, 0, subMarker.length()).equals(subMarker))) {
                    return processor;
                }
            }
        }
        return null;
    }

    public static JpgSegmentPluginFactory find(Class<? extends IFieldDefinition> tagClass) {
        if (tagClass != null) {
            for (JpgSegmentPluginFactory processor : factories) {
                if (processor != null) {
                    for (Class<? extends IFieldDefinition> candidate : processor.fieldDefs) {
                        if (candidate.isAssignableFrom(tagClass))
                        {
                            return processor;
                        }
                    }
                }
            }
        }
        return null;
    }

    public IMetadata create(byte[] data) {
        try {
            final int subMarkerLen = (subMarker == null) ? 0 : subMarker.length();
            data = ArrayUtils.subArray(data, subMarkerLen, data.length - subMarkerLen);
            Constructor<? extends IMetadata> constructor = jpegExifClass.getConstructor(byte[].class);
            final IMetadata metadata = constructor.newInstance(data);
            if (debug) {
                metadata.setDebugMessageBuffer(new StringBuilder());
            }
            return metadata;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override public String toString() {
        return jpegExifClass.getSimpleName() + "[" + marker.getName() + "," + subMarker +  "]";
    }

    public JpgSegmentPluginFactory setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }
}
