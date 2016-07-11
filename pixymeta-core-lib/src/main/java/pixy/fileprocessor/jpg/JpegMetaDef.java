package pixy.fileprocessor.jpg;

/**
 * Created by k3b on 11.07.2016.
 */
public class JpegMetaDef {
    // Constants
    public static final String XMP_ID = "http://ns.adobe.com/xap/1.0/\0";
    // This is a non_standard XMP identifier which sometimes found in images from GettyImages
    public static final String NON_STANDARD_XMP_ID = "XMP\0://ns.adobe.com/xap/1.0/\0";
    public static final String XMP_EXT_ID = "http://ns.adobe.com/xmp/extension/\0";
    // Photoshop AdobeIRBSegment identification with trailing byte [0x00].
    public static final String PHOTOSHOP_IRB_ID = "Photoshop 3.0\0";
    // EXIF identifier with trailing bytes [0x00, 0x00].
    public static final String EXIF_ID = "ExifMetaSegment\0\0";
    // Largest size for each extended XMP chunk
    protected static final int MAX_EXTENDED_XMP_CHUNK_SIZE = 65458;
    protected static final int MAX_XMP_CHUNK_SIZE = 65504;
}
