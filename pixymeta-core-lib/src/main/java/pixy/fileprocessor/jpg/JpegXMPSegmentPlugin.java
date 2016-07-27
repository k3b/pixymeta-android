package pixy.fileprocessor.jpg;

import org.slf4j.LoggerFactory;

import java.util.Arrays;

import pixy.image.jpeg.JpegSegmentMarker;
import pixy.io.IOUtils;
import pixy.meta.MetadataType;
import pixy.meta.jpeg.JpegXMP;
import pixy.meta.xmp.XmpTag;
import pixy.string.XMLUtils;
import pixy.util.ArrayUtils;

/**
 * A jpg-file specific {@link JpegXMP} plugin for {@link JpgFileProcessor}
 * Created by k3b on 09.07.2016.
 */
public class JpegXMPSegmentPlugin extends JpegXMP {
    // Obtain a logger instance
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(JpegXMPSegmentPlugin.class);

    static {
        JpgSegmentPluginFactory.register(MetadataType.XMP, JpegSegmentMarker.JPG_SEGMENT_EXIF_XMP_APP1,
                JpegMetaDef.XMP_ID, JpegXMPSegmentPlugin.class);
        JpgSegmentPluginFactory.register(MetadataType.XMP, JpegSegmentMarker.JPG_SEGMENT_EXIF_XMP_APP1,
                JpegMetaDef.NON_STANDARD_XMP_ID, JpegXMPSegmentPlugin.class);
        JpgSegmentPluginFactory.register(MetadataType.XMP, JpegSegmentMarker.JPG_SEGMENT_EXIF_XMP_APP1,
                JpegMetaDef.XMP_EXT_ID, JpegXMPSegmentPlugin.class);
    }

    /**
     * make shure that static constructor was called
     */
    public static void register() {
    }

    /**
     * Created via reflection by {@link pixy.fileprocessor.jpg.JpgSegmentPluginFactory}
     */
    public JpegXMPSegmentPlugin(byte[] data) {
        super(data);
    }

    private byte[] extendedXMP = null;

    /**
     * data non-null: collect extendedXMP without processing.
     * data null: start processing extended xmp if available
     * @param data
     */
    @Override
    public void merge(byte[] data) {
        if (data != null) {
            // We found ExtendedXMP, add the data to ExtendedXMP memory buffer

            String xmpGUID = XMLUtils.getAttribute(getXmpDocument(), XmpTag.Note_HasExtendedXMP.getXmlElementName(), XmpTag.Note_HasExtendedXMP.getAttribute());

            int i = 0;
            // 128-bit MD5 digest of the full ExtendedXMP serialization
            String guid = new String(data, i, i + 32); //  ArrayUtils.subArray(data, i, 32);
            if (Arrays.equals(guid.getBytes(), xmpGUID.getBytes())) { // We have matched the GUID, copy it
                i += 32;
                long extendedXMPLength = IOUtils.readUnsignedIntMM(data, i);
                i += 4;
                //!!! else assert extendedXMPLength == extendedXMP.size
                // Offset for the current jpegSegment
                long offset = IOUtils.readUnsignedIntMM(data, i);
                i += 4;
                final int curSegmentDataLenght = data.length - i; // -41

                final String mergeContext = offset +
                        ".." + (offset + curSegmentDataLenght) +
                        " > " + extendedXMPLength;
                if (extendedXMP == null) {
                    if (extendedXmpLogging) {
                        LOGGER.info(this.getClass().getSimpleName() + ".merge create extendet: " + mergeContext);
                    }
                    extendedXMP = new byte[(int) extendedXMPLength];
                } else {
                    if (extendedXMPLength != extendedXMP.length) {
                        String dbg = new String(data);
                        LOGGER.error(this.getClass().getSimpleName()
                                + ".merge create append size mismatch: got " + extendedXMPLength +
                                ". expected " + extendedXMP.length +
                                " for " + mergeContext+ "\n" + dbg);
                        return;
                    }
                    if (extendedXmpLogging) {
                        LOGGER.info(this.getClass().getSimpleName() + ".merge append extendet: " + mergeContext);
                    }
                }

                byte[] xmpBytes = ArrayUtils.subArray(data, i, curSegmentDataLenght);
                System.arraycopy(xmpBytes, 0, extendedXMP, (int) offset, xmpBytes.length);
            } else {
                String dbg = new String(data);
                LOGGER.error(this.getClass().getSimpleName()
                        + ".merge ignoring wrong guid: got " + guid +
                        ". expected " + xmpGUID +
                        " for \n" + dbg);
            }
        } else if (extendedXMP != null) {
            // here actual merging happens
            super.merge(extendedXMP);
            extendedXMP = null;
        }
    }

}
