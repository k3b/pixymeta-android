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
 * Metadata.java
 *
 * Who   Date       Description
 * ====  =========  ======================================================
 * WY    26Sep2015  Added insertComment(InputStream, OutputStream, String}
 * WY    06Jul2015  Added insertXMP(InputSream, OutputStream, XMP)
 * WY    16Apr2015  Changed insertIRB() parameter List to Collection
 * WY    16Apr2015  Removed ICC_Profile related code
 * WY    13Mar2015  initial creation
 */

package pixy.meta;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.api.IMetadata;
import pixy.image.IBitmap;
import pixy.image.ImageType;
import pixy.meta.adobe.AdobyMetadataBase;
import pixy.meta.exif.ExifMetaSegment;
import pixy.meta.iptc.IPTCFieldValue;
import pixy.meta.tiff.TIFFMetaUtils;
import pixy.util.MetadataUtils;
import pixy.meta.bmp.BMPMeta;
import pixy.meta.gif.GIFMeta;
import pixy.meta.jpeg.JPEGMeta;
import pixy.meta.png.PNGMeta;
import pixy.meta.xmp.XMP;
import pixy.io.FileCacheRandomAccessInputStream;
import pixy.io.FileCacheRandomAccessOutputStream;
import pixy.io.PeekHeadInputStream;
import pixy.io.RandomAccessInputStream;
import pixy.io.RandomAccessOutputStream;

/**
 * Base class for image metadata.
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/12/2015
 */
public abstract class Metadata extends MetadataBase {
	public static final int IMAGE_MAGIC_NUMBER_LEN = 4;

	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(Metadata.class);		
	
	public static void  extractThumbnails(File image, String pathToThumbnail) throws IOException {
		FileInputStream fin = new FileInputStream(image);
		extractThumbnails(fin, pathToThumbnail);
		fin.close();
	}
	
	public static void extractThumbnails(InputStream is, String pathToThumbnail) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);
		// Delegate thumbnail extracting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGMeta.extractThumbnails(peekHeadInputStream, pathToThumbnail);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				TIFFMetaUtils.extractThumbnail(randIS, pathToThumbnail);
				randIS.shallowClose();
				break;
			case PNG:
				LOGGER.info("PNG image format does not contain any thumbnail");
				break;
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not contain any thumbnails", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("Thumbnail extracting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void extractThumbnails(String image, String pathToThumbnail) throws IOException {
		extractThumbnails(new File(image), pathToThumbnail);
	}
	
	public static void insertComment(InputStream is, OutputStream os, String comment) throws IOException {
		insertComments(is, os, Arrays.asList(comment));
	}
	
	public static void insertComments(InputStream is, OutputStream os, List<String> comments) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate IPTC inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGMeta.insertComments(peekHeadInputStream, os, comments);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(os);
				TIFFMetaUtils.insertComments(comments, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
				PNGMeta.insertComments(peekHeadInputStream, os, comments);
				break;
			case GIF:
				GIFMeta.insertComments(peekHeadInputStream, os, comments);
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support comment data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("comment data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	/**
	 * @param is input image stream 
	 * @param os output image stream
	 * @param exif ExifMetaSegment instance
	 * @throws IOException 
	 */
	public static void insertExif(InputStream is, OutputStream os, ExifMetaSegment exif) throws IOException {
		insertExif(is, os, exif, false);
	}
	
	/**
	 * @param is input image stream 
	 * @param os output image stream
	 * @param exif ExifMetaSegment instance
	 * @param update true to keep the original data, otherwise false
	 * @throws IOException 
	 */
	public static void insertExif(InputStream is, OutputStream os, ExifMetaSegment exif, boolean update) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate EXIF inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGMeta.insertExif(peekHeadInputStream, os, exif, update);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(os);
				TIFFMetaUtils.insertExif(randIS, randOS, exif, update);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case GIF:
			case PCX:
			case TGA:
			case BMP:
			case PNG:
				LOGGER.info("{} image format does not support EXIF data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("EXIF data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertICCProfile(InputStream is, OutputStream out, byte[] icc_profile) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate ICCP inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGMeta.insertICCProfile(peekHeadInputStream, out, icc_profile);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFMetaUtils.insertICCProfile(icc_profile, 0, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support ICCProfile data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("ICCProfile data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}

	public static void insertIPTC(InputStream is, OutputStream out, Collection<IPTCFieldValue> iptcs) throws IOException {
		insertIPTC(is, out, iptcs, false);
	}
	
	public static void insertIPTC(InputStream is, OutputStream out, Collection<IPTCFieldValue> iptcs, boolean update) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate IPTC inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGMeta.insertIPTC(peekHeadInputStream, out, iptcs, update);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFMetaUtils.insertIPTC(randIS, randOS, iptcs, update);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support IPTC data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("IPTC data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertIRB(InputStream is, OutputStream out, Collection<AdobyMetadataBase> bims) throws IOException {
		insertIRB(is, out, bims, false);
	}
	
	public static void insertIRB(InputStream is, OutputStream os, Collection<AdobyMetadataBase> bims, boolean update) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate AdobeIRBSegment inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGMeta.insertIRB(peekHeadInputStream, os, bims, update);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(os);
				TIFFMetaUtils.insertIRB(randIS, randOS, bims, update);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support AdobeIRBSegment data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("AdobeIRBSegment data inserting is not supported for " + imageType + " image");
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertIRBThumbnail(InputStream is, OutputStream out, IBitmap thumbnail) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate AdobeIRBSegment thumbnail inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGMeta.insertIRBThumbnail(peekHeadInputStream, out, thumbnail);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFMetaUtils.insertThumbnail(randIS, randOS, thumbnail);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support AdobeIRBSegment thumbnail", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("AdobeIRBSegment thumbnail inserting is not supported for " + imageType + " image");
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertXMP(InputStream is, OutputStream out, XMP xmp) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate XMP inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGMeta.insertXMP(peekHeadInputStream, out, xmp); // No ExtendedXMP
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFMetaUtils.insertXMP(xmp, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
				PNGMeta.insertXMP(peekHeadInputStream, out, xmp);
				break;
			case GIF:
				GIFMeta.insertXMPApplicationBlock(peekHeadInputStream, out, xmp);
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support XMP data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("XMP inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertXMP(InputStream is, OutputStream out, String xmp) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate XMP inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGMeta.insertXMP(peekHeadInputStream, out, xmp, null); // No ExtendedXMP
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFMetaUtils.insertXMP(xmp, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
				PNGMeta.insertXMP(peekHeadInputStream, out, xmp);
				break;
			case GIF:
				GIFMeta.insertXMPApplicationBlock(peekHeadInputStream, out, xmp);
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support XMP data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("XMP inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static Map<MetadataType, IMetadata> readMetadata(File image) throws IOException {
		FileInputStream fin = new FileInputStream(image);
		Map<MetadataType, IMetadata> metadataMap = readMetadata(fin);
		fin.close();
		
		return metadataMap; 
	}
	
	/**
	 * Reads all metadata associated with the input image
	 *
	 * @param is InputStream for the image
	 * @return a list of Metadata for the input stream
	 * @throws IOException
	 */
	public static Map<MetadataType, IMetadata> readMetadata(InputStream is) throws IOException {
		// Metadata map for all the Metadata read
		Map<MetadataType, IMetadata> metadataMap = new HashMap<MetadataType, IMetadata>();
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate metadata reading to corresponding image tweakers.
		switch(imageType) {
			case JPG:
				metadataMap = JPEGMeta.readMetadata(peekHeadInputStream);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				metadataMap = TIFFMetaUtils.readMetadata(randIS);
				randIS.shallowClose();
				break;
			case PNG:
				metadataMap = PNGMeta.readMetadata(peekHeadInputStream);
				break;
			case GIF:
				metadataMap = GIFMeta.readMetadata(peekHeadInputStream);
				break;
			case BMP:
				metadataMap = BMPMeta.readMetadata(peekHeadInputStream);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("Metadata reading is not supported for " + imageType + " image");
				
		}	
		peekHeadInputStream.shallowClose();
		
		return metadataMap;
	}
	
	public static Map<MetadataType, IMetadata> readMetadata(String image) throws IOException {
		return readMetadata(new File(image));
	}
	
	/**
	 * Remove meta data from image
	 * 
	 * @param is InputStream for the input image
	 * @param os OutputStream for the output image
	 * @throws IOException
	 */
	public static void removeMetadata(InputStream is, OutputStream os, MetadataType ...metadataTypes) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate meta data removing to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGMeta.removeMetadata(peekHeadInputStream, os, metadataTypes);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(os);
				TIFFMetaUtils.removeMetadata(randIS, randOS, metadataTypes);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support meta data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("Metadata removing is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public Metadata(MetadataType type, byte[] data) {
		super(type, data);
	}

}