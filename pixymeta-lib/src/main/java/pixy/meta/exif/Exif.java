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
 * Exif.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    10Apr2015  Moved data loaded checking to ExifReader
 * WY    31Mar2015  Fixed bug with getImageIFD() etc
 * WY    13Mar2015  Initial creation
 */

package pixy.meta.exif;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.api.IDirectory;
import pixy.image.IBitmap;
import pixy.image.tiff.Tag;
import pixy.meta.Metadata;
import pixy.meta.MetadataType;
import pixy.meta.Thumbnail;
import pixy.meta.tiff.TIFFMeta;
import pixy.image.tiff.FieldType;
import pixy.image.tiff.IFD;
import pixy.image.tiff.TiffField;
import pixy.image.tiff.TiffTag;
import pixy.io.FileCacheRandomAccessInputStream;
import pixy.io.FileCacheRandomAccessOutputStream;
import pixy.io.IOUtils;
import pixy.io.RandomAccessInputStream;
import pixy.io.RandomAccessOutputStream;

/**
 * EXIF wrapper
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2014
 */
public abstract class Exif extends Metadata {
	protected ExifThumbnail thumbnail;

	private boolean containsThumbnail;
	private boolean isThumbnailRequired;

	public static final int FIRST_IFD_OFFSET = 0x08;

	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(Exif.class);

	public Exif() {
		super(MetadataType.EXIF, null);
		isDataRead = true;
	}

	public Exif(byte[] data) {
		super(MetadataType.EXIF, data);
		ensureDataRead();
	}

	public Exif(IFD imageIFD) {
		this();
		setImageIFD(imageIFD);
	}

	public Exif(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}

	public void addField(Tag tag, FieldType type, Object data) {
		final String typName = tag.getClass().getSimpleName();
		IFD ifd = getOrCreateIfd(typName);

		TiffField<?> field = FieldType.createField(tag, type, data);
		if (field != null)
			ifd.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required " + typName +
					" field");
	}

	protected static final String ID_gpsSubIFD = GPSTag.class.getSimpleName();
	protected static final String ID_exifSubIFD = ExifTag.class.getSimpleName();
	protected static final String ID_imageIFD = TiffTag.class.getSimpleName();

	HashMap<String, IFD> ifds = new HashMap<String, IFD>();
	private IFD getOrCreateIfd(String typName) {
		IFD result = getIfd(typName);

		if (result == null) {
			result = new IFD();
			ifds.put(typName, result);
		}
		return result;
	}

	protected IFD getIfd(String typName) {
		return ifds.get(typName);
	}

	public boolean containsThumbnail() {
		if (containsThumbnail)
			return true;
		if (thumbnail != null)
			return true;
		return false;
	}

	public IFD getExifIFD() {
		IFD exifSubIFD = getIfd(ID_exifSubIFD);

		if (exifSubIFD != null) {
			return new IFD(exifSubIFD);
		}

		return null;
	}

	public IFD getGPSIFD() {
		IFD gpsSubIFD = getIfd(ID_gpsSubIFD);
		if (gpsSubIFD != null) {
			return new IFD(gpsSubIFD);
		}

		return null;
	}

	public IFD getImageIFD() {
		IFD imageIFD = getIfd(ID_imageIFD);
		if (imageIFD != null) {
			return new IFD(imageIFD);
		}

		return null;
	}

	public ExifThumbnail getThumbnail() {
		if (thumbnail != null)
			return new ExifThumbnail(thumbnail);

		return null;
	}

	public boolean isThumbnailRequired() {
		return isThumbnailRequired;
	}

	public void read() throws IOException {
		if (!isDataRead) {
			RandomAccessInputStream exifIn = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(data));
			List<IFD> ifds = new ArrayList<IFD>(3);
			TIFFMeta.readIFDs(ifds, exifIn);
			if (ifds.size() > 0) {
				setImageIFD(ifds.get(0));
			}
			// We have thumbnail IFD
			if (ifds.size() >= 2) {
				IFD thumbnailIFD = ifds.get(1);
				int width = -1;
				int height = -1;
				TiffField<?> field = thumbnailIFD.getField(TiffTag.IMAGE_WIDTH);
				if (field != null)
					width = field.getDataAsLong()[0];
				field = thumbnailIFD.getField(TiffTag.IMAGE_LENGTH);
				if (field != null)
					height = field.getDataAsLong()[0];
				field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT);
				if (field != null) { // JPEG format, save as JPEG
					int thumbnailOffset = field.getDataAsLong()[0];
					field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH);
					int thumbnailLen = field.getDataAsLong()[0];
					exifIn.seek(thumbnailOffset);
					byte[] thumbnailData = new byte[thumbnailLen];
					exifIn.readFully(thumbnailData);
					thumbnail = new ExifThumbnail(width, height, Thumbnail.DATA_TYPE_KJpegRGB, thumbnailData, thumbnailIFD);
					containsThumbnail = true;
				} else { // Uncompressed TIFF
					field = thumbnailIFD.getField(TiffTag.STRIP_OFFSETS);
					if (field == null)
						field = thumbnailIFD.getField(TiffTag.TILE_OFFSETS);
					if (field != null) {
						exifIn.seek(0);
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						RandomAccessOutputStream tiffout = new FileCacheRandomAccessOutputStream(bout);
						TIFFMeta.retainPages(exifIn, tiffout, 1);
						tiffout.close(); // Auto flush when closed
						thumbnail = new ExifThumbnail(width, height, Thumbnail.DATA_TYPE_TIFF, bout.toByteArray(), thumbnailIFD);
						containsThumbnail = true;
					}
				}
			}
			exifIn.shallowClose();
			isDataRead = true;
		}
	}

	public void setExifIFD(IFD exifSubIFD) {
		ifds.put(ID_exifSubIFD, exifSubIFD);
	}

	public void setGPSIFD(IFD gpsSubIFD) {
		ifds.put(ID_gpsSubIFD, gpsSubIFD);
	}

	public IFD setImageIFD(IFD imageIFD) {
		if (imageIFD == null)
			throw new IllegalArgumentException("Input image IFD is null");
		ifds.put(ID_imageIFD, imageIFD);

		setGPSIFD(imageIFD.getChild(TiffTag.GPS_SUB_IFD));
		setExifIFD(imageIFD.getChild(TiffTag.EXIF_SUB_IFD));
		return imageIFD;
	}

	/**
	 * @param thumbnail a Thumbnail instance. If null, a thumbnail
	 *                  will be generated from the input image.
	 */
	public void setThumbnail(ExifThumbnail thumbnail) {
		this.thumbnail = thumbnail;
	}

	public void setThumbnailImage(IBitmap thumbnail) {
		if (this.thumbnail == null)
			this.thumbnail = new ExifThumbnail();
		this.thumbnail.setImage(thumbnail);
	}

	public void setThumbnailRequired(boolean isThumbnailRequired) {
		this.isThumbnailRequired = isThumbnailRequired;
	}

	@Override
	public void showMetadata() {
		ensureDataRead();
		LOGGER.info("Exif output starts =>");
		IFD imageIFD = getIfd(ID_imageIFD);

		if (imageIFD != null) {
			LOGGER.info("<<Image IFD starts>>");
			TIFFMeta.printIFD(imageIFD, TiffTag.class, "");
			LOGGER.info("<<Image IFD ends>>");
		}
		if (containsThumbnail) {
			LOGGER.info("Exif thumbnail format: {}", (thumbnail.getDataType() == 1 ? "DATA_TYPE_JPG" : "DATA_TYPE_TIFF"));
			LOGGER.info("Exif thumbnail data length: {}", thumbnail.getCompressedImage().length);
		}
		LOGGER.info("<= Exif output ends");
	}

	public abstract void write(OutputStream os) throws IOException;

	/**
	 * @return directories that belong to this MetaData
	 * */
	@Override
	public List<IDirectory> getMetaData() {
		return getDirectories(new String[]{"", "-sub", "-gps", "-thumb"}, getIfd(ID_imageIFD), getIfd(ID_exifSubIFD), getIfd(ID_gpsSubIFD),
				(thumbnail != null) ? thumbnail.getMetaData() : null);
	}


	protected List<IDirectory> getDirectories(String[] names,  IDirectory... directories) {
		ArrayList<IDirectory> result = new ArrayList<IDirectory>();
		for (int i= 0; i < directories.length; i++) {
			IDirectory dir = directories[i];
			if (dir != null) {
				dir.setName(getClass().getSimpleName()+names[i]);
				result.add(dir);
			}
		}
		if (result.size() > 0) return result;
		return null;
	}


}