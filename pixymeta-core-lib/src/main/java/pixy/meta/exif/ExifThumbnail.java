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
 * ExifThumbnail.java
 *
 * Who   Date         Description
 * ====  ==========   ===============================================
 * WY    27Apr2015    Added copy constructor
 * WY    10Apr2015    Added new constructor, changed write()
 * WY    13Mar2015    initial creation
 */

package pixy.meta.exif;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import pixy.api.IDirectory;
import pixy.image.IBitmap;
import pixy.image.exifFields.*;
import pixy.meta.Thumbnail;
import pixy.io.FileCacheRandomAccessInputStream;
import pixy.io.MemoryCacheRandomAccessOutputStream;
import pixy.io.RandomAccessInputStream;
import pixy.io.RandomAccessOutputStream;

/**
 * Encapsulates image EXIF thumbnail metadata
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2015
 */
public class ExifThumbnail extends Thumbnail {
	// Comprised of an IFD and an associated image
	// Create thumbnail IFD (IFD1 in the case of JPEG EXIF segment)
	private IFD thumbnailIFD = new IFD();
		
	public ExifThumbnail() { }
	
	public ExifThumbnail(IBitmap thumbnail) {
		super(thumbnail);
	}
	
	public ExifThumbnail(ExifThumbnail other) { // Copy constructor
		this.dataType = other.dataType;
		this.height = other.height;
		this.width = other.width;
		this.thumbnail = other.thumbnail;
		this.compressedThumbnail = other.compressedThumbnail;
		this.thumbnailIFD = other.thumbnailIFD;
	}
	
	public ExifThumbnail(int width, int height, int dataType, byte[] compressedThumbnail) {
		super(width, height, dataType, compressedThumbnail);
	}
	
	public ExifThumbnail(int width, int height, int dataType, byte[] compressedThumbnail, IFD thumbnailIFD) {
		super(width, height, dataType, compressedThumbnail);
		this.thumbnailIFD = thumbnailIFD;
	}
	
	public void write(OutputStream os) throws IOException {
		RandomAccessOutputStream randOS = null;
		if(os instanceof RandomAccessOutputStream) randOS = (RandomAccessOutputStream)os;
		else randOS = new MemoryCacheRandomAccessOutputStream(os);
		int offset = (int)randOS.getStreamPointer(); // Get current write position
		if(getDataType() == Thumbnail.DATA_TYPE_KJpegRGB) { // Compressed old-style JPEG format
			byte[] compressedImage = getCompressedImage();
			if(compressedImage == null) throw new IllegalArgumentException("Expected compressed thumbnail data does not exist!");
			thumbnailIFD.addField(new LongField(ExifImageTag.JPEG_INTERCHANGE_FORMAT, new int[] {0})); // Placeholder
			thumbnailIFD.addField(new LongField(ExifImageTag.JPEG_INTERCHANGE_FORMAT_LENGTH, new int[] {compressedImage.length}));
			offset = thumbnailIFD.write(randOS, offset);
			// This line is very important!!!
			randOS.seek(offset);
			randOS.write(getCompressedImage());
			// Update fields
			randOS.seek(thumbnailIFD.getField(ExifImageTag.JPEG_INTERCHANGE_FORMAT).getDataOffset());
			randOS.writeInt(offset);
		} else if(getDataType() == Thumbnail.DATA_TYPE_TIFF) { // Uncompressed TIFF format
			// Read the IFDs into a list first
			List<IFD> list = new ArrayList<IFD>();			   
			RandomAccessInputStream tiffIn = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(getCompressedImage()));
			IfdMetaUtils.readIFDs(list, tiffIn);
			ExifField<?> stripOffset = list.get(0).getField(ExifImageTag.STRIP_OFFSETS);
    		if(stripOffset == null) 
    			stripOffset = list.get(0).getField(ExifImageTag.TILE_OFFSETS);
    		ExifField<?> stripByteCounts = list.get(0).getField(ExifImageTag.STRIP_BYTE_COUNTS);
    		if(stripByteCounts == null) 
    			stripByteCounts = list.get(0).getField(ExifImageTag.TILE_BYTE_COUNTS);
    		offset = list.get(0).write(randOS, offset); // Write out the thumbnail IFD
    		int[] off = new int[0];;
    		if(stripOffset != null) { // Write out image data and update offset array
    			off = stripOffset.getDataAsLong();
    			int[] counts = stripByteCounts.getDataAsLong();
    			for(int i = 0; i < off.length; i++) {
    				tiffIn.seek(off[i]);
    				byte[] temp = new byte[counts[i]];
    				tiffIn.readFully(temp);
    				randOS.seek(offset);
    				randOS.write(temp);
    				off[i] = offset;
    				offset += counts[i];    				
    			}
    		}
    		tiffIn.shallowClose();
    		// Update offset field
			randOS.seek(stripOffset.getDataOffset());
			for(int i : off)
				randOS.writeInt(i);		
		} else {
			IBitmap thumbnail = getRawImage();
			if(thumbnail == null) throw new IllegalArgumentException("Expected raw data thumbnail does not exist!");
			// We are going to write the IFD and associated thumbnail
			int thumbnailWidth = thumbnail.getWidth();
			int thumbnailHeight = thumbnail.getHeight();
			thumbnailIFD.addField(new ShortField(ExifImageTag.IMAGE_WIDTH, new short[]{(short)thumbnailWidth}));
			thumbnailIFD.addField(new ShortField(ExifImageTag.IMAGE_LENGTH, new short[]{(short)thumbnailHeight}));
			thumbnailIFD.addField(new LongField(ExifImageTag.JPEG_INTERCHANGE_FORMAT, new int[]{0})); // Place holder
			thumbnailIFD.addField(new LongField(ExifImageTag.JPEG_INTERCHANGE_FORMAT_LENGTH, new int[]{0})); // Place holder
			// Other related tags
			thumbnailIFD.addField(new RationalField(ExifImageTag.X_RESOLUTION, new int[] {thumbnailWidth, 1}));
			thumbnailIFD.addField(new RationalField(ExifImageTag.Y_RESOLUTION, new int[] {thumbnailHeight, 1}));
			thumbnailIFD.addField(new ShortField(ExifImageTag.RESOLUTION_UNIT, new short[]{1})); //No absolute unit of measurement
			thumbnailIFD.addField(new ShortField(ExifImageTag.PHOTOMETRIC_INTERPRETATION, new short[]{(short) ExifFieldEnum.PhotoMetric.YCbCr.getValue()}));
			thumbnailIFD.addField(new ShortField(ExifImageTag.SAMPLES_PER_PIXEL, new short[]{3}));
			thumbnailIFD.addField(new ShortField(ExifImageTag.BITS_PER_SAMPLE, new short[]{8, 8, 8}));
			thumbnailIFD.addField(new ShortField(ExifImageTag.YCbCr_SUB_SAMPLING, new short[]{1, 1}));
			thumbnailIFD.addField(new ShortField(ExifImageTag.PLANAR_CONFIGURATTION, new short[]{(short) ExifFieldEnum.PlanarConfiguration.CONTIGUOUS.getValue()}));
			thumbnailIFD.addField(new ShortField(ExifImageTag.COMPRESSION, new short[]{(short) ExifFieldEnum.Compression.OLD_JPG.getValue()}));
			thumbnailIFD.addField(new ShortField(ExifImageTag.ROWS_PER_STRIP, new short[]{(short)thumbnailHeight}));
			// Write the thumbnail IFD
			// This line is very important!!!
			randOS.seek(thumbnailIFD.write(randOS, offset));
			// This is amazing. We can actually keep track of how many bytes have been written to
			// the underlying stream
			long startOffset = randOS.getStreamPointer();
			try {
				thumbnail.compressJPG(writeQuality, randOS);
			} catch (Exception e) {
				throw new RuntimeException("Unable to compress thumbnail as JPEG");
			}
			long finishOffset = randOS.getStreamPointer();			
			int totalOut = (int)(finishOffset - startOffset);
			// Update fields
			randOS.seek(thumbnailIFD.getField(ExifImageTag.JPEG_INTERCHANGE_FORMAT).getDataOffset());
			randOS.writeInt((int)startOffset);
			randOS.seek(thumbnailIFD.getField(ExifImageTag.JPEG_INTERCHANGE_FORMAT_LENGTH).getDataOffset());
			randOS.writeInt(totalOut);
		}
		// Close the RandomAccessOutputStream instance if we created it locally
		if(!(os instanceof RandomAccessOutputStream)) randOS.shallowClose();
	}

	public IDirectory getMetaData() {
		return (thumbnailIFD != null) ? thumbnailIFD : null;
	}
}