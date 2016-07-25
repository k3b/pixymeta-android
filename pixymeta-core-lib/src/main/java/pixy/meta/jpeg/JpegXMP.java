package pixy.meta.jpeg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pixy.fileprocessor.jpg.JpegMetaDef;
import pixy.image.jpeg.JpegSegmentMarker;
import pixy.io.IOUtils;
import pixy.meta.MetadataType;
import pixy.meta.xmp.XMP;
import pixy.string.StringUtils;
import pixy.string.XMLUtils;
import pixy.util.ArrayUtils;

public class JpegXMP extends XMP {

	// Largest size for each extended XMP chunk
	private static final int MAX_EXTENDED_XMP_CHUNK_SIZE = 65458;
	private static final int MAX_XMP_CHUNK_SIZE = 65504;
	private static final int GUID_LEN = 32;

	public JpegXMP(byte[] data) {
		super(data);
	}
	
	public JpegXMP(String xmp) {
		super(xmp);
	}
	
	/**
	 * @param xmp XML string for the XMP - Assuming in UTF-8 format.
	 * @param extendedXmp XML string for the extended XMP - Assuming in UTF-8 format
	 */
	public JpegXMP(String xmp, String extendedXmp) {
		super(xmp, extendedXmp);
	}

	/* !!! todo must implement merge
	@Override
	public void merge(byte[] data) {
		// this.data = ArrayUtils.concat(this.data, data);
		// if ((data != null) && isDebugEnabled()) debug("merge(" + data.length + ") => " + this.data.length);

		// 32 byte ASCII hex string
		// Retrieve and remove XMP GUID if available
		String xmpGUID = XMLUtils.getAttribute(this.getXmpDocument(), "rdf:Description", "xmpNote:HasExtendedXMP");

		// We found ExtendedXMP, add the data to ExtendedXMP memory buffer
		int i = 0;
		// 128-bit MD5 digest of the full ExtendedXMP serialization
		byte[] guid = ArrayUtils.subArray(data, i, 32);
		if(Arrays.equals(guid, xmpGUID.getBytes())) { // We have matched the GUID, copy it
			i += 32;
			long extendedXMPLength = IOUtils.readUnsignedIntMM(data, i);
			i += 4;
			if(extendedXMP == null)
				extendedXMP = new byte[(int)extendedXMPLength];
			// Offset for the current jpegSegment
			long offset = IOUtils.readUnsignedIntMM(data, i);
			i += 4;
			byte[] xmpBytes = ArrayUtils.subArray(data, i, length - XMP_EXT_ID.length() - 42);
			System.arraycopy(xmpBytes, 0, extendedXMP, (int)offset, xmpBytes.length);
		}

		this.data = ArrayUtils.concat(this.data, data);
		this.isDataRead = false;

		if ((data != null) && isDebugEnabled()) debug("merge(" + data.length + ") => " + this.data.length);
	}
*/

	@Override
	public void write(OutputStream os) throws IOException {
		// Add packet wrapper to the XMP document
		// Add PI at the beginning and end of the document, we will support only UTF-8, no BOM
		Document xmpDoc = getXmpDocument();
		XMLUtils.insertLeadingPI(xmpDoc, "xpacket", "begin='' id='W5M0MpCehiHzreSzNTczkc9d'");
		XMLUtils.insertTrailingPI(xmpDoc, "xpacket", "end='r'");

		// TODO generate extended if size to big
		byte[] extendedXmp = null;
		String guid = null;
		if(extendedXmp != null) { // We have ExtendedXMP
			guid = StringUtils.generateMD5(extendedXmp);
			NodeList descriptions = xmpDoc.getElementsByTagName("rdf:Description");
			int length = descriptions.getLength();
			if(length > 0) {
				Element node = (Element)descriptions.item(length - 1);
				node.setAttribute("xmlns:xmpNote", "http://ns.adobe.com/xmp/extension/");
				node.setAttribute("xmpNote:HasExtendedXMP", guid);
			}
		}
		// Serialize XMP to byte array
		byte[] xmp = XMLUtils.serializeToByteArray(xmpDoc);
		if(xmp.length > MAX_XMP_CHUNK_SIZE)
			throw new RuntimeException("XMP data size exceededs JPEG segment size");
		// Write XMP segment
		IOUtils.writeShortMM(os, JpegSegmentMarker.JPG_SEGMENT_EXIF_XMP_APP1.getValue());
		// Write segment length
		IOUtils.writeShortMM(os, JpegMetaDef.XMP_ID.length() + 2 + xmp.length);
		// Write segment data
		os.write(JpegMetaDef.XMP_ID.getBytes());
		os.write(xmp);
		// Write ExtendedXMP if we have
		if(extendedXmp != null) {
			int numOfChunks = extendedXmp.length / MAX_EXTENDED_XMP_CHUNK_SIZE;
			int extendedXmpLen = extendedXmp.length;
			int offset = 0;
			
			for(int i = 0; i < numOfChunks; i++) {
				IOUtils.writeShortMM(os, JpegSegmentMarker.JPG_SEGMENT_EXIF_XMP_APP1.getValue());
				// Write segment length
				IOUtils.writeShortMM(os, 2 + JpegMetaDef.XMP_EXT_ID.length() + GUID_LEN + 4 + 4 + MAX_EXTENDED_XMP_CHUNK_SIZE);
				// Write segment data
				os.write(JpegMetaDef.XMP_EXT_ID.getBytes());
				os.write(guid.getBytes());
				IOUtils.writeIntMM(os, extendedXmpLen);
				IOUtils.writeIntMM(os, offset);
				os.write(ArrayUtils.subArray(extendedXmp, offset, MAX_EXTENDED_XMP_CHUNK_SIZE));
				offset += MAX_EXTENDED_XMP_CHUNK_SIZE;			
			}
			
			int leftOver = extendedXmp.length % MAX_EXTENDED_XMP_CHUNK_SIZE;
			
			if(leftOver != 0) {
				IOUtils.writeShortMM(os, JpegSegmentMarker.JPG_SEGMENT_EXIF_XMP_APP1.getValue());
				// Write segment length
				IOUtils.writeShortMM(os, 2 + JpegMetaDef.XMP_EXT_ID.length() + GUID_LEN + 4 + 4 + leftOver);
				// Write segment data
				os.write(JpegMetaDef.XMP_EXT_ID.getBytes());
				os.write(guid.getBytes());
				IOUtils.writeIntMM(os, extendedXmpLen);
				IOUtils.writeIntMM(os, offset);
				os.write(ArrayUtils.subArray(extendedXmp, offset, leftOver));
			}
		}
	}
}