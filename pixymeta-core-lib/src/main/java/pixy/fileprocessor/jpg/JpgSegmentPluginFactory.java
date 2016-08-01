package pixy.fileprocessor.jpg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import pixy.api.IFieldDefinition;
import pixy.api.IMetadata;
import pixy.image.jpeg.JpegSegmentMarker;
import pixy.io.IOUtils;
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

    private final MetadataType metadataType;
    private final JpegSegmentMarker segmentMarker;
    private final String subMarker;
    private final Class<? extends IMetadata> metadataClass;
    private final Class<? extends IFieldDefinition>[] fieldDefClasses;
    private static boolean debug = true;

    private JpgSegmentPluginFactory(MetadataType metadataType, JpegSegmentMarker segmentMarker
            , String subMarker
            , Class<? extends IMetadata> metadataClass
            , Class<? extends IFieldDefinition>... fieldDefClasses) {
        this.metadataType = metadataType;
        this.segmentMarker = segmentMarker;
        this.subMarker = subMarker;
        this.metadataClass = metadataClass;
        this.fieldDefClasses = fieldDefClasses;
    }

    /** each plugin implementation registeres here in it-s static constructor */
    public static JpgSegmentPluginFactory register(MetadataType metadataType
            , JpegSegmentMarker segmentMarker, String subMarker
            , Class<? extends IMetadata> metadataClass
            , Class<? extends IFieldDefinition>... fieldDefClasses) {
        String message = "JpgSegmentPluginFactory.register " + metadataClass.getSimpleName() + "; " + segmentMarker;
        if (subMarker != null) message += " + " + subMarker;
        LOGGER.info(message);
        final JpgSegmentPluginFactory factory = new JpgSegmentPluginFactory(metadataType, segmentMarker, subMarker, metadataClass, fieldDefClasses);
        factories.add(factory);
        return factory;
    }

    public static JpgSegmentPluginFactory find(JpegSegmentMarker marker, byte[] data) {
        if (data != null) {
            for (JpgSegmentPluginFactory processor : factories) {
                if ((processor.segmentMarker != null) && (marker == processor.segmentMarker)) {
                    String subMarker = processor.subMarker;
                    final String currentSubMarker = (subMarker == null) ? null : new String(data, 0, subMarker.length());
                    if ((subMarker == null) || (subMarker.equals(currentSubMarker))) {
                        if (debug) {
                            String message = "find-byMarker(" + marker;
                            if (subMarker != null) message += " + " + subMarker;
                            LOGGER.info(message + ") : " + processor);
                        }
                        return processor;
                    }
                }
            }
            if (debug) {
                String message = "find-byMarker(" + marker;
                int len = IOUtils.find(data, (byte) 0, (byte) 31, 0);
                if ((len > 0) && (len < 40)) {
                    message += " + " + new String(data, 0, len);
                }
                LOGGER.info(message + ") : " + null);
            }
        }
        return null;
    }

    public static JpgSegmentPluginFactory find(Class<? extends IFieldDefinition> tagClass) {
        if (tagClass != null) {
            for (JpgSegmentPluginFactory processor : factories) {
                if (processor != null) {
                    for (Class<? extends IFieldDefinition> candidate : processor.fieldDefClasses) {
                        if (candidate.isAssignableFrom(tagClass))
                        {
                            if (debug) LOGGER.info("find-byClass(" + tagClass + ") : " + processor);
                            return processor;
                        }
                    }
                }
            }
        }
        if (debug) LOGGER.info("find-byClass(" + tagClass + ") : " + null);
        return null;
    }

    public IMetadata create(byte[] data) {
        try {
            data = getBytesWithoutHeader(data);
            Constructor<? extends IMetadata> constructor = metadataClass.getConstructor(byte[].class);
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

    public byte[] getBytesWithoutHeader(byte[] data) {
        final int subMarkerLen = (subMarker == null) ? 0 : subMarker.length();
        data = ArrayUtils.subArray(data, subMarkerLen, data.length - subMarkerLen);
        return data;
    }

    @Override public String toString() {
        String metaName = (metadataClass == null) ? null : metadataClass.getSimpleName();
        String segmentMarkerName =  (segmentMarker == null) ? null : segmentMarker.getName();
        return metaName + "[" + segmentMarkerName + "," + subMarker +  "]";
    }

    public JpgSegmentPluginFactory setDebug(boolean newDebugValue) {
        debug = newDebugValue;
        return this;
    }

    public JpegSegmentMarker getSegmentMarker() {
        return segmentMarker;
    }

    public MetadataType getMetadataType() {
        return metadataType;
    }
}
