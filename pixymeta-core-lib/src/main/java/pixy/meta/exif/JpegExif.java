/**
 * Copyright (c) 2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * JpegExif.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    13Mar2015  Initial creation
 */

package pixy.meta.exif;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import pixy.image.exifFields.*;
import pixy.image.exifFields.ExifTag;
import pixy.image.jpeg.JpegSegmentMarker;
import pixy.io.IOUtils;
import pixy.io.MemoryCacheRandomAccessOutputStream;
import pixy.io.RandomAccessOutputStream;
import pixy.io.WriteStrategyMM;

public class JpegExif extends ExifMetaSegment {
	public JpegExif() {
		;
	}
	
	public JpegExif(byte[] data) {
		super(data);
	}

	private IFD createImageIFD() {
		// Create Image IFD (IFD0)
		IFD imageIFD = new IFD();
		ExifField<?> exifField = new ASCIIField(ExifTag.IMAGE_DESCRIPTION, "ExifMetaSegment created by JPEGTweaker");
		imageIFD.addField(exifField);
		String softWare = "JPEGTweaker 1.0";
		exifField = new ASCIIField(ExifTag.SOFTWARE, softWare);
		imageIFD.addField(exifField);
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		exifField = new ASCIIField(pixy.image.exifFields.ExifTag.DATETIME, formatter.format(new Date()));
		imageIFD.addField(exifField);

		return setImageIFD(imageIFD);
	}
	
	/** 
	 * Write the EXIF data to the OutputStream
	 * 
	 * @param os OutputStream
	 * @throws Exception 
	 */
	@Override
	public void write(OutputStream os) throws IOException {
		ensureDataRead();
		// Wraps output stream with a RandomAccessOutputStream
		RandomAccessOutputStream randOS = new MemoryCacheRandomAccessOutputStream(os);
		// Write JPEG the EXIF data
		// Writes JPG_SEGMENT_EXIF_XMP_APP1 marker
		IOUtils.writeShortMM(os, JpegSegmentMarker.JPG_SEGMENT_EXIF_XMP_APP1.getValue());
		// TIFF structure starts here
		short endian = IOUtils.BIG_ENDIAN;
		short tiffID = 0x2a; //'*'
		randOS.setWriteStrategy(WriteStrategyMM.getInstance());
		randOS.writeShort(endian);
		randOS.writeShort(tiffID);
		// First IFD offset relative to TIFF structure
		randOS.seek(0x04);
		randOS.writeInt(FIRST_IFD_OFFSET);
		// Writes IFDs
		randOS.seek(FIRST_IFD_OFFSET);

		IFD imageIFD = getIfd(ID_imageIFD);
		if(imageIFD == null) imageIFD = createImageIFD();

		// Attach EXIIF and/or GPS SubIFD to main image IFD
		IFD exifSubIFD = getIfd(ID_exifSubIFD);
		if(exifSubIFD != null) {
			imageIFD.addField(new LongField(ExifTag.EXIF_SUB_IFD, new int[]{0})); // Place holder
			imageIFD.addChild(ExifTag.EXIF_SUB_IFD, exifSubIFD);
		}

		IFD gpsSubIFD = getIfd(ID_gpsSubIFD);
		if(gpsSubIFD != null) {
			imageIFD.addField(new LongField(ExifTag.GPS_SUB_IFD, new int[]{0})); // Place holder
			imageIFD.addChild(ExifTag.GPS_SUB_IFD, gpsSubIFD);
		}
		int offset = imageIFD.write(randOS, FIRST_IFD_OFFSET);
		if(thumbnail != null && thumbnail.containsImage()) {
			imageIFD.setNextIFDOffset(randOS, offset);
			randOS.seek(offset); // Set the stream pointer to the correct position
			thumbnail.write(randOS);
		}
		// Now it's time to update the segment length
		int length = (int)randOS.getLength();
		// Update segment length
		IOUtils.writeShortMM(os, length + 8);
		// Add EXIF identifier with trailing bytes [0x00,0x00].
		byte[] exif = {0x45, 0x78, 0x69, 0x66, 0x00, 0x00};
		IOUtils.write(os, exif);
		// Dump randOS to normal output stream and we are done!
		randOS.seek(0);
		randOS.writeToStream(length);
		randOS.shallowClose();
	}
}