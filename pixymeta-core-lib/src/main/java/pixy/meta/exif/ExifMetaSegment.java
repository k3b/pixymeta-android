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
 * ExifMetaSegment.java
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

import pixy.api.IDataType;
import pixy.api.IDirectory;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.image.IBitmap;
import pixy.meta.MetadataBase;
import pixy.meta.MetadataType;
import pixy.meta.Thumbnail;
import pixy.image.exifFields.FieldType;
import pixy.image.exifFields.IFD;
import pixy.image.exifFields.ExifField;
import pixy.io.FileCacheRandomAccessInputStream;
import pixy.io.FileCacheRandomAccessOutputStream;
import pixy.io.IOUtils;
import pixy.io.RandomAccessInputStream;
import pixy.io.RandomAccessOutputStream;
import pixy.util.ClassUtils;

/**
 * EXIF wrapper
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2014
 */
public abstract class ExifMetaSegment extends MetadataBase {
	protected ExifThumbnail thumbnail;

	private boolean containsThumbnail;
	private boolean isThumbnailRequired;

	public static final int FIRST_IFD_OFFSET = 0x08;

	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(ExifMetaSegment.class);

	public ExifMetaSegment() {
		super(MetadataType.EXIF, null);
		isDataRead = true;
	}

	public ExifMetaSegment(byte[] data) {
		super(MetadataType.EXIF, data);
		ensureDataRead();
	}

	public ExifMetaSegment(IFD imageIFD) {
		this();
		setImageIFD(imageIFD);
	}

	public ExifMetaSegment(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}

	public void addField(IFieldDefinition tag, Object data) {
		addField(tag, tag.getDataType(), data);
	}

	/*
	public IDirectory getDirectory(Class<? extends IFieldDefinition> tagClass, boolean createIfNotFound) {
		final String typName = tagClass.getSimpleName();
		IFD ifd = getOrCreateIfd(typName, createIfNotFound);
		return ifd;
	}
	*/

	public IFieldValue getValue(IFieldDefinition tag) {
		IFD ifd = getOrCreateIfd(tag, false);

		if (ifd != null) return ifd.getValue(tag);
		return null;
	}

	public void addField(IFieldDefinition tag, IDataType type, Object data) {
		IFD ifd = getOrCreateIfd(tag, true);

		ExifField<?> field = FieldType.createField((Tag) tag, (FieldType) type, data);
		if (field != null)
			ifd.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required " + ClassUtils.getSimpleClassName(tag) +
					" field");
	}

	protected static final String ID_gpsSubIFD = GPSTag.class.getSimpleName();
	protected static final String ID_exifSubIFD = ExifSubTag.class.getSimpleName();
	protected static final String ID_exifImageIFD = ExifImageTag.class.getSimpleName();

	HashMap<String, IFD> ifds = new HashMap<String, IFD>();
	private IFD getOrCreateIfd(String typName, boolean createIfNotFound) {
		IFD result = getIfd(typName);

		if (result == null) {
			result = new IFD();
			ifds.put(typName, result);
		}
		return result;
	}

	protected IFD getOrCreateIfd(IFieldDefinition tag, boolean createIfNotFound) {
		if (tag instanceof ExifCompositeTag) return getOrCreateIfd(ID_gpsSubIFD, createIfNotFound);
		final String typName = ClassUtils.getSimpleClassName(tag);
		return getOrCreateIfd(typName, createIfNotFound);
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
		IFD imageIFD = getIfd(ID_exifImageIFD);
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
			RandomAccessInputStream exifIn = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(getData()));
			List<IFD> ifds = new ArrayList<IFD>(3);
			IfdMetaUtils.readIFDs(ifds, exifIn);
			if (ifds.size() > 0) {
				setImageIFD(ifds.get(0));
			}
			// We have thumbnail IFD
			if (ifds.size() >= 2) {
				IFD thumbnailIFD = ifds.get(1);
				int width = -1;
				int height = -1;
				ExifField<?> field = thumbnailIFD.getField(ExifImageTag.IMAGE_WIDTH);
				if (field != null)
					width = field.getDataAsLong()[0];
				field = thumbnailIFD.getField(ExifImageTag.IMAGE_LENGTH);
				if (field != null)
					height = field.getDataAsLong()[0];
				field = thumbnailIFD.getField(ExifImageTag.JPEG_INTERCHANGE_FORMAT);
				if (field != null) { // JPEG format, save as JPEG
					int thumbnailOffset = field.getDataAsLong()[0];
					field = thumbnailIFD.getField(ExifImageTag.JPEG_INTERCHANGE_FORMAT_LENGTH);
					int thumbnailLen = field.getDataAsLong()[0];
					exifIn.seek(thumbnailOffset);
					byte[] thumbnailData = new byte[thumbnailLen];
					exifIn.readFully(thumbnailData);
					thumbnail = new ExifThumbnail(width, height, Thumbnail.DATA_TYPE_KJpegRGB, thumbnailData, thumbnailIFD);
					containsThumbnail = true;
				} else { // Uncompressed TIFF
					field = thumbnailIFD.getField(ExifImageTag.STRIP_OFFSETS);
					if (field == null)
						field = thumbnailIFD.getField(ExifImageTag.TILE_OFFSETS);
					if (field != null) {
						exifIn.seek(0);
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						RandomAccessOutputStream tiffout = new FileCacheRandomAccessOutputStream(bout);
						IfdMetaUtils.retainPages(exifIn, tiffout, 1);
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
		ifds.put(ID_exifImageIFD, imageIFD);

		setGPSIFD(imageIFD.getChild(ExifImageTag.GPS_SUB_IFD));
		setExifIFD(imageIFD.getChild(ExifImageTag.EXIF_SUB_IFD));
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
		LOGGER.info("ExifMetaSegment output starts =>");
		IFD imageIFD = getIfd(ID_exifImageIFD);

		if (imageIFD != null) {
			LOGGER.info("<<Image IFD starts>>");
			IfdMetaUtils.printIFD(imageIFD, ExifImageTag.class, "");
			LOGGER.info("<<Image IFD ends>>");
		}
		if (containsThumbnail) {
			LOGGER.info("ExifMetaSegment thumbnail format: {}", (thumbnail.getDataType() == 1 ? "DATA_TYPE_JPG" : "DATA_TYPE_TIFF"));
			LOGGER.info("ExifMetaSegment thumbnail data length: {}", thumbnail.getCompressedImage().length);
		}
		LOGGER.info("<= ExifMetaSegment output ends");
	}

	public abstract void write(OutputStream os) throws IOException;

	/**
	 * @return directories that belong to this MetaData
	 * */
	@Override
	public List<IDirectory> getMetaData() {
		return getDirectories(new String[]{"", "-sub", "-gps", "-thumb"}, getIfd(ID_exifImageIFD), getIfd(ID_exifSubIFD), getIfd(ID_gpsSubIFD),
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