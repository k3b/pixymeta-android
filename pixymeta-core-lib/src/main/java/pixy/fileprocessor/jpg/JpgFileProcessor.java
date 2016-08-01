package pixy.fileprocessor.jpg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pixy.api.DebuggableBase;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.api.IMetadata;
import pixy.image.jpeg.JpegSegment;
import pixy.image.jpeg.JpegSegmentMarker;
import pixy.image.jpeg.UnknownSegment;
import pixy.io.FileCacheRandomAccessInputStream;
import pixy.io.IOUtils;
import pixy.io.RandomAccessInputStream;
import pixy.meta.MetadataType;
import pixy.meta.adobe.AdobeIRBSegment;
import pixy.meta.adobe.ImageResourceID;
import pixy.meta.adobe.AdobyMetadataBase;
import pixy.meta.iptc.IPTC;

/**
 * Processes one jpg-file to read, copy,replace meta data from/to jpg files.
 *
 * * JPG_SEGMENT_START_OF_IMAGE_SOI
 * * 0..n JpegSegment-s
 * * JpegSegmentMarker.SOS
 * * rest of image (not interpreted)
 *
 * Created by k3b on 07.07.2016.
 */
public class JpgFileProcessor extends DebuggableBase {
    // uninterpreted raw meta data
    private List<JpegSegment> jpegSegments = null;

    /** for interprete on demand: true when data has been parsed */
    private boolean dataParsed = false;

    // interpreted meta data
    private Map<MetadataType, IMetadata> metadataMap = new HashMap<MetadataType, IMetadata>();

    private final InputStream is;

    public JpgFileProcessor(InputStream is) {
        this.is = is;
    }

    public void load() throws IOException {
        this.jpegSegments = onReadSegments(is);
    }

    /**
     * @param os output image stream (or null if no writing should be done)
     * @throws IOException
     */
    public void save(OutputStream os) throws IOException {
        if (os != null) {
            IOUtils.writeShortMM(os, JpegSegmentMarker.JPG_SEGMENT_START_OF_IMAGE_SOI.getValue());

            onWriteSegments(os, this.jpegSegments);
            IOUtils.writeShortMM(os, JpegSegmentMarker.SOS.getValue());

            onCopyImageData(is, os);

        }
        // Close the input stream in case it's an instance of RandomAccessInputStream
        if(is instanceof RandomAccessInputStream) {
            ((FileCacheRandomAccessInputStream) is).shallowClose();
        }
    }

    /** read all Segments from start of file. on return the stream is positioned behind the segments. */
    protected List<JpegSegment> onReadSegments(InputStream is) throws IOException {
        // The very first currentJpegSegmentMarkerCode should be the start_of_image currentJpegSegmentMarkerCode!
        if(JpegSegmentMarker.fromShort(IOUtils.readShortMM(is)) != JpegSegmentMarker.JPG_SEGMENT_START_OF_IMAGE_SOI)	{
            throw new IOException("Invalid JPEG image, expected JPG_SEGMENT_START_OF_IMAGE_SOI currentJpegSegmentMarkerCode not found!");
        }

        // Create a list to hold the temporary Segments
        List<JpegSegment> jpegSegments = new ArrayList<JpegSegment>();

        int segLengthInclMarker = 0;
        short currentJpegSegmentMarkerCode;
        JpegSegmentMarker currentJpegSegmentMarker;

        currentJpegSegmentMarkerCode = IOUtils.readShortMM(is);
        currentJpegSegmentMarker = JpegSegmentMarker.fromShort(currentJpegSegmentMarkerCode);

        while (currentJpegSegmentMarker != JpegSegmentMarker.SOS) { // Read through and add the jpegSegments to a list until SOS
            if (currentJpegSegmentMarker == JpegSegmentMarker.JPG_SEGMENT_PADDING) {
                // padding without prior segment
                int paddingCount = 1; // 2 bytes of current marker minus first 0xff of next marker
                int nextByte = 0;
                while ((nextByte = IOUtils.read(is)) == 0xff) {
                    paddingCount++;
                }
                jpegSegments.add(new JpegSegment(currentJpegSegmentMarker, null).addPadding(paddingCount));

                // last 0xff is first part of next marker
                currentJpegSegmentMarkerCode = (short) ((0xff << 8) | nextByte);

            } else {
                segLengthInclMarker = IOUtils.readUnsignedShortMM(is);

                JpegSegment lastSegment = null;
                if (isSkipSegment(currentJpegSegmentMarker)) {
                    // no more processing
                    IOUtils.skipFully(is, segLengthInclMarker - 2);
                } else {
                    // copy segment to buffer
                    lastSegment = onReadSegment(is, jpegSegments, segLengthInclMarker, currentJpegSegmentMarker);
                }
                currentJpegSegmentMarkerCode = IOUtils.readShortMM(is);

                if (currentJpegSegmentMarkerCode == JpegSegmentMarker.JPG_SEGMENT_PADDING.getValue()) {
                    // padding after segment
                    int paddingCount = 1; // 2 bytes of current marker minus first 0xff of next marker
                    int nextByte = 0;
                    while ((nextByte = IOUtils.read(is)) == 0xff) {
                        paddingCount++;
                    }

                    if (lastSegment != null)
                        lastSegment.addPadding(paddingCount);
                    // else also ignore padding after ignored segment

                    // last 0xff is first part of next marker
                    currentJpegSegmentMarkerCode = (short) ((0xff << 8) | nextByte);
                }
            }
            currentJpegSegmentMarker = JpegSegmentMarker.fromShort(currentJpegSegmentMarkerCode);
        }
        return jpegSegments;
    }

    /**
     * To be overwritten: if segmentes should not be processed and not be copied to outputstream
     *
     * @param currentJpegSegmentMarker
     * @return true if segment should not be read and not be copied to output
     */
    protected boolean isSkipSegment(JpegSegmentMarker currentJpegSegmentMarker) {
        return false;
    }

    /** load one un-interpreted meta data segment from jpg */
    protected JpegSegment onReadSegment(InputStream is, List<JpegSegment> jpegSegments,
                               int segLengthInclMarker,
                               JpegSegmentMarker currentJpegSegmentMarker) throws IOException {
        byte[] buf = new byte[segLengthInclMarker-2];
        IOUtils.readFully(is, buf);
        JpegSegment segment;
        if (currentJpegSegmentMarker == JpegSegmentMarker.JPG_SEGMENT_UNKNOWN) {
            segment = new UnknownSegment(currentJpegSegmentMarker.getValue(), buf);
        } else {
            segment = new JpegSegment(currentJpegSegmentMarker, buf);
        }
        jpegSegments.add(segment);
        return segment;
    }

    protected void interpretAllJpgSegments(List<JpegSegment> jpegSegments) {
        dataParsed = true;
        if (jpegSegments != null) {
            for (JpegSegment segment : jpegSegments) {
                if (segment != null) {
                    interpretJpgSegment(segment);
                }
            }
        }

        AdobeIRBSegment adobeIrbSegment = (AdobeIRBSegment) metadataMap.get(MetadataType.PHOTOSHOP_IRB);
        if ((adobeIrbSegment != null)) {
            AdobyMetadataBase iptc = adobeIrbSegment.get8BIM(ImageResourceID.IPTC_NAA.getValue());

            // Extract IPTC as stand-alone meta
            if (iptc != null) {
                metadataMap.put(MetadataType.IPTC, new IPTC(iptc.getData()));
            }
        }

        IMetadata xmp = metadataMap.get(MetadataType.XMP);
        if ((xmp != null)) {
            xmp.merge(null); // non-null: collect extendedXMP without processing. null: start processing extended xmp if available
        }
    }

    protected void interpretJpgSegment(JpegSegment jpegSegment) {
        final byte[] jpegSegmentData = jpegSegment.getData();
        final JpegSegmentMarker jpegSegmentMarker = jpegSegment.getJpegSegmentMarker();
        JpgSegmentPluginFactory definition = JpgSegmentPluginFactory.find(jpegSegmentMarker, jpegSegmentData);
        if (definition != null) {
            IMetadata meta = metadataMap.get(definition.getMetadataType());

            if (meta == null) {
                meta = definition.create(jpegSegmentData);
                if (meta != null) {
                    metadataMap.put(definition.getMetadataType(), meta);
                }
            } else {
                meta.merge(definition.getBytesWithoutHeader(jpegSegmentData));
            }
        } else  if (isDebugEnabled()) {
            String message = "Unknown jpeg segment: " + jpegSegmentMarker;
            int len = IOUtils.find(jpegSegmentData, (byte) 0, (byte) 31, 0);
            if ((len > 0) && (len < 40)) {
                message += "+" + new String(jpegSegmentData, 0, len);
            }
            debug(message);
        }

    }

    protected void onWriteSegments(OutputStream os, List<JpegSegment> jpegSegments) throws IOException {
        if (jpegSegments != null) {
            for (JpegSegment segment : jpegSegments) {
                if (segment != null) onWriteSegment(os, segment);
            }
        }
    }

    protected void onWriteSegment(OutputStream os, JpegSegment segment) throws IOException {
        segment.write(os);
    }

    protected void onCopyImageData(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[10240]; // 10k buffer
        int bytesRead = -1;

        while((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
    }

    public Map<MetadataType, IMetadata> getMetadataMap() {
        if (!dataParsed) interpretAllJpgSegments(this.jpegSegments);

        return metadataMap;
    }

    public IFieldValue getValue(IFieldDefinition tag) {
        JpgSegmentPluginFactory found = JpgSegmentPluginFactory.find(tag.getClass());
        MetadataType metadataType = (found == null) ? null : found.getMetadataType();
        IMetadata metadata = (metadataType == null) ? null : getMetadataMap().get(metadataType);

        return (metadata == null) ? null : metadata.getValue(tag);
    }
}
